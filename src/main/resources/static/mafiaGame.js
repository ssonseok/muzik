const ROOMS_API = `${API_BASE}/mafia/rooms`;

const myInfo = document.getElementById('myInfo');
const roomListTable = document.getElementById('roomListTable');
const friendList = document.getElementById('friendList');

const createRoomModal = document.getElementById('createRoomModal');
const createRoomForm = document.getElementById('createRoomForm');

const refreshBtn = document.getElementById('refreshBtn');
const createRoomBtn = document.getElementById('createRoomBtn');
const cancelCreateBtn = document.getElementById('cancelCreateBtn');
const logoutBtn = document.getElementById('logoutBtn');
const gameMenuBtn = document.getElementById('gameMenuBtn');


// ================================
// JWT
// ================================

function getAccessToken() {
    const token = localStorage.getItem('accessToken');

    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = 'index.html';
        return null;
    }

    return token;
}


function getAuthHeaders(includeContentType = false) {

    const token = getAccessToken();

    if (!token) {
        return null;
    }

    const headers = {
        'Authorization': `Bearer ${token}`
    };

    if (includeContentType) {
        headers['Content-Type'] = 'application/json';
    }

    return headers;
}


// ================================
// 내 정보
// ================================

function loadMyInfo() {

    const nickname = localStorage.getItem('nickname');

    if (nickname) {
        myInfo.textContent = nickname;
    } else {
        myInfo.textContent = '사용자';
    }
}


// ================================
// 방 목록 조회
// ================================

async function fetchRooms() {

    const headers = getAuthHeaders();

    if (!headers) {
        return;
    }

    try {

        const response = await fetch(ROOMS_API, {
            method: 'GET',
            headers: headers
        });


        if (response.status === 401) {
            alert('로그인이 만료되었습니다.');

            localStorage.removeItem('accessToken');

            window.location.href = 'index.html';

            return;
        }


        if (!response.ok) {
            throw new Error('방 목록 조회 실패');
        }


        const rooms = await response.json();

        renderRoomList(rooms);

    } catch (error) {

        console.error('방 목록 조회 실패:', error);

        roomListTable.innerHTML = `
            <tr>
                <td colspan="5">
                    방 목록을 불러오지 못했습니다.
                </td>
            </tr>
        `;
    }
}
async function fetchLobbyFriendList() {
    if (!friendList) {
        return;
    }

    const userId = localStorage.getItem('userId');

    if (!userId) {
        return;
    }

    try {
        const response = await fetch(
            `${SERVER_URL}/api/friends?userId=${userId}`,
            {
                method: 'GET',
                headers: getAuthHeaders()
            }
        );

        if (!response.ok) {
            throw new Error('친구 목록 조회 실패');
        }

        const friends = await response.json();

        renderLobbyFriends(friends);

    } catch (error) {
        console.error('친구 목록 불러오기 실패:', error);

        friendList.innerHTML = `
            <div class="friend-loading">
                친구 목록을 불러오지 못했습니다.
            </div>
        `;
    }
}
function renderLobbyFriends(friends) {
    if (!friendList) {
        return;
    }

    friendList.innerHTML = '';

    if (!friends || friends.length === 0) {
        friendList.innerHTML = `
            <div class="friend-loading">
                등록된 친구가 없습니다.<br>
                [관리]에서 친구를 추가해보세요.
            </div>
        `;
        return;
    }

    friends.forEach(friend => {
        const item = document.createElement('div');
        item.className = 'friend-item';

        let statusColor = '#777';
        let statusText = '오프라인';
        let nameClass = 'offline';

        switch (friend.status) {
            case 'ONLINE':
                statusColor = '#6fa36f';
                statusText = '온라인';
                nameClass = 'online';
                break;

            case 'IN_ROOM':
                statusColor = '#b08a4a';
                statusText = '방 입장';
                nameClass = 'online';
                break;

            case 'PLAYING':
                statusColor = '#8f3b3b';
                statusText = '게임 중';
                nameClass = 'playing';
                break;

            case 'OFFLINE':
            default:
                statusColor = '#777';
                statusText = '오프라인';
                nameClass = 'offline';
                break;
        }

        item.innerHTML = `
            <div class="friend-name-area">
                <span
                    class="friend-status"
                    style="background-color: ${statusColor};">
                </span>

                <span class="friend-name ${nameClass}">
                    ${escapeHtml(friend.friendNickname)}
                </span>
            </div>

            <span
                class="friend-status-text"
                style="color: ${statusColor};">
                ${statusText}
            </span>
        `;

        friendList.appendChild(item);
    });
}



