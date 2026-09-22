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
const nightActionBtn = document.getElementById('nightActionBtn');
const voteTargetArea = document.getElementById('voteTargetArea');
const voteBtn = document.getElementById('voteBtn');

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
const gameLog = document.getElementById('gameLog');
const defenseAgreeBtn =
    document.getElementById('defenseAgreeBtn');
const defenseDisagreeBtn =
    document.getElementById('defenseDisagreeBtn');
const defenseQuestion =
    document.getElementById('defenseQuestion');
let defenseTargetNickname = null;
let mafiaChatSubscription = null;
let policeSubscription = null;
let soldierShieldSubscription = null;
let currentDay = 1;
let isFirstNight = true;


// ================================
// STOMP
// ================================

let stompClient = null;
let timerInterval = null;
let myRoleValue = null;
let selectedTargetId = null;
let isMyHost = false;

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
    playerCount.textContent = `${participants.length}명`;

    participants.forEach(participant => {

        const playerElement = document.createElement('div');

        let text = participant.nickname;

        if (participant.host) {
            text += ' (방장)';
        }

        if (!participant.alive) {
            text += ' (사망)';
        }

        playerElement.textContent = text;

        playerList.appendChild(playerElement);
    });
}

async function sendNominationVote() {

    if (!selectedTargetId) {
        alert('투표할 플레이어를 선택하세요.');
        return;
    }

    try {

        const response = await fetch(
            `${MAFIA_API}/vote/nomination`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify({
                    roomId: Number(roomId),
                    targetId: selectedTargetId
                })
            }
        );

        if (!response.ok) {

            const message =
                await response.text();

            alert(
                message ||
                '투표에 실패했습니다.'
            );

            return;
        }

        const message =
            await response.text();

        console.log(
            '1차 지목 투표 결과:',
            message
        );

        alert('투표가 접수되었습니다.');

        voteBtn.disabled = true;

    } catch (error) {

        console.error(
            '1차 지목 투표 실패:',
            error
        );

        alert('투표 중 오류가 발생했습니다.');
    }
}

async function sendDefenseVote(isAgree) {

    try {

        const response = await fetch(
            `${MAFIA_API}/vote/defense`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify({
                    roomId: Number(roomId),
                    agree: isAgree
                })
            }
        );

        if (!response.ok) {

            const message =
                await response.text();

            alert(
                message ||
                '찬반 투표에 실패했습니다.'
            );

            return;
        }

        console.log(
            '찬반 투표 완료:',
            isAgree ? '찬성' : '반대'
        );

        defenseAgreeBtn.disabled = true;
        defenseDisagreeBtn.disabled = true;

    } catch (error) {

        console.error(
            '찬반 투표 실패:',
            error
        );

        alert('찬반 투표 중 오류가 발생했습니다.');
    }
}

function renderVoteTargets(participants) {

    voteTargetArea.innerHTML = '';

    participants.forEach(participant => {

        if (!participant.alive) {
            return;
        }

        const button =
            document.createElement('button');

        button.textContent =
            participant.nickname;

        button.type = 'button';

        button.addEventListener(
            'click',
            function () {

                selectedTargetId =
                    participant.participantId;

                console.log(
                    '투표 대상 선택:',
                    participant
                );

                // 선택된 버튼 표시
                const buttons =
                    voteTargetArea.querySelectorAll('button');

                buttons.forEach(btn => {
                    btn.style.fontWeight = 'normal';
                });

                button.style.fontWeight = 'bold';
            }
        );

        voteTargetArea.appendChild(button);
    });
}

function addGameLog(message) {

    const logElement =
        document.createElement('div');

    logElement.textContent = message;

    gameLog.appendChild(logElement);

    gameLog.scrollTop =
        gameLog.scrollHeight;
}


