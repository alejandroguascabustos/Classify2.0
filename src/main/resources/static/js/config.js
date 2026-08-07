// Modal de configuración. Se carga en menu.html y en las tres vistas de
// noticias, pero el disparador #btnConfig solo existe en el menú: en noticias
// la línea de abajo reventaba con "Cannot set properties of null" y, al morir
// ahí el script, tampoco quedaba registrado el cierre del modal.
document.addEventListener("DOMContentLoaded", function () {

    const btnConfig = document.getElementById("btnConfig");
    const modalConfig = document.getElementById("modalConfig");
    const closeConfig = document.querySelector(".modal-config-close");

    // Sin modal no hay nada que abrir ni que cerrar.
    if (!modalConfig) return;

    if (btnConfig) {
        btnConfig.onclick = function (e) {
            e.preventDefault();
            modalConfig.style.display = "block";
        }
    }

    if (closeConfig) {
        closeConfig.onclick = function () {
            modalConfig.style.display = "none";
        }
    }

    window.onclick = function (event) {
        if (event.target === modalConfig) {
            modalConfig.style.display = "none";
        }
    }

});
