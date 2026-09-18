const ROOMS_API = `${API_BASE}/mafia/rooms`;
const MAFIA_API = `${API_BASE}/mafia`;


// ================================
// URL / JWT
// ================================

const params = new URLSearchParams(window.location.search);
const roomId = params.get('roomId');

const accessToken = localStorage.getItem('accessToken');

if (!roomId) {
    alert('방 정보가 없습니다.');
    window.location.href = 'mafiaGame.html';
}

if (!accessToken) {
    alert('로그인이 필요합니다.');
    window.location.href = 'index.html';
}


// ================================
// DOM
// ================================

const myNickname = document.getElementById('myNickname');

const roomName = document.getElementById('roomName');
const playerCount = document.getElementById('playerCount');
const gamePhase = document.getElementById('gamePhase');
const timer = document.getElementById('timer');

const playerList = document.getElementById('playerList');

const myInfoNickname = document.getElementById('myInfoNickname');
const myAliveStatus = document.getElementById('myAliveStatus');
const myRole = document.getElementById('myRole');

const gameMessage = document.getElementById('gameMessage');

const nightArea = document.getElementById('nightArea');
const dayArea = document.getElementById('dayArea');
const voteArea = document.getElementById('voteArea');
const defenseArea = document.getElementById('defenseArea');

const nightTargetArea = document.getElementById('nightTargetArea');
const voteTargetArea = document.getElementById('voteTargetArea');

const chatMessages = document.getElementById('chatMessages');
const chatInput = document.getElementById('chatInput');

const hostArea = document.getElementById('hostArea');
const startGameBtn = document.getElementById('startGameBtn');

const leaveRoomBtn = document.getElementById('leaveRoomBtn');
const nightRoleMessage =
    document.getElementById('nightRoleMessage');
const mafiaChatArea =
    document.getElementById('mafiaChatArea');

const mafiaChatMessages =
    document.getElementById('mafiaChatMessages');

const mafiaChatInput =
    document.getElementById('mafiaChatInput');

const mafiaChatSendBtn =
    document.getElementById('mafiaChatSendBtn');


// ================================
// STOMP
// ================================

let stompClient = null;
let timerInterval = null;
let myRoleValue = null;
let selectedTargetId = null;


// ================================
// 내 정보 조회
// ================================

