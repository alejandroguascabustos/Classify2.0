package com.classify20.service;

import com.classify20.dao.PermisosDao;
import com.classify20.domain.Modulo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Control de acceso por rol y por usuario (RBAC) para los módulos de Classify.
 *
 * <p>Modelo, alineado con la práctica del mercado (roles → permisos, con
 * overrides por usuario que prevalecen):</p>
 * <ul>
 *   <li><b>rol_permiso</b>: permiso base que cada rol hereda por módulo.</li>
 *   <li><b>usuario_permiso</b>: excepción explícita por usuario (conceder o
 *       revocar) que gana sobre el permiso del rol.</li>
 * </ul>
 *
 * <p>Reglas invariantes de seguridad:</p>
 * <ul>
 *   <li>El módulo <b>menu</b> es siempre accesible (evita dejar al usuario sin navegación).</li>
 *   <li>El administrador siempre tiene acceso a todo (no puede autobloquearse).</li>
 *   <li>La <b>gestión de permisos</b> es exclusiva del administrador y no es editable.</li>
 * </ul>
 */
@Service
public class PermisosService {

    private static final Logger logger = LoggerFactory.getLogger(PermisosService.class);

    public static final String ROL_ADMIN = "administrador";
    public static final String CLAVE_MENU = "menu";
    public static final String CLAVE_PERMISOS = "gestion-permisos";

    /** Roles del sistema (derivados de registro_usuarios.tipo_usuario). */
    public static final List<String> ROLES = List.of(
            "administrador", "coordinador", "docente", "acudiente", "estudiante");

    /** Catálogo de módulos administrables con sus valores por defecto. */
    private static final List<Modulo> CATALOGO = List.of(
            new Modulo(0, CLAVE_MENU, "Inicio", "/menu", "home.png", 1, true, true),
            new Modulo(0, "agenda", "Agendar clase", "/agenda", "agenda.png", 2, true, false),
            new Modulo(0, "noticias", "Noticias", "/noticias", "noticias.png", 3, true, false),
            new Modulo(0, "programacion", "Programación", "/programacion", "programacion.png", 4, true, false),
            new Modulo(0, "gestion-registros", "Gestión de registros", "/gestion-registros", "gestioncuenta.png", 5, true, false),
            new Modulo(0, "aprende", "Aprende", "/aprende", "aprende.png", 6, true, false),
            new Modulo(0, "contacta", "Contacta a un profe", "/contacta", "gmail.png", 7, true, false),
            new Modulo(0, "mismateriales", "Mis Materiales", "/mismateriales", "descargamaterial.png", 8, true, false),
            new Modulo(0, "materiales", "Carga Materiales", "/materiales", "upload.png", 9, true, false),
            new Modulo(0, CLAVE_PERMISOS, "Gestión de permisos", "/gestion-permisos", "gestioncuenta.png", 10, true, true));

    private final PermisosDao dao;

    // ── Caché en memoria (se recarga tras cada escritura) ────────────────
    private volatile List<Modulo> modulos = List.of();
    private volatile Map<String, Set<Long>> permisosPorRol = Map.of();
    private volatile Map<Long, Map<Long, Boolean>> overridesPorUsuario = Map.of();

    public PermisosService(PermisosDao dao) {
        this.dao = dao;
    }

    @PostConstruct
    void iniciar() {
        try {
            sembrar();
            recargarCache();
        } catch (RuntimeException e) {
            logger.error("No fue posible inicializar el módulo de permisos.", e);
        }
    }

    // ── Seed idempotente del catálogo y de los permisos por defecto ──────
    private void sembrar() {
        for (Modulo m : CATALOGO) {
            if (!dao.existeModulo(m.clave())) {
                dao.insertarModulo(m);
            }
        }
        List<Modulo> existentes = dao.listarModulos();
        for (Modulo m : existentes) {
            for (String rol : ROLES) {
                // Solo se siembra si aún no existe: no piso ediciones del administrador.
                if (!dao.existePermisoRol(rol, m.id())) {
                    dao.upsertPermisoRol(rol, m.id(), permitidoPorDefecto(rol, m.clave()));
                }
            }
        }
    }

    /** Permiso por defecto que reproduce el comportamiento actual de la app. */
    private boolean permitidoPorDefecto(String rol, String clave) {
        if (ROL_ADMIN.equals(rol)) {
            return true; // el administrador ve todo
        }
        return switch (clave) {
            case "gestion-registros" -> "coordinador".equals(rol);
            case CLAVE_PERMISOS -> false; // exclusivo del administrador
            default -> true;              // el resto de módulos estaba abierto a todos
        };
    }

    private synchronized void recargarCache() {
        this.modulos = dao.listarModulos();
        this.permisosPorRol = dao.permisosPorRol();
        this.overridesPorUsuario = dao.overridesPorUsuario();
    }

    // ── Lecturas para la UI ──────────────────────────────────────────────

    public List<Modulo> getModulos() {
        return modulos;
    }

