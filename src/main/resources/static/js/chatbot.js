// Chat de Ayuda - Classify
class ClassifyChat {
    constructor() {
        this.chatOpen = false;
        this.conversationStarted = false;
        this.currentStep = 'welcome';
        // Historial que se envía al asistente inteligente (pares rol/texto)
        this.historial = [];
        this.esperandoRespuesta = false;
        this.init();
    }

    init() {
        this.createChatElements();
        this.attachEventListeners();
    }

    getAssistantImage() {
        return `${this.getBaseUrl()}img/help-me-lord-help-me.gif`;
    }

    createChatElements() {
        const chatHTML = `
            <!-- Botón flotante del chat -->
            <div class="chat-bubble" id="chatBubble">
                <img src="${this.getAssistantImage()}" alt="Ayuda">
            </div>

            <!-- Contenedor del chat -->
            <div class="chat-container" id="chatContainer">
                <!-- Modal de bienvenida -->
                <div class="chat-welcome-modal" id="chatWelcomeModal">
                    <img src="${this.getAssistantImage()}" alt="Asistente Classify">
                    <h2>¡Hola! Soy tu Asistente</h2>
                    <p>Estoy aquí para ayudarte con cualquier duda sobre Classify. Antes de comenzar, te invito a revisar nuestra <a href="https://drive.google.com/file/d/1MK4acZ-7dLlQMEUQYd6oThOaQEPmCiIA/view?usp=sharing" target="_blank" style="color: #008000; font-weight: bold; text-decoration: underline;">política de datos</a>.</p>
                    <div class="chat-welcome-buttons">
                        <button class="chat-accept-btn" id="chatAcceptBtn">Acepto</button>
                        <button class="chat-decline-btn" id="chatDeclineBtn">No acepto</button>
                    </div>
                </div>

                <!-- Chat principal -->
                <div class="chat-header">
                    <img src="${this.getAssistantImage()}" alt="Asistente">
                    <div class="chat-header-info">
                        <h3>Asistente Classify</h3>
                        <p>En línea</p>
                    </div>
                    <button class="chat-close" id="chatClose">×</button>
                </div>

                <div class="chat-body" id="chatBody">
                    <!-- Los mensajes se agregarán aquí dinámicamente -->
                </div>

                <div class="chat-input-container">
                    <input type="text" class="chat-input" id="chatInput" placeholder="Escribe tu mensaje...">
                    <button class="chat-send-btn" id="chatSendBtn">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
                        </svg>
                    </button>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', chatHTML);
    }

    getBaseUrl() {
        // Derivar la base real desde el href del CSS principal (evita errores tipo /public/... en producción)
        const styleLink = document.querySelector('link[rel="stylesheet"][href*="css/style.css"]');
        if (styleLink) {
            try {
                const href = styleLink.getAttribute('href') || '';
                const url = new URL(href, window.location.href);
                let basePath = url.pathname.replace(/css\/style\.css.*$/, '');
                if (!basePath.endsWith('/')) basePath += '/';
                return basePath;
            } catch (_) {
                // fallback abajo
            }
        }

        // Fallback: en CI4 con DocumentRoot apuntando a /public, la base es "/"
        return '/';
    }

    attachEventListeners() {
        const chatBubble = document.getElementById('chatBubble');
        const chatContainer = document.getElementById('chatContainer');
        const chatClose = document.getElementById('chatClose');
        const chatAcceptBtn = document.getElementById('chatAcceptBtn');
        const chatDeclineBtn = document.getElementById('chatDeclineBtn');
        const chatSendBtn = document.getElementById('chatSendBtn');
        const chatInput = document.getElementById('chatInput');

        chatBubble.addEventListener('click', () => this.toggleChat());
        chatClose.addEventListener('click', () => this.closeChat());
        chatAcceptBtn.addEventListener('click', () => this.acceptPolicy());
        chatDeclineBtn.addEventListener('click', () => this.declinePolicy());
        chatSendBtn.addEventListener('click', () => this.sendMessage());
        chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.sendMessage();
        });
    }

    toggleChat() {
        const chatContainer = document.getElementById('chatContainer');
        this.chatOpen = !this.chatOpen;

        if (this.chatOpen) {
            chatContainer.classList.add('active');
        } else {
            chatContainer.classList.remove('active');
        }
    }

    closeChat() {
        const chatContainer = document.getElementById('chatContainer');
        chatContainer.classList.remove('active');
        this.chatOpen = false;
    }

    acceptPolicy() {
        const modal = document.getElementById('chatWelcomeModal');
        modal.classList.add('hidden');
        this.conversationStarted = true;
        this.showWelcomeMessage();
    }

    declinePolicy() {
        this.closeChat();
        setTimeout(() => {
            alert('Para usar el asistente, es necesario aceptar la política de datos.');
        }, 300);
    }

    showWelcomeMessage() {
        setTimeout(() => {
            this.addBotMessage('¡Hola! 👋 Soy el asistente de Classify. Pregúntame lo que necesites sobre la plataforma o el colegio y te lo explico.');
        }, 500);

        setTimeout(() => {
            this.addBotMessage('Por ejemplo: <em>"¿qué clases tiene 5°B mañana?"</em>, <em>"¿cómo recupero mi contraseña?"</em> o <em>"¿cómo cargo materiales?"</em>');
        }, 1300);
    }

    addBotMessage(message) {
        const chatBody = document.getElementById('chatBody');
        const messageHTML = `
            <div class="chat-message message-bot">
                <img src="${this.getAssistantImage()}" alt="Bot">
                <div class="message-content">
                    <p>${message}</p>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', messageHTML);
        this.scrollToBottom();
    }

