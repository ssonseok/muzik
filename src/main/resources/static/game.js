//    const SERVER_URL = 'http://localhost:8080';
    const urlParams = new URLSearchParams(window.location.search);
    const roomId = parseInt(urlParams.get('roomId'));

    // 💡 다중 탭 테스트를 위해 localStorage 사용
    const userId = parseInt(localStorage.getItem('userId'));
    const nickname = localStorage.getItem('nickname');

    if (!roomId || !userId) {
        alert("잘못된 접근입니다. 다시 로그인해 주세요.");
        window.location.href = 'login.html';
    } else {
        document.getElementById('myInfo').innerText = `${nickname} (ID: ${userId})`;
        document.getElementById('roomTitle').innerText = `#${roomId}`;
    }

    let stompClient = null;
    let ytPlayer = null;
    let isPlayerReady = false;

    // --- 1. YouTube API 초기화 ---
    function onYouTubeIframeAPIReady() {
        ytPlayer = new YT.Player('youtubePlayer', {
            height: '0',
            width: '0',
            events: {
            'onReady': function() {
                isPlayerReady = true;
                const currentVol = document.getElementById('volumeSlider').value || 50;
                ytPlayer.setVolume(currentVol);
            }
        }
        });
    }
    function changeVolume(volume) {
        if (ytPlayer && typeof ytPlayer.setVolume === 'function') {
            ytPlayer.setVolume(volume);
            document.getElementById('volumeValue').innerText = `${volume}%`;
        }
    }



    // --- 2. 페이지 로드 시 웹소켓 연결 ---
    window.onload = function() {
        connectGameSocket();
    };

    // --- 3. 게임방 웹소켓 연결 (/topic/rooms/{roomId}) ---
    function connectGameSocket() {
        const socket = new SockJS('${SERVER_URL}/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function (frame) {
            console.log('게임방 웹소켓 연결 성공!');

            // 서버가 브로드캐스팅하는 /topic/rooms/{roomId} 구독
            stompClient.subscribe(`/topic/rooms/${roomId}`, function (message) {
                const msgData = JSON.parse(message.body);
                handleIncomingMessage(msgData);
            });

            // 백엔드 @MessageMapping("/game/chat") 및 ChatMessageDto 규격에 맞춰 ENTER 메시지 전송
            stompClient.send('/app/game/chat', {}, JSON.stringify({
                type: 'ENTER',
                roomId: roomId,
                senderId: userId,
                sender: nickname,
                message: `${nickname}님이 입장하셨습니다.`
            }));

        }, function (error) {
            console.error('게임방 웹소켓 연결 에러:', error);
        });
    }

    // --- 4. 서버 수신 메시지 처리 (`processedMessage`) ---
    function handleIncomingMessage(data) {
        const chatBox = document.getElementById('chatBox');

        // A. 채팅 및 시스템 메시지 출력
        if (data.message) {
            const p = document.createElement('p');
            p.style.margin = "3px 0";

            if (data.type === 'TALK') {
                p.innerHTML = `<b>[${data.sender || '알림'}]</b>: ${data.message}`;
            } else if (data.type === 'ANSWER_AND_NEXT') {
                p.style.color = "green";
                p.style.fontWeight = "bold";
                p.innerText = data.message;
            } else if (data.type === 'SKIP_AND_NEXT') {
                p.style.color = "orange";
                p.innerText = data.message;
            } else if (data.type === 'START') {
                p.style.color = "blue";
                p.style.fontWeight = "bold";
                p.innerText = data.message;
            } else if (data.type === 'GAME_END') {
                p.style.color = "purple";
                p.style.fontWeight = "bold";
                p.innerText = data.message;
            } else if (data.type === 'ENTER' || data.type === 'LEAVE') {
                p.style.color = "gray";
                p.innerHTML = `<i>${data.message}</i>`;
            } else {
                p.innerText = data.message;
            }

            chatBox.appendChild(p);
            chatBox.scrollTop = chatBox.scrollHeight;
        }

        // B. 유튜브 음악 자동 재생 로직 (백엔드 GamePlayServiceImpl의 youtubeId 기반)
        if (data.youtubeId && isPlayerReady) {
            console.log("유튜브 재생 시작 - Video ID:", data.youtubeId);

            ytPlayer.loadVideoById(data.youtubeId);
            const currentVol = document.getElementById('volumeSlider').value || 50;
            ytPlayer.setVolume(currentVol);
            ytPlayer.playVideo();

            const currentRound = data.round || 1;
            document.getElementById('gameStatusText').innerText = `🎵 ${currentRound}라운드 진행 중!`;
        }

        // C. 게임 종료 처리 (GAME_END)
        if (data.type === 'GAME_END') {
            if (ytPlayer && isPlayerReady) ytPlayer.stopVideo();
            document.getElementById('gameStatusText').innerText = "🏆 게임이 종료되었습니다!";
        }
    }

    // --- 5. 메시지 전송 함수 (백엔드 /app/game/chat 으로 통합 전송) ---

    // A. 일반 채팅 및 정답 입력
    document.getElementById('chatForm').addEventListener('submit', function (e) {
        e.preventDefault();
        const input = document.getElementById('chatInput');
        const content = input.value.trim();

        if (content && stompClient) {
            stompClient.send('/app/game/chat', {}, JSON.stringify({
                type: 'TALK',
                roomId: roomId,
                senderId: userId,
                sender: nickname,
                message: content
            }));
            input.value = '';
        }
    });

    // B. 게임 시작 (START)
    function sendStart() {
        if (stompClient) {
            stompClient.send('/app/game/chat', {}, JSON.stringify({
                type: 'START',
                roomId: roomId,
                senderId: userId,
                sender: nickname,
                message: '게임 시작'
            }));
        }
    }

    // C. 과반수 스킵 투표 (SKIP)
    function sendSkip() {
        if (stompClient) {
            stompClient.send('/app/game/chat', {}, JSON.stringify({
                type: 'SKIP',
                roomId: roomId,
                senderId: userId,
                sender: nickname,
                message: '스킵 투표'
            }));
        }
    }

    // D. 방 나가기
    async function leaveRoom() {
    try {
        // 백엔드의 @PostMapping("/{roomId}/leave") 호출
        await fetch(`${API_BASE}/${roomId}/leave?userId=${userId}`, {
            method: 'POST'
        });
    } catch (e) {
        console.error("퇴장 요청 에러:", e);
    } finally {
        if (stompClient) stompClient.disconnect();
        window.location.href = 'lobby.html'; // 로비로 이동
    }
}