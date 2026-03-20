
function abrirModalEditar(id, nombre) {
    document.getElementById('modalEditar').style.display = 'flex';
    document.getElementById('edit-id').value = id;
    document.getElementById('edit-nombre').value = nombre;
}

function cerrarModalEditar() {
    document.getElementById('modalEditar').style.display = 'none';
}
