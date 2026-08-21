const BASE_URL = `${API_BASE}/users`;

// ★ 동적 로드 후 실행할 수 있도록 함수로 감싸기 (수정)
function initLoginPage() {
    const loginForm = document.getElementById('loginForm');
    if (!loginForm) return;

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const loginData = {
            loginId: document.getElementById('username').value,
            password: document.getElementById('password').value
        };

        try {
            const response = await fetch(`${BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(loginData)
            });

            if (response.ok) {
                const result = await response.json();

                localStorage.setItem('userId', result.userId);
                localStorage.setItem('nickname', result.nickname);

                alert(`로그인 성공! ${result.nickname}님 환영합니다.`);

                // ★ 기존 location.href 대신 BGM 유지 라우터 호출 (수정)
                navigateTo('gamemenu'); 
            } else {
                const errorText = await response.text();
                alert(errorText || "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
        } catch (error) {
            console.error('Error:', error);
            alert('서버 연결 실패');
        }
    });
}