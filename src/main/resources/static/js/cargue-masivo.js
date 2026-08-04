// Cargue masivo de clases (CLS-133): sube la plantilla diligenciada y pinta
// el resultado fila a fila. El filtro CSRF de la app parchea fetch() y añade
// X-CSRF-TOKEN solo, también para FormData.
document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('archivoCargueMasivo');
    const boton = document.getElementById('btnCargueMasivo');
    const resultado = document.getElementById('cargueResultado');
    if (!input || !boton || !resultado) return;

    boton.addEventListener('click', () => input.click());

    input.addEventListener('change', async () => {
        const archivo = input.files[0];
        if (!archivo) return;

        boton.disabled = true;
        const textoOriginal = boton.textContent;
        boton.textContent = 'Cargando...';
        resultado.className = 'cargue-resultado';
        resultado.innerHTML = '';

        try {
            const datos = new FormData();
            datos.append('archivo', archivo);

            const respuesta = await fetch('/agenda/cargue-masivo', {
                method: 'POST',
                body: datos
            });
            const json = await respuesta.json().catch(() => ({}));

            if (!respuesta.ok) {
                mostrarError(json.message || 'No fue posible procesar el archivo.');
            } else {
                mostrarResumen(json);
            }
        } catch (e) {
            mostrarError('No fue posible conectar con el servidor. Inténtalo de nuevo.');
        } finally {
            boton.disabled = false;
            boton.textContent = textoOriginal;
            input.value = '';
        }
    });

    function mostrarResumen(json) {
        const fallidas = json.fallidas || [];
        let html = '';
        if (json.guardadas > 0) {
            html += `<p class="cargue-ok">✅ ${json.guardadas} de ${json.total} clases agendadas correctamente.</p>`;
        }
        if (fallidas.length > 0) {
            html += `<p class="cargue-error">⚠️ ${fallidas.length} fila(s) no se pudieron agendar:</p><ul>`;
            for (const f of fallidas) {
                html += `<li>Fila ${escapar(String(f.fila))}: ${escapar(f.mensaje || 'error desconocido')}</li>`;
            }
            html += '</ul>';
        }
        resultado.innerHTML = html;
        resultado.classList.add('visible');
    }

    function mostrarError(mensaje) {
        resultado.innerHTML = `<p class="cargue-error">⚠️ ${escapar(mensaje)}</p>`;
        resultado.classList.add('visible');
    }

    function escapar(texto) {
        const div = document.createElement('div');
        div.textContent = texto;
        return div.innerHTML;
    }
});
