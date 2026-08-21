let items = ["A팀/1번", "B팀/2번", "A팀/3번", "B팀/4번", "A팀/5번", "B팀/6번"];
const colors = ["#ff4757", "#ffa502", "#2ed573", "#1e90ff", "#3742fa", "#8e44ad", "#e84393", "#00b894"];

let selectedArrowCount = 1;
let currentRotation = 0;
let isSpinning = false;

window.onload = () => {
    renderItemList();
    updateArrowOptions();
    drawWheel();
    renderArrows();
};

function renderItemList() {
    const itemList = document.getElementById('itemList');
    itemList.innerHTML = '';

    items.forEach((item, index) => {
        const row = document.createElement('div');
        row.className = 'item-row';
        row.innerHTML = `
            <input type="text" value="${item}" onchange="updateItemText(${index}, this.value)">
            ${items.length > 2 ? `<button class="btn del-btn" onclick="deleteItem(${index})">X</button>` : ''}
        `;
        itemList.appendChild(row);
    });
}

function updateArrowOptions() {
    const select = document.getElementById('arrowCount');
    const currentVal = selectedArrowCount;
    select.innerHTML = '';

    // 화살표는 최대 (항목 개수 - 1)개까지만 선택 가능
    for (let i = 1; i < items.length; i++) {
        const opt = document.createElement('option');
        opt.value = i;
        opt.innerText = `${i}개 화살표 (${i}명 당첨)`;
        if (i === currentVal) opt.selected = true;
        select.appendChild(opt);
    }
    selectedArrowCount = parseInt(select.value) || 1;
}

function updateArrowCount() {
    selectedArrowCount = parseInt(document.getElementById('arrowCount').value);
    renderArrows();
}

function renderArrows() {
    const overlay = document.getElementById('arrowOverlay');
    overlay.innerHTML = '';

    // 화살표들을 360도에 균등한 간격으로 배치
    const step = 360 / selectedArrowCount;
    for (let i = 0; i < selectedArrowCount; i++) {
        const pin = document.createElement('div');
        pin.className = 'pin';
        pin.style.transform = `rotate(${i * step}deg)`;
        overlay.appendChild(pin);
    }
}

function addItem() {
    items.push(`항목 ${items.length + 1}`);
    renderItemList();
    updateArrowOptions();
    drawWheel();
    renderArrows();
}

function deleteItem(index) {
    if (items.length <= 2) return;
    items.splice(index, 1);
    renderItemList();
    updateArrowOptions();
    drawWheel();
    renderArrows();
}

function updateItemText(index, value) {
    items[index] = value;
    drawWheel();
}

function updateWheel() {
    drawWheel();
    alert('룰렛이 새로 적용되었습니다!');
}

function drawWheel() {
    const canvas = document.getElementById('wheelCanvas');
    const ctx = canvas.getContext('2d');
    const numItems = items.length;
    const arcSize = (2 * Math.PI) / numItems;
    const radius = canvas.width / 2;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    items.forEach((item, i) => {
        const angle = i * arcSize;
        ctx.beginPath();
        ctx.fillStyle = colors[i % colors.length];
        ctx.moveTo(radius, radius);
        ctx.arc(radius, radius, radius, angle, angle + arcSize);
        ctx.lineTo(radius, radius);
        ctx.fill();

        ctx.save();
        ctx.translate(radius, radius);
        ctx.rotate(angle + arcSize / 2);
        ctx.textAlign = "right";
        ctx.fillStyle = "#ffffff";
        ctx.font = "bold 14px Arial";
        ctx.fillText(item, radius - 20, 5);
        ctx.restore();
    });
}

function spinWheel() {
    if (isSpinning) return;
    isSpinning = true;

    document.getElementById('resetBtn').style.display = 'none';
    document.getElementById('resultText').innerText = "두구두구두구...";

    const arcDegree = 360 / items.length;
    let randomDegree = Math.floor(Math.random() * 360) + 1800;

    // [실금 보정] 혹시라도 경계선에 딱 걸리면 조각의 중앙으로 약간 당겨줌
    const targetOffset = (currentRotation + randomDegree) % arcDegree;
    if (targetOffset < 2 || targetOffset > arcDegree - 2) {
        randomDegree += 5; // 5도 더 돌려 경계선 탈출
    }

    currentRotation += randomDegree;

    const canvas = document.getElementById('wheelCanvas');
    canvas.style.transform = `rotate(${currentRotation}deg)`;

    setTimeout(() => {
        const winners = [];
        const actualDegree = currentRotation % 360;
        const arrowStep = 360 / selectedArrowCount;

        // 각 화살표가 가리키는 항목 계산
        for (let i = 0; i < selectedArrowCount; i++) {
            const arrowAngle = (i * arrowStep) % 360;
            const effectiveAngle = (360 - (actualDegree % 360) + arrowAngle + 270) % 360;
            const selectedIndex = Math.floor(effectiveAngle / arcDegree) % items.length;

            if (!winners.includes(items[selectedIndex])) {
                winners.push(items[selectedIndex]);
            }
        }

        document.getElementById('resultText').innerHTML = `🎉 당첨 (${winners.length}명):<br>[ ${winners.join(', ')} ]`;
        document.getElementById('resetBtn').style.display = 'inline-block';
        isSpinning = false;
    }, 4000);
}

function resetSpin() {
    document.getElementById('resultText').innerText = "SPIN 버튼을 눌러 다시 돌려보세요!";
    document.getElementById('resetBtn').style.display = 'none';
}