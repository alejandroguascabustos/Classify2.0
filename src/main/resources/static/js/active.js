
  /* ------------------ Marca link activo en el sidebar ------------------ */
  const links = document.querySelectorAll(".sidebar a");
  if (links.length) {
    links.forEach(link => {
      // compara solo pathname+search para evitar diferencias con hash o trailing slash si quieres
      try {
        if (link.href === window.location.href) {
          link.classList.add("active");
        }
      } catch (e) {
        // enlace con href relativo mal formado: lo ignoramos
      }
    });
  }