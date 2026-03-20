document.addEventListener('DOMContentLoaded', function() {
    // Elementos corregidos
    const filtroMateria = document.getElementById('filtroMateria');
    const filtroProfesor = document.getElementById('filtroProfesor');
    const filtroCurso = document.getElementById('filtroCurso');
    const tabla = document.querySelector('.programacion-tabla tbody');
    
    if (!tabla) return;

    function filtrarTabla() {
        const textoMateria = filtroMateria ? filtroMateria.value.toLowerCase() : '';
        const textoProfesor = filtroProfesor ? filtroProfesor.value.toLowerCase() : '';
        const textoCurso = filtroCurso ? filtroCurso.value.toLowerCase() : '';
        
        const filas = tabla.querySelectorAll('tr');
        
        filas.forEach(fila => {
            const celdas = fila.querySelectorAll('td');
            
            if (celdas.length >= 5) {
                const materia = celdas[1].textContent.toLowerCase();
                const profesor = celdas[2].textContent.toLowerCase();
                const curso = celdas[4].textContent.toLowerCase();
                
                const mostrarFila = 
                    materia.includes(textoMateria) &&
                    profesor.includes(textoProfesor) &&
                    curso.includes(textoCurso);
                
                fila.style.display = mostrarFila ? '' : 'none';
            }
        });
    }
    
    // Event listeners
    if (filtroMateria) filtroMateria.addEventListener('input', filtrarTabla);
    if (filtroProfesor) filtroProfesor.addEventListener('input', filtrarTabla);
    if (filtroCurso) filtroCurso.addEventListener('input', filtrarTabla);
});