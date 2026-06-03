/* ==========================================================================
   Google Messages Web Sync: Core Dashboard & Synchronizer (dashboard.js)
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  // --- State Configuration ---
  const state = {
    token: localStorage.getItem('messages_web_token') || '',
    user: null,
    threads: [],
    activeThreadPhone: null,
    activeFilter: 'all',
    searchQuery: '',
    batteryLevel: 100,
    networkSignal: 'Strong',
    telemetryOnline: true
  };

  const API_URL = ''; // Relative paths since frontend is served statically by backend

  // --- UI Elements Cache ---
  const ui = {
    authScreen: document.getElementById('auth-screen'),
    dashboardScreen: document.getElementById('dashboard-screen'),
    
    // Auth elements
    loginForm: document.getElementById('login-form'),
    loginEmail: document.getElementById('login-email'),
    loginPassword: document.getElementById('login-password'),
    tabLogin: document.getElementById('tab-login'),
    tabPair: document.getElementById('tab-pair'),
    loginFormContainer: document.getElementById('login-form-container'),
    pairContainer: document.getElementById('pair-container'),
    pairTokenDisplay: document.getElementById('pair-token-display'),
    
    // Header telemetry
    pairedDeviceBattery: document.getElementById('paired-device-battery'),
    pairedDeviceNetwork: document.getElementById('paired-device-network'),
    batteryIcon: document.getElementById('battery-icon'),
    networkIcon: document.getElementById('network-icon'),
    
    // Sidebar elements
    searchInput: document.getElementById('search-input'),
    searchClear: document.getElementById('search-clear'),
    filterPills: document.querySelectorAll('.pill'),
    convListContainer: document.getElementById('conv-list-container'),
    
    // Chat window elements
    chatEmptyState: document.getElementById('chat-empty-state'),
    chatView: document.getElementById('chat-view'),
    activeAvatar: document.getElementById('active-avatar'),
    activeContactName: document.getElementById('active-contact-name'),
    activeContactPhone: document.getElementById('active-contact-phone'),
    messagesTimeline: document.getElementById('messages-timeline'),
    chatTextarea: document.getElementById('chat-textarea'),
    sendMsgBtn: document.getElementById('send-msg-btn'),
    typingIndicator: document.getElementById('typing-indicator'),
    
    // Sliding Drawers
    forwardDrawer: document.getElementById('forward-drawer'),
    toggleForwardPanel: document.getElementById('toggle-forward-panel'),
    closeForwardDrawer: document.getElementById('close-forward-drawer'),
    forwardEnableToggle: document.getElementById('forward-enable-toggle'),
    forwardPhoneInput: document.getElementById('forward-phone-input'),
    saveForwardingBtn: document.getElementById('save-forwarding-btn'),
    
    // SMS Simulator drawer
    simulatorDrawer: document.getElementById('simulator-drawer'),
    toggleSimulator: document.getElementById('toggle-simulator'),
    closeSimulatorDrawer: document.getElementById('close-simulator-drawer'),
    simForm: document.getElementById('simulator-form'),
    simPreset: document.getElementById('sim-sender-preset'),
    simPhoneInput: document.getElementById('sim-sender-phone'),
    simPhoneContainer: document.getElementById('sim-custom-number-container'),
    simBody: document.getElementById('sim-body'),
    simLogsContainer: document.getElementById('logs-container'),
    clearLogsBtn: document.getElementById('clear-logs-btn'),
    
    // Extras
    logoutBtn: document.getElementById('logout-btn'),
    toast: document.getElementById('toast'),
    toastIcon: document.getElementById('toast-icon'),
    toastMsg: document.getElementById('toast-message')
  };


  // ==========================================================================
  // 1. CORE ROUTING & AUTHENTICATION HANDSHAKES
  // ==========================================================================
  
  function checkSession() {
    if (state.token) {
      fetchProfile();
    } else {
      showScreen('auth');
    }
  }

  function showScreen(screen) {
    if (screen === 'auth') {
      ui.dashboardScreen.classList.remove('active');
      ui.authScreen.classList.add('active');
    } else {
      ui.authScreen.classList.remove('active');
      ui.dashboardScreen.classList.add('active');
    }
  }

  // Fetch paired user profile & credentials
  async function fetchProfile() {
    try {
      const res = await fetch(`${API_URL}/api/users/profile`, {
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      if (!res.ok) throw new Error('Session handshake expired.');
      
      const user = await res.json();
      state.user = user;
      
      // Update Settings UI inputs
      ui.forwardEnableToggle.checked = user.is_forward_enabled === 1;
      ui.forwardPhoneInput.value = user.forward_number || '';
      
      // Update Telemetry indicators
      updateTelemetryUI(user);
      
      // Load messages
      fetchMessages();
      showScreen('dashboard');
      showToast('verified_user', 'Session restored. Synced up securely!');

      // Set up background sync interval (every 4 seconds) to simulate live synchronization
      if (window.syncInterval) clearInterval(window.syncInterval);
      window.syncInterval = setInterval(fetchMessages, 4000);

    } catch (e) {
      console.error(e);
      state.token = '';
      localStorage.removeItem('messages_web_token');
      showScreen('auth');
    }
  }

  // Login handler
  ui.loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = ui.loginEmail.value.trim();
    const password = ui.loginPassword.value;

    try {
      const res = await fetch(`${API_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const data = await res.json();

      if (!res.ok) return showToast('error', data.error || 'Login failed.');

      // Save persistent 99-year token
      state.token = data.token;
      localStorage.setItem('messages_web_token', data.token);
      
      // Set pairing code visual
      ui.pairTokenDisplay.textContent = data.pair_token;

      fetchProfile();
    } catch (e) {
      showToast('error', 'Unable to reach sync server.');
    }
  });

  // Toggle Login/Pair tabs on authentication card
  ui.tabLogin.addEventListener('click', () => {
    ui.tabPair.classList.remove('active');
    ui.tabLogin.classList.add('active');
    ui.pairContainer.classList.remove('active');
    ui.loginFormContainer.classList.add('active');
  });

  ui.tabPair.addEventListener('click', () => {
    ui.tabLogin.classList.remove('active');
    ui.tabPair.classList.add('active');
    ui.loginFormContainer.classList.remove('active');
    ui.pairContainer.classList.add('active');
  });

  // Sign out
  ui.logoutBtn.addEventListener('click', () => {
    state.token = '';
    localStorage.removeItem('messages_web_token');
    if (window.syncInterval) clearInterval(window.syncInterval);
    showScreen('auth');
    showToast('logout', 'Signed out successfully.');
  });


  // ==========================================================================
  // 2. TELEMETRY & DEVICE INTEGRATIONS
  // ==========================================================================
  
  function updateTelemetryUI(user) {
    ui.pairedDeviceBattery.textContent = `${user.battery_level}%`;
    ui.pairedDeviceNetwork.textContent = user.network_signal;

    // Battery icon state
    if (user.battery_level > 80) ui.batteryIcon.textContent = 'battery_full';
    else if (user.battery_level > 30) ui.batteryIcon.textContent = 'battery_std';
    else ui.batteryIcon.textContent = 'battery_alert';

    // Network icon state
    if (user.network_signal === 'Strong') ui.networkIcon.textContent = 'signal_cellular_4_bar';
    else if (user.network_signal === 'Moderate') ui.networkIcon.textContent = 'signal_cellular_3_bar';
    else ui.networkIcon.textContent = 'signal_cellular_1_bar';

    // Update pair token badge just in case
    ui.pairTokenDisplay.textContent = user.pair_token;
  }


  // ==========================================================================
  // 3. SYNCHRONIZING THREADS & MESSAGE LISTS
  // ==========================================================================
  
  async function fetchMessages(force = false) {
    if (!state.token) return;
    
    try {
      const url = new URL(`${window.location.origin}/api/messages`);
      if (state.searchQuery) url.searchParams.append('query', state.searchQuery);
      if (state.activeFilter !== 'all') url.searchParams.append('category', state.activeFilter);

      const res = await fetch(url.toString(), {
        headers: { 'Authorization': `Bearer ${state.token}` }
      });
      if (!res.ok) throw new Error('Sync error.');

      const threads = await res.json();
      state.threads = threads;

      renderConversations();

      // Refresh active thread if selected
      if (state.activeThreadPhone) {
        renderActiveThread();
      }
    } catch (e) {
      console.warn('Sync server polling error:', e.message);
    }
  }

  // Render conversations sidebar
  function renderConversations() {
    ui.convListContainer.innerHTML = '';
    
    if (state.threads.length === 0) {
      ui.convListContainer.innerHTML = `
        <div class="empty-state-side">
          <span class="material-icons-round">forum</span>
          <p>No messages found</p>
        </div>`;
      return;
    }

    state.threads.forEach(thread => {
      const isActive = thread.phone_number === state.activeThreadPhone;
      const date = new Date(thread.last_timestamp);
      const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const avatarChar = (thread.sender_name || '?').charAt(0).toUpperCase();

      const convItem = document.createElement('div');
      convItem.className = `conv-item ${isActive ? 'active' : ''}`;
      convItem.innerHTML = `
        <div class="conv-avatar">${avatarChar}</div>
        <div class="conv-details">
          <div class="conv-row">
            <div class="conv-name">${thread.sender_name || thread.phone_number}</div>
            <div class="conv-time">${timeStr}</div>
          </div>
          <div class="conv-msg">${thread.last_message}</div>
        </div>
      `;

      convItem.addEventListener('click', () => {
        state.activeThreadPhone = thread.phone_number;
        // Re-render conversation items to show active background
        document.querySelectorAll('.conv-item').forEach(el => el.classList.remove('active'));
        convItem.classList.add('active');
        
        ui.chatEmptyState.classList.remove('active');
        ui.chatView.classList.add('active');
        
        renderActiveThread();
      });

      ui.convListContainer.appendChild(convItem);
    });
  }

  // Render messaging history pane
  function renderActiveThread() {
    const thread = state.threads.find(t => t.phone_number === state.activeThreadPhone);
    if (!thread) return;

    ui.activeContactName.textContent = thread.sender_name || thread.phone_number;
    ui.activeContactPhone.textContent = thread.phone_number;
    ui.activeAvatar.textContent = (thread.sender_name || '?').charAt(0).toUpperCase();

    // Check if user scrolled to bottom to auto-scroll
    const isAtBottom = ui.messagesTimeline.scrollHeight - ui.messagesTimeline.scrollTop <= ui.messagesTimeline.clientHeight + 100;

    ui.messagesTimeline.innerHTML = '';
    
    thread.messages.forEach(msg => {
      const date = new Date(msg.timestamp);
      const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const isOutgoing = msg.direction === 'outgoing';

      const bubbleWrapper = document.createElement('div');
      bubbleWrapper.className = `msg-bubble-wrapper ${isOutgoing ? 'outgoing' : 'incoming'}`;
      
      let attachmentHTML = '';
      
      // Handle MMS files
      if (msg.type === 'mms' && msg.mms_attachment_url) {
        if (msg.mms_attachment_url.startsWith('data:audio/') || msg.mms_attachment_url.includes('audio')) {
          // Playable voice memo mockup
          attachmentHTML = `
            <div class="mms-audio-block" data-audio-src="${msg.mms_attachment_url}">
              <button class="audio-btn">
                <span class="material-icons-round">play_arrow</span>
              </button>
              <div class="audio-visualizer-wave">
                <div class="audio-progress-bar"></div>
              </div>
              <span class="msg-time">0:04</span>
            </div>
          `;
        } else {
          // Rich photo attachment
          attachmentHTML = `<img src="${msg.mms_attachment_url}" class="mms-photo" alt="MMS Photo attachment">`;
        }
      }

      bubbleWrapper.innerHTML = `
        <div class="msg-bubble">
          ${attachmentHTML}
          <div>${msg.body}</div>
        </div>
        <div class="msg-meta-row">
          <button class="msg-star-btn ${msg.is_starred ? 'active' : ''}" data-msg-id="${msg.message_id}">
            <span class="material-icons-round">star</span>
          </button>
          <span class="msg-time">${timeStr}</span>
          ${isOutgoing ? `<span class="material-icons-round msg-status-check">done_all</span>` : ''}
        </div>
      `;

      // Set up click interactions for Playable Voice Memos
      const playBtn = bubbleWrapper.querySelector('.audio-btn');
      if (playBtn) {
        playBtn.addEventListener('click', () => toggleVoiceMemo(bubbleWrapper));
      }

      // Set up Star handlers
      const starBtn = bubbleWrapper.querySelector('.msg-star-btn');
      starBtn.addEventListener('click', async (e) => {
        e.stopPropagation();
        const isStarred = !starBtn.classList.contains('active');
        try {
          const res = await fetch(`${API_URL}/api/messages/${msg.message_id}/star`, {
            method: 'POST',
            headers: { 
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${state.token}`
            },
            body: JSON.stringify({ is_starred: isStarred })
          });
          if (res.ok) {
            starBtn.classList.toggle('active', isStarred);
            fetchMessages();
          }
        } catch (e) {
          console.error(e);
        }
      });

      ui.messagesTimeline.appendChild(bubbleWrapper);
    });

    if (isAtBottom) {
      ui.messagesTimeline.scrollTop = ui.messagesTimeline.scrollHeight;
    }
  }

  // Voice memo playback animator
  function toggleVoiceMemo(wrapper) {
    const playBtn = wrapper.querySelector('.audio-btn');
    const playIcon = playBtn.querySelector('span');
    const progressBar = wrapper.querySelector('.audio-progress-bar');
    
    if (playIcon.textContent === 'play_arrow') {
      playIcon.textContent = 'pause';
      progressBar.style.width = '0%';
      
      let progress = 0;
      const playInterval = setInterval(() => {
        progress += 2.5;
        progressBar.style.width = `${progress}%`;
        
        if (progress >= 100) {
          clearInterval(playInterval);
          playIcon.textContent = 'play_arrow';
          progressBar.style.width = '0%';
        }
      }, 100);
      
      playBtn.dataset.intervalId = playInterval;
    } else {
      playIcon.textContent = 'play_arrow';
      clearInterval(parseInt(playBtn.dataset.intervalId));
      progressBar.style.width = '0%';
    }
  }


  // ==========================================================================
  // 4. SMS / MMS SENDING ACTION (SYNCED BACK TO PHONE)
  // ==========================================================================
  
  async function sendMessage() {
    const body = ui.chatTextarea.value.trim();
    if (!body || !state.activeThreadPhone) return;

    const mockMsgId = 'MSG-WEB-' + Math.floor(Math.random() * 9000000);
    const payload = {
      messages: [{
        message_id: mockMsgId,
        phone_number: state.activeThreadPhone,
        sender_name: 'Me',
        body: body,
        timestamp: Date.now(),
        direction: 'outgoing',
        type: 'sms',
        mms_attachment_url: '',
        is_starred: 0
      }]
    };

    // Optimistically empty textarea
    ui.chatTextarea.value = '';

    try {
      // Direct Web to Android Sync mechanism
      const res = await fetch(`${API_URL}/api/sync/sms`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'x-device-token': state.token // Reuses the token for web authorization
        },
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        addLogEntry('forward', `Sent synced SMS to ${state.activeThreadPhone}: "${body}"`);
        fetchMessages();
        
        // Trigger simulated reply from presets if talking to them
        simulateAutoReplies(body);
      }
    } catch (e) {
      showToast('error', 'Failed to route message through cellular link.');
    }
  }

  ui.sendMsgBtn.addEventListener('click', sendMessage);
  ui.chatTextarea.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });


  // ==========================================================================
  // 5. AUTO-REPLIES BOT MATRIX PRESETS
  // ==========================================================================
  
  function simulateAutoReplies(userMsg) {
    if (!state.activeThreadPhone) return;
    
    const lowerMsg = userMsg.toLowerCase();
    let reply = '';
    let delay = 2500;
    let type = 'sms';
    let attachment = '';

    // Choose bot by active conversation number
    if (state.activeThreadPhone.includes('1234567')) { // Mom bot
      ui.typingIndicator.classList.add('active');
      setTimeout(async () => {
        ui.typingIndicator.classList.remove('active');
        
        const replies = [
          "Oh how lovely dear! Sending you lots of hugs! ❤️",
          "That sounds wonderful. Let me know when you get home so I know you're safe.",
          "Check out the pie I baked this afternoon! 🥧",
          "Okay honey, don't forget to eat something warm tonight."
        ];
        reply = replies[Math.floor(Math.random() * replies.length)];
        
        // MMS photo mock
        if (lowerMsg.includes('pie') || lowerMsg.includes('eat') || Math.random() > 0.6) {
          type = 'mms';
          attachment = 'https://images.unsplash.com/photo-1519869325930-281384150729?w=400&q=80'; // high quality stock pie
          reply = "I made this cherry pie just for you! It's still fresh 🥧❤️";
        }
        
        await triggerIncomingMock('Mom ❤️', state.activeThreadPhone, reply, type, attachment);
      }, delay);

    } else if (state.activeThreadPhone.includes('9876543')) { // PM Boss Bot
      ui.typingIndicator.classList.add('active');
      setTimeout(async () => {
        ui.typingIndicator.classList.remove('active');
        reply = "Acknowledged. Let's make sure the Playstore build is verified. Provide status ASAP.";
        if (lowerMsg.includes('status') || lowerMsg.includes('done')) {
          reply = "Excellent work. Please synchronize logs to the URL portal. I will check the records shortly.";
        }
        await triggerIncomingMock('Project Manager 💼', state.activeThreadPhone, reply, 'sms', '');
      }, delay);
      
    } else if (state.activeThreadPhone.includes('3141592')) { // Tech Geek Bot
      ui.typingIndicator.classList.add('active');
      setTimeout(async () => {
        ui.typingIndicator.classList.remove('active');
        reply = "Hello! Cellular connection strength is at maximum. Device database is synchronized via SQLite v3. Ask me 'system logs' for detailed stats!";
        
        if (lowerMsg.includes('log') || lowerMsg.includes('sys')) {
          reply = `[SYSTEM LOG] SQLite Active | Synced tokens: 99-year longevity | Auto-forward check listener online. All clear!`;
        }
        await triggerIncomingMock('Tech Geek 🤖', state.activeThreadPhone, reply, 'sms', '');
      }, delay);
    }
  }


  // ==========================================================================
  // 6. AUTO-FORWARDING DRAWER MANAGER
  // ==========================================================================
  
  ui.toggleForwardPanel.addEventListener('click', () => {
    ui.simulatorDrawer.classList.remove('active');
    ui.forwardDrawer.classList.add('active');
  });

  ui.closeForwardDrawer.addEventListener('click', () => {
    ui.forwardDrawer.classList.remove('active');
  });

  // Save auto forwarding to cloud db
  ui.saveForwardingBtn.addEventListener('click', async () => {
    const isEnabled = ui.forwardEnableToggle.checked;
    const phone = ui.forwardPhoneInput.value.trim();

    if (isEnabled && !phone) {
      return showToast('error', 'Forwarding phone number is required.');
    }

    try {
      const res = await fetch(`${API_URL}/api/users/forwarding`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.token}`
        },
        body: JSON.stringify({
          forward_number: phone,
          is_forward_enabled: isEnabled
        })
      });
      const data = await res.json();
      
      if (!res.ok) return showToast('error', data.error);

      showToast('verified_user', 'Auto-forwarding configurations saved remotely!');
      addLogEntry('forward', `Auto-forward settings updated. Status: ${isEnabled ? 'ON' : 'OFF'} | Target: ${phone}`);
      
      // Close drawer after short delay
      setTimeout(() => { ui.forwardDrawer.classList.remove('active'); }, 1200);

    } catch (e) {
      showToast('error', 'Failed to connect to configurations API.');
    }
  });


  // ==========================================================================
  // 7. TELEPHONY SMS TRAFFIC SIMULATOR
  // ==========================================================================
  
  ui.toggleSimulator.addEventListener('click', () => {
    ui.forwardDrawer.classList.remove('active');
    ui.simulatorDrawer.classList.toggle('active');
  });

  ui.closeSimulatorDrawer.addEventListener('click', () => {
    ui.simulatorDrawer.classList.remove('active');
  });

  // Change preset phone inputs
  ui.simPreset.addEventListener('change', () => {
    const val = ui.simPreset.value;
    if (val === 'custom') {
      ui.simPhoneContainer.style.display = 'block';
      ui.simPhoneInput.value = '';
    } else {
      ui.simPhoneContainer.style.display = 'none';
      const selectedOption = ui.simPreset.options[ui.simPreset.selectedIndex];
      ui.simPhoneInput.value = selectedOption.dataset.phone;
      
      // Update preset descriptions
      if (val.includes('Mom')) ui.simBody.value = "Hey sweetie! Hope you are having a wonderful day! Let me know if you want me to cook something special! ❤️";
      else if (val.includes('Manager')) ui.simBody.value = "Update on the SMS replication project. Ensure standard APK compilation configs are met. Report progress.";
      else if (val.includes('Geek')) ui.simBody.value = "Hey there! I just analyzed the 99-year token encryption mechanism. It operates flawlessly!";
      else if (val.includes('Scam')) ui.simBody.value = "CONGRATULATIONS! You won a $1,000,000 cash lottery prize! Click here: http://scam-prize.com to claim immediately!";
    }
  });

  // Form submission: Triggering Simulated Incoming SMS
  ui.simForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const presetName = ui.simPreset.value;
    const senderName = presetName === 'custom' ? 'Unknown Number' : presetName;
    const phone = ui.simPhoneInput.value.trim();
    const body = ui.simBody.value.trim();
    const mediaType = document.querySelector('input[name="sim-type"]:checked').value;

    let type = 'sms';
    let attachment = '';

    if (mediaType === 'mms-img') {
      type = 'mms';
      attachment = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&q=80'; // Stock salad/food image
    } else if (mediaType === 'mms-audio') {
      type = 'mms';
      attachment = 'data:audio/mp3;base64,mockAudioWaveformBytes...';
    }

    await triggerIncomingMock(senderName, phone, body, type, attachment);
  });

  async function triggerIncomingMock(name, phone, body, type, attachment) {
    try {
      const res = await fetch(`${API_URL}/api/messages/mock-receive`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${state.token}`
        },
        body: JSON.stringify({
          phone_number: phone,
          sender_name: name,
          body: body,
          type: type,
          mms_attachment_url: attachment
        })
      });
      const data = await res.json();
      if (!res.ok) return showToast('error', data.error);

      // Log the events in simulator
      addLogEntry('incoming', `Incoming SMS captured by Android App: From: ${name} (${phone}) - "${body.substring(0, 30)}..."`);
      
      if (data.forwarded) {
        addLogEntry('forward', `Auto-Forward triggered: Sent text to target: ${data.forwarded_to}`);
        showToast('shortcut', `SMS Auto-forwarded to ${data.forwarded_to}!`);
      }

      fetchMessages();

      // Switch active thread to view incoming message
      state.activeThreadPhone = phone;
      ui.chatEmptyState.classList.remove('active');
      ui.chatView.classList.add('active');
      renderActiveThread();

    } catch (e) {
      showToast('error', 'Simulator sync pipeline failed.');
    }
  }

  function addLogEntry(type, text) {
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    const time = new Date().toLocaleTimeString();
    entry.textContent = `[${time}] ${text}`;
    ui.simLogsContainer.appendChild(entry);
    ui.simLogsContainer.scrollTop = ui.simLogsContainer.scrollHeight;
  }

  ui.clearLogsBtn.addEventListener('click', () => {
    ui.simLogsContainer.innerHTML = '<div class="log-entry system">Logs cleared. Ready for operations.</div>';
  });


  // ==========================================================================
  // 8. SIDEBAR FILTERS & SEARCH INDEXING
  // ==========================================================================
  
  // Search bar
  ui.searchInput.addEventListener('input', () => {
    state.searchQuery = ui.searchInput.value.trim();
    if (state.searchQuery) {
      ui.searchClear.classList.remove('hidden');
    } else {
      ui.searchClear.classList.add('hidden');
    }
    fetchMessages();
  });

  ui.searchClear.addEventListener('click', () => {
    ui.searchInput.value = '';
    state.searchQuery = '';
    ui.searchClear.classList.add('hidden');
    fetchMessages();
  });

  // Filter Pill buttons
  ui.filterPills.forEach(pill => {
    pill.addEventListener('click', () => {
      ui.filterPills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      state.activeFilter = pill.dataset.category;
      fetchMessages();
    });
  });


  // ==========================================================================
  // 9. TOAST BANNER UTILITY
  // ==========================================================================
  
  function showToast(icon, message) {
    ui.toastIcon.textContent = icon === 'error' ? 'error_outline' : icon;
    ui.toastMsg.textContent = message;
    ui.toast.classList.add('active');
    
    // Clear toast after 4s
    if (window.toastTimeout) clearTimeout(window.toastTimeout);
    window.toastTimeout = setTimeout(() => {
      ui.toast.classList.remove('active');
    }, 4000);
  }


  // --- Run on boot ---
  checkSession();
});
