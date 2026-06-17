/*
 * Módulo Programación (Python/Django) — lógica del cliente.
 * Combina, en un solo archivo y sin dependencias externas:
 *   1. Filtros por materia / profesor / curso.
 *   2. Paginación (10 registros por página) sobre las filas filtradas.
 *   3. Edición de una agenda mediante modal (carga datos vía JSON y los
 *      guarda con POST a /programacion/editar/<id>).
 *   4. Eliminación con confirmación (POST a /programacion/eliminar/<id>).
 *
 * Las peticiones envían el header X-Requested-With: XMLHttpRequest para que
 * el backend responda en JSON (mismo contrato que el resto de la plataforma).
 */
document.addEventListener('DOMContentLoaded', function () {
    const tabla = document.querySelector('#agendaTable tbody');
    if (!tabla) return;

    const filtroMateria = document.getElementById('filtroMateria');
    const filtroProfesor = document.getElementById('filtroProfesor');
    const filtroCurso = document.getElementById('filtroCurso');
    const contenedorPaginacion = document.getElementById('paginacion');

    const REGISTROS_POR_PAGINA = 10;
    let paginaActual = 1;
    let filasFiltradas = [];

    const todasLasFilas = Array.from(tabla.querySelectorAll('tr'));

    // ── 1 y 2. Filtros + paginación ────────────────────────────────
    function texto(fila, indice) {
        const celdas = fila.querySelectorAll('td');
        return celdas[indice] ? celdas[indice].textContent.toLowerCase() : '';
    }

    function aplicarFiltros() {
        const m = (filtroMateria?.value || '').toLowerCase();
        const p = (filtroProfesor?.value || '').toLowerCase();
        const c = (filtroCurso?.value || '').toLowerCase();

        filasFiltradas = todasLasFilas.filter(fila =>
            texto(fila, 1).includes(m) &&
            texto(fila, 2).includes(p) &&
            texto(fila, 4).includes(c)
        );
        paginaActual = 1;
        mostrarPagina();
    }

    function mostrarPagina() {
        todasLasFilas.forEach(fila => { fila.style.display = 'none'; });

        const inicio = (paginaActual - 1) * REGISTROS_POR_PAGINA;
        const fin = inicio + REGISTROS_POR_PAGINA;
        filasFiltradas.slice(inicio, fin).forEach(fila => { fila.style.display = ''; });

        renderPaginacion();
    }

    function renderPaginacion() {
        if (!contenedorPaginacion) return;
        const totalPaginas = Math.ceil(filasFiltradas.length / REGISTROS_POR_PAGINA);
        let html = '';

        if (paginaActual > 1) {
            html += `<button class="btn-paginacion" data-pagina="${paginaActual - 1}">‹ Anterior</button>`;
        }
        for (let i = 1; i <= totalPaginas; i++) {
            html += (i === paginaActual)
                ? `<span class="pagina-actual">${i}</span>`
                : `<button class="btn-paginacion" data-pagina="${i}">${i}</button>`;
        }
        if (paginaActual < totalPaginas) {
            html += `<button class="btn-paginacion" data-pagina="${paginaActual + 1}">Siguiente ›</button>`;
        }

        const desde = filasFiltradas.length ? inicioRegistro() : 0;
        const hasta = Math.min(paginaActual * REGISTROS_POR_PAGINA, filasFiltradas.length);
        html += `<div class="info-paginacion">Estas viendo ${desde}-${hasta} de ${filasFiltradas.length} registros</div>`;

        contenedorPaginacion.innerHTML = html;
        contenedorPaginacion.querySelectorAll('.btn-paginacion').forEach(btn => {
            btn.addEventListener('click', () => {
                paginaActual = parseInt(btn.dataset.pagina, 10);
                mostrarPagina();
            });
        });
    }

    function inicioRegistro() {
        return (paginaActual - 1) * REGISTROS_POR_PAGINA + 1;
    }

    [filtroMateria, filtroProfesor, filtroCurso].forEach(input => {
        if (input) input.addEventListener('input', aplicarFiltros);
    });

    // ── 3. Edición vía modal ───────────────────────────────────────
    const modal = document.getElementById('modalEditar');
    const formEditar = document.getElementById('formEditar');
    const msgModal = document.getElementById('modalEditarMsg');

    function setValor(id, valor) {
        const el = document.getElementById(id);
        if (!el) return;
        valor = (valor === null || valor === undefined) ? '' : String(valor);
        // Si es un <select> y el valor no existe como opción, lo agregamos
        // para no perder el dato original guardado en la base de datos.
        if (el.tagName === 'SELECT' && valor !== '') {
            const existe = Array.from(el.options).some(o => o.value === valor);
            if (!existe) {
                const opt = document.createElement('option');
                opt.value = valor;
                opt.textContent = valor;
                el.appendChild(opt);
            }
        }
        el.value = valor;
    }

    function abrirModal() {
        if (msgModal) { msgModal.textContent = ''; msgModal.className = 'modal-prog-msg'; }
        modal.classList.add('abierto');
        modal.setAttribute('aria-hidden', 'false');
    }

    function cerrarModal() {
        modal.classList.remove('abierto');
        modal.setAttribute('aria-hidden', 'true');
    }

    function cargarAgenda(id) {
        fetch(`/programacion/agenda/${id}`, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(r => r.json())
            .then(data => {
                if (!data.success) { alert(data.message || 'No se pudo cargar la agenda.'); return; }
                const a = data.agenda;
                setValor('ed_id', a.id);
                setValor('ed_grado', a.grado);
                setValor('ed_grupo', a.grupo);
                setValor('ed_profesor', a.profesor);
                setValor('ed_materia', a.materia);
                setValor('ed_fecha', a.fecha);
                setValor('ed_horaInicio', a.horaInicio);
                setValor('ed_duracion', a.duracion);
                setValor('ed_modalidad', a.modalidad);
                setValor('ed_materialesBasicos', a.materialesBasicos);
                setValor('ed_temaPrincipal', a.temaPrincipal);
                setValor('ed_objetivos', a.objetivos);
                setValor('ed_dificultades', a.dificultades);
                setValor('ed_recursosNecesarios', a.recursosNecesarios);
                document.getElementById('modalEditarId').textContent = '#' + a.id;
                abrirModal();
            })
            .catch(() => alert('Error de conexión al cargar la agenda.'));
    }

    if (formEditar) {
        formEditar.addEventListener('submit', function (e) {
            e.preventDefault();
            const id = document.getElementById('ed_id').value;
            const datos = new URLSearchParams(new FormData(formEditar));
            fetch(`/programacion/editar/${id}`, {
                method: 'POST',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: datos.toString()
            })
                .then(r => r.json())
                .then(data => {
                    if (data.success) {
                        msgModal.textContent = data.message || '¡Guardado!';
                        msgModal.className = 'modal-prog-msg ok';
                        setTimeout(() => window.location.reload(), 600);
                    } else {
                        msgModal.textContent = data.message || 'No se pudo guardar.';
                        msgModal.className = 'modal-prog-msg error';
                    }
                })
                .catch(() => {
                    msgModal.textContent = 'Error de conexión al guardar.';
                    msgModal.className = 'modal-prog-msg error';
                });
        });
    }

    // ── 4. Eliminación con confirmación ────────────────────────────
    function eliminar(id, info) {
        const detalle = info ? `\n\n${info}` : '';
        if (!confirm(`¿Eliminar esta agenda?${detalle}\n\nEsta acción no se puede deshacer.`)) return;

        fetch(`/programacion/eliminar/${id}`, {
            method: 'POST',
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(r => r.json())
            .then(data => {
                if (data.success) {
                    const fila = tabla.querySelector(`tr[data-id="${id}"]`);
                    if (fila) {
                        const idx = todasLasFilas.indexOf(fila);
                        if (idx > -1) todasLasFilas.splice(idx, 1);
                        fila.remove();
                    }
                    aplicarFiltros();
                } else {
                    alert(data.message || 'No se pudo eliminar.');
                }
            })
            .catch(() => alert('Error de conexión al eliminar.'));
    }

    // Delegación de eventos para editar/eliminar (un solo listener).
    tabla.addEventListener('click', function (e) {
        const btnEditar = e.target.closest('.btn-editar');
        const btnEliminar = e.target.closest('.btn-eliminar');
        if (btnEditar) { cargarAgenda(btnEditar.dataset.id); }
        else if (btnEliminar) { eliminar(btnEliminar.dataset.id, btnEliminar.dataset.info); }
    });

    // Cerrar modal: botón X, Cancelar, clic fuera y tecla Escape.
    document.getElementById('modalCerrar')?.addEventListener('click', cerrarModal);
    document.getElementById('modalCancelar')?.addEventListener('click', cerrarModal);
    modal?.addEventListener('click', e => { if (e.target === modal) cerrarModal(); });
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape' && modal.classList.contains('abierto')) cerrarModal();
    });

    // ── Inicialización ─────────────────────────────────────────────
    filasFiltradas = todasLasFilas.slice();
    mostrarPagina();
});
