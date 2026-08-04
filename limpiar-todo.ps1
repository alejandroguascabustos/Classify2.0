# ============================================================
#  Classify 2.0 - Eliminacion de codigo no usado (Windows)
#
#  USO (PowerShell, desde la raiz del proyecto):
#     .\limpiar-todo.ps1
#
#  Ya estas en la rama 'limpieza-huerfanos'.
#
#  NOTA: no se elimina nada de Python porque NO EXISTE codigo
#  Python en el repo. El codigo ajeno es PHP (CodeIgniter 4).
# ============================================================

$ErrorActionPreference = "Stop"
$borrados = 0

function Remove-Archivo($ruta) {
    if (Test-Path -LiteralPath $ruta) {
        Remove-Item -LiteralPath $ruta -Force -Recurse
        Write-Host "  borrado: $ruta" -ForegroundColor DarkGray
        $script:borrados++
    }
}

# --- Verificacion de ubicacion ---
if (-not (Test-Path "pom.xml")) {
    Write-Host "ERROR: ejecuta este script desde la raiz del proyecto (donde esta pom.xml)" -ForegroundColor Red
    exit 1
}

$rama = (git rev-parse --abbrev-ref HEAD)
Write-Host "Rama actual: $rama" -ForegroundColor Cyan
if ($rama -eq "main") {
    Write-Host "ADVERTENCIA: estas en main. Se recomienda una rama aparte." -ForegroundColor Yellow
    $r = Read-Host "Continuar? (s/N)"
    if ($r -ne "s") { exit 1 }
}
Write-Host ""

# ============================================================
Write-Host "1. PLANTILLAS PHP (CodeIgniter 4)" -ForegroundColor Green
# ============================================================
# Spring Boot nunca usa estas vistas. Contienen <?php y <?=
Remove-Archivo "src\main\resources\templates\errors"
# header.html contiene UNICAMENTE la linea "<?php"
# chatbot.html usa <?= base_url() ?> (funcion de CodeIgniter)
Remove-Archivo "src\main\resources\templates\layout\header.html"
Remove-Archivo "src\main\resources\templates\layout\chatbot.html"
Write-Host ""

# ============================================================
Write-Host "2. JAVASCRIPT SIN REFERENCIAS" -ForegroundColor Green
# ============================================================
Remove-Archivo "src\main\resources\static\js\califica.js"   # /califica sin vista
Remove-Archivo "src\main\resources\static\js\upload.js"     # subida antigua
Remove-Archivo "src\main\resources\static\js\aprende.js"    # usa <script> inline
Remove-Archivo "src\main\resources\static\js\contacto.js"   # usa <script> inline
Write-Host ""

# ============================================================
Write-Host "3. IMAGENES SIN REFERENCIAS (~17.9 MB)" -ForegroundColor Green
# ============================================================
$img = "src\main\resources\static\img"

# Nombres SIN acentos: coincidencia exacta
$sinAcentos = @(
    "agenda academica.PNG",
    "agenda.gif",
    "asistente Classify.PNG",
    "background.png",
    "backgroundagenda.png",
    "backgroundaprende.png",
    "backgroundcontacta.png",
    "backgroundgeneral.png",
    "backgroundterconpol.png",
    "btnchat.png",
    "btnchatdos.png",
    "cerebro.gif",
    "chart.svg",
    "chat.svg",
    "chatboot-icono.png",
    "chatbot-final.png",
    "chatbot-premium-removebg-preview.png",
    "chatbot-premium.png",
    "config.png",
    "contacta con un profe.PNG",
    "contacto.gif",
    "correo.png",
    "crear.png",
    "descargas.png",
    "editar.png",
    "eliminar.png",
    "gestion de clases.PNG",
    "help-me-lord-help-me.gif",
    "img.gif",
    "material academico.PNG",
    "megaphone.svg",
    "menu_h.svg",
    "modulo carga de materiales.PNG",
    "money.svg",
    "notification.svg",
    "ok.gif",
    "perfil.png",
    "periodico.gif",
    "robot_hello.png",
    "search.svg",
    "separador.png",
    "tools.svg",
    "udemy.svg",
    "unnamed.png",
    "unnamedlapiz.png",
    "usuario.png",
    "wallet.svg",
    "x.png",
    "Asistente Classify con IA icono.png",
    "Carga de materiales icono.png",
    "Contacta a un profesor icono.png"
)
foreach ($f in $sinAcentos) { Remove-Archivo (Join-Path $img $f) }

