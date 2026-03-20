// Espera a que el DOM (Document Object Model) esté completamente cargado
// Esto asegura que todos los elementos HTML existan antes de que el JavaScript intente acceder a ellos
document.addEventListener('DOMContentLoaded', function() {
    
    // Obtenermos referencias del id de cada dato
    // --------------------------------------------------
    // Busca en el documento los elementos por su ID y los almacena en variables
    const fileInput = document.getElementById('file-input');      // El input tipo file (oculto)
    const numOfFiles = document.getElementById('num-of-files');   // El div que muestra la cantidad de archivos
    const filesList = document.getElementById('files-list');      // La lista ul donde se mostrarán los archivos

    // DEBUG: Verificar en consola que los elementos se encontraron correctamente
    // !!fileInput convierte el elemento a booleano (true si existe, false si no)
    console.log('JavaScript cargado - elementos encontrados:', {
        fileInput: !!fileInput,      // true si encontró el input file
        numOfFiles: !!numOfFiles,    // true si encontró el contador
        filesList: !!filesList       // true si encontró la lista
    });

    // Configurar el event listener para el input file
    // --------------------------------------------------
    // Escucha el evento 'change' que se dispara cuando el usuario selecciona archivos
    fileInput.addEventListener('change', function() {
        
        // DEBUG: Mostrar en consola información sobre los archivos seleccionados
        console.log('Archivos seleccionados:', this.files);
        
        // Limpiar la lista anterior
        // --------------------------------------------------
        // Elimina todo el contenido HTML dentro de la lista ul
        // Esto es necesario para que no se acumulen archivos de selecciones anteriores
        filesList.innerHTML = '';
        
        // Valida si hay archivos seleccionados
        // --------------------------------------------------
        // Si no hay archivos seleccionados (length === 0) o el usuario canceló
        if (this.files.length === 0) {
            // Mostrar mensaje indicando que no hay archivos
            numOfFiles.textContent = 'No hay archivos seleccionados';
            // Salir de la función temprano (return) para no ejecutar el resto del código
            return;
        }

        // Actualiza el contador de archivos
        // Usa template literals (``) para insertar la cantidad de archivos en el texto
        // ${this.files.length} se reemplaza por el número real de archivos
        numOfFiles.textContent = `${this.files.length} Archivo(s) seleccionado(s)`;

        // Itera sobre cada archivo seleccionado usando un array
        // Recorre todos los archivos en el array this.files
        for (let i = 0; i < this.files.length; i++) {
            
            // Obtener el archivo actual en la iteración
            const file = this.files[i];
            
            // DEBUG: Mostrar información detallada de cada archivo en consola
            console.log(`Archivo ${i}:`, file.name, file.size);
            
            // Crea un elemento li por cada archivo de la lista
            // --------------------------------------------------
            // Crea un nuevo elemento <li> (list item)
            let listItem = document.createElement('li');
            
            // Configurar el contenido de texto del <li>
            // file.name es el nombre del archivo
            // Math.round(file.size / 1024) convierte bytes a KB y redondea
            listItem.textContent = `📄 ${file.name} (${Math.round(file.size / 1024)} KB)`;
            
            // Agrega el elemento a la lista
            // --------------------------------------------------
            // Añade el <li> recién creado como hijo del <ul> (filesList)
            // Esto hace que el archivo aparezca visualmente en la página
            filesList.appendChild(listItem);
        }
    });
});