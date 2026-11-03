// Thin wrappers over the three services, reached through the Vite proxy (see vite.config.js).

async function call(url, options) {
  const res = await fetch(url, options)
  if (!res.ok) throw new Error(`${options?.method ?? 'GET'} ${url}: HTTP ${res.status}`)
  return res.status === 204 ? null : res.json()
}

export const api = {
  // graph-service: suspects after suspicion has followed the money, account -> signals
  suspects: () => call('/graph/suspects'),
  trail: (transferId) => call(`/graph/trail/${transferId}`),
  communities: () => call('/graph/communities', { method: 'POST' }),
  // rule-engine: live auto-freezes since its last start
  freezes: () => call('/rules/freezes'),
  // core-banking
  account: (no) => call(`/bank/api/accounts/${no}`),
  statement: (no, size = 200) => call(`/bank/api/accounts/${no}/statement?size=${size}`),
  freeze: (no) => call(`/bank/api/admin/accounts/${no}/freeze`, { method: 'POST' }),
  unfreeze: (no) => call(`/bank/api/admin/accounts/${no}/unfreeze`, { method: 'POST' }),
}

// Same tiers as rule-engine: three different stream rules freeze; graph suspicion never does.
export const FREEZE_THRESHOLD = 3
export const streamRules = (signals) => signals.filter((s) => s !== 'from_suspect')
export const tier = (signals) => (streamRules(signals).length >= FREEZE_THRESHOLD ? 'freeze' : 'review')

export const thb = (satang) =>
  (satang / 100).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const bangkokTime = (iso) =>
  new Date(iso).toLocaleString('en-GB', { timeZone: 'Asia/Bangkok', dateStyle: 'medium', timeStyle: 'short' })
