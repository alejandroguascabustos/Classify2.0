/**
 * agenda.js — Classify
 * Validaciones en tiempo real para el formulario de agendar clase:
 *   1. Hora en intervalos de 15 minutos.
 *   2. Bloqueo si la clase empieza en menos de 1 hora desde ahora.
 *   3. Alerta si el profesor ya tiene una clase que se superpone.
 *   4. Relación automática Profesor ↔ Materia (bidireccional).
 */
(function () {
    "use strict";

    /* ══════════════════════════════════════════════════════════════════
       MAPA PROFESOR ↔ MATERIA
       Cada profesor tiene asignada su materia principal.
       ══════════════════════════════════════════════════════════════════ */
    const PROFESOR_MATERIA = {
        "Ana García":      "Español",
        "Carlos Méndez":   "Matematicas",
        "Laura Fernández": "Historia",
        "Jorge Ramírez":   "Ingles",
        "Sofía Torres":    "Etica y valores",
        "Andrés López":    "Educación fisica",
        "Marta Ríos":      "Informatica"
    };

    // Inverso: materia → lista de profesores que la imparten
    const MATERIA_PROFESORES = {};
    for (const [prof, mat] of Object.entries(PROFESOR_MATERIA)) {
        if (!MATERIA_PROFESORES[mat]) MATERIA_PROFESORES[mat] = [];
        MATERIA_PROFESORES[mat].push(prof);
    }

    /* ── Referencias DOM ─────────────────────────────────────────────── */
    const form               = document.getElementById('agendaForm');
    const fechaInput         = document.getElementById('fecha');
    const horaInput          = document.getElementById('horaInicio');
    const horaPickerContainer= document.getElementById('horaPickerContainer');
    const horaHoras          = document.getElementById('horaHoras');
    const horaMinutos        = document.getElementById('horaMinutos');
    const duracionSel        = document.getElementById('duracion');
    const profesorSel        = document.getElementById('profesor');
    const materiaSel         = document.getElementById('materia');
    const gradoSel           = document.getElementById('grado');
    const grupoInput         = document.getElementById('grupo');
    const alertaBox          = document.getElementById('alertaHorario');
    const alertaIcono        = document.getElementById('alertaIcono');
    const alertaTitulo       = document.getElementById('alertaTitulo');
    const alertaMensaje      = document.getElementById('alertaMensaje');
    const alertaDetalle      = document.getElementById('alertaDetalle');
    const horaHint           = document.getElementById('horaHint');

    /** Sincroniza los selects de hora con el input hidden */
    function sincronizarHora() {
        const h = horaHoras.value;
        const m = horaMinutos.value;
        horaInput.value = h ? `${h}:${m || '00'}` : '';
    }

    /* Estado */
    let hayConflicto    = false;
    let hayAnticipacion = false;
    let ignorarCambioProfesor = false; // evita bucle al actualizar selects programáticamente
    let ignorarCambioMateria  = false;

    /* ══════════════════════════════════════════════════════════════════
       UTILIDADES DE TIEMPO
       ══════════════════════════════════════════════════════════════════ */

    /** Convierte "HH:MM" o "HH:MM:SS" a minutos desde medianoche */
    function horaAMinutos(str) {
        if (!str) return null;
        const partes = str.split(':').map(Number);
        return partes[0] * 60 + (partes[1] || 0); // ← corrección: era * 30
    }

    /** Redondea minutos al múltiplo de 15 más cercano hacia arriba */
    function redondear15(minutos) {
        return Math.ceil(minutos / 15) * 15;
    }

    /** Formatea minutos-desde-medianoche a "HH:MM" */
    function minutosAHora(min) {
        const h = Math.floor(min / 60) % 24;
        const m = min % 60;
        return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
    }

    /** Normaliza horaInicio de la API (array o string) → "HH:MM" */
    function normalizarHora(valor) {
        if (!valor) return null;
        if (Array.isArray(valor)) {
            return `${String(valor[0]).padStart(2,'0')}:${String(valor[1]||0).padStart(2,'0')}`;
        }
        return String(valor).substring(0, 5);
    }

    /** Normaliza fecha de la API (array o string) → "YYYY-MM-DD" */
    function normalizarFecha(valor) {
        if (!valor) return null;
        if (Array.isArray(valor)) {
            return `${valor[0]}-${String(valor[1]).padStart(2,'0')}-${String(valor[2]).padStart(2,'0')}`;
        }
        return String(valor).substring(0, 10);
    }

    /* ══════════════════════════════════════════════════════════════════
       1. HORA EN INTERVALOS DE 15 MINUTOS
       Los selects solo ofrecen :00 :15 :30 :45, así que no hace
       falta redondear — simplemente sincronizamos y validamos.
       ══════════════════════════════════════════════════════════════════ */
    function onHoraCambia() {
        sincronizarHora();
        if (!horaInput.value) return;
        validarAnticipacion();
        verificarConflicto();
    }
    horaHoras.addEventListener('change', onHoraCambia);
    horaMinutos.addEventListener('change', onHoraCambia);

    /* ══════════════════════════════════════════════════════════════════
       2. BLOQUEO DE ANTICIPACIÓN MÍNIMA (1 hora)
       ══════════════════════════════════════════════════════════════════ */

    function calcularHoraMinima() {
        const fechaVal = fechaInput.value;
        if (!fechaVal) return null;
        const ahora = new Date();
        const hoy   = ahora.toISOString().split('T')[0];
        if (fechaVal !== hoy) return null;

        // Minutos actuales + 60, redondeados hacia arriba al siguiente cuarto
        const minAhora = ahora.getHours() * 60 + ahora.getMinutes();
        const minMin   = redondear15(minAhora + 60);
        if (minMin >= 1440) return '23:59';
        return minutosAHora(minMin);
    }

    function actualizarMinHora() {
        const min = calcularHoraMinima();
        if (min) {
            horaInput.min = min;
        } else {
            horaInput.removeAttribute('min');
        }
        validarAnticipacion();
    }

    function validarAnticipacion() {
        const fechaVal = fechaInput.value;
        const horaVal  = horaInput.value;

        hayAnticipacion = false;
        horaPickerContainer.classList.remove('campo-invalido');
        horaHint.className   = 'hora-hint';
        horaHint.textContent = '';

        if (!fechaVal || !horaVal) return;

        const hoy = new Date().toISOString().split('T')[0];

        if (fechaVal !== hoy) {
            horaHint.textContent = 'Las clases se programan en intervalos de 15 minutos.';
            horaHint.classList.add('visible');
            return;
        }

        const horaMin    = calcularHoraMinima();
        if (!horaMin) return;

        const minActual  = horaAMinutos(horaMin);
        const minElegido = horaAMinutos(horaVal);

        if (minElegido < minActual) {
            hayAnticipacion = true;
            horaPickerContainer.classList.add('campo-invalido');
            horaHint.textContent = `⏱ Debes programar con al menos 1 hora de anticipación. Mínimo hoy: ${horaMin}`;
            horaHint.classList.add('visible', 'error');
            mostrarAlerta('anticipacion',
                'No puedes programar con menos de 1 hora de anticipación',
                `La clase debe agendarse a partir de las ${horaMin} para empezar hoy.`,
                null
            );
        } else {
            horaHint.textContent = 'Las clases se programan en intervalos de 15 minutos.';
            horaHint.classList.add('visible');
            if (!hayConflicto) ocultarAlerta();
        }
    }

    fechaInput.addEventListener('change', function () {
        actualizarMinHora();
        verificarConflicto();
    });

    /* ══════════════════════════════════════════════════════════════════
       3. DETECCIÓN DE CONFLICTOS: SALÓN Y PROFESOR
       Regla 1 – mismo salón (grado + grupo) con horario superpuesto.
       Regla 2 – mismo profesor en cualquier salón con horario superpuesto.
       ══════════════════════════════════════════════════════════════════ */
    let abortController = null;

    /** Devuelve true si dos franjas se superponen */
    function seSolapan(inicioA, finA, inicioB, finB) {
        return inicioA < finB && finA > inicioB;
    }

    async function verificarConflicto() {
        const fecha    = fechaInput.value;
        const hora     = horaInput.value;
        const duracion = parseInt(duracionSel.value, 10);
        const grado    = gradoSel.value;
        const grupo    = (grupoInput.value || '').trim().toLowerCase();
        const profesor = profesorSel.value;

        hayConflicto = false;
        // Se necesita al menos fecha, hora y grado para verificar
        if (!fecha || !hora || !grado) return;

        if (abortController) abortController.abort();
        abortController = new AbortController();

        try {
            const resp = await fetch('/api/agendas', { signal: abortController.signal });
            if (!resp.ok) return;

            const agendas   = await resp.json();
            const nuevaIni  = horaAMinutos(hora);
            const nuevaFin  = nuevaIni + duracion;

            // ── Conflicto 1: mismo salón ───────────────────────────────
            const clasesSalon = agendas.filter(a =>
                normalizarFecha(a.fecha) === fecha &&
                String(a.grado) === String(grado) &&
                (a.grupo || '').trim().toLowerCase() === grupo
            );

            for (const clase of clasesSalon) {
                const horaClase   = normalizarHora(clase.horaInicio);
                if (!horaClase) continue;
                const claseIni = horaAMinutos(horaClase);
                const claseFin = claseIni + (clase.duracion || 60);

                if (seSolapan(nuevaIni, nuevaFin, claseIni, claseFin)) {
                    hayConflicto = true;
                    const salon   = `${grado}°${grupo ? ' ' + grupo.toUpperCase() : ''}`;
                    const hFinFmt = minutosAHora(claseFin);
                    mostrarAlerta('conflicto',
                        `El salón ${salon} ya tiene clase en ese horario`,
                        `"${clase.materia || 'Otra materia'}" con ${clase.profesor || '–'} ` +
                        `de ${horaClase.substring(0,5)} a ${hFinFmt}.`,
                        `📅 ${fecha}   🕐 ${horaClase.substring(0,5)} – ${hFinFmt}`
                    );
                    return;
                }
            }

            // ── Conflicto 2: mismo profesor en otro salón ──────────────
            if (profesor) {
                const clasesProfesor = agendas.filter(a =>
                    a.profesor === profesor && normalizarFecha(a.fecha) === fecha
                );

                for (const clase of clasesProfesor) {
                    const horaClase   = normalizarHora(clase.horaInicio);
                    if (!horaClase) continue;
                    const claseIni = horaAMinutos(horaClase);
                    const claseFin = claseIni + (clase.duracion || 60);

                    if (seSolapan(nuevaIni, nuevaFin, claseIni, claseFin)) {
                        hayConflicto = true;
                        const salonEx = `${clase.grado || '?'}°${clase.grupo ? ' ' + clase.grupo.toUpperCase() : ''}`;
                        const hFinFmt = minutosAHora(claseFin);
                        mostrarAlerta('conflicto',
                            `${profesor} ya tiene clase en ese horario`,
                            `Está en el salón ${salonEx} con "${clase.materia || '–'}" ` +
                            `de ${horaClase.substring(0,5)} a ${hFinFmt}.`,
                            `📅 ${fecha}   🕐 ${horaClase.substring(0,5)} – ${hFinFmt}`
                        );
                        return;
                    }
                }
            }

            if (!hayAnticipacion) ocultarAlerta();
        } catch (err) {
            if (err.name !== 'AbortError') console.warn('[Classify] Error verificando conflictos:', err);
        }
    }

    gradoSel.addEventListener('change', verificarConflicto);
    grupoInput.addEventListener('input', verificarConflicto);
    profesorSel.addEventListener('change', verificarConflicto);
    duracionSel.addEventListener('change', verificarConflicto);

    /* ══════════════════════════════════════════════════════════════════
       4. RELACIÓN AUTOMÁTICA PROFESOR ↔ MATERIA
       ══════════════════════════════════════════════════════════════════ */

    /** Guarda las opciones originales del select de profesores */
    const opcionesOrigProfesor = Array.from(profesorSel.options).map(o => ({
        value: o.value,
        text:  o.text
    }));

    /** Restaura todas las opciones del select de profesores */
    function restaurarProfesores() {
        profesorSel.innerHTML = '';
        opcionesOrigProfesor.forEach(({ value, text }) => {
            const opt = new Option(text, value);
            profesorSel.appendChild(opt);
        });
    }

    /** Al cambiar de profesor → auto-selecciona su materia */
    profesorSel.addEventListener('change', function () {
        if (ignorarCambioProfesor) return;
        const materia = PROFESOR_MATERIA[this.value];
        if (materia) {
            ignorarCambioMateria = true;
            materiaSel.value = materia;
            ignorarCambioMateria = false;
        }
        verificarConflicto();
    });

    /** Al cambiar de materia → filtra los profesores que la imparten */
    materiaSel.addEventListener('change', function () {
        if (ignorarCambioMateria) return;
        const materia = this.value;

        ignorarCambioProfesor = true;
        restaurarProfesores();
        ignorarCambioProfesor = false;

        if (!materia) return; // "Selecciona la materia" → lista completa

        const profsDeMateria = MATERIA_PROFESORES[materia] || [];
        if (profsDeMateria.length === 0) return; // materia sin prof asignado → lista completa

        // Ocultar los profesores que no imparten esta materia
        Array.from(profesorSel.options).forEach(opt => {
            if (opt.value === '') return; // siempre dejar el placeholder
            if (!profsDeMateria.includes(opt.value)) {
                opt.style.display = 'none';
                opt.disabled = true;
            }
        });

        // Si el profesor ya elegido imparte la materia, mantenerlo; si no, limpiar
        if (profesorSel.value && !profsDeMateria.includes(profesorSel.value)) {
            profesorSel.value = '';
        }

        // Si hay un único profesor para esa materia, seleccionarlo automáticamente
        if (profsDeMateria.length === 1) {
            profesorSel.value = profsDeMateria[0];
            verificarConflicto();
        }
    });

    /* ══════════════════════════════════════════════════════════════════
       FUNCIONES DE ALERTA
       ══════════════════════════════════════════════════════════════════ */

    function mostrarAlerta(tipo, titulo, mensaje, detalle) {
        alertaBox.className = `alerta-horario ${tipo} visible`;
        alertaIcono.textContent   = tipo === 'conflicto' ? '⚠️' : '🚫';
        alertaTitulo.textContent  = titulo;
        alertaMensaje.textContent = mensaje;
        if (detalle) {
            alertaDetalle.textContent    = detalle;
            alertaDetalle.style.display  = 'inline-block';
        } else {
            alertaDetalle.style.display  = 'none';
        }
        alertaBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function ocultarAlerta() {
        alertaBox.className = 'alerta-horario';
    }

    /* ══════════════════════════════════════════════════════════════════
       BLOQUEO EN EL SUBMIT
       ══════════════════════════════════════════════════════════════════ */
    form.addEventListener('submit', function (e) {
        sincronizarHora();
        validarAnticipacion();
        if (hayAnticipacion) {
            e.preventDefault();
            mostrarAlerta('anticipacion',
                'No puedes programar con menos de 1 hora de anticipación',
                'Elige una hora que sea al menos 1 hora desde ahora.', null);
            horaHoras.focus();
            return;
        }
        if (hayConflicto) {
            e.preventDefault();
            mostrarAlerta('conflicto',
                'Conflicto de horario sin resolver',
                'El profesor ya tiene clase en ese horario. Por favor elige otra hora o fecha.', null);
            horaHoras.focus();
        }
    });

    /* ══════════════════════════════════════════════════════════════════
       LIMPIAR AL RESETEAR
       ══════════════════════════════════════════════════════════════════ */
    form.addEventListener('reset', function () {
        hayConflicto    = false;
        hayAnticipacion = false;
        ocultarAlerta();
        horaPickerContainer.classList.remove('campo-invalido');
        horaHint.className   = 'hora-hint';
        horaHint.textContent = '';
        setTimeout(function () {
            horaHoras.value    = '';
            horaMinutos.value  = '00';
            horaInput.value    = '';
        }, 0);
        restaurarProfesores();
    });

    /* Bloquear fechas pasadas al cargar */
    fechaInput.min = new Date().toISOString().split('T')[0];

})();