import React, { useState } from 'react';
import { 
  Shield, 
  ShieldAlert, 
  Database, 
  RefreshCw, 
  Plus, 
  Trash2, 
  ToggleLeft, 
  ToggleRight, 
  CheckCircle, 
  X, 
  ArrowRight,
  AlertTriangle,
  History,
  FileText
} from 'lucide-react';

export default function SmsBackupDashboard() {
  // Active State Toggle
  const [isActiveProtection, setIsActiveProtection] = useState(true);
  
  // Simulated Backups State
  const [backups, setBackups] = useState([
    { id: 1, sender: '+15551234567', text: 'Hey, I arrived safely at the hotel!', timestamp: '2026-06-18 10:12' },
    { id: 2, sender: '+15559876543', text: 'Could you please pick up some groceries on your way back?', timestamp: '2026-06-18 09:45' }
  ]);

  // Simulated Routing Rules State
  const [rules, setRules] = useState([
    { id: 1, label: 'Spouse Forward', pattern: 'All Contacts/Numbers', destination: '+15559998888', isEnabled: true },
    { id: 2, label: 'Secondary Backup Line', pattern: '+15551234567', destination: '+15557776666', isEnabled: false }
  ]);

  // Simulated Audit Security Logs
  const [logs, setLogs] = useState([
    { id: 1, sender: '+155******67', status: 'FORWARDED', msg: 'Successfully forwarded message to +15559998888 under spouse rule.', timestamp: '10:12:04' },
    { id: 2, sender: '+188******12', status: 'BLOCKED_OTP', msg: 'Forwarding blocked. Message matched OTP security regex: Contains credit card pin layout.', timestamp: '09:50:11' },
    { id: 3, sender: '+155******43', status: 'BACKED_UP', msg: 'Securely stored encrypted message locally at rest.', timestamp: '09:45:00' }
  ]);

  // Add Rule Dialog State
  const [showAddModal, setShowAddModal] = useState(false);
  const [newLabel, setNewLabel] = useState('');
  const [newPattern, setNewPattern] = useState('');
  const [newDestination, setNewDestination] = useState('');

  // Handler functions
  const handleToggleRule = (id) => {
    setRules(rules.map(rule => rule.id === id ? { ...rule, isEnabled: !rule.isEnabled } : rule));
  };

  const handleDeleteRule = (id) => {
    setRules(rules.filter(rule => rule.id !== id));
  };

  const handleAddRule = (e) => {
    e.preventDefault();
    if (!newDestination) return;
    const newRule = {
      id: Date.now(),
      label: newLabel || `Rule #${rules.length + 1}`,
      pattern: newPattern || 'All Contacts/Numbers',
      destination: newDestination,
      isEnabled: true
    };
    setRules([...rules, newRule]);
    setNewLabel('');
    setNewPattern('');
    setNewDestination('');
    setShowAddModal(false);
  };

  const handlePurgeSingleBackup = (id) => {
    setBackups(backups.filter(b => b.id !== id));
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white font-sans p-6">
      {/* Header Bar */}
      <header className="max-w-6xl mx-auto mb-8 flex justify-between items-center border-b border-slate-800 pb-5">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-600 p-2.5 rounded-xl text-white shadow-lg shadow-indigo-600/30">
            <Shield className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-extrabold tracking-tight">SafeForward</h1>
            <p className="text-xs text-slate-400">Privacy-First User-Authorized SMS Backup Systems</p>
          </div>
        </div>

        {/* Global Protection Toggle */}
        <div className="flex items-center gap-4 bg-slate-900 border border-slate-800 px-4 py-2.5 rounded-2xl">
          <div className="text-right">
            <span className="block text-xs font-semibold text-slate-400 uppercase tracking-wider">Active Monitoring</span>
            <span className={`text-sm font-bold ${isActiveProtection ? 'text-emerald-400' : 'text-rose-400'}`}>
              {isActiveProtection ? 'ARMED & SAFE' : 'PAUSED'}
            </span>
          </div>
          <button 
            onClick={() => setIsActiveProtection(!isActiveProtection)}
            className="focus:outline-none transition-transform active:scale-95"
          >
            {isActiveProtection ? (
              <ToggleRight className="h-9 w-9 text-indigo-500 fill-indigo-500/20" />
            ) : (
              <ToggleLeft className="h-9 w-9 text-slate-500" />
            )}
          </button>
        </div>
      </header>

      <main className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left Columns - Rules and Controls */}
        <section className="lg:col-span-2 space-y-6">
          
          {/* Main Warning Guardrail Indicator */}
          <div className="bg-gradient-to-r from-amber-500/10 to-amber-600/5 border border-amber-500/20 rounded-2xl p-5 flex gap-4 items-start">
            <div className="bg-amber-500/10 p-2.5 rounded-xl text-amber-400 mt-1">
              <ShieldAlert className="h-5 w-5" />
            </div>
            <div>
              <h3 className="font-bold text-amber-300 text-sm">Play Store Compliant Bank/OTP Shield Enabled</h3>
              <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                The integrated deep-scanning parser automatically strips and drops transaction alerts, debit updates, verification logins, credentials, and verification PINs. SafeForward secures personal SMS routes.
              </p>
            </div>
          </div>

          {/* Active Forwarding Rules Row */}
          <div className="bg-slate-900/50 border border-slate-800 rounded-3xl p-6">
            <div className="flex justify-between items-center mb-5">
              <div>
                <h2 className="text-lg font-bold tracking-tight">Forwarding Rules Matrix</h2>
                <p className="text-xs text-slate-400">Authorized dynamic cell criteria targets</p>
              </div>
              <button 
                onClick={() => setShowAddModal(true)}
                className="bg-indigo-600 hover:bg-indigo-500 transition-colors px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5"
              >
                <Plus className="h-4 w-4" /> Add Rule
              </button>
            </div>

            {rules.length === 0 ? (
              <div className="text-center py-8 text-slate-500 text-sm">
                No local forwarding rules configured. Complete target setup.
              </div>
            ) : (
              <div className="space-y-3">
                {rules.map((rule) => (
                  <div key={rule.id} className="bg-slate-910 border border-slate-800/80 rounded-2xl p-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className={`h-2.5 w-2.5 rounded-full ${rule.isEnabled ? 'bg-emerald-500' : 'bg-slate-600'}`} />
                      <div>
                        <h4 className="font-semibold text-sm text-slate-200">{rule.label}</h4>
                        <div className="text-xs text-slate-400 flex items-center gap-1.5 mt-0.5">
                          <span>From: {rule.pattern}</span>
                          <ArrowRight className="h-3 w-3 text-slate-600" />
                          <span className="text-indigo-400 font-mono font-medium">{rule.destination}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <button 
                        onClick={() => handleToggleRule(rule.id)}
                        className="text-slate-400 hover:text-white transition-colors"
                      >
                        {rule.isEnabled ? (
                          <ToggleRight className="h-7 w-7 text-emerald-400" />
                        ) : (
                          <ToggleLeft className="h-7 w-7 text-slate-600" />
                        )}
                      </button>
                      <button 
                        onClick={() => handleDeleteRule(rule.id)}
                        className="text-slate-500 hover:text-rose-400 transition-colors p-1"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Secure At-Rest Decrypted Backups Trace */}
          <div className="bg-slate-900/50 border border-slate-800 rounded-3xl p-6">
            <div className="flex items-center justify-between mb-5">
              <div className="flex items-center gap-2">
                <Database className="h-4 w-4 text-indigo-400" />
                <h2 className="text-lg font-bold">Local Encrypted Vault Backup</h2>
              </div>
              <span className="text-[10px] font-bold bg-emerald-500/10 text-emerald-400 px-2 py-0.5 rounded-full">AES-256-GCM Locked</span>
            </div>

            {backups.length === 0 ? (
              <div className="text-center py-8 text-slate-500 text-sm">
                No local encrypted backups currently in the system catalog.
              </div>
            ) : (
              <div className="space-y-3">
                {backups.map((bk) => (
                  <div key={bk.id} className="bg-slate-910/70 border border-slate-800 rounded-2xl p-4 flex justify-between items-start">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold font-mono text-slate-300">{bk.sender}</span>
                        <span className="text-[10px] text-slate-500">{bk.timestamp}</span>
                      </div>
                      <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">{bk.text}</p>
                    </div>
                    <button 
                      onClick={() => handlePurgeSingleBackup(bk.id)}
                      className="text-slate-500 hover:text-rose-400 p-1"
                      title="Wipe from Vault"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

        </section>

        {/* Right Column - Chronological Audit Logs */}
        <section className="space-y-6">
          <div className="bg-slate-900/50 border border-slate-800 rounded-3xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <History className="h-4 w-4 text-slate-400" />
                <h2 className="text-md font-bold">Trace Auditing Trail</h2>
              </div>
              <button 
                onClick={() => setLogs([])}
                className="text-[10px] text-slate-400 hover:text-rose-400 transition-colors"
              >
                Reset logs
              </button>
            </div>

            {logs.length === 0 ? (
              <div className="text-center py-10 text-slate-600 text-xs">
                Log records quiet. All systems functional.
              </div>
            ) : (
              <div className="space-y-3">
                {logs.map((log) => {
                  let badgeColors = 'bg-slate-800 text-slate-400';
                  if (log.status === 'FORWARDED') badgeColors = 'bg-emerald-500/10 text-emerald-400';
                  if (log.status === 'BLOCKED_OTP') badgeColors = 'bg-rose-500/10 text-rose-400';
                  if (log.status === 'BACKED_UP') badgeColors = 'bg-blue-500/10 text-blue-400';

                  return (
                    <div key={log.id} className="bg-slate-910 p-3.5 rounded-xl border border-slate-850 space-y-1.5">
                      <div className="flex justify-between items-center text-[10px]">
                        <span className={`px-1.5 py-0.5 rounded-md font-extrabold ${badgeColors}`}>
                          {log.status}
                        </span>
                        <span className="text-slate-500 font-mono">{log.timestamp}</span>
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed">{log.msg}</p>
                      <p className="text-[10px] text-slate-500 font-mono">Scope: {log.sender}</p>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </section>
      </main>

      {/* Add New Rule Modal Dialog */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/75 flex items-center justify-center p-4 z-50 animate-fade-in">
          <div className="bg-slate-900 border border-slate-800 w-full max-w-md rounded-3xl p-6 space-y-4">
            <div>
              <h3 className="text-lg font-bold">New Routing Rule</h3>
              <p className="text-xs text-slate-400">Rules apply in real-time when criteria conditions are matched on incoming signals.</p>
            </div>

            <form onSubmit={handleAddRule} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Target Name</label>
                <input 
                  type="text" 
                  value={newLabel}
                  onChange={(e) => setNewLabel(e.target.value)}
                  placeholder="e.g. Spouse Sync"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Criteria Pattern (Sender Number/All)</label>
                <input 
                  type="text" 
                  value={newPattern}
                  onChange={(e) => setNewPattern(e.target.value)}
                  placeholder="All Contacts/Numbers or Specific Number"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Destination Forward Mobile *</label>
                <input 
                  type="text" 
                  value={newDestination}
                  onChange={(e) => setNewDestination(e.target.value)}
                  placeholder="e.g. +1... recipient contact"
                  required
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="flex gap-3 pt-2 justify-end">
                <button 
                  type="button" 
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 hover:bg-slate-805 rounded-xl text-xs text-slate-300 font-semibold"
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="bg-indigo-600 hover:bg-indigo-500 transition-colors px-4 py-2 rounded-xl text-xs font-bold"
                >
                  Confirm Rule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
