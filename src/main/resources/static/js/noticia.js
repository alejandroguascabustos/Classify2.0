// Carrusel horizontal de noticias (Cartelera Informativa)
// El filtrado por tipo/fecha y la descarga en PDF se resuelven en el
// servidor (ver NoticiaController#verNoticias y #generarPdf) mediante el
// formulario GET #formFiltrosNoticias.
(function () {
    function initNoticiasCarousel() {
        const track = document.getElementById('noticiasScroll');
        const btnPrev = document.getElementById('btnNoticiaPrev');
        const btnNext = document.getElementById('btnNoticiaNext');

        if (!track || !btnPrev || !btnNext) return;

        function getScrollStep() {
            const primeraCard = track.querySelector('.noticia-card');
            if (!primeraCard) return track.clientWidth;
            const estilos = window.getComputedStyle(track);
            const gap = parseFloat(estilos.columnGap || estilos.gap || '0') || 0;
            return primeraCard.getBoundingClientRect().width + gap;
        }

        function actualizarBotones() {
            const maxScroll = track.scrollWidth - track.clientWidth - 1;
            btnPrev.disabled = track.scrollLeft <= 0;
            btnNext.disabled = track.scrollLeft >= maxScroll;
        }

        btnPrev.addEventListener('click', function () {
            track.scrollBy({ left: -getScrollStep(), behavior: 'smooth' });
        });

        btnNext.addEventListener('click', function () {
            track.scrollBy({ left: getScrollStep(), behavior: 'smooth' });
        });

        track.addEventListener('scroll', actualizarBotones, { passive: true });
        window.addEventListener('resize', actualizarBotones);

        actualizarBotones();
    }

    // ── "Ver más..." + modal de noticia completa (Cartelera) ──────────
    function initVerMasNoticias() {
        const overlay = document.getElementById('modalNoticiaOverlay');
        if (!overlay) return;

        const imgEl = document.getElementById('modalNoticiaImg');
        const tituloEl = document.getElementById('modalNoticiaTitulo');
        const metaEl = document.getElementById('modalNoticiaMeta');
        const cuerpoEl = document.getElementById('modalNoticiaCuerpo');
        const btnCerrar = document.getElementById('modalNoticiaCerrar');

        function abrirModal(card) {
            const tituloTexto = card.querySelector('.titlecontentnoticia')
                ? card.querySelector('.titlecontentnoticia').textContent.trim() : '';
            const contenidoEl = card.querySelector('.noticia-contenido');
            const cuerpoTexto = contenidoEl ? contenidoEl.textContent.trim() : '';
            const metaOriginal = card.querySelector('.noticia-meta');
            const imagen = card.querySelector('.noticia-card-img');

            tituloEl.textContent = tituloTexto;
            cuerpoEl.textContent = cuerpoTexto;
            metaEl.innerHTML = metaOriginal ? metaOriginal.innerHTML : '';

            if (imagen && imagen.getAttribute('src')) {
                imgEl.src = imagen.getAttribute('src');
                imgEl.style.display = 'block';
            } else {
                imgEl.removeAttribute('src');
                imgEl.style.display = 'none';
            }

            overlay.classList.add('activo');
            document.body.style.overflow = 'hidden';
        }

        function cerrarModal() {
            overlay.classList.remove('activo');
            document.body.style.overflow = '';
        }

        // Solo se muestra el botón "Ver más..." si el contenido realmente
        // se desborda de las 4 líneas visibles (line-clamp en CSS).
        document.querySelectorAll('.noticia-card').forEach(function (card) {
            const btn = card.querySelector('.noticia-ver-mas');
            const contenido = card.querySelector('.noticia-contenido');
            if (!btn || !contenido) return;

            if (contenido.scrollHeight > contenido.clientHeight + 2) {
                btn.style.display = 'inline-block';
            }

            btn.addEventListener('click', function () {
                abrirModal(card);
            });
        });

        if (btnCerrar) btnCerrar.addEventListener('click', cerrarModal);
        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) cerrarModal();
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && overlay.classList.contains('activo')) cerrarModal();
        });
    }

    function init() {
        initNoticiasCarousel();
        initVerMasNoticias();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

// Muestra preview de la imagen antes de guardar (formulario crear/editar noticia)
function previewNuevaImagen(input) {
    const preview = document.getElementById('nuevaPreview');
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            preview.src = e.target.result;
            preview.style.display = 'block';
        };
        reader.readAsDataURL(input.files[0]);
    } else {
        preview.style.display = 'none';
    }
}