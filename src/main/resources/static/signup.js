const BASE_URL = 'http://localhost:8080/api/users';

    document.getElementById('signupForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const signupData = {
            loginId: document.getElementById('username').value,
            password: document.getElementById('password').value,
            nickname: document.getElementById('nickname').value
        };

        try {
            const response = await fetch(`${BASE_URL}/signup`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(signupData)
            });

            const message = await response.text();

            if (response.ok) {
                alert('회원가입 완료');
                window.location.href = 'login.html';
            } else {
                alert(message || "회원가입 실패");
            }
        } catch (error) {
            console.error('Error:', error);
            alert('서버 연결 실패');
        }
    });