// ================================
// 내 정보 출력
// ================================
function renderMyInfo(data) {

    myNickname.textContent = data.nickname;
    myInfoNickname.textContent = data.nickname;
    myAliveStatus.textContent = data.alive ? '생존' : '사망';
    myRoleValue = data.mafiaRole;

    // ================================
    // 마피아 채팅
    // ================================

    if (data.mafiaRole === 'MAFIA') {

        mafiaChatArea.style.display = 'block';

        if (!mafiaChatSubscription &&
            stompClient &&
            stompClient.connected) {

            mafiaChatSubscription = stompClient.subscribe(
                `/sub/room/${roomId}/mafia-chat`,
                function (message) {

                    const chatData =
                        JSON.parse(message.body);

                    console.log(
                        'Mafia Chat:',
                        chatData
                    );

                    handleMafiaChatMessage(chatData);
                }
            );
        }

        mafiaChatInput.disabled = !data.alive;
        mafiaChatSendBtn.disabled = !data.alive;

    } else {

        mafiaChatArea.style.display = 'none';

        mafiaChatInput.disabled = false;
        mafiaChatSendBtn.disabled = false;
    }

    // ================================
    // 경찰 조사 결과 구독
    // ================================
    if (data.mafiaRole === 'POLICE') {

        if (!policeSubscription &&
            stompClient &&
            stompClient.connected) {

            policeSubscription = stompClient.subscribe(
                `/sub/room/${roomId}/police`,
                function (message) {

                    const policeData =
                        JSON.parse(message.body);

                    console.log(
                        '경찰 조사 결과:',
                        policeData
                    );

                    handlePoliceInvestigationResult(
                        policeData
                    );
                }
            );
        }
    }

    // ================================
    // 군인 방패 발동 결과 구독
    // ================================
    if (data.mafiaRole === 'SOLDIER') {

        if (!soldierShieldSubscription &&
            stompClient &&
            stompClient.connected) {

            soldierShieldSubscription = stompClient.subscribe(
                `/sub/room/${roomId}/soldier`,
                function (message) {

                    const soldierData =
                        JSON.parse(message.body);

                    handleSoldierShieldMessage(
                        soldierData
                    );
                }
            );
        }
    }

    // ================================
    // 역할 표시
    // ================================

    if (data.mafiaRole) {
        myRole.textContent = data.mafiaRole;
    } else {
        myRole.textContent = '게임 시작 후 공개';
    }

    // ================================
    // 방장
    // ================================

    if (data.host) {
        isMyHost = true;
        hostArea.style.display = 'block';
    } else {
        isMyHost = false;
        hostArea.style.display = 'none';
    }

    // ================================
    // 게임 단계
    // ================================

    if (data.gamePhase) {
        gamePhase.textContent = data.gamePhase;
    }

    renderNightAction();
}


function handlePoliceInvestigationResult(data) {

    if (!data) {
        return;
    }

    // 경찰 본인만 처리
    if (myRoleValue !== 'POLICE') {
        return;
    }

    const resultMessage =
        data.isMafia
            ? `${data.targetNickname}님은 마피아입니다.`
            : `${data.targetNickname}님은 마피아가 아닙니다.`;

    const messageElement =
        document.createElement('div');

    messageElement.textContent =
        `🔎 [경찰 조사] ${resultMessage}`;

    // 경찰 조사 결과 전용 스타일
    messageElement.style.fontWeight = 'bold';
    messageElement.style.color = '#d4a017';
    messageElement.style.margin = '5px 0';

    chatMessages.appendChild(messageElement);

    chatMessages.scrollTop =
        chatMessages.scrollHeight;

    console.log(
        '경찰 조사 결과:',
        resultMessage
    );
}
function handleSoldierShieldMessage(data) {

    if (!data) {
        return;
    }

    const messageElement =
        document.createElement('div');

    messageElement.textContent =
        `${data.message}`;

    messageElement.style.fontWeight = 'bold';
    messageElement.style.margin = '5px 0';

    chatMessages.appendChild(messageElement);

    chatMessages.scrollTop =
        chatMessages.scrollHeight;
}

