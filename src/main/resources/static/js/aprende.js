/**
 * Módulo Aprende JS - Classify
 * Lógica de IA, caché y renderizado dinámico.
 */

(function() {
    "use strict";

    const $ = s => document.querySelector(s);
    
    // Variables de estado
    let currentSubject = '';
    let currentTopic = '';
    let allSubtopics = [];
    const contentCache = new Map();

    // Referencias DOM
    const agendaForm = $('#agendaForm');
    const loadingUI = $('#loadingUI');
    const loaderText = $('#loaderText');
    const contentWrap = $('#contentWrap');
    const topicInput = $('#tema');
    const subjectInput = $('#materia');

    /**
     * Comunicación con la API LLM
     */
    async function callAI(messages) {
        try {
            const response = await fetch(`${window.classifyBaseUrl}aprende/chat`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ messages: messages }) // Ajuste: pasar como objeto messages
            });
            
            if (!response.ok) throw new Error('Error en la comunicación con el profesor IA');
            
            const json = await response.json();
            return json.content; // Extraer el contenido de la respuesta JSON del controller
        } catch (e) {
            console.error("Error en callAI:", e);
            throw e;
        }
    }

    /**
     * Limpieza y parseo de JSON robusto
     */
    function parseLLMJson(str) {
        try {
            const cleaned = str.replace(/```json|```/g, '').trim();
            return JSON.parse(cleaned);
        } catch (e) {
            console.error("Error parseando JSON de IA:", str);
            throw new Error("La respuesta del profesor no tiene un formato válido. Reintentando...");
        }
    }

    function showLoading(active, text = 'Cargando...') {
        loadingUI.classList.toggle('active', active);
        if (active) {
            loaderText.textContent = text;
            contentWrap.style.display = 'none';
        }
    }

    function showMsg(msg, type = 'error') {
        const div = document.createElement('div');
        div.className = `result-card ${type}`;
        div.style.borderLeft = `5px solid ${type === 'error' ? '#ef4444' : '#008000'}`;
        div.innerHTML = `<p>${msg}</p>`;
        contentWrap.innerHTML = '';
        contentWrap.appendChild(div);
        contentWrap.style.display = 'block';
    }

    /**
     * Renderizado de la clase completa
     */
    function renderClassroom(data) {
        contentWrap.innerHTML = '';
        contentWrap.style.display = 'block';

        let html = `
            <div class="result-card" style="border-top: 4px solid #008000;">
                <div class="breadcrumb">${currentSubject} <span style="opacity:0.3; margin:0 5px;">/</span> ${currentTopic}</div>
                <h3 style="margin-top:0;">${data.title || currentTopic}</h3>
                <p class="muted" style="font-size:18px; font-weight:500;">${data.synopsis || ''}</p>
                <div style="margin-top:20px;">
                    ${(data.key_concepts || []).map(k => `<span class="pill">${k}</span>`).join('')}
                </div>
            </div>
            <div class="two-col">
                <div class="main-content">`;

        // 1. Secciones de contenido
        (data.sections || []).forEach(sec => {
            const keyword = sec.image_keyword ? `${sec.image_keyword},education` : null;
            const secImg = keyword ? `https://source.unsplash.com/featured/800x450/?${encodeURIComponent(keyword)}` : null;
            
            html += `
            <div class="result-card">
                <h4>${sec.header}</h4>
                ${secImg ? `
                    <img src="${secImg}" 
                         class="section-img" 
                         onerror="this.style.display='none'" 
                         loading="lazy">` : ''}
                <p class="muted">${sec.content}</p>
                ${sec.vocabulary ? `
                    <div style="margin-top:20px;">
                        ${sec.vocabulary.map(v => `<span class="badge" title="${v.meaning}">📖 ${v.term}</span>`).join('')}
                    </div>
                ` : ''}
            </div>`;
        });

        // 2. Actividad
        if (data.quick_activity) {
            html += `
            <div class="result-card" style="border-left: 4px solid #008000; background:#f0fdf4;">
                <h4 style="margin-top:0;">🎯 Actividad: ${data.quick_activity.title}</h4>
                <ul class="muted">${(data.quick_activity.steps || []).map(s => `<li style="margin-bottom:8px;">${s}</li>`).join('')}</ul>
            </div>`;
        }

        html += `</div><div class="side-content">`;

        // 3. Cronología
        if (data.timeline && data.timeline.length) {
            html += `
            <div class="resource-box">
                <div class="suggestions-title">⏳ Cronología Clave</div>
                ${data.timeline.map(t => `
                    <div class="timeline-item">
                        <div style="font-weight:700; font-size:12px; color:#008000;">${t.year}</div>
                        <div style="font-size:13px; color:#475569;">${t.event}</div>
                    </div>`).join('')}
            </div>`;
        }

        // 4. Personajes
        if (data.key_people && data.key_people.length) {
            html += `
            <div class="resource-box">
                <div class="suggestions-title">👤 Protagonistas</div>
                ${data.key_people.map(p => `
                    <div class="person-card">
                        <div style="font-size:20px;">👤</div>
                        <div>
                            <div style="font-weight:600; font-size:13px;">${p.name}</div>
                            <div style="font-size:11px; opacity:0.7;">${p.role}</div>
                        </div>
                    </div>`).join('')}
            </div>`;
        }

        // 5. Curiosidades
        if (data.fun_facts && data.fun_facts.length) {
            html += `
            <div class="resource-box" style="background: #fffbeb; border-color: #fde68a;">
                <div class="suggestions-title" style="color: #92400e;">💡 Curiosidades</div>
                <ul class="muted" style="font-size:13px; padding-left:15px; color: #92400e;">
                    ${data.fun_facts.map(f => `<li style="margin-bottom:8px;">${f}</li>`).join('')}
                </ul>
            </div>`;
        }

        // 6. Quiz
        if (data.quick_quiz && data.quick_quiz.length) {
            html += `
            <div class="resource-box" style="background: #f0fdf4;">
                <div class="suggestions-title">📝 Refuerzo Rápido</div>
                ${data.quick_quiz.map((q, i) => `
                    <div style="margin-bottom:15px;">
                        <div style="font-size:13px; font-weight:600;">${i+1}. ${q.question}</div>
                        ${q.options.map(o => `<div class="quiz-option">${o}</div>`).join('')}
                    </div>`).join('')}
            </div>`;
        }

        // 7. Referencias
        if (data.references && data.references.length) {
            html += `
            <div class="resource-box">
                <div class="suggestions-title">📄 Material de Consulta</div>
                ${data.references.map(ref => `
                    <a href="${ref.url}" target="_blank" class="reference-link">
                        <div style="font-size:18px;">${ref.type === 'video' ? '📺' : '📄'}</div>
                        <div style="flex:1;">
                            <div style="font-weight:600; font-size:12px;">${ref.title}</div>
                            <div style="font-size:10px; opacity:0.6;">${ref.source}</div>
                        </div>
                    </a>`).join('')}
            </div>`;
        }

        html += `</div></div>`; // Fin side-content y two-col

        // 8. Pie de página (Contenidos Relacionados)
        if (allSubtopics.length) {
            html += `
            <div class="suggestions-section" style="width:100%; border-radius: 20px; border: 1px dashed #aec9a5; background: #f0fdf4; margin-top:20px;">
                <div class="suggestions-title" style="text-align:center;">🚀 ¿Qué quieres aprender después?</div>
                <div class="suggestions-buttons">
                    ${allSubtopics.filter(s => s.title !== data.title).map(s => `
                        <button class="suggestion-btn" data-topic="${s.title}">${s.title}</button>
                    `).join('')}
                </div>
            </div>`;
        }

        contentWrap.innerHTML = html;
        window.scrollTo({ top: 0, behavior: 'smooth' });

        // Event listener para sugerencias
        contentWrap.querySelectorAll('.suggestion-btn').forEach(btn => {
            btn.addEventListener('click', () => selectTopic(btn.dataset.topic));
        });
    }

    async function selectTopic(subtopic) {
        if (contentCache.has(subtopic)) {
            renderClassroom(contentCache.get(subtopic));
            return;
        }

        showLoading(true, `Generando material sobre: ${subtopic}...`);
        try {
            const raw = await callAI([
                { 
                    role: "system", 
                    content: "Eres Classify, experto en pedagogía. Genera un JSON educativo COMPLETO. OBLIGATORIO: EXACTAMENTE 4 referencias reales, un image_keyword para el HERO y palabras clave visuales por sección. También incluye cronología, personajes y quiz." 
                },
                { 
                    role: "user", 
                    content: `Materia: ${currentSubject}. Tema: ${currentTopic}. Subtema: ${subtopic}. Genera JSON con: 
                    title, synopsis, key_concepts[], 
                    sections[{header, content, image_keyword, vocabulary[{term,meaning}]}],
                    timeline[{year, event}], 
                    key_people[{name, role}], 
                    fun_facts[], 
                    quick_quiz[{question, options[], correct}],
                    quick_activity{title, steps[]}, 
                    references[{title, url, type, source}]` 
                }
            ]);
            const data = parseLLMJson(raw);
            contentCache.set(subtopic, data);
            renderClassroom(data);
        } catch (err) {
            showMsg(err.message, "error");
        } finally {
            showLoading(false);
        }
    }

    // Inicialización del formulario
    agendaForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        currentSubject = subjectInput.value.trim();
        currentTopic = topicInput.value.trim();
        
        showLoading(true, "Identificando subtemas clave...");
        try {
            const raw = await callAI([
                { role: "system", content: "Genera exactamente 4 subtemas en JSON: { 'subtopics': [{ 'title': '...' }] }" },
                { role: "user", content: `Materia: ${currentSubject}, Tema: ${currentTopic}` }
            ]);
            const json = parseLLMJson(raw);
            allSubtopics = json.subtopics || [];
            if (allSubtopics.length) await selectTopic(allSubtopics[0].title);
        } catch (err) {
            showMsg(err.message, "error");
        } finally {
            showLoading(false);
        }
    });

})();
