/**
 * Sistema de Mensagens - EduMania Pro
 * JavaScript para interatividade básica
 */

// ============================================
// SELETORES DE ELEMENTOS
// ============================================
const conversasLista = document.querySelector('.conversas-lista');
const conversasItems = document.querySelectorAll('.conversa-item');
const mensagemForm = document.querySelector('.mensagem-form');
const mensagemInput = document.getElementById('mensagem-input');
const mensagensArea = document.querySelector('.mensagens-area');
const buscaInput = document.getElementById('busca-conversas');
const chatNome = document.querySelector('.chat-nome');
const chatStatus = document.querySelector('.chat-status');
const chatAvatar = document.querySelector('.chat-avatar img');

// ============================================
// TROCA DE CONVERSAS
// ============================================

/**
 * Função para trocar a conversa ativa
 * @param {HTMLElement} conversaItem - Item da conversa clicado
 */
function trocarConversa(conversaItem) {
    // Remove classe 'ativa' de todas as conversas
    conversasItems.forEach(item => item.classList.remove('ativa'));
    
    // Adiciona classe 'ativa' na conversa clicada
    conversaItem.classList.add('ativa');
    
    // Pega informações da conversa
    const nome = conversaItem.querySelector('.conversa-nome').textContent.trim();
    const avatar = conversaItem.querySelector('.conversa-avatar img').src;
    const conversaId = conversaItem.dataset.conversaId;
    
    // Atualiza header do chat
    chatNome.textContent = nome;
    chatAvatar.src = avatar;
    chatAvatar.alt = `Avatar de ${nome}`;
    
    // Remove badge de não lidas
    const badge = conversaItem.querySelector('.badge-nao-lidas');
    if (badge) {
        badge.remove();
    }
    
    // Carrega mensagens da conversa (simulado)
    carregarMensagens(conversaId);
    
    // Em mobile, mostra o chat e esconde a sidebar
    if (window.innerWidth <= 768) {
        document.querySelector('.conversas-sidebar').classList.add('oculto');
        document.querySelector('.chat-principal').classList.add('ativo');
    }
}

/**
 * Adiciona event listeners para cada conversa
 */
conversasItems.forEach(item => {
    // Click
    item.addEventListener('click', () => {
        trocarConversa(item);
    });
    
    // Enter key (acessibilidade)
    item.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            trocarConversa(item);
        }
    });
});

// ============================================
// ENVIO DE MENSAGENS
// ============================================

/**
 * Função para enviar mensagem
 * @param {Event} e - Evento do formulário
 */
function enviarMensagem(e) {
    e.preventDefault();
    
    const texto = mensagemInput.value.trim();
    
    // Valida se há texto
    if (!texto) {
        return;
    }
    
    // Cria nova mensagem
    const novaMensagem = criarElementoMensagem(texto, true);
    
    // Remove indicador de digitação se existir
    const digitando = document.querySelector('.digitando');
    if (digitando) {
        digitando.remove();
    }
    
    // Adiciona mensagem na área
    mensagensArea.appendChild(novaMensagem);
    
    // Limpa input
    mensagemInput.value = '';
    
    // Scroll para o final
    scrollParaFinal();
    
    // Simula resposta automática após 2 segundos
    setTimeout(() => {
        simularResposta();
    }, 2000);
}

/**
 * Cria elemento HTML de mensagem
 * @param {string} texto - Texto da mensagem
 * @param {boolean} enviada - Se é mensagem enviada ou recebida
 * @returns {HTMLElement} - Elemento da mensagem
 */
function criarElementoMensagem(texto, enviada = false) {
    const mensagem = document.createElement('div');
    mensagem.className = enviada ? 'mensagem enviada' : 'mensagem recebida';
    
    const agora = new Date();
    const horario = `${agora.getHours().toString().padStart(2, '0')}:${agora.getMinutes().toString().padStart(2, '0')}`;
    
    if (!enviada) {
        mensagem.innerHTML = `
            <figure class="mensagem-avatar">
                <img src="${chatAvatar.src}" alt="${chatAvatar.alt}">
            </figure>
            <div class="mensagem-conteudo">
                <div class="mensagem-bubble">
                    <p class="mensagem-texto">${escaparHTML(texto)}</p>
                    <footer class="mensagem-footer">
                        <time class="mensagem-horario" datetime="${agora.toISOString()}">${horario}</time>
                    </footer>
                </div>
            </div>
        `;
    } else {
        mensagem.innerHTML = `
            <div class="mensagem-conteudo">
                <div class="mensagem-bubble">
                    <p class="mensagem-texto">${escaparHTML(texto)}</p>
                    <footer class="mensagem-footer">
                        <time class="mensagem-horario" datetime="${agora.toISOString()}">${horario}</time>
                        <span class="mensagem-status" aria-label="Mensagem enviada">✓</span>
                    </footer>
                </div>
            </div>
        `;
    }
    
    return mensagem;
}

