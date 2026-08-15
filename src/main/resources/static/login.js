//const BASE_URL = 'http://localhost:8080/api/users';
const BASE_URL = `${API_BASE}/users`;
    document.getElementById('loginForm').addEventListener('submit', async (e) => {
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

                if (typeof result === 'object') {
                    localStorage.setItem('userId', result.userId);
                    localStorage.setItem('nickname', result.nickname);
                } else {
                    localStorage.setItem('userId', result);
                    localStorage.setItem('nickname', loginData.loginId); // 임시로 아이디를 닉네임으로 저장
                }

                alert(`로그인 성공!`);
                window.location.href = 'lobby.html';
            } else {
                const errorText = await response.text();
                alert(errorText || "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
        } catch (error) {
            console.error('Error:', error);
            alert('서버 연결 실패');
        }
    });