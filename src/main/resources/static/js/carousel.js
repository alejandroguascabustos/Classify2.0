/**
 * Carousel.js - Carrusel automático para la página de inicio
 * Classify - Sistema de gestión escolar
 */

document.addEventListener('DOMContentLoaded', function() {
    const slides = document.querySelectorAll('.carousel-slide');
    const indicators = document.querySelectorAll('.indicator');
    let currentSlide = 0;
    let autoplayInterval;

    // Función para cambiar de slide
    function goToSlide(index) {
        // Remover clase active de todos los slides e indicadores
        slides.forEach(slide => slide.classList.remove('active'));
        indicators.forEach(indicator => indicator.classList.remove('active'));

        // Agregar clase active al slide e indicador actual
        slides[index].classList.add('active');
        indicators[index].classList.add('active');

        currentSlide = index;
    }

    // Función para ir al siguiente slide
    function nextSlide() {
        let next = (currentSlide + 1) % slides.length;
        goToSlide(next);
    }

    // Función para ir al slide anterior
    function prevSlide() {
        let prev = (currentSlide - 1 + slides.length) % slides.length;
        goToSlide(prev);
    }

    // Iniciar autoplay
    function startAutoplay() {
        autoplayInterval = setInterval(nextSlide, 4000); // Cambiar cada 4 segundos
    }

    // Detener autoplay
    function stopAutoplay() {
        clearInterval(autoplayInterval);
    }

    // Event listeners para los indicadores
    indicators.forEach((indicator, index) => {
        indicator.addEventListener('click', () => {
            stopAutoplay();
            goToSlide(index);
            startAutoplay(); // Reiniciar autoplay después del click
        });
    });

    // Pausar autoplay cuando el mouse está sobre el carrusel
    const carouselContainer = document.querySelector('.carousel-container');
    if (carouselContainer) {
        carouselContainer.addEventListener('mouseenter', stopAutoplay);
        carouselContainer.addEventListener('mouseleave', startAutoplay);
    }

    // Iniciar el carrusel
    startAutoplay();

    // Animación de aparición para las tarjetas del ecosistema
    const ecoCards = document.querySelectorAll('.eco-card');

    // Intersection Observer para animar las tarjetas cuando entran en viewport
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver(function(entries) {
        entries.forEach((entry, index) => {
            if (entry.isIntersecting) {
                setTimeout(() => {
                    entry.target.style.opacity = '1';
                    entry.target.style.transform = 'translateY(0)';
                }, index * 100); // Delay escalonado
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    // Aplicar estilos iniciales y observar las tarjetas
    ecoCards.forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(30px)';
        card.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(card);
    });


    // Animación suave para los enlaces internos (EXCEPTO modales y cierres)
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        const href = this.getAttribute('href');
        const target = document.querySelector(href);
        
        // Solo aplicar smooth scroll si el target existe Y no es un modal
        // Los modales usan :target de CSS, no necesitan scroll
        if (target && !target.classList.contains('info-panel') && href !== '#') {
            e.preventDefault();
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

    // Efecto parallax sutil en el hero
    window.addEventListener('scroll', function() {
        const scrolled = window.pageYOffset;
        const heroContent = document.querySelector('.hero-content');
        const carouselContainer = document.querySelector('.carousel-container');

        if (heroContent && scrolled < 600) {
            heroContent.style.transform = `translateY(${scrolled * 0.3}px)`;
        }

        if (carouselContainer && scrolled < 600) {
            carouselContainer.style.transform = `translateY(${scrolled * -0.2}px)`;
        }
    });

    // Efecto hover en las tarjetas del ecosistema
    ecoCards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)';
        });

        card.addEventListener('mouseleave', function() {
            this.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)';
        });
    });

    // Contador animado para estadísticas (si agregas números en el futuro)
    function animateCounter(element, target, duration = 2000) {
        let start = 0;
        const increment = target / (duration / 16);

        const timer = setInterval(() => {
            start += increment;
            if (start >= target) {
                element.textContent = Math.round(target);
                clearInterval(timer);
            } else {
                element.textContent = Math.round(start);
            }
        }, 16);
    }

    // Detectar cuando el usuario llega al final de la página para mostrar más contenido
    let lastScrollTop = 0;
    window.addEventListener('scroll', function() {
        let scrollTop = window.pageYOffset || document.documentElement.scrollTop;

        // Detectar dirección del scroll
        if (scrollTop > lastScrollTop) {
            // Scrolling down
            document.body.classList.add('scrolling-down');
            document.body.classList.remove('scrolling-up');
        } else {
            // Scrolling up
            document.body.classList.add('scrolling-up');
            document.body.classList.remove('scrolling-down');
        }

        lastScrollTop = scrollTop <= 0 ? 0 : scrollTop;
    }, false);

    // Prevenir el comportamiento predeterminado en dispositivos móviles para mejorar el rendimiento
    let touchStartX = 0;
    let touchEndX = 0;

    if (carouselContainer) {
        carouselContainer.addEventListener('touchstart', e => {
            touchStartX = e.changedTouches[0].screenX;
        });

        carouselContainer.addEventListener('touchend', e => {
            touchEndX = e.changedTouches[0].screenX;
            handleSwipe();
        });

        function handleSwipe() {
            if (touchEndX < touchStartX - 50) {
                // Swipe left
                stopAutoplay();
                nextSlide();
                startAutoplay();
            }

            if (touchEndX > touchStartX + 50) {
                // Swipe right
                stopAutoplay();
                prevSlide();
                startAutoplay();
            }
        }
    }

    // Log para debug
    console.log('Carousel initialized with', slides.length, 'slides');
});