/**
 * Escapa HTML para prevenir XSS
 * @param {string} texto - Texto a ser escapado
 * @returns {string} - Texto escapado
 */
function escaparHTML(texto) {
    const div = document.createElement('div');
    div.textContent = texto;
    return div.innerHTML;
}

/**
 * Scroll suave para o final da área de mensagens
 */
function scrollParaFinal() {
    mensagensArea.scrollTo({
        top: mensagensArea.scrollHeight,
        behavior: 'smooth'
    });
}

// Event listener para envio de mensagem
mensagemForm.addEventListener('submit', enviarMensagem);

// ============================================
// BUSCA DE CONVERSAS
// ============================================

/**
 * Filtra conversas baseado no texto de busca
 */
function buscarConversas() {
    const termoBusca = buscaInput.value.toLowerCase().trim();
    
    conversasItems.forEach(item => {
        const nome = item.querySelector('.conversa-nome').textContent.toLowerCase();
        const mensagem = item.querySelector('.ultima-mensagem').textContent.toLowerCase();
        
        if (nome.includes(termoBusca) || mensagem.includes(termoBusca)) {
            item.style.display = 'flex';
        } else {
            item.style.display = 'none';
        }
    });
}

// Event listener para busca
buscaInput.addEventListener('input', buscarConversas);

// ============================================
// SIMULAÇÃO DE RESPOSTA AUTOMÁTICA
// ============================================

/**
 * Simula uma resposta automática
 */
function simularResposta() {
    // Mostra indicador de digitação
    mostrarDigitando();
    
    // Após 1.5 segundos, envia resposta
    setTimeout(() => {
        const respostas = [
            'Entendi! 👍',
            'Perfeito!',
            'Pode deixar!',
            'Combinado então!',
            'Ótima ideia!',
            'Vou verificar e te aviso',
            'Obrigado pela informação!'
        ];
        
        const respostaAleatoria = respostas[Math.floor(Math.random() * respostas.length)];
        
        // Remove indicador de digitação
        const digitando = document.querySelector('.digitando');
        if (digitando) {
            digitando.remove();
        }
        
        // Cria e adiciona mensagem de resposta
        const mensagemResposta = criarElementoMensagem(respostaAleatoria, false);
        mensagensArea.appendChild(mensagemResposta);
        
        // Scroll para o final
        scrollParaFinal();
        
        // Atualiza última mensagem na lista de conversas
        atualizarUltimaMensagem(respostaAleatoria);
    }, 1500);
}

/**
 * Mostra indicador de digitação
 */
function mostrarDigitando() {
    const digitando = document.createElement('div');
    digitando.className = 'mensagem recebida digitando';
    digitando.innerHTML = `
        <figure class="mensagem-avatar">
            <img src="${chatAvatar.src}" alt="${chatAvatar.alt}">
        </figure>
        <div class="mensagem-conteudo">
            <div class="mensagem-bubble">
                <div class="digitando-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        </div>
    `;
    
    mensagensArea.appendChild(digitando);
    scrollParaFinal();
}

/**
 * Atualiza última mensagem na lista de conversas
 * @param {string} texto - Texto da última mensagem
 */
function atualizarUltimaMensagem(texto) {
    const conversaAtiva = document.querySelector('.conversa-item.ativa');
    if (conversaAtiva) {
        const ultimaMensagem = conversaAtiva.querySelector('.ultima-mensagem');
        if (ultimaMensagem) {
            ultimaMensagem.innerHTML = `<strong>${chatNome.textContent}:</strong> ${texto}`;
        }
        
        const horario = conversaAtiva.querySelector('.conversa-horario');
        if (horario) {
            const agora = new Date();
            horario.textContent = `${agora.getHours().toString().padStart(2, '0')}:${agora.getMinutes().toString().padStart(2, '0')}`;
            horario.setAttribute('datetime', agora.toISOString());
        }
    }
}

// ============================================
// CARREGAR MENSAGENS (SIMULADO)
// ============================================

/**
 * Carrega mensagens de uma conversa específica
 * @param {string} conversaId - ID da conversa
 */
