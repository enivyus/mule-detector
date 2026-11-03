// Thin wrappers over the three services, reached through the Vite proxy (see vite.config.js).

// HTTP Basic for the investigator, kept in memory only: a reload or a closed tab means logging in again
let authorization = null
let onUnauthorized = () => {}

export class LoginError extends Error {}

async function call(url, options = {}) {
  const headers = { ...options.headers, ...(authorization && { Authorization: authorization }) }
  if (options.body) headers['Content-Type'] = 'application/json'
  const res = await fetch(url, { ...options, headers })
  if (res.status === 401) onUnauthorized()
  if (!res.ok) throw new Error(`${options.method ?? 'GET'} ${url}: HTTP ${res.status}`)
  return res.status === 204 ? null : res.json()
}

// btoa only takes Latin-1; Spring decodes Basic credentials as UTF-8, so Thai passwords work too
const basic = (user, password) =>
  'Basic ' + btoa(String.fromCharCode(...new TextEncoder().encode(`${user}:${password}`)))

/** Checks the login against core-banking; only the INVESTIGATOR role may use the dashboard. */
export async function login(user, password, whenLoggedOut) {
  const header = basic(user, password)
  const res = await fetch('/bank/api/me', { headers: { Authorization: header } })
  if (res.status === 401) throw new LoginError('badLogin')
  if (!res.ok) throw new Error(`GET /bank/api/me: HTTP ${res.status}`)
  const me = await res.json()
  if (!me.roles.includes('INVESTIGATOR')) throw new LoginError('notInvestigator')
  authorization = header
  onUnauthorized = whenLoggedOut
  return me
}

export function logout() {
  authorization = null
  onUnauthorized = () => {}
}

const reason = (text) => JSON.stringify({ reason: text })

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
  freeze: (no, why) => call(`/bank/api/admin/accounts/${no}/freeze`, { method: 'POST', body: reason(why) }),
  unfreeze: (no, why) => call(`/bank/api/admin/accounts/${no}/unfreeze`, { method: 'POST', body: reason(why) }),
  audit: (no) => call(`/bank/api/admin/accounts/${no}/audit`),
}

// Same tiers as rule-engine: three different stream rules freeze; graph suspicion never does.
export const FREEZE_THRESHOLD = 3
export const streamRules = (signals) => signals.filter((s) => s !== 'from_suspect')
export const tier = (signals) => (streamRules(signals).length >= FREEZE_THRESHOLD ? 'freeze' : 'review')

export const thb = (satang) =>
  (satang / 100).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// Today's date on the sheet's title block, in Bangkok time
export const bangkokDate = (lang) =>
  new Date().toLocaleDateString(lang === 'th' ? 'th-TH' : 'en-GB', { timeZone: 'Asia/Bangkok', dateStyle: 'medium' })

// Bank time is Bangkok time; th-TH shows the Buddhist year, as Thai staff expect
export const bangkokTime = (iso, locale = 'en-GB') =>
  new Date(iso).toLocaleString(locale, { timeZone: 'Asia/Bangkok', dateStyle: 'medium', timeStyle: 'short' })
