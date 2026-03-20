document.addEventListener('DOMContentLoaded', function() {
    const formCambiarPassword = document.getElementById('formCambiarPassword');
    const mensajeError = document.getElementById('mensajeError');
    const mensajeExito = document.getElementById('mensajeExito');
    const modalOverlay = document.getElementById('modalCambiarPassword');

    if (formCambiarPassword) {
        formCambiarPassword.addEventListener('submit', function(e) {
            e.preventDefault();

            // Limpiar mensajes anteriores
            mensajeError.style.display = 'none';
            mensajeExito.style.display = 'none';

            // Obtener valores
            const nuevaPassword = document.getElementById('nueva_password').value;
            const confirmarPassword = document.getElementById('confirmar_password').value;

            // Validaciones en el frontend
            if (nuevaPassword.length < 6) {
                mostrarError('La contraseña debe tener al menos 6 caracteres');
                return;
            }

            if (nuevaPassword !== confirmarPassword) {
                mostrarError('Las contraseñas no coinciden');
                return;
            }

            // Deshabilitar el botón mientras se procesa
            const btnSubmit = formCambiarPassword.querySelector('button[type="submit"]');
            const textoOriginal = btnSubmit.textContent;
            btnSubmit.disabled = true;
            btnSubmit.textContent = 'Procesando...';

            // Usar la URL que viene desde PHP
            const url = window.cambiarPasswordUrl || '/classify/cambiar-password';

            console.log('URL completa:', url);

            // Enviar datos al servidor usando XMLHttpRequest para mejor compatibilidad
            const xhr = new XMLHttpRequest();
            xhr.open('POST', url, true);
            xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');

            xhr.onload = function() {
                btnSubmit.disabled = false;
                btnSubmit.textContent = textoOriginal;

                console.log('Status:', xhr.status);
                console.log('Response:', xhr.responseText);

                if (xhr.status === 200) {
                    try {
                        const data = JSON.parse(xhr.responseText);
                        console.log('Data parsed:', data);

                        if (data.success) {
                            mostrarExito(data.message);

                            // Cerrar el modal después de 2 segundos
                            setTimeout(() => {
                                if (modalOverlay) {
                                    modalOverlay.style.display = 'none';
                                }
                            }, 2000);
                        } else {
                            mostrarError(data.message || 'Error desconocido');
                        }
                    } catch (e) {
                        console.error('Error parseando JSON:', e);
                        mostrarError('Error al procesar la respuesta del servidor');
                    }
                } else {
                    mostrarError('Error del servidor (código: ' + xhr.status + ')');
                }
            };

            xhr.onerror = function() {
                btnSubmit.disabled = false;
                btnSubmit.textContent = textoOriginal;
                console.error('Error de red');
                mostrarError('Error de conexión. Por favor intente nuevamente.');
            };

            // Preparar datos
            const formData = new FormData();
            formData.append('nueva_password', nuevaPassword);
            formData.append('confirmar_password', confirmarPassword);

            xhr.send(formData);
        });
    }

    function mostrarError(mensaje) {
        mensajeError.textContent = mensaje;
        mensajeError.style.display = 'block';
        mensajeExito.style.display = 'none';
    }

    function mostrarExito(mensaje) {
        mensajeExito.textContent = mensaje;
        mensajeExito.style.display = 'block';
        mensajeError.style.display = 'none';
    }
});
