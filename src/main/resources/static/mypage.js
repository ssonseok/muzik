document.addEventListener('DOMContentLoaded', async () => {
    const userId = localStorage.getItem('userId');
    const token = localStorage.getItem('accessToken');

    if (!userId || !token) {
        alert("로그인 정보가 없습니다. 다시 로그인해 주세요.");
        window.location.href = 'lobby.html';
        return;
    }

    try {
        const response = await fetch(`${SERVER_URL}/api/users/${userId}/stats`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const data = await response.json();

            document.getElementById('totalGames').innerText = data.totalGames ?? 0;
            document.getElementById('winCount').innerText = data.winCount ?? 0;
            document.getElementById('winRate').innerText = data.winRate ?? 0;
            document.getElementById('maxScore').innerText = data.maxScore ?? 0;
        } else {
            alert("전적 정보를 불러오지 못했습니다.");
        }
    } catch (error) {
        console.error("마이페이지 조회 에러:", error);
    }
});