# Nombres CON acentos: se usan comodines para evitar problemas
# de codificacion entre UTF-8 y la consola de Windows.
$conAcentos = @(
    "Agenda acad*mica icono *.png",   # Agenda académica icono .png
    "Gesti*n de clases icono.png",    # Gestión de clases icono.png
    "Material acad*mico icono.png"    # Material académico icono.png
)
foreach ($p in $conAcentos) {
    Get-ChildItem -Path $img -Filter $p -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-Item -LiteralPath $_.FullName -Force
        Write-Host "  borrado: $($_.Name)" -ForegroundColor DarkGray
        $script:borrados++
    }
}
# OJO: loader.gif NO se borra. Ver seccion 5.
Write-Host ""

# ============================================================
Write-Host "4. BASURA DE macOS (.DS_Store)" -ForegroundColor Green
# ============================================================
Get-ChildItem -Path . -Filter ".DS_Store" -Recurse -Force -ErrorAction SilentlyContinue |
    ForEach-Object {
        Remove-Item -LiteralPath $_.FullName -Force
        Write-Host "  borrado: $($_.FullName)" -ForegroundColor DarkGray
        $script:borrados++
    }
Write-Host ""

# ============================================================
Write-Host "5. CORRECCION DE RUTA ROTA EN style.css" -ForegroundColor Green
# ============================================================
# style.css:844 apunta a ../img/icons/loader.gif pero el archivo
# esta en ../img/loader.gif (la carpeta icons/ no existe).
# Por ese 404 el preloader se ve en blanco.
$css = "src\main\resources\static\css\style.css"
if (Test-Path -LiteralPath $css) {
    $txt = [System.IO.File]::ReadAllText((Resolve-Path $css))
    if ($txt -match "img/icons/loader\.gif") {
        $nuevo = $txt -replace "\.\./img/icons/loader\.gif", "../img/loader.gif"
        $utf8SinBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText((Resolve-Path $css), $nuevo, $utf8SinBom)
        Write-Host "  CORREGIDO: ../img/loader.gif" -ForegroundColor Cyan
    } else {
        Write-Host "  (ya estaba corregido)" -ForegroundColor DarkGray
    }
}
Write-Host ""

# ============================================================
Write-Host "VERIFICACION" -ForegroundColor Green
# ============================================================
Write-Host "Total de elementos eliminados: $borrados" -ForegroundColor Cyan
Write-Host ""
Write-Host "Compilando..." -ForegroundColor Cyan
& .\mvnw.cmd -q clean compile
if ($LASTEXITCODE -eq 0) {
    Write-Host ">>> COMPILA OK" -ForegroundColor Green
} else {
    Write-Host ">>> ERROR DE COMPILACION - revisar antes de commit" -ForegroundColor Red
}
Write-Host ""
git status --short
Write-Host ""

Write-Host @"
############################################
 PENDIENTE - REQUIERE DECISION TUYA
############################################

A) /programacion - ROTO, NO BORRAR, RESTAURAR
   La plantilla fue borrada por error en el commit 4891538
   ("estilos generales"). El modulo SI existe: controlador con
   datos, 4 archivos JS, modulo activo en PermisosService.

   Ver que mas borro ese commit:
     git --no-pager show --stat 4891538

   Restaurar:
     git checkout 4891538^ -- src/main/resources/templates/programacion/programacion.html

   Por eso NO se borraron programacion.js, programacion-acciones.js,
   filtrosprog.js ni paginacionprog.js: son del modulo.

B) /califica y /notas - ROTOS (error 500)
   Si nunca existieron, eliminar de VistasController.java los
   metodos mostrarCalifica() y mostrarNotas(), los enlaces de
   layout/fragments.html (~65, ~71, ~119) y las entradas de
   WebConfig.java (~30, ~35).

C) DOS JS INEXISTENTES REFERENCIADOS (404)
   - /js/active.js       en materiales.html y mismateriales.html
   - /js/perfilmodal.js  en contacta.html
     Probable typo: existe modalperfil.js (quedo huerfano).
     Cambiar a @{/js/modalperfil.js} y probar el modal.

D) NO BORRAR: controladores, Application.java y los *ServiceImpl.
   Spring los instancia por anotacion, no por referencia textual.
"@ -ForegroundColor Yellow