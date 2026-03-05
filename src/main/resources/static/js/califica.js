// Script para el sistema de calificación con estrellas
document.addEventListener('DOMContentLoaded', function() {

    const ratingContainers = document.querySelectorAll('.rating');

    ratingContainers.forEach(container => {
        const stars = container.querySelectorAll('.star');
        const criterioName = container.getAttribute('data-name');
        const hiddenInput = container.parentElement.querySelector('.rating-value');

        stars.forEach(star => {

            // Hover (resaltar)
            star.addEventListener('mouseenter', function() {
                const value = parseInt(this.getAttribute('data-value'));
                highlightStars(stars, value);
            });

            // Click (selecciona la calificación)
            star.addEventListener('click', function() {
                const value = parseInt(this.getAttribute('data-value'));
                hiddenInput.value = value;

                stars.forEach(s => s.classList.remove('active'));
                for (let i = 0; i < value; i++) {
                    stars[i].classList.add('active');
                }
            });
        });

        // Salir del contenedor (restaurar)
        container.addEventListener('mouseleave', function() {
            const currentValue = parseInt(hiddenInput.value) || 0;
            highlightStars(stars, currentValue);
        });
    });

    // Función para pintar estrellas
    function highlightStars(stars, value) {
        stars.forEach((star, index) => {
            star.style.color = index < value ? '#ffa500' : '#ddd';
        });
    }


    // Validación del formulario
    const form = document.getElementById('formCalifica');

    if (form) {
        form.addEventListener('submit', function(e) {

            const criterios = ['claridad', 'respeto', 'puntualidad', 'dominio', 'motivacion'];
            let faltantes = [];

            criterios.forEach(criterio => {
                const input = document.querySelector('input[name="${criterio}"]');
                if (!input || !input.value) {
                    const label = document.querySelector('.rating[data-name = "${criterio}"]')
                        .parentElement.querySelector('label').textContent;

                    faltantes.push(label);
                }
            });

            if (faltantes.length > 0) {
                e.preventDefault();
                alert("Falta calificar:\n" + faltantes.join(", "));
                return false;
            }

     });
    }
});