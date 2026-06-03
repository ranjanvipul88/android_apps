const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'google_messages_super_secret_key_12345';
const DB_PATH = path.join(__dirname, 'database.sqlite');

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Connect to SQLite Database
const db = new sqlite3.Database(DB_PATH, (err) => {
  if (err) {
    console.error('Failed to connect to SQLite database:', err.message);
  } else {
    console.log('Connected to SQLite database at', DB_PATH);
    initializeDatabase();
  }
});

// Initialize Database Tables
function initializeDatabase() {
  db.serialize(() => {
    // Create Users Table
    db.run(`
      CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        pair_token TEXT UNIQUE NOT NULL,
        device_paired INTEGER DEFAULT 0,
        forward_number TEXT DEFAULT '',
        is_forward_enabled INTEGER DEFAULT 0,
        battery_level INTEGER DEFAULT 100,
        network_signal TEXT DEFAULT 'Strong'
      )
    `);

    // Create Messages Table
    db.run(`
      CREATE TABLE IF NOT EXISTS messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        message_id TEXT UNIQUE NOT NULL,
        phone_number TEXT NOT NULL,
        sender_name TEXT DEFAULT '',
        body TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        direction TEXT CHECK(direction IN ('incoming', 'outgoing')) NOT NULL,
        type TEXT CHECK(type IN ('sms', 'mms')) NOT NULL,
        mms_attachment_url TEXT DEFAULT '',
        is_starred INTEGER DEFAULT 0
      )
    `);

    // Seed Default Account for instant testing
    const defaultEmail = 'admin@messages.sms';
    const defaultPassword = 'admin123';
    
    db.get('SELECT id FROM users WHERE email = ?', [defaultEmail], (err, row) => {
      if (err) return console.error('Database selection error:', err.message);
      if (!row) {
        const salt = bcrypt.genSaltSync(10);
        const hash = bcrypt.hashSync(defaultPassword, salt);
        const pairToken = 'MSG-' + Math.floor(100000 + Math.random() * 900000); // e.g. MSG-583920
        db.run(
          'INSERT INTO users (email, password_hash, pair_token) VALUES (?, ?, ?)',
          [defaultEmail, hash, pairToken],
          (err) => {
            if (err) console.error('Failed to seed default user:', err.message);
            else {
              console.log('Seeded Default Account successfully:');
              console.log(`- Web Login Email: ${defaultEmail}`);
              console.log(`- Web Login Password: ${defaultPassword}`);
              console.log(`- Mobile Pair Token: ${pairToken}`);
            }
          }
        );
      } else {
        db.get('SELECT pair_token FROM users WHERE email = ?', [defaultEmail], (err, row) => {
          if (row) {
            console.log('Default Account Available:');
            console.log(`- Web Login Email: ${defaultEmail}`);
            console.log(`- Mobile Pair Token: ${row.pair_token}`);
          }
        });
      }
    });
  });
}

// ================= AUTHENTICATION MIDDLEWARE =================
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  
  if (!token) return res.status(401).json({ error: 'Access denied. No session token provided.' });
  
  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).json({ error: 'Session expired or invalid.' });
    req.user = user;
    next();
  });
}

// ================= WEB PORTAL AUTH ENDPOINTS =================

// Register Endpoint
app.post('/api/auth/register', (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) return res.status(400).json({ error: 'Email and password are required.' });

  const salt = bcrypt.genSaltSync(10);
  const hash = bcrypt.hashSync(password, salt);
  const pairToken = 'MSG-' + Math.floor(100000 + Math.random() * 900000);

  db.run(
    'INSERT INTO users (email, password_hash, pair_token) VALUES (?, ?, ?)',
    [email, hash, pairToken],
    function (err) {
      if (err) {
        if (err.message.includes('UNIQUE constraint failed')) {
          return res.status(400).json({ error: 'Email already registered.' });
        }
        return res.status(500).json({ error: 'Registration failed.' });
      }
      res.status(201).json({ message: 'User registered.', pair_token: pairToken });
    }
  );
});

