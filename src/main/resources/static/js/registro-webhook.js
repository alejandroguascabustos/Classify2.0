/**
 * Classify – Registro Webhook Handler
 * Intercepta el formulario de registro y envía el correo de bienvenida
 * vía webhook de n8n después del registro exitoso.
 */
document.addEventListener('DOMContentLoaded', () => {
  const registroForm = document.querySelector('.form-registro');
  if (!registroForm) return;

  registroForm.addEventListener('submit', () => {
    const emailInput = registroForm.querySelector('input[name="correo"]');
    const nombreInput = registroForm.querySelector('input[name="nombre"]');
    const apellidoInput = registroForm.querySelector('input[name="apellido"]');

    const email = (emailInput?.value || '').trim();
    const nombre = (nombreInput?.value || '').trim();
    const apellido = (apellidoInput?.value || '').trim();

    if (!email) return;

    // Enviar webhook en segundo plano (no bloquea el submit del form)
    const payload = {
      email: email,
      source: 'registro',
      nombre: `${nombre} ${apellido}`.trim()
    };

    // Usar sendBeacon para no bloquear la navegación del formulario
    const blob = new Blob([JSON.stringify(payload)], {
      type: 'application/json'
    });

    // Fallback: si sendBeacon no está disponible, usar fetch
    if (navigator.sendBeacon) {
      navigator.sendBeacon('/api/welcome', blob);
    } else {
      fetch('/api/welcome', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        keepalive: true
      }).catch(() => {});
    }
  });
});
