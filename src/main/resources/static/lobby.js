const ROOMS_API = `${API_BASE}/rooms`;

const token = localStorage.getItem('accessToken');
const userId = localStorage.getItem('userId');
const nickname = localStorage.getItem('nickname');

if (!token || !userId) {
    alert("로그인이 필요합니다.");
    window.location.href = 'index.html';
} else {
    const myInfoElem = document.getElementById('myInfo');
    if (myInfoElem) myInfoElem.innerText = nickname;
}

function getAuthHeaders(contentType = 'application/json') {
    const headers = {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    };
    if (contentType) {
        headers['Content-Type'] = contentType;
    }
    return headers;
}

let stompClient = null;

window.onload = function() {
    fetchRooms();
    connectLobbySocket();
    fetchLobbyFriendList();
};

// 방 목록 불러오기 (GET)
async function fetchRooms() {
    try {
        const response = await fetch(ROOMS_API, {
            method: 'GET',
            headers: getAuthHeaders(null)
        });

        if (response.ok) {
            const rooms = await response.json();
            renderRoomList(rooms);
        } else if (response.status === 401 || response.status === 403) {
            alert("인증이 만료되었습니다. 다시 로그인해주세요.");
            logout();
        }
    } catch (error) {
        console.error("방 목록 불러오기 실패:", error);
    }
}

// 방 목록 HTML 테이블 렌더링
function renderRoomList(rooms) {
    const tbody = document.getElementById('roomListTable');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (!rooms || rooms.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">생성된 방이 없습니다. 방을 만들어보세요!</td></tr>';
        return;
    }

    rooms.forEach(room => {
        const tr = document.createElement('tr');

        tr.innerHTML = `
            <td>#${room.roomId}</td>
            <td style="font-weight: 600; text-align: left; padding-left: 20px;">${room.roomName}</td>
            <td><span class="genre-badge">${room.genre || '전체'}</span></td>
            <td><b style="color:#00F2FE;">${room.currentPlayers}</b> / ${room.maxPlayers}</td>
            <td style="color: ${room.roomStatus === 'WAITING' ? '#00FF87' : '#FF007F'};">${room.roomStatus || 'WAITING'}</td>
            <td>
                <button class="neon-btn neon-btn-pink" style="padding: 6px 14px; font-size: 13px;" onclick="enterRoom(${room.roomId})">입장</button>
            </td>
        `;

        tbody.appendChild(tr);
    });
}

// 방 생성 요청 (POST)
const createRoomForm = document.getElementById('createRoomForm');
if (createRoomForm) {
    createRoomForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const createData = {
            hostUserId: parseInt(userId),
            roomName: document.getElementById('roomName').value,
            maxPlayers: parseInt(document.getElementById('maxPlayers').value),
            password: document.getElementById('password').value,
            genre: document.getElementById('genre').value,
            musicCount: parseInt(document.getElementById('musicCount').value),
            startYear: parseInt(document.getElementById('startYear').value),
            endYear: parseInt(document.getElementById('endYear').value)
        };

        try {
            const response = await fetch(ROOMS_API, {
                method: 'POST',
                headers: getAuthHeaders('application/json'),
                body: JSON.stringify(createData)
            });

            if (response.ok) {
                const roomResponse = await response.json();
                alert(`방이 생성되었습니다! (방 번호: ${roomResponse.roomId})`);
                window.location.href = `game.html?roomId=${roomResponse.roomId}`;
            } else {
                const errorMsg = await response.text();
                alert("방 생성 실패: " + errorMsg);
            }

        } catch (error) {
            console.error("방 생성 에러:", error);
        }
    });
}

// 방 입장 요청 (POST)
async function enterRoom(roomId) {
    try {
        const response = await fetch(`${ROOMS_API}/${roomId}/enter?userId=${userId}`, {
            method: 'POST',
            headers: getAuthHeaders(null)
        });

        if (response.ok) {
            window.location.href = `game.html?roomId=${roomId}`;
        } else {
            const errorMsg = await response.text();
            alert("입장 실패: " + errorMsg);
        }

    } catch (error) {
        console.error("입장 에러:", error);
    }
}

// 친구 목록 및 접속 상태 조회
async function fetchLobbyFriendList() {
    const friendListContainer = document.getElementById('friendList');
    if (!friendListContainer) return;

    try {
        const response = await fetch(`${SERVER_URL}/api/friends?userId=${userId}`, {
            method: 'GET',
            headers: getAuthHeaders(null)
        });
        if (!response.ok) throw new Error('친구 목록 조회 실패');

        const friends = await response.json();
        renderLobbyFriends(friends);
    } catch (error) {
        console.error("로비 친구 목록 불러오기 실패:", error);
        friendListContainer.innerHTML = '<div style="color: #FF007F; font-size: 12px; text-align: center; padding: 15px 0;">목록을 불러오지 못했습니다.</div>';
    }
}

// 친구 상태 목록 화면
function renderLobbyFriends(friends) {
    const friendListContainer = document.getElementById('friendList');
    if (!friendListContainer) return;
    friendListContainer.innerHTML = '';

    if (!friends || friends.length === 0) {
        friendListContainer.innerHTML = '<div style="color: #888; font-size: 13px; text-align: center; padding: 20px 0;">등록된 친구가 없습니다.<br>상단 [관리]에서 친구를 추가해보세요!</div>';
        return;
    }

    friends.forEach(friend => {
        const item = document.createElement('div');
        item.className = 'friend-item';
        item.style.cssText = 'display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-bottom: 1px solid rgba(255, 255, 255, 0.05);';

        let statusClass = 'status-offline';
        let statusText = '오프라인';
        let nameColor = 'color: #777;';

        if (friend.status === 'ONLINE' || friend.status === 'LOBBY') {
            statusClass = 'status-online';
            statusText = '대기실';
            nameColor = 'color: #FFF; font-weight: 600;';
        } else if (friend.status === 'IN_GAME') {
            statusClass = 'status-ingame';
            statusText = '게임 중';
            nameColor = 'color: #FF007F; font-weight: 600;';
        }

        item.innerHTML = `
            <div style="display: flex; align-items: center; gap: 10px;">
                <span class="friend-status ${statusClass}"></span>
                <span style="${nameColor}">${friend.friendNickname}</span>
            </div>
            <span style="font-size: 11px; color: #AAA;">${statusText}</span>
        `;

        friendListContainer.appendChild(item);
    });
}

// 로그아웃
function logout() {
    localStorage.clear();
    window.location.href = 'index.html';
}

// 로비 웹소켓 연결
function connectLobbySocket() {
    const socket = new SockJS(`${SERVER_URL}/ws`);
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({ 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` }, function (frame) {
        stompClient.subscribe('/topic/lobby', function (message) {
            const updatedRooms = JSON.parse(message.body);
            renderRoomList(updatedRooms);
        });
    }, function(error) {
        console.error("로비 웹소켓 연결 에러:", error);
    });
}