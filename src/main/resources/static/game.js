const urlParams = new URLSearchParams(window.location.search);
    const roomId = parseInt(urlParams.get('roomId'));

    const userId = parseInt(localStorage.getItem('userId'));1
    const nickname = localStorage.getItem('nickname');

    if (!roomId || !userId) {
        alert("잘못된 접근입니다. 다시 로그인해 주세요.");
        window.location.href = 'index.html';
    } else {
        document.getElementById('myInfo').innerText = nickname;
        document.getElementById('roomTitle').innerText = `#${roomId}`;
    }

    let stompClient = null;
    let ytPlayer = null;
    let isPlayerReady = false;

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



    window.onload = function() {
        connectGameSocket();
    };

    function connectGameSocket() {
        const socket = new SockJS(`${SERVER_URL}/ws`);
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function (frame) {


            stompClient.subscribe(`/topic/rooms/${roomId}`, function (message) {
                const msgData = JSON.parse(message.body);
                handleIncomingMessage(msgData);
            });

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

    function handleIncomingMessage(data) {
        const chatBox = document.getElementById('chatBox');

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

        if (data.youtubeId && isPlayerReady) {

            ytPlayer.loadVideoById(data.youtubeId);
            const currentVol = document.getElementById('volumeSlider').value || 50;
            ytPlayer.setVolume(currentVol);
            ytPlayer.playVideo();

            const currentRound = data.round || 1;
            document.getElementById('gameStatusText').innerText = `🎵 ${currentRound}라운드 진행 중!`;
        }

        if (data.type === 'GAME_END') {
            if (ytPlayer && isPlayerReady) ytPlayer.stopVideo();
            document.getElementById('gameStatusText').innerText = "🏆 게임이 종료되었습니다!";
        }
    }

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

    async function leaveRoom() {
    try {
        await fetch(`${API_BASE}/${roomId}/leave?userId=${userId}`, {
            method: 'POST'
        });
    } catch (e) {
        console.error("퇴장 요청 에러:", e);
    } finally {
        if (stompClient) stompClient.disconnect();
        window.location.href = 'lobby.html';
    }
}