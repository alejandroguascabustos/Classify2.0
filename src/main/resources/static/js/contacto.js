// Logic for the main Contact page
document.addEventListener('DOMContentLoaded', () => {
    const contactForm = document.querySelector('.contacto-form');
    
    if (contactForm) {
        contactForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const submitBtn = contactForm.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            
            // Get form data
            const formData = {
                nombre: document.getElementById('nombre').value,
                correo: document.getElementById('correo').value,
                mensaje: document.getElementById('mensaje').value,
                tipo: 'contacto_directo',
                fecha: new Date().toISOString()
            };
            
            // UI Feedback
            submitBtn.disabled = true;
            submitBtn.textContent = 'Enviando...';
            
            try {
                const response = await fetch('https://n8n.classify.in.net/webhook/5dbffea7-2dc0-4085-a152-eb981f99da1a', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(formData)
                });
                
                if (response.ok) {
                    alert('¡Mensaje enviado con éxito! Nuestro equipo jurídico-pedagógico te responderá pronto.');
                    contactForm.reset();
                } else {
                    throw new Error('Error en la respuesta del servidor');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('Hubo un error al enviar tu mensaje. Por favor intenta de nuevo.');
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            }
        });
    }
});
