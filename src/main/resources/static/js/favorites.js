document.addEventListener('DOMContentLoaded', function() {
    // Обработка добавления в избранное через AJAX
    const favoriteForms = document.querySelectorAll('.favorite-form');

    favoriteForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault();

            const formData = new FormData(this);
            const url = this.getAttribute('action');
            const button = this.querySelector('button');
            const isFavoritePage = window.location.pathname.includes('favorites');

            fetch(url, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
                .then(response => {
                    if (response.ok) {
                        // Если мы на странице избранного, то удаляем карточку
                        if (isFavoritePage) {
                            const movieCard = this.closest('.movie-card');
                            if (movieCard) {
                                movieCard.style.opacity = '0';
                                setTimeout(() => {
                                    movieCard.remove();
                                    // Если не осталось фильмов, показываем сообщение
                                    if (document.querySelectorAll('.movie-card').length === 0) {
                                        const emptyMessage = document.createElement('div');
                                        emptyMessage.className = 'empty-favorites';
                                        emptyMessage.textContent = 'У вас пока нет избранных фильмов.';
                                        emptyMessage.style.cssText = 'text-align: center; padding: 40px; color: #666;';
                                        document.querySelector('.movies-grid').appendChild(emptyMessage);
                                    }
                                }, 300);
                            }
                        } else {
                            // Переключаем состояние кнопки
                            button.classList.toggle('active');

                            // Обновляем текст кнопки если это не сердечко
                            if (button.textContent.includes('В избранное') ||
                                button.textContent.includes('Убрать из избранного')) {
                                button.textContent = button.textContent.includes('В избранное')
                                    ? 'Убрать из избранного'
                                    : 'В избранное';
                            }

                            // Для сердечка меняем цвет
                            if (button.innerHTML.includes('&#10084;')) {
                                if (button.classList.contains('active')) {
                                    button.style.color = '#ec4899';
                                } else {
                                    button.style.color = '#d1d5db';
                                }
                            }
                        }

                        // Показываем уведомление
                        showNotification(isFavoritePage ? 'Убрано из избранного' : 'Добавлено в избранное');
                        return response.text();
                    }
                    throw new Error('Network response was not ok');
                })
                .catch(error => {
                    console.error('Error:', error);
                    showNotification('Ошибка при обновлении избранного', 'error');
                });
        });
    });

    function showNotification(message, type = 'success') {
        // Удаляем существующие уведомления
        const existingNotifications = document.querySelectorAll('.notification');
        existingNotifications.forEach(notification => notification.remove());

        // Создаем элемент уведомления
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${type === 'success' ? '#10b981' : '#ef4444'};
            color: white;
            padding: 12px 20px;
            border-radius: 8px;
            z-index: 10000;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            transition: all 0.3s ease;
        `;

        document.body.appendChild(notification);

        // Анимация появления
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
        }, 10);

        // Удаляем уведомление через 3 секунды
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                notification.remove();
            }, 300);
        }, 3000);
    }
});