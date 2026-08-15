document.addEventListener('DOMContentLoaded', async () => {
        const userId = localStorage.getItem('userId');

        if (!userId) {
            alert("로그인 정보가 없습니다.");
            window.location.href = 'lobby.html';
            return;
        }

        try {
            const response = await fetch(`${API_BASE}/users/${userId}/stats`);
            if (response.ok) {
                const data = await response.json();
//                document.getElementById('nickname').innerText = data.nickname;
                document.getElementById('totalGames').innerText = data.totalGames;
                document.getElementById('winCount').innerText = data.winCount;
                document.getElementById('winRate').innerText = data.winRate;
                document.getElementById('maxScore').innerText = data.maxScore;
            } else {
                alert("전적 정보를 불러오지 못했습니다.");
            }
        } catch (error) {
            console.error("마이페이지 조회 에러:", error);
        }
    });