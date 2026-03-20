// Funciones únicas para el modal de agendamiento
class AgendaModal {
    constructor() {
        this.modal = document.getElementById('agendaConfirmModal');
        this.init();
    }
    
    init() {
        this.bindEvents();
    }
    
    bindEvents() {
        // Manejar envío del formulario de agendamiento
        const agendaForm = document.getElementById('agendaForm');
        if (agendaForm) {
            agendaForm.addEventListener('submit', this.handleFormSubmit.bind(this));
        }
        
        // Cerrar modal al hacer click fuera
        if (this.modal) {
            this.modal.addEventListener('click', (e) => {
                if (e.target === this.modal) {
                    this.hide();
                }
            });
        }
    }
    
    handleFormSubmit(event) {
        event.preventDefault();
        
        const form = event.target;
        const formData = new FormData(form);
        
        // Mostrar loading state
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.textContent = 'Guardando...';
        submitBtn.disabled = true;
        
        // Enviar via AJAX
        fetch(form.action, {
            method: 'POST',
            body: formData,
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Error en la respuesta del servidor');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                this.show();
                form.reset();
                
                // Redirigir después de 1.5 segundos - USANDO baseUrl
                setTimeout(() => {
                    // Usar la variable baseUrl definida globalmente
                    const redirectUrl = (typeof baseUrl !== 'undefined' ? baseUrl : '') + '/programacion';
                    console.log('Redirigiendo a:', redirectUrl);
                    window.location.href = redirectUrl;
                }, 1500);
            } else {
                this.showError(data.message || 'Error al guardar la clase');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            // Si falla AJAX, enviar el formulario de forma normal
            this.showError('Error de conexión. Enviando formulario de forma normal...');
            setTimeout(() => {
                form.submit();
            }, 1500);
        })
        .finally(() => {
            // Restaurar botón
            submitBtn.textContent = originalText;
            submitBtn.disabled = false;
        });
    }
    
    show() {
        if (this.modal) {
            this.modal.style.display = 'block';
            
            // Reiniciar animación de la barra de progreso
            const progressBar = this.modal.querySelector('.agenda-progress-bar');
            if (progressBar) {
                progressBar.style.animation = 'none';
                void progressBar.offsetWidth; // Trigger reflow
                progressBar.style.animation = 'agendaProgress 3s linear forwards';
            }
        }
    }
    
    hide() {
        if (this.modal) {
            this.modal.style.display = 'none';
        }
    }
    
    showError(message) {
        // Crear un toast de error temporal
        const errorToast = document.createElement('div');
        errorToast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #f44336;
            color: white;
            padding: 15px 20px;
            border-radius: 5px;
            z-index: 10000;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        `;
        errorToast.textContent = message;
        document.body.appendChild(errorToast);
        
        setTimeout(() => {
            document.body.removeChild(errorToast);
        }, 5000);
    }
}

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    new AgendaModal();
});