// Web Login Endpoint with NEVER-EXPIRING token (99 years!)
app.post('/api/auth/login', (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) return res.status(400).json({ error: 'Email and password are required.' });

  db.get('SELECT * FROM users WHERE email = ?', [email], (err, user) => {
    if (err) return res.status(500).json({ error: 'Database authentication error.' });
    if (!user) return res.status(401).json({ error: 'Invalid email or password.' });

    const passwordIsValid = bcrypt.compareSync(password, user.password_hash);
    if (!passwordIsValid) return res.status(401).json({ error: 'Invalid email or password.' });

    // Generate token with extreme 99-year expiration
    // 99 years = 99 * 365 * 24 * 3600 = 3,122,064,000 seconds
    const token = jwt.sign(
      { id: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '99y' }
    );

    res.json({
      message: 'Authentication successful',
      token: token,
      email: user.email,
      pair_token: user.pair_token,
      device_paired: user.device_paired === 1
    });
  });
});

// Get Current User Profile & Telemetry
app.get('/api/users/profile', authenticateToken, (req, res) => {
  db.get(
    'SELECT email, pair_token, device_paired, forward_number, is_forward_enabled, battery_level, network_signal FROM users WHERE id = ?',
    [req.user.id],
    (err, user) => {
      if (err) return res.status(500).json({ error: 'Database selection error.' });
      res.json(user);
    }
  );
});

// Update Auto-Forwarding remotely from the Web Portal
app.post('/api/users/forwarding', authenticateToken, (req, res) => {
  const { forward_number, is_forward_enabled } = req.body;
  
  db.run(
    'UPDATE users SET forward_number = ?, is_forward_enabled = ? WHERE id = ?',
    [forward_number, is_forward_enabled ? 1 : 0, req.user.id],
    function (err) {
      if (err) return res.status(500).json({ error: 'Failed to update forwarding configurations.' });
      res.json({ message: 'Auto-forwarding settings updated successfully.' });
    }
  );
});

// ================= MOBILE PHONE SYNC ENDPOINTS =================

// Mobile Phone pairing sync endpoint
app.post('/api/sync/pair', (req, res) => {
  const { pair_token, battery_level, network_signal } = req.body;
  if (!pair_token) return res.status(400).json({ error: 'Pair token is required.' });

  db.get('SELECT * FROM users WHERE pair_token = ?', [pair_token.trim()], (err, user) => {
    if (err) return res.status(500).json({ error: 'Database error.' });
    if (!user) return res.status(404).json({ error: 'Invalid pair token. Check your screen.' });

    // Mark as paired and update telemetry
    db.run(
      'UPDATE users SET device_paired = 1, battery_level = ?, network_signal = ? WHERE id = ?',
      [battery_level || 100, network_signal || 'Strong', user.id],
      function (err) {
        if (err) return res.status(500).json({ error: 'Failed to update pairing status.' });
        res.json({
          message: 'Device paired successfully!',
          device_token: jwt.sign({ id: user.id, device: true }, JWT_SECRET, { expiresIn: '100y' }),
          forward_number: user.forward_number,
          is_forward_enabled: user.is_forward_enabled === 1
        });
      }
    );
  });
});