    addUserMessage(message) {
        const chatBody = document.getElementById('chatBody');
        const messageHTML = `
            <div class="chat-message message-user">
                <div class="message-content">
                    <p>${message}</p>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', messageHTML);
        this.scrollToBottom();
    }

    // Convierte texto plano (posiblemente del modelo) en HTML seguro:
    // escapa etiquetas y conserva los saltos de línea.
    escapeHtml(texto) {
        const div = document.createElement('div');
        div.textContent = texto;
        return div.innerHTML.replace(/\n/g, '<br>');
    }

    sendMessage() {
        const chatInput = document.getElementById('chatInput');
        const message = chatInput.value.trim();

        if (message === '' || this.esperandoRespuesta) return;

        this.addUserMessage(this.escapeHtml(message));
        chatInput.value = '';
        this.askAssistant(message);
    }

    async askAssistant(message) {
        this.esperandoRespuesta = true;
        this.showTypingIndicator();

        try {
            // El filtro CSRF de la app parchea fetch() y añade X-CSRF-TOKEN solo.
            const response = await fetch(`${this.getBaseUrl()}api/chatbot`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    mensaje: message,
                    historial: this.historial
                })
            });

            const data = await response.json().catch(() => ({}));
            this.removeTypingIndicator();

            if (response.ok && data.success && data.respuesta) {
                // [SOPORTE] es una marca interna del asistente: significa que la
                // duda necesita ayuda humana. Se quita del texto y se ofrece el
                // formulario de contacto solo en ese caso.
                const necesitaSoporte = data.respuesta.includes('[SOPORTE]');
                const texto = data.respuesta.replace(/\[SOPORTE\]/g, '').trim();

                if (texto) this.addBotMessage(this.escapeHtml(texto));
                if (necesitaSoporte) this.offerSupport();

                this.historial.push({ rol: 'usuario', texto: message });
                this.historial.push({ rol: 'asistente', texto: texto });
                // Solo se conservan los últimos turnos: el servidor también recorta
                if (this.historial.length > 12) {
                    this.historial = this.historial.slice(-12);
                }
            } else if (response.status === 503) {
                // Asistente sin configurar: se ofrece el canal humano directamente
                this.addBotMessage('En este momento no puedo responderte en línea, pero puedes dejarnos tu consulta y te contestamos por correo:');
                this.offerSupport();
            } else {
                this.addBotMessage(this.escapeHtml(
                    (data && data.message) || 'No pude procesar tu mensaje. Inténtalo de nuevo en un momento.'));
            }
        } catch (error) {
            this.removeTypingIndicator();
            this.addBotMessage('No pude conectarme con el asistente. Revisa tu conexión e inténtalo de nuevo, o déjanos tu consulta:');
            this.offerSupport();
        } finally {
            this.esperandoRespuesta = false;
        }
    }

    // Botón único de "Contactar con soporte": solo aparece cuando el asistente
    // no pudo resolver la duda (marca [SOPORTE]), cuando no está disponible,
    // o cuando falla la conexión.
    offerSupport() {
        const chatBody = document.getElementById('chatBody');
        const html = `
            <div class="chat-message">
                <div class="chat-options">
                    <button class="chat-option-btn">💬 Contactar con soporte</button>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', html);

        const boton = chatBody.lastElementChild.querySelector('button');
        boton.addEventListener('click', () => {
            boton.classList.add('used');
            boton.style.pointerEvents = 'none';
            this.showContactForm();
        }, { once: true });

        this.scrollToBottom();
    }

