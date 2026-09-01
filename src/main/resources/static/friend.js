const userId = localStorage.getItem('userId');
const nickname = localStorage.getItem('nickname');

if (!userId) {
    alert("로그인이 필요합니다.");
    window.location.href = 'index.html';
} else {
    document.getElementById('myInfo').innerText = `${nickname} (ID: ${userId})`;
}

window.onload = function() {
    fetchReceivedRequests();
    fetchFriendList();
};

function sendFriendRequest() {
    const targetNickname = document.getElementById('targetNickname').value.trim();
    if (!targetNickname) {
        alert('상대방 닉네임을 입력하세요.');
        return;
    }

    const token = localStorage.getItem('token');

    fetch(`${SERVER_URL}/api/friends/request?userId=${userId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify({ targetNickname: targetNickname })
    })
    .then(async response => {
        const message = await response.text();
        if (!response.ok) throw new Error(message || '친구 요청 실패');
        alert(message);
        document.getElementById('targetNickname').value = '';
        fetchReceivedRequests(); // 요청 보낸 후 목록 새로고침
    })
    .catch(error => alert(error.message));
}

function fetchReceivedRequests() {
    const token = localStorage.getItem('token'); // 👈 토큰 추가

    fetch(`${SERVER_URL}/api/friends/requests/received?userId=${userId}`, {
        headers: {
            'Authorization': 'Bearer ' + token // 👈 헤더 추가
        }
    })
    .then(response => {
        if (!response.ok) throw new Error('받은 요청 목록 조회 실패');
        return response.json();
    })
    .then(data => {
        const tbody = document.getElementById('receivedRequestTable');
        tbody.innerHTML = '';

        if (!data || data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3">받은 친구 요청이 없습니다.</td></tr>';
            return;
        }

        data.forEach(req => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${req.friendshipId}</td>
                <td>${req.requesterNickname} (ID: ${req.requesterUserId})</td>
                <td>
                    <button onclick="acceptFriendRequest(${req.friendshipId})">✅ 수락</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    })
    .catch(error => {
        console.error(error);
        document.getElementById('receivedRequestTable').innerHTML = '<tr><td colspan="3">목록 로드 실패</td></tr>';
    });
}

function acceptFriendRequest(friendshipId) {
    const token = localStorage.getItem('token');

    fetch(`${SERVER_URL}/api/friends/accept/${friendshipId}?userId=${userId}`, {
        method: 'PATCH',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(async response => {
        const message = await response.text();
        if (!response.ok) throw new Error(message || '수락 실패');
        alert('친구 요청을 수락했습니다!');
        fetchReceivedRequests();
        fetchFriendList();
    })
    .catch(error => alert(error.message));
}

function fetchFriendList() {
    const token = localStorage.getItem('token'); // 👈 토큰 추가

    fetch(`${SERVER_URL}/api/friends?userId=${userId}`, {
        headers: {
            'Authorization': 'Bearer ' + token // 👈 헤더 추가
        }
    })
    .then(response => {
        if (!response.ok) throw new Error('친구 목록 조회 실패');
        return response.json();
    })
    .then(data => {
        const tbody = document.getElementById('friendListTable');
        tbody.innerHTML = '';

        if (!data || data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3">등록된 친구가 없습니다.</td></tr>';
            return;
        }

        data.forEach(friend => {
            const tr = document.createElement('tr');

            let statusText = friend.status;
            if (friend.status === 'ONLINE') statusText = '🟢 온라인';
            else if (friend.status === 'OFFLINE') statusText = '⚪ 오프라인';
            else if (friend.status === 'LOBBY') statusText = '🟡 대기실';
            else if (friend.status === 'IN_GAME') statusText = '🔴 게임 중';

            tr.innerHTML = `
                <td>${friend.friendUserId}</td>
                <td>${friend.friendNickname}</td>
                <td>${statusText}</td>
            `;
            tbody.appendChild(tr);
        });
    })
    .catch(error => {
        console.error(error);
        document.getElementById('friendListTable').innerHTML = '<tr><td colspan="3">목록 로드 실패</td></tr>';
    });
}