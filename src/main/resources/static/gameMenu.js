function initGameMenuPage() {
    const nickname = localStorage.getItem('nickname') || '플레이어';
}

function handleLogout() {
    localStorage.removeItem('userId');
    localStorage.removeItem('nickname');
    alert('로그아웃 되었습니다.');
    navigateTo('home');
}