    showTypingIndicator() {
        const chatBody = document.getElementById('chatBody');
        const typingHTML = `
            <div class="chat-message message-bot" id="typingIndicator">
                <img src="${this.getAssistantImage()}" alt="Bot">
                <div class="typing-indicator">
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', typingHTML);
        this.scrollToBottom();
    }

    removeTypingIndicator() {
        const indicator = document.getElementById('typingIndicator');
        if (indicator) {
            indicator.remove();
        }
    }

    showContactForm() {
        this.addBotMessage('📧 Para contactar con nuestro equipo de soporte, por favor completa el siguiente formulario:');

        const chatBody = document.getElementById('chatBody');
        const formHTML = `
            <div class="chat-message">
                <form class="support-contact-form" id="supportContactForm">
                    <div class="form-group">
                        <label for="supportName">Nombre completo:</label>
                        <input type="text" id="supportName" required placeholder="Tu nombre">
                    </div>
                    <div class="form-group">
                        <label for="supportEmail">Correo electrónico:</label>
                        <input type="email" id="supportEmail" required placeholder="tucorreo@ejemplo.com">
                    </div>
                    <div class="form-group">
                        <label for="supportQuestion">Tu pregunta o consulta:</label>
                        <textarea id="supportQuestion" required placeholder="Escribe tu consulta aquí..." rows="4"></textarea>
                    </div>
                    <button type="submit" class="support-submit-btn" id="supportSubmitBtn" disabled>
                        Enviar consulta
                    </button>
                </form>
            </div>
        `;

        chatBody.insertAdjacentHTML('beforeend', formHTML);
        this.scrollToBottom();

        // Agregar validación y evento de envío
        setTimeout(() => {
            const form = document.getElementById('supportContactForm');
            const nameInput = document.getElementById('supportName');
            const emailInput = document.getElementById('supportEmail');
            const questionInput = document.getElementById('supportQuestion');
            const submitBtn = document.getElementById('supportSubmitBtn');

            // Validar formulario
            const validateForm = () => {
                if (nameInput.value.trim() && emailInput.value.trim() && questionInput.value.trim()) {
                    submitBtn.disabled = false;
                    submitBtn.style.opacity = '1';
                    submitBtn.style.cursor = 'pointer';
                } else {
                    submitBtn.disabled = true;
                    submitBtn.style.opacity = '0.5';
                    submitBtn.style.cursor = 'not-allowed';
                }
            };

            nameInput.addEventListener('input', validateForm);
            emailInput.addEventListener('input', validateForm);
            questionInput.addEventListener('input', validateForm);

            // Enviar formulario
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.submitSupportForm(nameInput.value, emailInput.value, questionInput.value);
            });
        }, 100);
    }

    async submitSupportForm(name, email, question) {
        const submitBtn = document.getElementById('supportSubmitBtn');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Enviando...';

        try {
            const response = await fetch('https://n8n.classify.in.net/webhook/1b573e49-3bdc-4985-a993-ff2798735d57', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    nombre: name,
                    correo: email,
                    mensaje: question,
                    tema: "Chatbot Soporte",
                    rol: "Usuario Chatbot",
                    fecha: new Date().toISOString()
                })
            });

            if (response.ok) {
                this.addBotMessage('✅ ¡Tu consulta ha sido enviada exitosamente! Recibirás una respuesta en tu correo electrónico pronto. ¿Hay algo más en lo que pueda ayudarte?');
                document.getElementById('supportContactForm').remove();
            } else {
                throw new Error('Error al enviar');
            }
        } catch (error) {
            this.addBotMessage('❌ Hubo un error al enviar tu consulta. Por favor intenta nuevamente más tarde.');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Enviar consulta';
        }
    }

    scrollToBottom() {
        const chatBody = document.getElementById('chatBody');
        chatBody.scrollTop = chatBody.scrollHeight;
    }
}

// Inicializar el chat cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    new ClassifyChat();
});
