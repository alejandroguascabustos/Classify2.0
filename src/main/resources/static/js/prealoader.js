document.addEventListener('DOMContentLoaded', () => {
  /* ------------------ Preloader ------------------ */
  const preloader = document.getElementById('preloader');
  if (preloader) {
    preloader.style.transition = "opacity 1.0s";
    // ocultar después de 1.2s
    setTimeout(() => {
      preloader.style.opacity = '0';
      // retiramos del flujo tras la transición
      setTimeout(() => { preloader.style.display = 'none'; }, 1000);
    }, 1200);
  }
});