// ================================
// 방 목록 출력
// ================================

function renderRoomList(rooms) {

    roomListTable.innerHTML = '';


    if (!rooms || rooms.length === 0) {

        roomListTable.innerHTML = `
            <tr>
                <td colspan="5">
                    현재 생성된 방이 없습니다.
                </td>
            </tr>
        `;

        return;
    }


    rooms.forEach((room, index) => {

        const row = document.createElement('tr');

        row.innerHTML = `
            <td>${index + 1}</td>
            <td>${escapeHtml(room.roomName)}</td>
            <td>${room.currentPlayers} / ${room.maxPlayers}</td>
            <td>${getRoomStatusText(room.roomStatus)}</td>
            <td>
                <button type="button" class="join-room-btn">
                    입장
                </button>
            </td>
        `;


        const joinButton = row.querySelector('.join-room-btn');

        joinButton.addEventListener('click', () => {
            joinRoom(room.roomId);
        });


        roomListTable.appendChild(row);
    });
}


// ================================
// 방 입장
// ================================

async function joinRoom(roomId) {

    const headers = getAuthHeaders();

    if (!headers) {
        return;
    }


    try {

        const response = await fetch(
            `${ROOMS_API}/${roomId}/join`,
            {
                method: 'POST',
                headers: headers
            }
        );


        if (response.status === 401) {

            alert('로그인이 만료되었습니다.');

            localStorage.removeItem('accessToken');

            window.location.href = 'index.html';

            return;
        }


        if (!response.ok) {

            const message = await response.text();

            alert(message || '방 입장에 실패했습니다.');

            return;
        }


        window.location.href =
            `mafiaRoom.html?roomId=${roomId}`;

    } catch (error) {

        console.error('방 입장 실패:', error);

        alert('방 입장 중 오류가 발생했습니다.');
    }
}


// ================================
// 방 생성
// ================================

async function createRoom(event) {

    event.preventDefault();


    const roomName =
        document.getElementById('roomName').value.trim();

    const maxPlayers =
        Number(document.getElementById('maxPlayers').value);


    if (!roomName) {

        alert('방 제목을 입력해주세요.');

        return;
    }


    const headers = getAuthHeaders(true);

    if (!headers) {
        return;
    }


    // hostUserId는 보내지 않는다.
    // 서버가 JWT에서 현재 사용자를 확인한다.
    const requestData = {
        roomName: roomName,
        maxPlayers: maxPlayers
    };


    try {

        const response = await fetch(ROOMS_API, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(requestData)
        });


        if (response.status === 401) {

            alert('로그인이 만료되었습니다.');

            localStorage.removeItem('accessToken');

            window.location.href = 'index.html';

            return;
        }


        if (!response.ok) {

            const message = await response.text();

            alert(message || '방 생성에 실패했습니다.');

            return;
        }


        const room = await response.json();


        // 서버에서 이미 방장을 참가자로 등록했기 때문에
        // 별도의 joinRoom() 호출은 하지 않는다.
        window.location.href =
            `mafiaRoom.html?roomId=${room.roomId}`;

    } catch (error) {

        console.error('방 생성 실패:', error);

        alert('방 생성 중 오류가 발생했습니다.');
    }
}


// ================================
// 방 상태
// ================================

function getRoomStatusText(status) {

    switch (status) {

        case 'WAITING':
            return '대기중';

        case 'PLAYING':
            return '게임중';

        case 'FINISHED':
            return '종료';

        default:
            return status || '-';
    }
}


// ================================
// XSS 방지
// ================================

function escapeHtml(value) {

    if (value === null || value === undefined) {
        return '';
    }

    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}


// ================================
// 모달
// ================================

function openCreateModal() {
    createRoomModal.style.display = 'block';
}


function closeCreateModal() {
    createRoomModal.style.display = 'none';
}


// ================================
// 로그아웃,게임메뉴로
// ================================

function logout() {

    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('nickname');

    window.location.href = 'index.html';
}
function goToGameMenu() {
    window.location.href = 'index.html';
}


// ================================
// 이벤트
// ================================
refreshBtn.addEventListener('click', fetchRooms);
createRoomBtn.addEventListener('click', openCreateModal);
cancelCreateBtn.addEventListener('click', closeCreateModal);
logoutBtn.addEventListener('click', logout);
createRoomForm.addEventListener('submit', createRoom);
gameMenuBtn.addEventListener('click', goToGameMenu);


// ================================
// 페이지 시작
// ================================

loadMyInfo();
fetchRooms();
fetchLobbyFriendList();
setInterval(fetchLobbyFriendList, 3000);