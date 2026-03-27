/**
 * Classify – Welcome Webhook Handler
 * Captura el email del formulario de inicio y lo envía al backend,
 * que a su vez dispara el webhook de n8n para enviar un correo de bienvenida.
 */
document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('welcomeForm');
  if (!form) return;

  const emailInput = document.getElementById('welcomeEmail');
  const submitButton = document.getElementById('welcomeSubmit');
  const status = document.getElementById('welcomeStatus');

  /**
   * Muestra un mensaje de estado con icono y animación.
   */
  const setStatus = (message, type = 'info') => {
    if (!status) return;

    const icons = {
      success: '✅',
      error: '❌',
      loading: '⏳',
      info: 'ℹ️'
    };

    const colors = {
      success: '#008000',
      error: '#b00020',
      loading: '#F5B027',
      info: '#64748b'
    };

    status.innerHTML = `<span style="margin-right:6px">${icons[type]}</span>${message}`;
    status.style.color = colors[type];
    status.style.opacity = '0';
    status.style.transition = 'opacity 0.3s ease';

    requestAnimationFrame(() => {
      status.style.opacity = '1';
    });
  };

  /**
   * Valida el formato del email.
   */
  const isValidEmail = (email) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  };

  form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const email = (emailInput?.value || '').trim();

    if (!email) {
      setStatus('Por favor ingresa tu correo electrónico.', 'error');
      emailInput?.focus();
      return;
    }

    if (!isValidEmail(email)) {
      setStatus('El formato del correo no es válido.', 'error');
      emailInput?.focus();
      return;
    }

    // Deshabilitar botón y mostrar estado de carga
    if (submitButton) {
      submitButton.setAttribute('disabled', 'disabled');
      submitButton.style.opacity = '0.7';
      submitButton.style.cursor = 'wait';
    }
    setStatus('Enviando tu correo de bienvenida...', 'loading');

    try {
      const response = await fetch('/api/welcome', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: email,
          source: 'inicio',
          nombre: ''
        }),
      });

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        throw new Error(data?.message || 'Error al enviar el correo');
      }

      setStatus('🎉 ¡Revisa tu bandeja de entrada! Te enviamos un correo de bienvenida.', 'success');
      form.reset();
    } catch (error) {
      setStatus(
        error.message || 'No pudimos enviar tu correo. Intenta nuevamente.',
        'error'
      );
    } finally {
      if (submitButton) {
        submitButton.removeAttribute('disabled');
        submitButton.style.opacity = '1';
        submitButton.style.cursor = 'pointer';
      }
    }
  });
});
