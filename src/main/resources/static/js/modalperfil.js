const btnConfig = document.getElementById("btnConfig");
const modalConfig = document.getElementById("modalConfig");
const closeConfig = document.querySelector(".modal-config-close");

btnConfig.onclick = function(e) {
    e.preventDefault();
    modalConfig.style.display = "block";
}

closeConfig.onclick = function() {
    modalConfig.style.display = "none";
}

window.onclick = function(event) {
    if (event.target === modalConfig) {
        modalConfig.style.display = "none";
    }
}