    public List<String> getRoles() {
        return ROLES;
    }

    /** ¿El rol tiene permitido el módulo por defecto (permiso base, sin overrides)? */
    public boolean rolPermite(String rol, long moduloId) {
        return permisosPorRol.getOrDefault(rol, Set.of()).contains(moduloId);
    }

    /** Override explícito del usuario (moduloId → permitido). Vacío si hereda todo. */
    public Map<Long, Boolean> overridesDeUsuario(long usuarioId) {
        return overridesPorUsuario.getOrDefault(usuarioId, Map.of());
    }

    public List<Map<String, Object>> listarUsuarios() {
        return dao.listarUsuarios();
    }

    public Map<String, Object> buscarUsuario(long usuarioId) {
        return dao.buscarUsuario(usuarioId);
    }

    public Modulo moduloPorClave(String clave) {
        for (Modulo m : modulos) {
            if (m.clave().equals(clave)) {
                return m;
            }
        }
        return null;
    }

    // ── Escrituras (desde el módulo del administrador) ───────────────────

    /**
     * Guarda la matriz rol×módulo. {@code permitidos} contiene las claves
     * "rol|moduloId" marcadas. Los módulos protegidos no se tocan.
     */
    public void guardarMatrizRoles(Set<String> permitidos) {
        for (Modulo m : modulos) {
            if (m.protegido()) {
                continue; // menu y gestion-permisos no son editables
            }
            for (String rol : ROLES) {
                if (ROL_ADMIN.equals(rol)) {
                    continue; // el admin siempre tiene todo
                }
                boolean permitido = permitidos.contains(rol + "|" + m.id());
                dao.upsertPermisoRol(rol, m.id(), permitido);
            }
        }
        recargarCache();
    }

    /**
     * Aplica el estado de override para un usuario y módulo.
     *
     * @param estado "heredar" (borra el override), "permitir" o "bloquear".
     */
    public void guardarOverrideUsuario(long usuarioId, long moduloId, String estado) {
        Modulo m = moduloPorId(moduloId);
        if (m == null || m.protegido()) {
            return; // no se permiten overrides sobre módulos protegidos
        }
        switch (estado == null ? "" : estado) {
            case "permitir" -> dao.upsertOverrideUsuario(usuarioId, moduloId, true);
            case "bloquear" -> dao.upsertOverrideUsuario(usuarioId, moduloId, false);
            default -> dao.eliminarOverrideUsuario(usuarioId, moduloId);
        }
        recargarCache();
    }

    // ── Resolución de acceso efectivo (usada por el interceptor y el menú) ─

    /** ¿El usuario con ese rol puede entrar al módulo indicado por su clave? */
    public boolean puedeAcceder(long usuarioId, String rol, String clave) {
        Modulo m = moduloPorClave(clave);
        if (m == null) {
            return true; // ruta no administrada: no la bloqueamos aquí
        }
        return puedeAcceder(usuarioId, rol, m);
    }

    private boolean puedeAcceder(long usuarioId, String rolCrudo, Modulo m) {
        String rol = normalizar(rolCrudo);
        if (CLAVE_MENU.equals(m.clave())) {
            return true; // el menú siempre es accesible
        }
        if (CLAVE_PERMISOS.equals(m.clave())) {
            return ROL_ADMIN.equals(rol); // exclusivo del administrador
        }
        if (ROL_ADMIN.equals(rol)) {
            return true; // superadmin
        }
        Boolean override = overridesPorUsuario.getOrDefault(usuarioId, Map.of()).get(m.id());
        if (override != null) {
            return override; // el override del usuario prevalece
        }
        return rolPermite(rol, m.id());
    }

    /** Claves de los módulos de menú visibles para el usuario (para el sidebar). */
    public Set<String> modulosPermitidos(long usuarioId, String rol) {
        Set<String> permitidas = new LinkedHashSet<>();
        for (Modulo m : modulos) {
            if (m.enMenu() && puedeAcceder(usuarioId, rol, m)) {
                permitidas.add(m.clave());
            }
        }
        return permitidas;
    }

    /** Mapa clave→ruta de los módulos administrados (para mapear URIs en el interceptor). */
    public Map<String, String> rutasPorClave() {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (Modulo m : modulos) {
            mapa.put(m.clave(), m.ruta());
        }
        return mapa;
    }

    /** Devuelve la clave del módulo cuya ruta corresponde a la URI, o null. */
    public String claveDeRuta(String uri) {
        if (uri == null) {
            return null;
        }
        for (Modulo m : modulos) {
            String ruta = m.ruta();
            if (uri.equals(ruta) || uri.startsWith(ruta + "/")) {
                return m.clave();
            }
        }
        return null;
    }

    private Modulo moduloPorId(long id) {
        for (Modulo m : modulos) {
            if (m.id() == id) {
                return m;
            }
        }
        return null;
    }

    private String normalizar(String rol) {
        return rol == null ? "" : rol.trim().toLowerCase(Locale.ROOT);
    }
}
