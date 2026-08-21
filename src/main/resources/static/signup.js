const USER_BASE_URL = `${API_BASE}/users`;

function initSignupPage() {
    const signupForm = document.getElementById('signupForm');
    if (!signupForm) return;

    signupForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const signupData = {
            loginId: document.getElementById('username').value,
            password: document.getElementById('password').value,
            nickname: document.getElementById('nickname').value
        };

        try {
            const response = await fetch(`${USER_BASE_URL}/signup`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(signupData)
            });

            const message = await response.text();

            if (response.ok) {
                alert('회원가입이 완료되었습니다! 로그인해 주세요.');
                // ★ 페이지 전체 이동이 아닌 라우터 호출
                navigateTo('login');
            } else {
                alert(message || "회원가입 실패");
            }
        } catch (error) {
            console.error('Error:', error);
            alert('서버 연결 실패');
        }
    });
}