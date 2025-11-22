document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('.login-form');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirm-password');
    const errorMessage = document.getElementById('password-match-error');

    // Função principal de validação
    function validatePasswords() {
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        
        // Se a repetição de senha estiver vazia, não mostrar erro (ainda)
        if (confirmPassword.length === 0) {
            errorMessage.style.display = 'none';
            passwordInput.classList.remove('input-error');
            confirmPasswordInput.classList.remove('input-error');
            return true;
        }

        if (password !== confirmPassword) {
            // Senhas não coincidem
            errorMessage.style.display = 'block';
            passwordInput.classList.add('input-error');
            confirmPasswordInput.classList.add('input-error');
            return false;
        } else {
            // Senhas coincidem
            errorMessage.style.display = 'none';
            passwordInput.classList.remove('input-error');
            confirmPasswordInput.classList.remove('input-error');
            return true;
        }
    }

    // 1. Validação em tempo real (on input)
    passwordInput.addEventListener('input', validatePasswords);
    confirmPasswordInput.addEventListener('input', validatePasswords);
});