function renderNightAction() {

    nightTargetArea.innerHTML = '';
    selectedTargetId = null;

    if (!myRoleValue) {
        nightActionBtn.style.display = 'none';
        return;
    }

    switch (myRoleValue) {

        case 'MAFIA':
            nightRoleMessage.textContent =
                '죽일 플레이어를 선택하세요.';

            nightActionBtn.textContent = '살해하기';
            nightActionBtn.style.display = 'block';

            createNightTargetButtons();
            break;

        case 'POLICE':
            nightRoleMessage.textContent =
                '조사할 플레이어를 선택하세요.';

            nightActionBtn.textContent = '조사하기';
            nightActionBtn.style.display = 'block';

            createNightTargetButtons();
            break;

        case 'DOCTOR':
            nightRoleMessage.textContent =
                '살릴 플레이어를 선택하세요.';

            nightActionBtn.textContent = '치료하기';
            nightActionBtn.style.display = 'block';

            createNightTargetButtons();
            break;

        case 'CITIZEN':
            nightRoleMessage.textContent =
                '이번 판은 시민입니다. 밤에는 행동할 수 없습니다.';

            nightActionBtn.style.display = 'none';
            break;

        case 'SOLDIER':
            nightRoleMessage.textContent =
                '이번 판은 군인입니다. 밤에는 행동할 수 없습니다.';

            nightActionBtn.style.display = 'none';
            break;

        default:
            nightActionBtn.style.display = 'none';
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
    stompClient.subscribe(
        `/sub/room/${roomId}/game-end`,
        function (message) {

            const data = JSON.parse(message.body);

            console.log('게임 종료:', data);

            handleGameEnd(data);
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

        case 'WAITING':

            setGeneralChatState(true);

            gameMessage.textContent =
                '게임이 곧 시작됩니다.';

            startTimer(10);
            break;


        case 'NIGHT':

            // 밤이 새로 시작될 때 능력 버튼 초기화
            nightActionBtn.disabled = false;

            // 밤에는 공용 채팅 금지
            setGeneralChatState(false);

            if (!isFirstNight) {
                currentDay++;
            }

            isFirstNight = false;

            nightArea.style.display = 'block';

            gameMessage.textContent =
                '밤이 되었습니다.';

            addGameLog(
                `🌙 ${currentDay}일차 밤이 시작되었습니다.`
            );

            loadMyInfo();

            startTimer(30);

            break;


        case 'DAY': {

            // 낮에는 공용 채팅 가능
            setGeneralChatState(true);

            dayArea.style.display = 'block';

            gameMessage.textContent =
                '낮이 되었습니다. 토론을 시작하세요.';

            addGameLog(
                `☀️ ${currentDay}일차 낮이 시작되었습니다.`
            );

            const count =
                parseInt(playerCount.textContent);

            startTimer(
                30 + (count * 5)
            );

            break;
        }


        case 'VOTE': {

            // 투표 중에도 공용 채팅 가능
            setGeneralChatState(true);

            // 새로운 투표 시작
            voteBtn.disabled = false;

            voteArea.style.display = 'block';

            gameMessage.textContent =
                '투표할 플레이어를 선택하세요.';

            loadVoteTargets();

            addGameLog(
                `🗳️ ${currentDay}일차 투표가 시작되었습니다.`
            );

            const count =
                parseInt(playerCount.textContent);

            startTimer(
                15 + (count * 2)
            );

            break;
        }


        case 'DEFENSE':

            setGeneralChatState(true);

            // 새로운 반론 시작
            defenseAgreeBtn.disabled = false;
            defenseDisagreeBtn.disabled = false;

            defenseArea.style.display = 'block';

            gameMessage.textContent =
                '찬반 투표를 진행합니다.';

            addGameLog(
                `⚖️ ${currentDay}일차 반론이 시작되었습니다.`
            );

            startTimer(15);

            break;


        default:

            setGeneralChatState(false);

            timer.textContent = '-';

            break;
    }
}
function handleGameEnd(data) {

    console.log('최종 게임 결과:', data);
    console.log('게임 종료 시점 isMyHost:', isMyHost);

    gamePhase.textContent = 'END';
    timer.textContent = '-';

    hideAllGameAreas();

    // 게임 종료 메시지
    gameMessage.textContent =
        data.message || '게임이 종료되었습니다.';

    // 게임 로그
    addGameLog(
        `🏆 ${data.message}`
    );

    // ============================
    // 최종 결과 출력
    // ============================

    if (data.results && data.results.length > 0) {

        let resultMessage = '';

        resultMessage += '━━━━━━━━━━━━━━\n';
        resultMessage += '🏆 최종 게임 결과\n';
        resultMessage += '━━━━━━━━━━━━━━\n';

        resultMessage += `승리 팀: ${
            data.winner === 'MAFIA'
                ? '마피아 팀'
                : '시민 팀'
        }\n\n`;

        data.results.forEach(player => {

            const aliveText =
                player.alive
                    ? '생존'
                    : '사망';

            resultMessage +=
                `${player.nickname} → ` +
                `${player.role} / ` +
                `${aliveText}\n`;
        });

        resultMessage +=
            '━━━━━━━━━━━━━━';

        addGameLog(resultMessage);
    }
    console.log('버튼 표시 직전 isMyHost:', isMyHost);

    // ============================
    // 다시 게임 시작
    // ============================

    if (isMyHost) {

        startGameBtn.style.display = 'block';
        startGameBtn.disabled = false;
        startGameBtn.textContent = '다시 게임 시작';
        console.log('방장 버튼 표시 완료');

    } else {

        startGameBtn.style.display = 'none';
        console.log('방장이 아니므로 버튼 숨김');
    }
}
function setGeneralChatState(enabled) {

    chatInput.disabled = !enabled;

    if (!enabled) {
        chatInput.placeholder =
            '현재는 전체 채팅을 사용할 수 없습니다.';
    } else {
        chatInput.placeholder =
            '메시지를 입력하세요.';
    }
}
async function loadVoteTargets() {

    try {

        const response = await fetch(
            `${ROOMS_API}/${roomId}/participants`,
            {
                method: 'GET',
                headers: {
                    'Authorization':
                        `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(
                '투표 대상 조회 실패'
            );
        }

        const participants =
            await response.json();

        renderVoteTargets(participants);

    } catch (error) {

        console.error(
            '투표 대상 조회 실패:',
            error
        );
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

        case 'GAME_STARTED':

            // 게임 시작 후 내 역할 다시 조회
            loadMyInfo();

            break;

        case 'NIGHT_RESULT':

            gameMessage.textContent =
                data.message || '밤 결과가 발표되었습니다.';

            addGameLog(
                data.message || '밤 결과가 발표되었습니다.'
            );

            loadParticipants();
            loadMyInfo();

            break;

        case 'VOTE_TIE':
            gameMessage.textContent =
                data.message || '투표 결과 동률입니다.';

            break;

        case 'DEFENSE_START':

            defenseTargetNickname =
                data.message.replace(
                    '님의 최후 반론 시간입니다.',
                    ''
                );

            defenseQuestion.textContent =
                `${defenseTargetNickname}님을 처형하시겠습니까?`;

            gameMessage.textContent =
                data.message;

            addGameLog(
                `⚖️ ${data.message}`
            );

            setGeneralChatState(
                defenseTargetNickname === myNickname.textContent
            );

            break;

        case 'EXECUTION_RESULT':
            gameMessage.textContent =
                data.message;

            addGameLog(
                data.message
            );

            loadParticipants();
            loadMyInfo();

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
async function restartGame() {

    console.log('=== restartGame 호출 ===');
    console.log('accessToken 존재:', !!accessToken);
    console.log('roomId:', roomId);

    try {
        const response = await fetch(
            `${ROOMS_API}/${roomId}/restart`,
            {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                }
            }
        );

        console.log('restart 응답 상태:', response.status);

        if (!response.ok) {
            const message = await response.text();

            console.log('restart 응답 내용:', message);

            throw new Error(
                message || '게임 재시작에 실패했습니다.'
            );
        }

        console.log('게임 재시작 성공');

        currentDay = 1;
        isFirstNight = true;

        startGameBtn.disabled = false;
        startGameBtn.textContent = '게임 시작';

        loadMyInfo();
        loadParticipants();

    } catch (error) {
        console.error('게임 재시작 실패:', error);

        alert(
            error.message ||
            '게임 재시작 중 오류가 발생했습니다.'
        );
    }
}

async function sendNightAction() {

    console.log('🔥🔥 sendNightAction 호출됨');


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
    function () {

        if (gamePhase.textContent === 'END') {
            restartGame();
        } else {
            startGame();
        }

    }
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
voteBtn.addEventListener(
    'click',
    sendNominationVote
);
defenseAgreeBtn.addEventListener(
    'click',
    function () {
        sendDefenseVote(true);
    }
);

defenseDisagreeBtn.addEventListener(
    'click',
    function () {
        sendDefenseVote(false);
    }
);


// ================================
// 시작
// ================================

loadMyInfo();
loadParticipants();
connectWebSocket();