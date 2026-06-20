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
            return response.json().then(data => ({ status: response.status, data }));
        })
        .then(({ status, data }) => {
            if (status === 200 && data.success) {
                this.show();
                form.reset();
                setTimeout(() => {
                    const redirectUrl = (typeof baseUrl !== 'undefined' ? baseUrl : '') + '/programacion';
                    window.location.href = redirectUrl;
                }, 1500);
            } else if (status === 409 && data.conflicto) {
                this.showAlertaConflicto(data.message || 'Conflicto de horario detectado.');
            } else {
                this.showError(data.message || 'Error al guardar la clase');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            this.showError('Error de conexión. Reintentando de forma normal...');
            setTimeout(() => { form.submit(); }, 1500);
        })
        .finally(() => {
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
    
    showAlertaConflicto(message) {
        // Usa el bloque de alerta existente en agenda.html (mismo estilo visual)
        const alertaBox     = document.getElementById('alertaHorario');
        const alertaIcono   = document.getElementById('alertaIcono');
        const alertaTitulo  = document.getElementById('alertaTitulo');
        const alertaMensaje = document.getElementById('alertaMensaje');
        const alertaDetalle = document.getElementById('alertaDetalle');

        if (alertaBox) {
            alertaBox.className = 'alerta-horario conflicto visible';
            if (alertaIcono)   alertaIcono.textContent   = '🚫';
            if (alertaTitulo)  alertaTitulo.textContent  = 'Conflicto de horario';
            if (alertaMensaje) alertaMensaje.textContent = message;
            if (alertaDetalle) alertaDetalle.style.display = 'none';
            alertaBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        } else {
            this.showError(message);
        }
    }

    showError(message) {
        const errorToast = document.createElement('div');
        errorToast.style.cssText = `
            position: fixed; top: 20px; right: 20px;
            background: #ef4444; color: white;
            padding: 15px 20px; border-radius: 12px;
            z-index: 10000; box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            font-size: 0.9rem; max-width: 360px; line-height: 1.4;
        `;
        errorToast.textContent = message;
        document.body.appendChild(errorToast);
        setTimeout(() => { document.body.removeChild(errorToast); }, 5000);
    }
}

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    new AgendaModal();
});