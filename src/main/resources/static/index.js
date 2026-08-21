// 1. YouTube IFrame API 동적 로드
var tag = document.createElement('script');
tag.src = "https://www.youtube.com/iframe_api";
var firstScriptTag = document.getElementsByTagName('script')[0];
firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

var player;
var isPlaying = false;

// 2. YouTube 플레이어 준비
function onYouTubeIframeAPIReady() {
    player = new YT.Player('youtube-player', {
        height: '0',
        width: '0',
        videoId: 'j2uQ4l9aiVg', // Sam Smith - I'm Not The Only One Piano Inst
        playerVars: {
            'autoplay': 1,
            'controls': 0,
            'loop': 1,
            'playlist': 'j2uQ4l9aiVg'
        },
        events: {
            'onReady': onPlayerReady
        }
    });
}

// 3. 첫 클릭 시 BGM 자동 재생 시작
function onPlayerReady(event) {
    document.body.addEventListener('click', function startAudio() {
        if (!isPlaying) {
            player.playVideo();
            isPlaying = true;
            document.getElementById('bgm-toggle').innerText = '음악 끄기';
        }
        document.body.removeEventListener('click', startAudio);
    }, { once: true });
}

// 4. BGM 재생/일시정지 토글
function toggleBGM() {
    if (!player) return;

    if (isPlaying) {
        player.pauseVideo();
        isPlaying = false;
        document.getElementById('bgm-toggle').innerText = '음악 켜기';
    } else {
        player.playVideo();
        isPlaying = true;
        document.getElementById('bgm-toggle').innerText = '음악 끄기';
    }
}