function carregarMensagens(conversaId) {
    // Limpa área de mensagens
    mensagensArea.innerHTML = '';
    
    // Adiciona separador de data
    const dataSeparador = document.createElement('div');
    dataSeparador.className = 'data-separator';
    dataSeparador.innerHTML = '<time datetime="2025-10-25">Hoje</time>';
    mensagensArea.appendChild(dataSeparador);
    
    // Em uma aplicação real, aqui seria feita uma requisição para o backend
    // Por enquanto, mantém as mensagens existentes ou mostra mensagens vazias
    console.log(`Carregando mensagens da conversa ${conversaId}`);
}

// ============================================
// RESPONSIVIDADE MOBILE
// ============================================

/**
 * Configura botão de voltar
 */
function configurarBotaoVoltar() {
    const btnVoltar = document.querySelector('.btn-voltar');
    
    if (btnVoltar) {
        // Remove listeners anteriores
        btnVoltar.replaceWith(btnVoltar.cloneNode(true));
        
        // Pega o novo botão
        const novoBtnVoltar = document.querySelector('.btn-voltar');
        
        novoBtnVoltar.addEventListener('click', () => {
            document.querySelector('.conversas-sidebar').classList.remove('oculto');
            document.querySelector('.chat-principal').classList.remove('ativo');
        });
        
        // Mostra/esconde botão baseado no tamanho da tela
        if (window.innerWidth > 768) {
            novoBtnVoltar.style.display = 'none';
        } else {
            novoBtnVoltar.style.display = 'flex';
        }
    }
}

// Configura botão de voltar ao carregar
configurarBotaoVoltar();

// Reconfigura ao redimensionar
window.addEventListener('resize', configurarBotaoVoltar);

// ============================================
// ATALHOS DE TECLADO
// ============================================

/**
 * Adiciona atalhos de teclado
 */
document.addEventListener('keydown', (e) => {
    // Ctrl/Cmd + K: Focar na busca
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        buscaInput.focus();
    }
    
    // Esc: Limpar busca
    if (e.key === 'Escape' && document.activeElement === buscaInput) {
        buscaInput.value = '';
        buscarConversas();
        buscaInput.blur();
    }
});

// ============================================
// NOTIFICAÇÕES (SIMULADO)
// ============================================

/**
 * Solicita permissão para notificações
 */
function solicitarPermissaoNotificacao() {
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }
}

/**
 * Envia notificação
 * @param {string} titulo - Título da notificação
 * @param {string} corpo - Corpo da notificação
 */
function enviarNotificacao(titulo, corpo) {
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(titulo, {
            body: corpo,
            icon: '/favicon.ico',
            badge: '/favicon.ico'
        });
    }
}

// Solicita permissão ao carregar (opcional)
// solicitarPermissaoNotificacao();

// ============================================
// INICIALIZAÇÃO
// ============================================

/**
 * Inicializa o sistema de mensagens
 */
function inicializar() {
    console.log('Sistema de mensagens inicializado');
    
    // Scroll inicial para o final
    scrollParaFinal();
    
    // Foca no input de mensagem
    mensagemInput.focus();
}

// Inicializa quando o DOM estiver pronto
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inicializar);
} else {
    inicializar();
}

// ============================================
// VALIDAÇÃO DE FORMULÁRIO
// ============================================

/**
 * Valida input de mensagem em tempo real
 */
mensagemInput.addEventListener('input', () => {
    const btnEnviar = document.querySelector('.btn-enviar');
    
    if (mensagemInput.value.trim()) {
        btnEnviar.style.opacity = '1';
        btnEnviar.disabled = false;
    } else {
        btnEnviar.style.opacity = '0.5';
        btnEnviar.disabled = true;
    }
});

// ============================================
// ACESSIBILIDADE
// ============================================

/**
 * Anuncia mensagens para leitores de tela
 * @param {string} mensagem - Mensagem a ser anunciada
 */
function anunciarMensagem(mensagem) {
    const anuncio = document.createElement('div');
    anuncio.setAttribute('role', 'status');
    anuncio.setAttribute('aria-live', 'polite');
    anuncio.className = 'visually-hidden';
    anuncio.textContent = mensagem;
    
    document.body.appendChild(anuncio);
    
    setTimeout(() => {
        anuncio.remove();
    }, 1000);
}

// ============================================
// EXPORTAÇÕES (para uso em outros módulos)
// ============================================

// Se estiver usando módulos ES6, descomente:
// export { enviarMensagem, trocarConversa, buscarConversas };