// Endpoint for paired phone to sync SMS logs in batch
app.post('/api/sync/sms', (req, res) => {
  const deviceToken = req.headers['x-device-token'];
  if (!deviceToken) return res.status(401).json({ error: 'Missing device authentication header.' });

  jwt.verify(deviceToken, JWT_SECRET, (err, decoded) => {
    if (err) return res.status(403).json({ error: 'Invalid device credentials.' });

    const messages = req.body.messages; // expect array of messages
    if (!Array.isArray(messages)) return res.status(400).json({ error: 'Messages must be a JSON array.' });

    if (messages.length === 0) return res.json({ status: 'success', synced: 0 });

    const stmt = db.prepare(`
      INSERT OR REPLACE INTO messages 
      (message_id, phone_number, sender_name, body, timestamp, direction, type, mms_attachment_url, is_starred) 
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    db.serialize(() => {
      messages.forEach((msg) => {
        stmt.run([
          msg.message_id,
          msg.phone_number,
          msg.sender_name || '',
          msg.body,
          msg.timestamp,
          msg.direction,
          msg.type || 'sms',
          msg.mms_attachment_url || '',
          msg.is_starred ? 1 : 0
        ]);
      });

      stmt.finalize((err) => {
        if (err) {
          console.error('Error syncing batch messages:', err.message);
          return res.status(500).json({ error: 'Failed to sync database logs.' });
        }
        
        // Update device battery/signal telemetry if provided
        const { battery_level, network_signal } = req.body;
        if (battery_level !== undefined || network_signal !== undefined) {
          db.run(
            'UPDATE users SET battery_level = COALESCE(?, battery_level), network_signal = COALESCE(?, network_signal) WHERE id = ?',
            [battery_level, network_signal, decoded.id]
          );
        }

        // Return latest settings to device to keep it in sync
        db.get('SELECT forward_number, is_forward_enabled FROM users WHERE id = ?', [decoded.id], (err, user) => {
          res.json({
            status: 'success',
            synced: messages.length,
            forward_number: user ? user.forward_number : '',
            is_forward_enabled: user ? user.is_forward_enabled === 1 : false
          });
        });
      });
    });
  });
});

// Endpoint for Android app to check/fetch remote server configurations
app.get('/api/sync/settings', (req, res) => {
  const deviceToken = req.headers['x-device-token'];
  if (!deviceToken) return res.status(401).json({ error: 'Missing device authentication header.' });

  jwt.verify(deviceToken, JWT_SECRET, (err, decoded) => {
    if (err) return res.status(403).json({ error: 'Invalid device token.' });

    db.get('SELECT forward_number, is_forward_enabled FROM users WHERE id = ?', [decoded.id], (err, user) => {
      if (err) return res.status(500).json({ error: 'Failed to query system properties.' });
      res.json({
        forward_number: user ? user.forward_number : '',
        is_forward_enabled: user ? (user.is_forward_enabled === 1) : false
      });
    });
  });
});


// ================= SMS FETCH ENDPOINTS FOR THE WEB DASHBOARD =================

// Fetch messages for logged in user (with rich search, pagination, and categorizations)
app.get('/api/messages', authenticateToken, (req, res) => {
  const { query, category, phone } = req.query;
  
  let sql = 'SELECT * FROM messages WHERE 1=1';
  const params = [];

  if (phone) {
    sql += ' AND phone_number = ?';
    params.push(phone);
  }

  if (query) {
    sql += ' AND (body LIKE ? OR phone_number LIKE ? OR sender_name LIKE ?)';
    const searchWild = `%${query}%`;
    params.push(searchWild, searchWild, searchWild);
  }

  if (category) {
    if (category === 'starred') {
      sql += ' AND is_starred = 1';
    } else if (category === 'images') {
      sql += ' AND type = "mms" AND mms_attachment_url LIKE "data:image%" OR mms_attachment_url LIKE "http%"';
    } else if (category === 'audio') {
      sql += ' AND type = "mms" AND mms_attachment_url LIKE "data:audio%"';
    } else if (category === 'links') {
      sql += ' AND (body LIKE "%http://%" OR body LIKE "%https://%" OR body LIKE "%www.%")';
    }
  }

  sql += ' ORDER BY timestamp ASC';

  db.all(sql, params, (err, rows) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ error: 'Failed to fetch messages.' });
    }
    
    // Group conversations by phone number
    const threads = {};
    rows.forEach(msg => {
      const p = msg.phone_number;
      if (!threads[p]) {
        threads[p] = {
          phone_number: p,
          sender_name: msg.sender_name || p,
          last_message: msg.body,
          last_timestamp: msg.timestamp,
          messages: []
        };
      }
      threads[p].messages.push({
        id: msg.id,
        message_id: msg.message_id,
        body: msg.body,
        timestamp: msg.timestamp,
        direction: msg.direction,
        type: msg.type,
        mms_attachment_url: msg.mms_attachment_url,
        is_starred: msg.is_starred === 1
      });
      // update threads info
      threads[p].last_message = msg.body;
      threads[p].last_timestamp = msg.timestamp;
    });

    res.json(Object.values(threads));
  });
});

// Toggle star on a message
app.post('/api/messages/:msgId/star', authenticateToken, (req, res) => {
  const { msgId } = req.params;
  const { is_starred } = req.body;

  db.run(
    'UPDATE messages SET is_starred = ? WHERE message_id = ?',
    [is_starred ? 1 : 0, msgId],
    function (err) {
      if (err) return res.status(500).json({ error: 'Failed to star message.' });
      res.json({ message: 'Message star status updated.' });
    }
  );
});

// Delete message
app.delete('/api/messages/:msgId', authenticateToken, (req, res) => {
  const { msgId } = req.params;

  db.run('DELETE FROM messages WHERE message_id = ?', [msgId], function (err) {
    if (err) return res.status(500).json({ error: 'Failed to delete message.' });
    res.json({ message: 'Message deleted successfully.' });
  });
});


// ================= SMS SIMULATOR PORTAL ENDPOINT =================
// Web portal can trigger a mock incoming text which pretends to arrive at the phone 
// and triggers auto-forwarding & sync routines back.
app.post('/api/messages/mock-receive', authenticateToken, (req, res) => {
  const { phone_number, sender_name, body, type, mms_attachment_url } = req.body;
  if (!phone_number || !body) return res.status(400).json({ error: 'Sender phone number and text body are required.' });

  const mockMsgId = 'MOCK-SMS-' + Math.floor(10000000 + Math.random() * 90000000);
  const timestamp = Date.now();

  db.get('SELECT * FROM users WHERE id = ?', [req.user.id], (err, user) => {
    if (err || !user) return res.status(500).json({ error: 'Database matching user error.' });

    // Store incoming message directly in DB
    db.run(
      `INSERT INTO messages (message_id, phone_number, sender_name, body, timestamp, direction, type, mms_attachment_url, is_starred)
       VALUES (?, ?, ?, ?, ?, 'incoming', ?, ?, 0)`,
      [mockMsgId, phone_number, sender_name || '', body, timestamp, type || 'sms', mms_attachment_url || ''],
      function (err) {
        if (err) return res.status(500).json({ error: 'Failed to insert mock message.' });

        const syncLogs = [];
        
        // Auto forward simulated action
        let forwardedMsg = null;
        if (user.is_forward_enabled === 1 && user.forward_number) {
          forwardedMsg = {
            message_id: 'FWD-SMS-' + Math.floor(10000000 + Math.random() * 90000000),
            phone_number: user.forward_number,
            sender_name: 'Auto-Forwarder',
            body: `[FWD from ${phone_number}]: ${body}`,
            timestamp: Date.now() + 500,
            direction: 'outgoing',
            type: 'sms',
            mms_attachment_url: '',
            is_starred: 0
          };
          
          db.run(
            `INSERT INTO messages (message_id, phone_number, sender_name, body, timestamp, direction, type, mms_attachment_url, is_starred)
             VALUES (?, ?, ?, ?, ?, 'outgoing', 'sms', '', 0)`,
            [forwardedMsg.message_id, forwardedMsg.phone_number, forwardedMsg.sender_name, forwardedMsg.body, forwardedMsg.timestamp]
          );
        }

        res.json({
          message: 'Mock incoming message simulated successfully!',
          message_id: mockMsgId,
          timestamp: timestamp,
          forwarded: forwardedMsg !== null,
          forwarded_to: forwardedMsg ? forwardedMsg.phone_number : null
        });
      }
    );
  });
});

app.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(`GOOGLE MESSAGES WEB-SYNC SERVER IS RUNNING`);
  console.log(`- Web portal dashboard: http://localhost:${PORT}`);
  console.log(`- SQLite database file: ${DB_PATH}`);
  console.log(`=======================================================`);
});
