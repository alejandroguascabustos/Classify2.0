/* Abre/cierra el sidebar como panel off-canvas en móvil (<=900px).
   Se referencia desde layout/sidebar.html, así que está presente en
   todos los módulos autenticados sin duplicar la lógica por página. */
document.addEventListener('DOMContentLoaded', () => {
  const toggle = document.getElementById('menuToggle');
  const sidebar = document.querySelector('.sidebar');
  if (!toggle || !sidebar) return;

  let overlay = document.getElementById('overlay');
  if (!overlay) {
    overlay = document.createElement('div');
    overlay.id = 'overlay';
    document.body.appendChild(overlay);
  }

  const closeMenu = () => {
    toggle.classList.remove('active');
    sidebar.classList.remove('active');
    overlay.classList.remove('active');
  };

  toggle.addEventListener('click', () => {
    const willOpen = !sidebar.classList.contains('active');
    toggle.classList.toggle('active', willOpen);
    sidebar.classList.toggle('active', willOpen);
    overlay.classList.toggle('active', willOpen);
  });

  overlay.addEventListener('click', closeMenu);

  // Cerrar automáticamente al elegir un módulo del menú.
  sidebar.querySelectorAll('a').forEach((a) => a.addEventListener('click', closeMenu));

  // Si la ventana crece a escritorio con el panel abierto, lo reseteamos
  // para que no quede "activo" fuera de pantalla al volver a achicar.
  window.addEventListener('resize', () => {
    if (window.innerWidth > 900) closeMenu();
  });
});
