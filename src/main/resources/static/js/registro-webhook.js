/**
 * Classify – Registro Webhook Handler
 * Intercepta el formulario de registro y envía los datos al webhook de n8n.
 */
document.addEventListener('DOMContentLoaded', () => {
    const registroForm = document.querySelector('.form-registro');
    if (!registroForm) return;

    registroForm.addEventListener('submit', (e) => {
        // Solo enviar si el formulario es válido
        if (!registroForm.checkValidity()) return;

        const nombreInput = registroForm.querySelector('input[name="nombre"]');
        const apellidoInput = registroForm.querySelector('input[name="apellido"]');
        const emailInput = registroForm.querySelector('input[name="correo"]');

        const nombre = (nombreInput?.value || '').trim();
        const apellido = (apellidoInput?.value || '').trim();
        const correo = (emailInput?.value || '').trim();
        const usuario = (registroForm.querySelector('input[name="nombre_usu"]')?.value || '').trim();

        if (!correo) return;

        // Webhook publico de registro
        const webhookUrl = 'https://n8n.classify.in.net/webhook/bbab4100-3e6e-44cd-98e6-f62e6d3f65af';

        // Enviar webhook en segundo plano (no bloquea el submit del form)
        const payload = {
            nombre: nombre,
            apellido: apellido,
            correo: correo,
            nombre_usuario: usuario,
            nombre_completo: `${nombre} ${apellido}`.trim(),
            source: 'registro_publico'
        };

        // Enviar los datos al webhook de n8n
        fetch(webhookUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload),
            mode: 'no-cors', // Usamos no-cors para evitar problemas de CORS
            keepalive: true  // Asegura que la petición se complete aunque la página cambie
        }).catch(err => console.error('Error enviando al webhook:', err));
        
        // También enviamos al endpoint local original si es necesario, 
        // pero el usuario solo pidió el de n8n.
    });
});
