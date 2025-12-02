(function () {
    const STORAGE_KEY = 'cinema-theme';
    const root = document.documentElement;

    function applyTheme(theme) {
        if (theme === 'dark') {
            root.setAttribute('data-theme', 'dark');
        } else {
            root.removeAttribute('data-theme'); // светлая по умолчанию
        }

        const toggle = document.querySelector('.theme-toggle');
        if (toggle) {
            toggle.textContent = theme === 'dark' ? '☀ Светлая' : '🌙 Тёмная';
        }
    }

    window.toggleTheme = function () {
        const current = root.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
        const next = current === 'dark' ? 'light' : 'dark';
        localStorage.setItem(STORAGE_KEY, next);
        applyTheme(next);
    };

    // при загрузке читаем сохранённую тему
    const saved = localStorage.getItem(STORAGE_KEY) || 'light';
    applyTheme(saved);
})();