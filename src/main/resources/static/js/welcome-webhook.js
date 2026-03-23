document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('welcomeForm');
  if (!form) {
    return;
  }

  const emailInput = document.getElementById('welcomeEmail');
  const submitButton = document.getElementById('welcomeSubmit');
  const status = document.getElementById('welcomeStatus');

  const setStatus = (message, isError = false) => {
    if (!status) {
      return;
    }
    status.textContent = message;
    status.style.color = isError ? '#b00020' : '#1b5e20';
  };

  form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const email = (emailInput?.value || '').trim();
    if (!email) {
      setStatus('Por favor ingresa tu correo electronico.', true);
      emailInput?.focus();
      return;
    }

    if (emailInput && !emailInput.checkValidity()) {
      emailInput.reportValidity();
      return;
    }

    submitButton?.setAttribute('disabled', 'disabled');
    setStatus('Enviando tu bienvenida...');

    try {
      const response = await fetch('/api/welcome', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email }),
      });

      if (!response.ok) {
        throw new Error('Webhook error');
      }

      setStatus('Revisa tu bandeja de entrada!');
      form.reset();
    } catch (error) {
      setStatus('No pudimos enviar tu correo. Intenta nuevamente.', true);
    } finally {
      submitButton?.removeAttribute('disabled');
    }
  });
});
