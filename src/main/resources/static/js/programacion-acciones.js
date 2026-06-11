/**
 * programacion-acciones.js — Classify
 * Gestión de acciones Editar y Eliminar sobre las clases programadas.
 * Trabaja sobre el modal definido en programacion.html.
 */
(function () {
    "use strict";

    /* ── Referencias del modal ───────────────────────────────────────── */
    const modal           = document.getElementById('modalEditar');
    const modalOverlay    = document.getElementById('modalOverlay');
    const btnCerrarModal  = document.getElementById('btnCerrarModal');
    const btnCancelar     = document.getElementById('btnCancelarEditar');
    const formEditar      = document.getElementById('formEditar');
    const alertaProg      = document.getElementById('alertaProgramacion');

    /* ── Mapa Profesor ↔ Materia (igual que en agenda.js) ────────────── */
    const PROFESOR_MATERIA = {
        "Ana García":      "Español",
        "Carlos Méndez":   "Matematicas",
        "Laura Fernández": "Historia",
        "Jorge Ramírez":   "Ingles",
        "Sofía Torres":    "Etica y valores",
        "Andrés López":    "Educación fisica",
        "Marta Ríos":      "Informatica"
    };
    const MATERIA_PROFESORES = {};
    for (const [p, m] of Object.entries(PROFESOR_MATERIA)) {
        if (!MATERIA_PROFESORES[m]) MATERIA_PROFESORES[m] = [];
        MATERIA_PROFESORES[m].push(p);
    }

    /* ════════════════════════════════════════════════════════════════════
       ABRIR MODAL DE EDICIÓN
       Recibe el botón con los datos en data-* attributes.
       ════════════════════════════════════════════════════════════════════ */
    function abrirModalEditar(btn) {
        const id = btn.dataset.id;
        formEditar.dataset.id = id;

        // Rellenar campos
        setVal('editId',           id);
        setVal('editMateria',      btn.dataset.materia);
        setVal('editProfesor',     btn.dataset.profesor);
        setVal('editFecha',        btn.dataset.fecha);
        setVal('editHoraInicio',   btn.dataset.hora);
        setVal('editDuracion',     btn.dataset.duracion);
        setVal('editGrado',        btn.dataset.grado);
        setVal('editGrupo',        btn.dataset.grupo);
        setVal('editModalidad',    btn.dataset.modalidad);
        setVal('editTemaPrincipal',btn.dataset.tema);

        ocultarAlertaProg();
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function cerrarModal() {
        modal.style.display = 'none';
        document.body.style.overflow = '';
        ocultarAlertaProg();
    }

    function setVal(id, value) {
        const el = document.getElementById(id);
        if (el) el.value = value || '';
    }

    /* ════════════════════════════════════════════════════════════════════
       GUARDAR EDICIÓN  →  PUT /api/agendas/{id}
       ════════════════════════════════════════════════════════════════════ */
    formEditar.addEventListener('submit', async function (e) {
        e.preventDefault();
        const id = formEditar.dataset.id;

        const payload = {
            materia:       getVal('editMateria'),
            profesor:      getVal('editProfesor'),
            fecha:         getVal('editFecha'),
            horaInicio:    getVal('editHoraInicio'),
            duracion:      parseInt(getVal('editDuracion'), 10),
            grado:         parseInt(getVal('editGrado'), 10) || null,
            grupo:         getVal('editGrupo'),
            modalidad:     getVal('editModalidad'),
            temaPrincipal: getVal('editTemaPrincipal')
        };

        try {
            const resp = await fetch(`/api/agendas/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);

            cerrarModal();
            mostrarAlertaProg('success', '✅ Clase actualizada correctamente. Recargando...');
            setTimeout(() => location.reload(), 1200);

        } catch (err) {
            mostrarAlertaProg('error', `❌ Error al guardar: ${err.message}`);
        }
    });

    /* ════════════════════════════════════════════════════════════════════
       ELIMINAR  →  DELETE /api/agendas/{id}
       ════════════════════════════════════════════════════════════════════ */
    document.addEventListener('click', async function (e) {
        const btn = e.target.closest('.btn-eliminar-clase');
        if (!btn) return;

        const id       = btn.dataset.id;
        const info     = btn.dataset.info || `ID ${id}`;
        const confirma = confirm(`¿Eliminar la clase "${info}"?\nEsta acción no se puede deshacer.`);
        if (!confirma) return;

        try {
            const resp = await fetch(`/api/agendas/${id}`, { method: 'DELETE' });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);

            // Remover la fila de la tabla sin recargar
            const fila = btn.closest('tr');
            if (fila) {
                fila.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
                fila.style.opacity    = '0';
                fila.style.transform  = 'translateX(20px)';
                setTimeout(() => fila.remove(), 420);
            }
            mostrarAlertaProg('success', '🗑️ Clase eliminada correctamente.');

        } catch (err) {
            mostrarAlertaProg('error', `❌ Error al eliminar: ${err.message}`);
        }
    });

    /* ════════════════════════════════════════════════════════════════════
       EVENTOS DEL MODAL
       ════════════════════════════════════════════════════════════════════ */
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('.btn-editar-clase');
        if (btn) abrirModalEditar(btn);
    });

    btnCerrarModal.addEventListener('click', cerrarModal);
    btnCancelar.addEventListener('click', cerrarModal);
    modalOverlay.addEventListener('click', cerrarModal);

    // Cerrar con Escape
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape' && modal.style.display === 'flex') cerrarModal();
    });

    /* ── Relación Profesor ↔ Materia dentro del modal ─────────────────── */
    const editProfesor = document.getElementById('editProfesor');
    const editMateria  = document.getElementById('editMateria');
    const opcionesOrigProf = Array.from(editProfesor.options).map(o => ({ value: o.value, text: o.text }));

    editProfesor.addEventListener('change', function () {
        const mat = PROFESOR_MATERIA[this.value];
        if (mat) editMateria.value = mat;
    });

    editMateria.addEventListener('change', function () {
        const mat = this.value;
        // Restaurar todas las opciones
        editProfesor.innerHTML = '';
        opcionesOrigProf.forEach(({ value, text }) => editProfesor.appendChild(new Option(text, value)));

        if (!mat) return;
        const profs = MATERIA_PROFESORES[mat] || [];
        Array.from(editProfesor.options).forEach(opt => {
            if (opt.value && !profs.includes(opt.value)) {
                opt.style.display = 'none';
                opt.disabled = true;
            }
        });
        if (editProfesor.value && !profs.includes(editProfesor.value)) editProfesor.value = '';
        if (profs.length === 1) editProfesor.value = profs[0];
    });

    /* ── Utilidades ───────────────────────────────────────────────────── */
    function getVal(id) {
        const el = document.getElementById(id);
        return el ? el.value : '';
    }

    function mostrarAlertaProg(tipo, msg) {
        alertaProg.textContent  = msg;
        alertaProg.className    = `alerta-programacion ${tipo} visible`;
        alertaProg.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function ocultarAlertaProg() {
        alertaProg.className = 'alerta-programacion';
        alertaProg.textContent = '';
    }

})();