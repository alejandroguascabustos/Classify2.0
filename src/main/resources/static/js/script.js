/* public/js/script.js (reemplazar archivo actual) */

document.addEventListener('DOMContentLoaded', () => {
  /* ------------------ Preloader ------------------ */
  const preloader = document.getElementById('preloader');
  if (preloader) {
    // Si quieres que se espere hasta window.load para recursos pesados,
    // puedes mover esto a window.addEventListener('load', ...) — aquí usamos DOMContentLoaded
    preloader.style.transition = "opacity 1.0s";
    // ocultar después de 1.2s
    setTimeout(() => {
      preloader.style.opacity = '0';
      // retiramos del flujo tras la transición
      setTimeout(() => { preloader.style.display = 'none'; }, 1000);
    }, 1200);
  }

  /* ------------------ Skeleton loader ------------------ */
  const skeleton = document.getElementById('skeleton-loader');
  if (skeleton) {
    setTimeout(() => {
      skeleton.style.display = 'none';
    }, 1500);
  }

  /* ------------------ Mensajes por query params ------------------ */
  (function() {
    const params = new URLSearchParams(window.location.search);
    const mensaje = document.getElementById('mensaje');
    if (!mensaje) return;
    if (params.has('success')) {
      mensaje.style.display = 'block';
      mensaje.style.color = 'green';
      mensaje.textContent = 'Clase agendada correctamente.';
    } else if (params.has('error')) {
      mensaje.style.display = 'block';
      mensaje.style.color = 'red';
      let error = params.get('error');
      if (error === 'missing') mensaje.textContent = 'Faltan datos obligatorios.';
      else if (error === 'server') mensaje.textContent = 'Error del servidor al guardar.';
      else if (error === 'exception') mensaje.textContent = 'Error inesperado.';
      else mensaje.textContent = 'Error desconocido.';
    }
  })();

  /* ------------------ Marca link activo en el sidebar ------------------ */
  const links = document.querySelectorAll(".sidebar a");
  if (links.length) {
    links.forEach(link => {
      // compara solo pathname+search para evitar diferencias con hash o trailing slash si quieres
      try {
        if (link.href === window.location.href) {
          link.classList.add("active");
        }
      } catch (e) {
        // enlace con href relativo mal formado: lo ignoramos
      }
    });
  }

  /* ------------------ Filtros y paginación (agenda) ------------------ */
  // Sólo definir listeners si los inputs existen en esta página
  const filtroMateria = document.getElementById('filtroMateria');
  const filtroProfesor = document.getElementById('filtroProfesor');
  const filtroCurso = document.getElementById('filtroCurso');

  // Solo añadir listeners si al menos uno existe
  if (filtroMateria || filtroProfesor || filtroCurso) {
    // Variables para el paginado (puedes mantenerlas globales si son usadas fuera)
    window.agendaData = window.agendaData || [];
    window.currentPage = window.currentPage || 1;
    window.pageSize = window.pageSize || 5;

    const aplicarFiltros = () => {
      window.currentPage = 1;
      renderAgendaTableSafely();
    };

    if (filtroMateria) filtroMateria.addEventListener('input', aplicarFiltros);
    if (filtroProfesor) filtroProfesor.addEventListener('input', aplicarFiltros);
    if (filtroCurso) filtroCurso.addEventListener('input', aplicarFiltros);

    // Implementación segura de renderAgendaTable (usa elementos sólo si existen)
    function renderAgendaTableSafely() {
      const tabla = document.querySelector('#agendaTable tbody');
      if (!tabla) return;
      const pm = document.getElementById('filtroMateria');
      const pp = document.getElementById('filtroProfesor');
      const pc = document.getElementById('filtroCurso');

      const filters = {
        materia: pm ? pm.value.toLowerCase() : '',
        profesor: pp ? pp.value.toLowerCase() : '',
        curso: pc ? pc.value.toLowerCase() : ''
      };

      const data = Array.isArray(window.agendaData) ? window.agendaData : [];

      let filtered = data.filter(a =>
        (filters.materia === '' || (a.materia || '').toLowerCase().includes(filters.materia)) &&
        (filters.profesor === '' || (a.profesor || '').toLowerCase().includes(filters.profesor)) &&
        (filters.curso === '' || (a.curso || '').toLowerCase().includes(filters.curso))
      );

      const totalPages = Math.max(1, Math.ceil(filtered.length / window.pageSize));
      window.currentPage = Math.min(window.currentPage, totalPages);
      const start = (window.currentPage - 1) * window.pageSize;
      const end = start + window.pageSize;

      tabla.innerHTML = '';
      filtered.slice(start, end).forEach(a => {
        const row = `
          <tr>
            <td>${a.id || ''}</td>
            <td>${a.materia || ''}</td>
            <td>${a.profesor || ''}</td>
            <td>${a.horario || ''}</td>
            <td>${a.curso || ''}</td>
            <td>${a.observaciones || ''}</td>
            <td>${a.fecha_creacion ? a.fecha_creacion.replace('T', ' ').substring(0, 19) : ''}</td>
          </tr>
        `;
        tabla.innerHTML += row;
      });

      const pagDOM = document.getElementById('paginacion');
      if (pagDOM) {
        pagDOM.innerHTML =
          `<button ${window.currentPage===1?'disabled':''} onclick='window.cambiarPagina(-1)'>Anterior</button> `+
          `Página ${window.currentPage} de ${totalPages} `+
          `<button ${window.currentPage===totalPages?'disabled':''} onclick='window.cambiarPagina(1)'>Siguiente</button>`;
      }
    }

    // Exponer funciones globalmente para que botones inline funcionen
    window.renderAgendaTable = renderAgendaTableSafely;
    window.cambiarPagina = (delta) => { window.currentPage += delta; renderAgendaTableSafely(); };
  }

  /* ------------------ Menú hamburguesa ------------------ */
  const menuToggle = document.getElementById('menuToggle');
  const sidebarEl = document.getElementById('sidebar');
  const overlay = document.getElementById('overlay');

  if (menuToggle && sidebarEl && overlay) {
    menuToggle.addEventListener('click', () => {
      menuToggle.classList.toggle('active');
      sidebarEl.classList.toggle('active');
      overlay.classList.toggle('active');
    });

    overlay.addEventListener('click', () => {
      menuToggle.classList.remove('active');
      sidebarEl.classList.remove('active');
      overlay.classList.remove('active');
    });
  } else {
    // útil para debugging: ver en consola qué elemento falta
    // console.log('menuToggle/sidebar/overlay missing:', !!menuToggle, !!sidebarEl, !!overlay);
  }
}); // fin DOMContentLoaded

