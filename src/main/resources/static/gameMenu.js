function initGameMenuPage() {
    const nickname = localStorage.getItem('nickname') || '플레이어';
}

window.logout = function() {
    localStorage.clear();
    alert('로그아웃 되었습니다.');

    if (typeof navigateTo === 'function') {
        navigateTo('home');
    } else {
        window.location.href = 'index.html';
    }
};