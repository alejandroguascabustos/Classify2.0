// Chat de Ayuda - Classify
class ClassifyChat {
    constructor() {
        this.chatOpen = false;
        this.conversationStarted = false;
        this.currentStep = 'welcome';
        this.init();
    }

    init() {
        this.createChatElements();
        this.attachEventListeners();
    }

    createChatElements() {
        const chatHTML = `
            <!-- Botón flotante del chat -->
            <div class="chat-bubble" id="chatBubble">
                <img src="${this.getBaseUrl()}img/img.gif" alt="Ayuda">
            </div>

            <!-- Contenedor del chat -->
            <div class="chat-container" id="chatContainer">
                <!-- Modal de bienvenida -->
                <div class="chat-welcome-modal" id="chatWelcomeModal">
                    <img src="${this.getBaseUrl()}img/img.gif" alt="Asistente Classify">
                    <h2>¡Hola! Soy tu Asistente</h2>
                    <p>Estoy aquí para ayudarte con cualquier duda sobre Classify. Antes de comenzar, te invito a revisar nuestra <a href="https://drive.google.com/file/d/1MK4acZ-7dLlQMEUQYd6oThOaQEPmCiIA/view?usp=sharing" target="_blank" style="color: #008000; font-weight: bold; text-decoration: underline;">política de datos</a>.</p>
                    <div class="chat-welcome-buttons">
                        <button class="chat-accept-btn" id="chatAcceptBtn">Acepto</button>
                        <button class="chat-decline-btn" id="chatDeclineBtn">No acepto</button>
                    </div>
                </div>

                <!-- Chat principal -->
                <div class="chat-header">
                    <img src="${this.getBaseUrl()}img/img.gif" alt="Asistente">
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
        const chatBody = document.getElementById('chatBody');

        setTimeout(() => {
            this.addBotMessage('¡Bienvenido a Classify! 👋 Estoy aquí para ayudarte.');
        }, 500);

        setTimeout(() => {
            this.showMainMenu();
        }, 1500);
    }

    showMainMenu() {
        this.addBotMessage('¿En qué puedo ayudarte hoy? Selecciona una opción:');

        const options = [
            { text: '📚 ¿Cómo crear una clase?', action: 'crear_clase' },
            { text: '🎌 Información sobre la izada de bandera', action: 'izada_bandera' },
            { text: '📤 ¿Cómo cargar materiales?', action: 'cargar_materiales' },
            { text: '⭐ ¿Cómo calificar a un profesor?', action: 'calificar_profesor' },
            { text: '🎓 ¿Cómo aprender en la plataforma?', action: 'aprender_plataforma' },
            { text: '📅 ¿Cómo agendar una clase?', action: 'agendar_clase' },
            { text: '👤 Gestión de perfil', action: 'gestion_perfil' },
            { text: '💬 Contactar con Soporte', action: 'contactar_soporte' },
            { text: '❓ Otras preguntas', action: 'otras_preguntas' }
        ];

        this.addOptions(options);
    }

    addBotMessage(message) {
        const chatBody = document.getElementById('chatBody');
        const messageHTML = `
            <div class="chat-message message-bot">
                <img src="${this.getBaseUrl()}img/img.gif" alt="Bot">
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

    addOptions(options) {
        const chatBody = document.getElementById('chatBody');
        const optionsHTML = `
            <div class="chat-message">
                <div class="chat-options">
                    ${options.map(opt => `
                        <button class="chat-option-btn" data-action="${opt.action}">
                            ${opt.text}
                        </button>
                    `).join('')}
                </div>
            </div>
        `;
        chatBody.insertAdjacentHTML('beforeend', optionsHTML);
        this.scrollToBottom();

        // Attach click events only to the newly added buttons
        const optionsContainer = chatBody.lastElementChild;
        const optionButtons = optionsContainer.querySelectorAll('.chat-option-btn');

        optionButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                // Prevent multiple clicks on the same set of options
                if (optionsContainer.getAttribute('data-used')) return;
                optionsContainer.setAttribute('data-used', 'true');

                // Disable other buttons in this set
                optionButtons.forEach(b => {
                    b.classList.add('used');
                    b.style.pointerEvents = 'none';
                });

                const action = btn.getAttribute('data-action');
                this.handleOption(action, btn.textContent.trim());
            });
        });
    }

    handleOption(action, buttonText) {
        this.addUserMessage(buttonText);

        setTimeout(() => {
            this.showTypingIndicator();

            setTimeout(() => {
                this.removeTypingIndicator();

                switch (action) {
                    case 'crear_clase':
                        this.explainCrearClase();
                        break;
                    case 'izada_bandera':
                        this.explainIzadaBandera();
                        break;
                    case 'cargar_materiales':
                        this.explainCargarMateriales();
                        break;
                    case 'calificar_profesor':
                        this.explainCalificarProfesor();
                        break;
                    case 'aprender_plataforma':
                        this.explainAprenderPlataforma();
                        break;
                    case 'agendar_clase':
                        this.explainAgendarClase();
                        break;
                    case 'gestion_perfil':
                        this.explainGestionPerfil();
                        break;
                    case 'contactar_soporte':
                        this.showContactForm();
                        break;
                    case 'otras_preguntas':
                        this.explainOtrasPreguntas();
                        break;
                    case 'menu_principal':
                        this.showMainMenu();
                        break;
                    case 'finalizar':
                        this.addBotMessage('¡Fue un placer ayudarte! 😊 Si necesitas algo más, no dudes en escribirme.');
                        break;
                    default:
                        this.showMainMenu();
                }
            }, 1500);
        }, 500);
    }

    explainCrearClase() {
        this.addBotMessage('Para crear una clase en Classify, sigue estos pasos:');
        this.addBotMessage('1. Inicia sesión como docente o coordinador<br>2. Ve al menú "Programación"<br>3. Haz clic en "Nueva Clase"<br>4. Completa los datos: materia, hora, curso y observaciones<br>5. Guarda la información');
        this.askForMore();
    }

    explainIzadaBandera() {
        this.addBotMessage('La Izada de Bandera es un evento importante en nuestra institución:');
        this.addBotMessage('📍 Puedes ver la información y programación en la sección "Izada" del menú principal.<br>🎌 Allí encontrarás fechas, horarios y responsables de cada ceremonia.<br>📝 Los estudiantes pueden ver su participación y detalles del evento.');
        this.askForMore();
    }

    explainCargarMateriales() {
        this.addBotMessage('Para cargar materiales educativos:');
        this.addBotMessage('1. Accede al menú "Materiales"<br>2. Haz clic en "Subir Material"<br>3. Selecciona el archivo desde tu computador (PDF, PPTX, DOCX, etc.)<br>4. Agrega un título descriptivo<br>5. Confirma la carga');
        this.addBotMessage('Los materiales quedaran disponibles para que los estudiantes los descarguen.');
        this.askForMore();
    }

    explainCalificarProfesor() {
        this.addBotMessage('Para calificar a un profesor:');
        this.addBotMessage('1. Ve a la sección "Califica"<br>2. Selecciona el profesor que deseas evaluar<br>3. Completa los criterios de evaluación<br>4. Agrega comentarios opcionales<br>5. Envía tu calificación');
        this.addBotMessage('Tu opinión es valiosa y ayuda a mejorar la calidad educativa. 📝⭐');
        this.askForMore();
    }

    explainAprenderPlataforma() {
        this.addBotMessage('En la sección "Aprende" encontrarás:');
        this.addBotMessage('📚 Materiales de estudio organizados por materia<br>🎥 Videos educativos<br>📝 Recursos complementarios<br>💡 Guías y tutoriales<br>📖 Material de consulta');
        this.addBotMessage('Explora todo el contenido disponible para complementar tu aprendizaje.');
        this.askForMore();
    }

    explainAgendarClase() {
        this.addBotMessage('Para agendar una clase o consulta:');
        this.addBotMessage('1. Dirígete a la sección "Agendar"<br>2. Selecciona la fecha y hora disponible<br>3. Elige el profesor o materia<br>4. Describe el motivo de la cita<br>5. Confirma tu agendamiento');
        this.addBotMessage('Recibirás una confirmación y recordatorio de tu cita. 📅✅');
        this.askForMore();
    }

    explainGestionPerfil() {
        this.addBotMessage('Para gestionar tu perfil:');
        this.addBotMessage('1. Haz clic en "Perfil" en el menú<br>2. Podrás actualizar tu información personal<br>3. Cambiar tu contraseña<br>4. Ver tu historial de actividades<br>5. Configurar preferencias');
        this.askForMore();
    }

    explainOtrasPreguntas() {
        this.addBotMessage('Para otras consultas, puedes:');
        this.addBotMessage('📧 Contactar al soporte técnico en la sección "Soporte"<br>📞 Comunicarte con el colegio en "Contacto"<br>📋 Revisar las políticas y términos en el pie de página');
        this.askForMore();
    }

    askForMore() {
        setTimeout(() => {
            this.addBotMessage('¿Hay algo más en lo que pueda ayudarte?');
            const options = [
                { text: '🏠 Volver al menú principal', action: 'menu_principal' },
                { text: '✅ No, gracias', action: 'finalizar' }
            ];
            this.addOptions(options);
        }, 1000);
    }

    sendMessage() {
        const chatInput = document.getElementById('chatInput');
        const message = chatInput.value.trim();

        if (message === '') return;

        this.addUserMessage(message);
        chatInput.value = '';

        setTimeout(() => {
            this.showTypingIndicator();

            setTimeout(() => {
                this.removeTypingIndicator();
                this.addBotMessage('Gracias por tu mensaje. Para brindarte una mejor ayuda, por favor selecciona una opción del menú:');
                this.showMainMenu();
            }, 1500);
        }, 500);
    }

    showTypingIndicator() {
        const chatBody = document.getElementById('chatBody');
        const typingHTML = `
            <div class="chat-message message-bot" id="typingIndicator">
                <img src="${this.getBaseUrl()}img/img.gif" alt="Bot">
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
                this.addBotMessage('✅ ¡Tu consulta ha sido enviada exitosamente! Recibirás una respuesta en tu correo electrónico pronto.');
                document.getElementById('supportContactForm').remove();
                this.askForMore();
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