/* ------------------ Funciones globales (si las usas desde HTML) ------------------ */
function mostrarMensaje() {
  const msg = document.getElementById('msg');
  if (!msg) return;
  msg.textContent = 'Enviando…';
  setTimeout(() => {
    msg.textContent = '¡Listo! Revisa tu correo.';
  }, 800);
}

function toggleCampos(rol) {
  const box = document.getElementById('extraCampos');
  if (!box) return;
  box.innerHTML = '';
  if (rol === 'estudiante') {
    box.innerHTML = `
      <label for="tema">¿De que tema quieres hablar?</label>
      <input  class="inputcontacta" id="tema" name="tema" type="text" placeholder="Disciplina, Rendimiento académico, Otro…" required>`;
  } else if (rol === 'docente') {
    box.innerHTML = `
      <label for="nombreEstudiante">Nombre del estudiante</label>
      <input id="nombreEstudiante" name="nombreEstudiante" type="text" placeholder="Ej. Ana Torres" required>`;
  }

// Capturar todos los enlaces
document.querySelectorAll("nav a").forEach(link => {
        link.addEventListener("click", function(e) {
            e.preventDefault(); // Evita que cambie de página

            // Ocultar todas las secciones
            document.querySelectorAll(".seccion").forEach(sec => sec.style.display = "none");

            // Mostrar la sección seleccionada
            const id = this.getAttribute("data-section");
            document.getElementById(id).style.display = "block";
        });
    });

}
