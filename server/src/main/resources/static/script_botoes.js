document.querySelectorAll('.menu-item').forEach(link => {
    link.addEventListener('click', function (e) {
        e.preventDefault();
        
        // Remover classe active de todos os itens
        document.querySelectorAll('.menu-item').forEach(item => {
            item.classList.remove('active');
        });
        
        // Adicionar classe active ao item clicado
        this.classList.add('active');
        
        // Obter o texto do botão clicado
        const buttonText = this.textContent;
        console.log('Botão clicado:', buttonText);
        
        // Atualizar o conteúdo da página
        const contentPlaceholder = document.querySelector('.content-placeholder');
        if (buttonText.includes('Home')) {
            contentPlaceholder.innerHTML = '<h1>🏠 Home</h1>';
        } else if (buttonText.includes('Disciplinas')) {
            contentPlaceholder.innerHTML = '<h1>📚 Disciplinas</h1>';
        }
    });
});

// Log de inicialização
console.log('Site com botões carregado com sucesso!');