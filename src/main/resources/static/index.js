// ==========================================
// 1. 유튜브 브금
// ==========================================
var tag = document.createElement('script');
tag.src = "https://www.youtube.com/iframe_api";
var firstScriptTag = document.getElementsByTagName('script')[0];
firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

var player;
var isPlaying = false;

function onYouTubeIframeAPIReady() {
    player = new YT.Player('youtube-player', {
        height: '0',
        width: '0',
        videoId: 'j2uQ4l9aiVg', // 기본 브금 유튜브id 11자 아무거나 설정가능함
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

function onPlayerReady(event) {
    player.setVolume(25); // 잔잔한 초기 볼륨 (25%)

    // 화면 첫 클릭 시 BGM 재생 시작
    document.body.addEventListener('click', function startAudio() {
        if (!isPlaying && player) {
            player.playVideo();
            isPlaying = true;
            updateBgmButtonText();
        }
        document.body.removeEventListener('click', startAudio);
    }, { once: true });
}

function toggleBGM() {
    if (!player) return;

    if (isPlaying) {
        player.pauseVideo();
        isPlaying = false;
    } else {
        player.playVideo();
        isPlaying = true;
    }
    updateBgmButtonText();
}

function updateBgmButtonText() {
    const btn = document.getElementById('bgm-toggle');
    if (btn) {
        btn.innerText = isPlaying ? '음악 끄기' : '음악 켜기';
    }
}

// ==========================================
// 2. SPA뷰 로직
// ==========================================

// 메인 홈 화면
function showHome() {
    stopGameTimer();
    const content = document.getElementById('content-area');
    content.innerHTML = `
        <div class="brand-sub">suckerson's</div>
        <h1 class="brand-title">AZIT</h1>
        <p class="phrase">
            찾아와 주셔서 감사합니다.<br>
            평안한 시간 되셨으면 좋겠습니다.
        </p>

        <div class="button-group">
            <button onclick="alert('로그인 서비스로 이동합니다.')" class="btn btn-login">로그인 서비스</button>
            <button onclick="showGuestMenu()" class="btn btn-guest">게스트 서비스</button>
        </div>

        <div class="music-player">
            <span>♪ BGM: I'm Not The Only One (Piano)</span>
            <button id="bgm-toggle" class="music-btn" onclick="toggleBGM()">${isPlaying ? '음악 끄기' : '음악 켜기'}</button>
        </div>
    `;
}

// 게스트 서비스 목록 화면
function showGuestMenu() {
    stopGameTimer();
    const content = document.getElementById('content-area');
    content.innerHTML = `
        <div class="brand-sub">Guest Lounge</div>
        <h1 class="brand-title">Guest Service</h1>
        <p class="phrase">
            로그인 없이 이용 가능한 서비스입니다.<br>
            감사합니다.
        </p>

        <div class="guest-grid">
            <div class="guest-card" onclick="openAppleGame()">
                <div class="icon">🍎</div>
                <h3>사과 게임</h3>
                <p>합이 10 만드는 드래그 게임</p>
            </div>
            <div class="guest-card" onclick="alert('빠르게 개발하겠습니다. 감사합니다.')">
                <div class="icon">🎯</div>
                <h3>룰렛</h3>
                <p>돌려돌려 돌림판</p>
            </div>
            <div class="guest-card" onclick="openInstLounge()">
                <div class="icon">🎧</div>
                <h3>Inst 라운지</h3>
                <p>원하는 BGM 선택</p>
            </div>
            <div class="guest-card" onclick="openRandomizer()">
                            <div class="icon">⚔️</div>
                            <h3>BSL</h3>
                            <p>븅신스타리그</p>
                        </div>
        </div>

        <div style="margin-top: 25px;">
            <button onclick="showHome()" class="btn btn-guest" style="padding: 10px 0; font-size: 0.85rem;">← 메인으로 돌아가기</button>
        </div>

        <div class="music-player">
            <span>♪ BGM: I'm Not The Only One (Piano)</span>
            <button id="bgm-toggle" class="music-btn" onclick="toggleBGM()">${isPlaying ? '음악 끄기' : '음악 켜기'}</button>
        </div>
    `;
}

// ==========================================
// 3. 사과게임
// ==========================================
let score = 0;
let timeLeft = 120;
let timerInterval = null;
let boardData = [];
let isDragging = false;
let selectedIndices = [];

function openAppleGame() {
    score = 0;
    timeLeft = 120;
    selectedIndices = [];

    const content = document.getElementById('content-area');
    content.innerHTML = `
        <button class="nav-back-btn" onclick="showGuestMenu()">← 게스트 목록으로</button>

        <div class="game-header">
            <div class="game-stat">점수: <span id="game-score">0</span>점</div>
            <div class="game-stat">남은 시간: <span id="game-timer">120</span>초</div>
        </div>

        <div class="apple-board" id="apple-board"></div>

        <div class="music-player" style="margin-top:20px;">
            <span>♪ BGM: I'm Not The Only One (Piano)</span>
            <button id="bgm-toggle" class="music-btn" onclick="toggleBGM()">${isPlaying ? '음악 끄기' : '음악 켜기'}</button>
        </div>
    `;

    initAppleBoard();
    startGameTimer();
}

function initAppleBoard() {
    const board = document.getElementById('apple-board');
    if(!board) return;
    board.innerHTML = '';
    boardData = [];

    for (let i = 0; i < 100; i++) {
        const val = Math.floor(Math.random() * 9) + 1;
        boardData.push({ id: i, val: val, removed: false });

        const cell = document.createElement('div');
        cell.className = 'apple-cell';
        cell.dataset.index = i;
        cell.innerText = val;
        board.appendChild(cell);
    }

    board.addEventListener('pointerdown', handlePointerDown);
    board.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
}

function handlePointerDown(e) {
    if (e.target.classList.contains('apple-cell') && !e.target.classList.contains('removed')) {
        isDragging = true;
        selectedIndices = [];
        toggleSelectCell(parseInt(e.target.dataset.index));
    }
}

function handlePointerMove(e) {
    if (!isDragging) return;
    const elem = document.elementFromPoint(e.clientX, e.clientY);
    if (elem && elem.classList.contains('apple-cell') && !elem.classList.contains('removed')) {
        const idx = parseInt(elem.dataset.index);
        if (!selectedIndices.includes(idx)) {
            toggleSelectCell(idx);
        }
    }
}

function handlePointerUp() {
    if (!isDragging) return;
    isDragging = false;
    checkAppleSum();
}

function toggleSelectCell(idx) {
    selectedIndices.push(idx);
    const cells = document.querySelectorAll('.apple-cell');
    if (cells[idx]) cells[idx].classList.add('selected');
}

function checkAppleSum() {
    let currentSum = 0;
    selectedIndices.forEach(idx => {
        currentSum += boardData[idx].val;
    });

    if (currentSum === 10) {
        score += selectedIndices.length * 10;
        const scoreElem = document.getElementById('game-score');
        if(scoreElem) scoreElem.innerText = score;

        const cells = document.querySelectorAll('.apple-cell');
        selectedIndices.forEach(idx => {
            boardData[idx].removed = true;
            if(cells[idx]) {
                cells[idx].classList.remove('selected');
                cells[idx].classList.add('removed');
            }
        });
    } else {
        const cells = document.querySelectorAll('.apple-cell');
        selectedIndices.forEach(idx => {
            if(cells[idx]) cells[idx].classList.remove('selected');
        });
    }
    selectedIndices = [];
}

function startGameTimer() {
    stopGameTimer();
    timerInterval = setInterval(() => {
        timeLeft--;
        const timerElem = document.getElementById('game-timer');
        if (timerElem) timerElem.innerText = timeLeft;

        if (timeLeft <= 0) {
            stopGameTimer();
            alert(`게임 종료! 최종 점수: ${score}점`);
            showGuestMenu();
        }
    }, 1000);
}

function stopGameTimer() {
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
}

// ==========================================
// Inst Lounge
// ==========================================

const longInstPlaylist = {
    ghibli:  { title: "지브리 오케스트라 / 피아노", id: "7lq6e4Lu4B8" },
    jazz:    { title: "감성 재즈 Lounge", id: "Llour2YvsiI" },
    pop:     { title: "팝송 피아노 Cover", id: "5yGyOIy_VPQ" },
    disney:  { title: "디즈니 명곡 피아노 메들리", id: "oz9l_Vi5ueE" },
    gayo:    { title: "감성 가요 Inst", id: "TupCRNmutvE" }
};

function openInstLounge() {
    const content = document.getElementById('content-area');

    content.innerHTML = `
        <button class="nav-back-btn" onclick="showGuestMenu()" style="background:none; border:none; color:#ccc; cursor:pointer; margin-bottom:15px;">← 게스트 메뉴로</button>

        <div class="brand-sub">Inst Lounge</div>
        <h1 class="brand-title" style="font-size: 2rem; margin-bottom: 10px;">Inst Select</h1>
        <p class="phrase" style="margin-bottom: 25px;">원하는 장르를 누르면 BGM이 즉시 전환됩니다.</p>

        <div class="inst-simple-group" style="display:flex; flex-direction:column; gap:10px; max-width:300px; margin:0 auto;">
            <button class="btn btn-guest" onclick="playLongTrack('ghibli')">Studio Ghibli</button>
            <button class="btn btn-guest" onclick="playLongTrack('jazz')">Jazz</button>
            <button class="btn btn-guest" onclick="playLongTrack('pop')">POP</button>
            <button class="btn btn-guest" onclick="playLongTrack('disney')">Disney</button>
            <button class="btn btn-guest" onclick="playLongTrack('gayo')">가요</button>
        </div>

        <div class="music-player" style="margin-top:25px;">
            <span>♪ BGM: I'm Not The Only One (Piano)</span>
            <button id="bgm-toggle" class="music-btn" onclick="toggleBGM()">${isPlaying ? '음악 끄기' : '음악 켜기'}</button>
        </div>
    `;
}

function playLongTrack(genreKey) {
    const track = longInstPlaylist[genreKey];
    if (!track || !player) return;

    // 선택한 유튜브 영상으로 교체 및 자동 재생
    player.loadVideoById(track.id);
    isPlaying = true;
    updateBgmButtonText();

    // 하단 재생 중인 음원 제목 업데이트
    const bgmSpans = document.querySelectorAll('.music-player span');
    bgmSpans.forEach(span => {
        span.innerText = `♪ 재생 중: ${track.title}`;
    });
}
// ==========================================
// 병신스타리그 추첨
// ==========================================

let customOptions = [
    { id: 1, title: "옵션 1", drawCount: 2, items: ["석현", "태훈", "민혁"] }
];
let nextOptionId = 2;

function openRandomizer() {
    const content = document.getElementById('content-area');

    content.innerHTML = `
        <button class="nav-back-btn" onclick="showGuestMenu()" style="background:none; border:none; color:#ccc; cursor:pointer; margin-bottom:10px;">← 게스트 메뉴로</button>

        <div class="brand-sub">BSL</div>
        <h1 class="brand-title" style="font-size: 1.8rem; margin-bottom: 5px;">븅신스타리그</h1>
        <p class="phrase" style="margin-bottom: 20px;">한놈뒤질때까지하는거야</p>

        <div id="options-container" style="display:flex; flex-direction:column; gap:12px; margin-bottom:15px; text-align:left;">
        </div>

        <button onclick="addOptionGroup()" style="width:100%; padding:10px; background:rgba(255,255,255,0.08); border:1px dashed rgba(255,255,255,0.3); color:#fff; border-radius:8px; cursor:pointer; font-size:0.85rem; margin-bottom:15px;">
            + 옵션 추가
        </button>

        <button class="btn btn-guest" onclick="runRandomizerDraw()" style="width:100%; padding:12px; font-weight:bold; background:rgba(255,255,255,0.2); margin-bottom:15px;">
            결과
        </button>

        <div id="randomizer-result" style="display:none; background:rgba(0,0,0,0.5); padding:15px; border-radius:8px; border:1px solid rgba(255,255,255,0.1); text-align:center;">
        </div>

        <div class="music-player" style="margin-top:20px;">
            <span>♪ BGM: I'm Not The Only One (Piano)</span>
            <button id="bgm-toggle" class="music-btn" onclick="toggleBGM()">${isPlaying ? '음악 끄기' : '음악 켜기'}</button>
        </div>
    `;

    renderOptionGroups();
}

function renderOptionGroups() {
    const container = document.getElementById('options-container');
    if (!container) return;

    container.innerHTML = customOptions.map((opt, groupIdx) => `
        <div style="background:rgba(255,255,255,0.05); padding:12px; border-radius:8px; border:1px solid rgba(255,255,255,0.1);">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                <input type="text" value="${opt.title}" onchange="updateOptionTitle(${opt.id}, this.value)" style="background:transparent; border:none; border-bottom:1px solid #666; color:#fff; font-size:0.9rem; font-weight:bold; width:60%; outline:none;" placeholder="옵션 이름">
                <div style="display:flex; align-items:center; gap:6px;">
                    <span style="font-size:0.75rem; color:#aaa;">뽑을 개수:</span>
                    <input type="number" min="1" value="${opt.drawCount}" onchange="updateOptionDrawCount(${opt.id}, this.value)" style="width:40px; padding:2px 4px; background:#222; border:1px solid #444; color:#fff; border-radius:4px; font-size:0.8rem; text-align:center;">
                    ${customOptions.length > 1 ? `<button onclick="removeOptionGroup(${opt.id})" style="background:none; border:none; color:#ff6b6b; cursor:pointer; font-weight:bold; margin-left:4px;">×</button>` : ''}
                </div>
            </div>

            <div style="display:flex; gap:5px; margin-bottom:8px;">
                <input type="text" id="opt-input-${opt.id}" placeholder="항목 입력" style="width:100%; padding:6px; background:#222; border:1px solid #444; color:#fff; border-radius:4px; font-size:0.8rem;" onkeypress="if(event.key==='Enter') addOptionItem(${opt.id})">
                <button onclick="addOptionItem(${opt.id})" style="padding:6px 10px; background:#444; color:#fff; border:none; border-radius:4px; cursor:pointer; font-size:0.8rem; white-space:nowrap;">추가</button>
            </div>

            <div style="display:flex; flex-wrap:wrap; gap:4px; max-height:70px; overflow-y:auto;">
                ${opt.items.map((item, itemIdx) => `
                    <span style="background:#333; padding:2px 6px; border-radius:4px; font-size:0.75rem; display:inline-flex; align-items:center; gap:4px;">
                        ${item} <b onclick="removeOptionItem(${opt.id}, ${itemIdx})" style="cursor:pointer; color:#ff6b6b;">×</b>
                    </span>
                `).join('')}
            </div>
        </div>
    `).join('');
}

function addOptionGroup() {
    customOptions.push({
        id: nextOptionId++,
        title: `옵션 ${customOptions.length + 1}`,
        drawCount: 1,
        items: []
    });
    renderOptionGroups();
}

function removeOptionGroup(id) {
    customOptions = customOptions.filter(opt => opt.id !== id);
    renderOptionGroups();
}

function updateOptionTitle(id, val) {
    const opt = customOptions.find(o => o.id === id);
    if (opt) opt.title = val.trim() || "옵션";
}

function updateOptionDrawCount(id, val) {
    const opt = customOptions.find(o => o.id === id);
    if (opt) opt.drawCount = Math.max(1, parseInt(val) || 1);
}

function addOptionItem(id) {
    const input = document.getElementById(`opt-input-${id}`);
    if (!input) return;
    const val = input.value.trim();
    const opt = customOptions.find(o => o.id === id);

    if (val && opt) {
        opt.items.push(val);
        input.value = '';
        renderOptionGroups();
    }
}

function removeOptionItem(id, itemIndex) {
    const opt = customOptions.find(o => o.id === id);
    if (opt) {
        opt.items.splice(itemIndex, 1);
        renderOptionGroups();
    }
}

function runRandomizerDraw() {
    if (customOptions.length === 0) {
        alert('QA하지말고 제대로 돌려라');
        return;
    }

    let results = [];

    for (let opt of customOptions) {
        if (opt.items.length === 0) continue;

        if (opt.items.length < opt.drawCount) {
            alert(`[${opt.title}] 항목 개수(${opt.items.length}개)가 뽑을 개수(${opt.drawCount}개)보다 적습니다!`);
            return;
        }

        const shuffled = [...opt.items].sort(() => Math.random() - 0.5);
        const picked = shuffled.slice(0, opt.drawCount);
        results.push({ title: opt.title, picked: picked });
    }

    if (results.length === 0) {
        alert('추첨할 항목을 최하 1개 이상 입력해 주세요!');
        return;
    }

    const resultDiv = document.getElementById('randomizer-result');
    resultDiv.style.display = 'block';

    resultDiv.innerHTML = `
        <h3 style="color:#4cd137; margin:0 0 10px 0; font-size:1.1rem;">결과</h3>
        <div style="display:flex; flex-direction:column; gap:8px; text-align:left; background:rgba(255,255,255,0.05); padding:10px; border-radius:6px; font-size:0.85rem;">
            ${results.map((res, idx) => `
                <div>
                    <b>결과 ${idx + 1} (${res.title}) :</b>
                    <span style="color:#00a8ff; font-weight:bold;">${res.picked.join(' vs ')}</span>
                </div>
            `).join('')}
        </div>
    `;
}