async function loadMyInfo() {

    try {

        const response = await fetch(
            `${ROOMS_API}/${roomId}/my-info`,
            {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );

        if (response.status === 401) {
            alert('로그인이 만료되었습니다.');
            localStorage.removeItem('accessToken');
            window.location.href = 'index.html';
            return;
        }

        if (!response.ok) {
            throw new Error('내 정보 조회 실패');
        }

        const data = await response.json();

        console.log('내 정보:', data);

        renderMyInfo(data);

    } catch (error) {

        console.error('내 정보 조회 실패:', error);

        gameMessage.textContent =
            '내 정보를 불러오지 못했습니다.';
    }
}
async function loadParticipants() {

    try {

        const response = await fetch(
            `${ROOMS_API}/${roomId}/participants`,
            {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );

        if (response.status === 401) {
            alert('로그인이 만료되었습니다.');
            localStorage.removeItem('accessToken');
            window.location.href = 'index.html';
            return;
        }

        if (!response.ok) {
            throw new Error('참가자 목록 조회 실패');
        }

        const participants = await response.json();

        console.log('참가자 목록:', participants);

        renderParticipants(participants);

    } catch (error) {

        console.error('참가자 목록 조회 실패:', error);

        gameMessage.textContent =
            '참가자 목록을 불러오지 못했습니다.';
    }
}

// ================================
// 타이머
// ================================
function startTimer(seconds) {

    clearInterval(timerInterval);

    let remainingSeconds = seconds;

    timer.textContent = `${remainingSeconds}초`;

    timerInterval = setInterval(() => {

        remainingSeconds--;

        timer.textContent = `${remainingSeconds}초`;

        if (remainingSeconds <= 0) {
            clearInterval(timerInterval);
        }

    }, 1000);
}
function renderParticipants(participants) {

    playerList.innerHTML = '';

    playerCount.textContent =
        `${participants.length}명`;

    participants.forEach(participant => {

        const playerElement =
            document.createElement('div');

        playerElement.textContent =
            `${participant.nickname}${participant.host ? ' 👑' : ''}`;

        playerList.appendChild(playerElement);
    });
}


// ================================
// 내 정보 출력
// ================================

function renderMyInfo(data) {

    console.log('내 정보:', data);

    myNickname.textContent = data.nickname;
    myInfoNickname.textContent = data.nickname;
    myAliveStatus.textContent = data.alive ? '생존' : '사망';

    myRoleValue = data.mafiaRole;

    if (data.mafiaRole === 'MAFIA' && data.alive) {
        mafiaChatArea.style.display = 'block';
    } else {
        mafiaChatArea.style.display = 'none';
    }

    if (data.mafiaRole) {
        myRole.textContent = data.mafiaRole;
    } else {
        myRole.textContent = '게임 시작 후 공개';
    }

    if (data.host) {
        hostArea.style.display = 'block';
    } else {
        hostArea.style.display = 'none';
    }

    if (data.gamePhase) {
        gamePhase.textContent = data.gamePhase;
    }

    // 밤 역할 UI 갱신
    renderNightAction();
}

function renderNightAction() {

    nightTargetArea.innerHTML = '';
    selectedTargetId = null;

    if (!myRoleValue) {
        nightActionBtn.style.display = 'none';
        return;
    }

    if (myRoleValue === 'CITIZEN') {
        nightRoleMessage.textContent =
            '이번 판은 시민입니다. 밤에는 행동할 수 없습니다.';

        nightActionBtn.style.display = 'none';
        return;
    }

    if (myRoleValue === 'SOLDIER') {
        nightRoleMessage.textContent =
            '이번 판은 군인입니다. 밤에는 행동할 수 없습니다.';

        nightActionBtn.style.display = 'none';
        return;
    }

    switch (myRoleValue) {

        case 'MAFIA':
            nightRoleMessage.textContent =
                '죽일 플레이어를 선택하세요.';
            createNightTargetButtons();
            break;

        case 'POLICE':
            nightRoleMessage.textContent =
                '조사할 플레이어를 선택하세요.';
            createNightTargetButtons();
            break;

        case 'DOCTOR':
            nightRoleMessage.textContent =
                '살릴 플레이어를 선택하세요.';
            createNightTargetButtons();
            break;
    }
}

function createNightTargetButtons() {

    loadParticipantsForNight();
}

async function loadParticipantsForNight() {

    try {

        const response = await fetch(
            `${ROOMS_API}/${roomId}/participants`,
            {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error('참가자 목록 조회 실패');
        }

        const participants = await response.json();

        nightTargetArea.innerHTML = '';

        participants
            .filter(participant => participant.alive)
            .forEach(participant => {

                const button =
                    document.createElement('button');

                button.type = 'button';

                button.textContent =
                    participant.nickname;

                button.addEventListener('click', () => {

                    selectedTargetId =
                        participant.participantId;

                    document
                        .querySelectorAll('#nightTargetArea button')
                        .forEach(btn => {
                            btn.classList.remove('selected');
                        });

                    button.classList.add('selected');

                    console.log(
                        '선택한 대상:',
                        selectedTargetId
                    );
                });

                nightTargetArea.appendChild(button);
            });

    } catch (error) {

        console.error(
            '밤 대상 목록 조회 실패:',
            error
        );

        gameMessage.textContent =
            '대상 목록을 불러오지 못했습니다.';
    }
}



// ================================
// STOMP 연결
// ================================

function connectWebSocket() {

    /*
     * 현재 프로젝트의 WebSocket endpoint가
     * /ws 이므로 SockJS로 연결한다.
     */

    const socket = new SockJS('/ws');

    stompClient = Stomp.over(socket);

    stompClient.connect(
        {
            Authorization: `Bearer ${accessToken}`
        },
        function (frame) {

            console.log('STOMP 연결 성공:', frame);

            gameMessage.textContent =
                '게임 서버에 연결되었습니다.';

            subscribeRoom();

        },
        function (error) {

            console.error('STOMP 연결 실패:', error);

            gameMessage.textContent =
                '게임 서버 연결에 실패했습니다.';
        }
    );
}


// ================================
// 방 구독
// ================================

function subscribeRoom() {

    // ============================
    // Phase
    // ============================

    stompClient.subscribe(
        `/sub/room/${roomId}/phase`,
        function (message) {

            const data = JSON.parse(message.body);

            console.log('Phase:', data);

            handlePhase(data);
        }
    );
        // ============================
        // Participants
        // ============================

        stompClient.subscribe(
            `/sub/room/${roomId}/participants`,
            function (message) {

                console.log('참가자 변경:', message.body);

                loadParticipants();
                loadMyInfo();
            }
        );

        // Mafia Chat
            stompClient.subscribe(
                `/sub/room/${roomId}/mafia-chat`,
                function (message) {

                    const data = JSON.parse(message.body);

                    console.log('Mafia Chat:', data);

                    handleMafiaChatMessage(data);
                }
            );


    // ============================
    // Game
    // ============================

    stompClient.subscribe(
        `/sub/room/${roomId}/game`,
        function (message) {

            const data = JSON.parse(message.body);

            console.log('Game:', data);

            handleGameMessage(data);
        }
    );


    // ============================
    // Chat
    // ============================

    stompClient.subscribe(
        `/sub/room/${roomId}/chat`,
        function (message) {

            const data = JSON.parse(message.body);

            console.log('Chat:', data);

            handleChatMessage(data);
        }
    );

    console.log('방 구독 완료:', roomId);
}


// ================================
// Phase 처리
// ================================

function handlePhase(data) {

    const phase = data.phase;

    if (!phase) return;

    gamePhase.textContent = phase;

    hideAllGameAreas();

    switch (phase) {

        case 'NIGHT':
            nightArea.style.display = 'block';
            gameMessage.textContent = '밤이 되었습니다.';

            loadMyInfo();
            startTimer(30);

            break;

        case 'DAY': {
            dayArea.style.display = 'block';
            gameMessage.textContent =
                '낮이 되었습니다. 토론을 시작하세요.';

            const count = parseInt(playerCount.textContent);

            startTimer(30 + (count * 5));

            break;
        }

        case 'VOTE': {
            voteArea.style.display = 'block';
            gameMessage.textContent =
                '투표할 플레이어를 선택하세요.';

            const count = parseInt(playerCount.textContent);

            startTimer(15 + (count * 2));

            break;
        }

        case 'DEFENSE':
            defenseArea.style.display = 'block';
            gameMessage.textContent =
                '찬반 투표를 진행합니다.';

            startTimer(15);

            break;

        default:
            timer.textContent = '-';
            break;
    }
}


// ================================
// 게임 메시지 처리
// ================================

function handleGameMessage(data) {

    console.log('게임 메시지 수신:', data);

    if (data.message) {
        gameMessage.textContent = data.message;
    }

    switch (data.type) {

        case 'VOTE_TIE':
            gameMessage.textContent =
                data.message || '투표 결과 동률입니다.';
            break;

        default:
            break;
    }
}


// ================================
// 채팅 처리
// ================================

function handleChatMessage(data) {

    const messageElement = document.createElement('div');

    messageElement.textContent =
        `${data.senderName} : ${data.message}`;

    chatMessages.appendChild(messageElement);

    chatMessages.scrollTop =
        chatMessages.scrollHeight;
}

function handleMafiaChatMessage(data) {

    const messageElement =
        document.createElement('div');

    messageElement.textContent =
        `${data.senderName} : ${data.message}`;

    mafiaChatMessages.appendChild(messageElement);

    mafiaChatMessages.scrollTop =
        mafiaChatMessages.scrollHeight;
}


// ================================
// 채팅 전송
// ================================

function sendChat() {

    const message = chatInput.value.trim();

    if (!message) {
        return;
    }

    if (!stompClient || !stompClient.connected) {
        alert('서버에 연결되지 않았습니다.');
        return;
    }

    stompClient.send(
        `/app/room/${roomId}/chat`,
        {},
        JSON.stringify({
            message: message
        })
    );

    chatInput.value = '';
}

function sendMafiaChat() {

    const message = mafiaChatInput.value.trim();

    if (!message) return;

    if (!stompClient || !stompClient.connected) {
        alert('서버에 연결되지 않았습니다.');
        return;
    }

    if (myRoleValue !== 'MAFIA') {
        return;
    }

    stompClient.send(
        `/app/room/${roomId}/mafia-chat`,
        {},
        JSON.stringify({
            message: message
        })
    );

    mafiaChatInput.value = '';
}


// ================================
// 게임 시작
// ================================

async function startGame() {

    try {

        const response = await fetch(
            `${ROOMS_API}/${roomId}/start`,
            {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
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

            alert(
                message || '게임 시작에 실패했습니다.'
            );

            return;
        }

        console.log('게임 시작 요청 성공');

    } catch (error) {

        console.error('게임 시작 실패:', error);

        alert('게임 시작 중 오류가 발생했습니다.');
    }
}

async function sendNightAction() {

    if (!selectedTargetId) {
        alert('행동할 대상을 선택하세요.');
        return;
    }

    let actionType;

    switch (myRoleValue) {

        case 'MAFIA':
            actionType = 'MAFIA_KILL';
            break;

        case 'DOCTOR':
            actionType = 'DOCTOR_HEAL';
            break;

        case 'POLICE':
            actionType = 'POLICE_INVESTIGATE';
            break;

        default:
            return;
    }

    try {

        const response = await fetch(
            `${MAFIA_API}/night/action`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify({
                    roomId: Number(roomId),
                    targetId: selectedTargetId,
                    actionType: actionType
                })
            }
        );

        if (!response.ok) {

            const message =
                await response.text();

            alert(
                message || '밤 행동에 실패했습니다.'
            );

            return;
        }

        alert('밤 행동이 접수되었습니다.');

        nightActionBtn.disabled = true;

    } catch (error) {

        console.error(
            '밤 행동 실패:',
            error
        );

        alert('밤 행동 중 오류가 발생했습니다.');
    }
}


// ================================
// 방 나가기
// ================================

async function leaveRoom() {

    const confirmLeave =
        confirm('정말 방을 나가시겠습니까?');

    if (!confirmLeave) {
        return;
    }

    try {

        const response = await fetch(
            `${ROOMS_API}/${roomId}/leave`,
            {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {

            const message = await response.text();

            alert(
                message || '방 나가기에 실패했습니다.'
            );

            return;
        }

        if (stompClient) {
            stompClient.disconnect();
        }

        window.location.href =
            'mafiaGame.html';

    } catch (error) {

        console.error('방 나가기 실패:', error);

        alert('방 나가기 중 오류가 발생했습니다.');
    }
}


// ================================
// 게임 영역 초기화
// ================================

function hideAllGameAreas() {

    nightArea.style.display = 'none';
    dayArea.style.display = 'none';
    voteArea.style.display = 'none';
    defenseArea.style.display = 'none';
}


// ================================
// 이벤트
// ================================

startGameBtn.addEventListener(
    'click',
    startGame
);

leaveRoomBtn.addEventListener(
    'click',
    leaveRoom
);

mafiaChatSendBtn.addEventListener(
    'click',
    sendMafiaChat
);
mafiaChatInput.addEventListener('keydown', function (event) {

    if (event.key === 'Enter') {
        sendMafiaChat();
    }

});

document.getElementById('chatSendBtn')
    .addEventListener(
        'click',
        sendChat
    );

chatInput.addEventListener(
    'keydown',
    function (event) {

        if (event.key === 'Enter') {
            sendChat();
        }
    }
);

nightActionBtn.addEventListener(
    'click',
    sendNightAction
);


// ================================
// 시작
// ================================

loadMyInfo();
loadParticipants();
connectWebSocket();