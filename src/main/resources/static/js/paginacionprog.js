document.addEventListener('DOMContentLoaded', function() {
    const filtroMateria = document.getElementById('filtroMateria');
    const filtroProfesor = document.getElementById('filtroProfesor');
    const filtroCurso = document.getElementById('filtroCurso');
    const tabla = document.querySelector('.programacion-tabla tbody');
    const contenedorPaginacion = document.getElementById('paginacion');
    
    if (!tabla) return;

    // Variables de paginación
    let paginaActual = 1;
    const registrosPorPagina = 10;
    let datosFiltrados = [];

    // Obtener todos los datos iniciales
    function obtenerDatosIniciales() {
        const filas = tabla.querySelectorAll('tr');
        return Array.from(filas).map(fila => {
            const celdas = fila.querySelectorAll('td');
            return {
                elemento: fila,
                materia: celdas[1]?.textContent.toLowerCase() || '',
                profesor: celdas[2]?.textContent.toLowerCase() || '',
                curso: celdas[4]?.textContent.toLowerCase() || '',
                visible: true
            };
        });
    }

    let todosLosDatos = obtenerDatosIniciales();

    function filtrarYMostrar() {
        const textoMateria = filtroMateria ? filtroMateria.value.toLowerCase() : '';
        const textoProfesor = filtroProfesor ? filtroProfesor.value.toLowerCase() : '';
        const textoCurso = filtroCurso ? filtroCurso.value.toLowerCase() : '';

        // Filtrar datos
        datosFiltrados = todosLosDatos.filter(item =>
            item.materia.includes(textoMateria) &&
            item.profesor.includes(textoProfesor) &&
            item.curso.includes(textoCurso)
        );

        // Resetear a página 1 cuando se filtra
        paginaActual = 1;
        mostrarPagina();
    }

    function mostrarPagina() {
        // Ocultar todas las filas primero
        todosLosDatos.forEach(item => {
            item.elemento.style.display = 'none';
        });

        // Calcular índices para la página actual
        const inicio = (paginaActual - 1) * registrosPorPagina;
        const fin = inicio + registrosPorPagina;
        const filasPagina = datosFiltrados.slice(inicio, fin);

        // Mostrar solo las filas de la página actual
        filasPagina.forEach(item => {
            item.elemento.style.display = '';
        });

        // Actualizar controles de paginación
        actualizarControlesPaginacion();
    }

    function actualizarControlesPaginacion() {
        if (!contenedorPaginacion) return;

        const totalPaginas = Math.ceil(datosFiltrados.length / registrosPorPagina);
        
        let htmlPaginacion = '';
        
        // Botón Anterior
        if (paginaActual > 1) {
            htmlPaginacion += `<button class="btn-paginacion" onclick="cambiarPagina(${paginaActual - 1})">‹ Anterior</button>`;
        }

        // Números de página
        for (let i = 1; i <= totalPaginas; i++) {
            if (i === paginaActual) {
                htmlPaginacion += `<span class="pagina-actual">${i}</span>`;
            } else {
                htmlPaginacion += `<button class="btn-paginacion" onclick="cambiarPagina(${i})">${i}</button>`;
            }
        }

        // Botón Siguiente
        if (paginaActual < totalPaginas) {
            htmlPaginacion += `<button class="btn-paginacion" onclick="cambiarPagina(${paginaActual + 1})">Siguiente ›</button>`;
        }

        // Información de registros
        const inicio = (paginaActual - 1) * registrosPorPagina + 1;
        const fin = Math.min(paginaActual * registrosPorPagina, datosFiltrados.length);
        
        htmlPaginacion += `<div class="info-paginacion">Estas viendo ${inicio}-${fin} de ${datosFiltrados.length} registros</div>`;

        contenedorPaginacion.innerHTML = htmlPaginacion;
    }

    // Función global para cambiar página
    window.cambiarPagina = function(nuevaPagina) {
        paginaActual = nuevaPagina;
        mostrarPagina();
    };

    // Event listeners para filtros
    if (filtroMateria) filtroMateria.addEventListener('input', filtrarYMostrar);
    if (filtroProfesor) filtroProfesor.addEventListener('input', filtrarYMostrar);
    if (filtroCurso) filtroCurso.addEventListener('input', filtrarYMostrar);

    // Inicializar
    datosFiltrados = [...todosLosDatos];
    mostrarPagina();
});