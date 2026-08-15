const ROOMS_API = `${API_BASE}/rooms`;

const userId = localStorage.getItem('userId');
const nickname = localStorage.getItem('nickname');

if (!userId) {
    alert("로그인이 필요합니다.");
    window.location.href = 'login.html';
} else {
    document.getElementById('myInfo').innerText = `${nickname} (ID: ${userId})`;
}

let stompClient = null;

window.onload = function() {
    fetchRooms();
    connectLobbySocket();
};

// 방 목록 불러오기
async function fetchRooms() {
    try {
        const response = await fetch(ROOMS_API);

        if (response.ok) {
            const rooms = await response.json();
            renderRoomList(rooms);
        }
    } catch (error) {
        console.error("방 목록 불러오기 실패:", error);
    }
}

// 방 목록 HTML 테이블 렌더링
function renderRoomList(rooms) {
    const tbody = document.getElementById('roomListTable');
    tbody.innerHTML = '';

    if (!rooms || rooms.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">생성된 방이 없습니다. 방을 만들어보세요!</td></tr>';
        return;
    }

    rooms.forEach(room => {
        const tr = document.createElement('tr');

        tr.innerHTML = `
            <td>${room.roomId}</td>
            <td>${room.roomName}</td>
            <td>${room.genre || '전체'}</td>
            <td><b>${room.currentPlayers} / ${room.maxPlayers}</b></td>
            <td>${room.roomStatus || 'WAITING'}</td>
            <td>
                <button onclick="enterRoom(${room.roomId})">입장하기</button>
            </td>
        `;

        tbody.appendChild(tr);
    });
}

// 방 생성 요청
document.getElementById('createRoomForm').addEventListener('submit', async (e) => {
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
            headers: {
                'Content-Type': 'application/json'
            },
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

// 방 입장 요청
async function enterRoom(roomId) {
    try {
        const response = await fetch(
            `${ROOMS_API}/${roomId}/enter?userId=${userId}`,
            {
                method: 'POST'
            }
        );

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

// 로그아웃
function logout() {
    localStorage.clear();
    window.location.href = 'login.html';
}

// 로비 웹소켓 연결
function connectLobbySocket() {
    const socket = new SockJS(`${SERVER_URL}/ws`);

    stompClient = Stomp.over(socket);

    // STOMP 디버그 로그 끄기
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log("로비 웹소켓 연결 성공!");

        stompClient.subscribe('/topic/lobby', function (message) {
            console.log("로비 실시간 데이터 수신:", message.body);

            const updatedRooms = JSON.parse(message.body);

            renderRoomList(updatedRooms);
        });

    }, function(error) {
        console.error("로비 웹소켓 연결 에러:", error);
    });
}