// Reward Play Hub Engine & Android AdMob Bridge
(function () {
  'use strict';

  // State
  let userCoins = 500;
  let userUsd = 0.50;
  let isSpinning = false;
  let activeTab = 'wheel';
  let isScratching = false;
  let scratchComplete = false;
  let minerEnergy = 20;
  let minerPendingCoins = 50;

  // Wheel Segments
  const segments = [
    { label: '50', coins: 50, color: '#f72585' },
    { label: '100', coins: 100, color: '#7209b7' },
    { label: '250', coins: 250, color: '#3a0ca3' },
    { label: '500', coins: 500, color: '#4361ee' },
    { label: '75', coins: 75, color: '#4cc9f0' },
    { label: '150', coins: 150, color: '#06d6a0' },
    { label: '1000', coins: 1000, color: '#ffd166' },
    { label: '300', coins: 300, color: '#ff8800' }
  ];

  let currentRotation = 0;

  // DOM Elements
  const headerCoinCount = document.getElementById('headerCoinCount');
  const headerUsdValue = document.getElementById('headerUsdValue');
  const adStatusPill = document.getElementById('adStatusPill');
  const statusDot = document.getElementById('statusDot');
  const statusText = document.getElementById('statusText');
  const rewardModal = document.getElementById('rewardModal');
  const modalRewardTitle = document.getElementById('modalRewardTitle');
  const modalRewardAmount = document.getElementById('modalRewardAmount');
  const modalRewardDesc = document.getElementById('modalRewardDesc');
  const modalCollectBtn = document.getElementById('modalCollectBtn');

  // --- ANDROID BRIDGE INTERFACE ---
  function hasAndroidBridge() {
    return typeof window.AndroidBridge !== 'undefined';
  }

  function callNativeShowAd(source, multiplier, bonusCoins) {
    if (hasAndroidBridge()) {
      window.AndroidBridge.showRewardedAd(source, multiplier, bonusCoins);
    } else {
      console.log(`[Web Standalone] Simulating AdMob Rewarded Ad for ${source}`);
      setTimeout(() => {
        window.onAdCompleted(source, bonusCoins * multiplier, userCoins + bonusCoins * multiplier);
      }, 1000);
    }
  }

  function callNativeRecordReward(gameType, coins) {
    if (hasAndroidBridge()) {
      window.AndroidBridge.recordGameReward(gameType, coins);
    } else {
      updateLocalCoins(coins);
    }
  }

  function callNativeHaptic(effect) {
    if (hasAndroidBridge()) {
      window.AndroidBridge.triggerHaptic(effect || 'medium');
    }
  }

  function callNativeToast(msg) {
    if (hasAndroidBridge()) {
      window.AndroidBridge.showToast(msg);
    } else {
      alert(msg);
    }
  }

  // --- NATIVE TO JS CALLBACKS ---
  window.onAdCompleted = function (source, bonusCoins, newTotalCoins) {
    console.log(`[AdMob Callback] Ad Completed: source=${source}, bonus=${bonusCoins}`);
    callNativeHaptic('heavy');
    if (typeof newTotalCoins === 'number') {
      userCoins = newTotalCoins;
      userUsd = userCoins / 1000.0;
      renderBalance();
    } else {
      updateLocalCoins(bonusCoins);
    }
    showRewardPopup("Ad Reward Unlocked!", `+${bonusCoins} COINS`, "You watched the sponsored video! Rewarded coins credited.");
  };

  window.onAdFailed = function (reason) {
    console.warn(`[AdMob Callback] Ad Failed: ${reason}`);
    callNativeToast(reason || "Ad failed or closed before completion.");
  };

  window.updateNativeBalance = function (coins, usdVal) {
    userCoins = coins;
    userUsd = usdVal;
    renderBalance();
  };

  window.setAdStatus = function (statusStr, isReady) {
    if (statusText && statusDot) {
      statusText.textContent = statusStr;
      if (isReady) {
        statusDot.className = "status-dot ready";
      } else {
        statusDot.className = "status-dot cooldown";
      }
    }
  };

  function updateLocalCoins(amount) {
    userCoins += amount;
    userUsd = userCoins / 1000.0;
    renderBalance();
  }

  function renderBalance() {
    if (headerCoinCount) headerCoinCount.textContent = userCoins.toLocaleString();
    if (headerUsdValue) headerUsdValue.textContent = `≈ $${userUsd.toFixed(2)}`;
  }

  function showRewardPopup(title, amountStr, desc) {
    modalRewardTitle.textContent = title;
    modalRewardAmount.textContent = amountStr;
    modalRewardDesc.textContent = desc;
    rewardModal.classList.add('show');
  }

  modalCollectBtn.addEventListener('click', () => {
    rewardModal.classList.remove('show');
    callNativeHaptic('light');
  });

  // --- TAB NAVIGATION ---
  const tabBtns = document.querySelectorAll('.tab-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetTab = btn.getAttribute('data-tab');
      switchTab(targetTab);
    });
  });

  function switchTab(tabId) {
    activeTab = tabId;
    tabBtns.forEach(b => b.classList.toggle('active', b.getAttribute('data-tab') === tabId));
    document.querySelectorAll('.game-section').forEach(sec => {
      sec.classList.toggle('active', sec.id === `tab-${tabId}`);
    });
    callNativeHaptic('light');

    if (tabId === 'wheel') drawWheel();
    if (tabId === 'scratch' && !scratchComplete) initScratchCard();
  }

  // --- TAB 1: LUCKY WHEEL IMPLEMENTATION ---
  const wheelCanvas = document.getElementById('wheelCanvas');
  const wheelCtx = wheelCanvas.getContext('2d');
  const spinFreeBtn = document.getElementById('spinFreeBtn');
  const spin2xAdBtn = document.getElementById('spin2xAdBtn');
  const wheelCenterBtn = document.getElementById('wheelCenterBtn');

  function drawWheel() {
    if (!wheelCanvas) return;
    const width = wheelCanvas.width;
    const height = wheelCanvas.height;
    const center = width / 2;
    const radius = center - 8;
    const arc = (2 * Math.PI) / segments.length;

    wheelCtx.clearRect(0, 0, width, height);

    segments.forEach((seg, i) => {
      const angle = i * arc;
      wheelCtx.beginPath();
      wheelCtx.fillStyle = seg.color;
      wheelCtx.moveTo(center, center);
      wheelCtx.arc(center, center, radius, angle, angle + arc);
      wheelCtx.lineTo(center, center);
      wheelCtx.fill();
      wheelCtx.lineWidth = 2;
      wheelCtx.strokeStyle = '#ffffff33';
      wheelCtx.stroke();

      // Text label
      wheelCtx.save();
      wheelCtx.translate(center, center);
      wheelCtx.rotate(angle + arc / 2);
      wheelCtx.textAlign = 'right';
      wheelCtx.fillStyle = '#ffffff';
      wheelCtx.font = 'bold 16px sans-serif';
      wheelCtx.shadowColor = 'rgba(0,0,0,0.6)';
      wheelCtx.shadowBlur = 4;
      wheelCtx.fillText(`${seg.label} 🪙`, radius - 20, 6);
      wheelCtx.restore();
    });

    // Outer ring
    wheelCtx.beginPath();
    wheelCtx.arc(center, center, radius, 0, 2 * Math.PI);
    wheelCtx.lineWidth = 6;
    wheelCtx.strokeStyle = '#ffb703';
    wheelCtx.stroke();
  }

  function spinWheel(multiplier) {
    if (isSpinning) return;
    isSpinning = true;
    callNativeHaptic('medium');

    const randomIndex = Math.floor(Math.random() * segments.length);
    const selected = segments[randomIndex];
    const arcDeg = 360 / segments.length;
    // Align segment to top pointer (270 degrees)
    const extraRounds = 5 * 360;
    const targetDeg = extraRounds + (360 - (randomIndex * arcDeg + arcDeg / 2)) - 90;

    currentRotation += targetDeg;
    wheelCanvas.style.transform = `rotate(${currentRotation}deg)`;

    setTimeout(() => {
      isSpinning = false;
      const winCoins = selected.coins * multiplier;
      callNativeRecordReward('LUCKY_SPIN', winCoins);
      showRewardPopup(
        multiplier > 1 ? "2X Booster Win!" : "Wheel Fortune Won!",
        `+${winCoins} COINS`,
        `You landed on ${selected.coins} coins with a ${multiplier}X reward boost!`
      );
    }, 3600);
  }

  spinFreeBtn.addEventListener('click', () => spinWheel(1));
  wheelCenterBtn.addEventListener('click', () => spinWheel(1));
  spin2xAdBtn.addEventListener('click', () => {
    callNativeShowAd('SPIN_2X', 2, 200);
  });

  // --- TAB 2: SCRATCH CARD IMPLEMENTATION ---
  const scratchCanvas = document.getElementById('scratchCanvas');
  const scratchCtx = scratchCanvas ? scratchCanvas.getContext('2d') : null;
  const scratchPercentEl = document.getElementById('scratchPercent');
  const scratchProgressFill = document.getElementById('scratchProgressFill');
  const resetScratchBtn = document.getElementById('resetScratchBtn');
  const doubleScratchAdBtn = document.getElementById('doubleScratchAdBtn');
  const scratchPrizeCoins = document.getElementById('scratchPrizeCoins');
  let currentScratchPrize = 250;

  function initScratchCard() {
    if (!scratchCanvas || !scratchCtx) return;
    const w = scratchCanvas.width;
    const h = scratchCanvas.height;
    scratchComplete = false;

    // Pick random prize
    const prizes = [100, 150, 250, 300, 500];
    currentScratchPrize = prizes[Math.floor(Math.random() * prizes.length)];
    if (scratchPrizeCoins) scratchPrizeCoins.textContent = `+${currentScratchPrize} COINS`;

    // Fill silver scratch coating
    scratchCtx.globalCompositeOperation = 'source-over';
    const grad = scratchCtx.createLinearGradient(0, 0, w, h);
    grad.addColorStop(0, '#adb5bd');
    grad.addColorStop(0.5, '#dee2e6');
    grad.addColorStop(1, '#6c757d');
    scratchCtx.fillStyle = grad;
    scratchCtx.fillRect(0, 0, w, h);

    // Decorative pattern
    scratchCtx.fillStyle = '#495057';
    scratchCtx.font = 'bold 16px sans-serif';
    scratchCtx.textAlign = 'center';
    scratchCtx.fillText('★ SCRATCH HERE TO WIN ★', w / 2, h / 2 - 10);
    scratchCtx.font = '12px sans-serif';
    scratchCtx.fillText('Swipe finger or mouse', w / 2, h / 2 + 15);

    updateScratchPercent(0);
  }

  function scratchAt(x, y) {
    if (!scratchCtx || scratchComplete) return;
    scratchCtx.globalCompositeOperation = 'destination-out';
    scratchCtx.beginPath();
    scratchCtx.arc(x, y, 22, 0, 2 * Math.PI);
    scratchCtx.fill();
    callNativeHaptic('light');

    checkScratchProgress();
  }

  function checkScratchProgress() {
    if (scratchComplete || !scratchCtx) return;
    const w = scratchCanvas.width;
    const h = scratchCanvas.height;
    const imgData = scratchCtx.getImageData(0, 0, w, h);
    const data = imgData.data;
    let transparentCount = 0;
    const sampleStep = 32; // check every 8th pixel for high performance

    for (let i = 3; i < data.length; i += sampleStep) {
      if (data[i] === 0) transparentCount++;
    }

    const totalSamples = data.length / sampleStep;
    const percent = Math.min(100, Math.round((transparentCount / totalSamples) * 100));
    updateScratchPercent(percent);

    if (percent >= 60 && !scratchComplete) {
      scratchComplete = true;
      // Clear remaining canvas
      scratchCtx.clearRect(0, 0, w, h);
      callNativeRecordReward('SCRATCH_CARD', currentScratchPrize);
      showRewardPopup("Card Scratched!", `+${currentScratchPrize} COINS`, "You fully scratched the lucky prize coat!");
    }
  }

  function updateScratchPercent(p) {
    if (scratchPercentEl) scratchPercentEl.textContent = `Scratched: ${p}%`;
    if (scratchProgressFill) scratchProgressFill.style.width = `${p}%`;
  }

  if (scratchCanvas) {
    const handleScratchMove = (e) => {
      if (!isScratching) return;
      const rect = scratchCanvas.getBoundingClientRect();
      const scaleX = scratchCanvas.width / rect.width;
      const scaleY = scratchCanvas.height / rect.height;
      let clientX = e.clientX;
      let clientY = e.clientY;
      if (e.touches && e.touches[0]) {
        clientX = e.touches[0].clientX;
        clientY = e.touches[0].clientY;
      }
      const x = (clientX - rect.left) * scaleX;
      const y = (clientY - rect.top) * scaleY;
      scratchAt(x, y);
    };

    scratchCanvas.addEventListener('mousedown', (e) => { isScratching = true; handleScratchMove(e); });
    window.addEventListener('mouseup', () => { isScratching = false; });
    scratchCanvas.addEventListener('mousemove', handleScratchMove);

    scratchCanvas.addEventListener('touchstart', (e) => { isScratching = true; handleScratchMove(e); }, { passive: true });
    window.addEventListener('touchend', () => { isScratching = false; });
    scratchCanvas.addEventListener('touchmove', handleScratchMove, { passive: true });
  }

  if (resetScratchBtn) {
    resetScratchBtn.addEventListener('click', initScratchCard);
  }

  if (doubleScratchAdBtn) {
    doubleScratchAdBtn.addEventListener('click', () => {
      callNativeShowAd('SCRATCH_3X', 3, currentScratchPrize);
    });
  }

  // --- TAB 3: CHESTS IMPLEMENTATION ---
  const chestBtns = document.querySelectorAll('.unlock-chest-btn');
  chestBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const chestId = btn.getAttribute('data-chest');
      const bonus = chestId === '1' ? 150 : (chestId === '2' ? 300 : 500);
      callNativeShowAd(`CHEST_${chestId}`, 1, bonus);
    });
  });

  // --- TAB 4: TURBO MINER IMPLEMENTATION ---
  const batteryFill = document.getElementById('batteryFill');
  const energyPercent = document.getElementById('energyPercent');
  const pendingCoinsEl = document.getElementById('pendingCoins');
  const tapChargeBtn = document.getElementById('tapChargeBtn');
  const superchargeAdBtn = document.getElementById('superchargeAdBtn');

  function updateMinerUI() {
    if (batteryFill) batteryFill.style.height = `${minerEnergy}%`;
    if (energyPercent) energyPercent.textContent = `${minerEnergy}%`;
    minerPendingCoins = Math.round(minerEnergy * 2.5);
    if (pendingCoinsEl) pendingCoinsEl.textContent = `+${minerPendingCoins} Coins`;
  }

  if (tapChargeBtn) {
    tapChargeBtn.addEventListener('click', () => {
      callNativeHaptic('light');
      minerEnergy = Math.min(100, minerEnergy + 10);
      updateMinerUI();

      if (minerEnergy >= 100) {
        minerEnergy = 10;
        const payout = 250;
        callNativeRecordReward('TURBO_MINER', payout);
        showRewardPopup("Battery Overcharged!", `+${payout} COINS`, "Full 100% capacity reached! Energy converted to coins.");
      }
    });
  }

  if (superchargeAdBtn) {
    superchargeAdBtn.addEventListener('click', () => {
      callNativeShowAd('MINER_SUPERCHARGE', 1, 300);
      minerEnergy = 100;
      updateMinerUI();
    });
  }

  // --- INITIALIZATION ---
  window.addEventListener('DOMContentLoaded', () => {
    drawWheel();
    initScratchCard();
    renderBalance();

    // Query balance from native bridge on startup
    if (hasAndroidBridge()) {
      try {
        const raw = window.AndroidBridge.getUserBalance();
        if (raw) {
          const data = JSON.parse(raw);
          userCoins = data.coins || 500;
          userUsd = data.usd || (userCoins / 1000.0);
          renderBalance();
        }
      } catch (e) {
        console.error('Error fetching balance from AndroidBridge', e);
      }
    }
  });

})();
