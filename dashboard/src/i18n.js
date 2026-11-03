import { createContext, useContext } from 'react'

// Every word on screen, in English and Thai. Signal names match the rule names the services return.
const text = {
  en: {
    title: 'Mule investigator',
    subtitle: 'Accounts suspected of passing on scam money',
    refresh: 'Refresh',
    error: 'Could not reach the services. Are core-banking, rule-engine and graph-service running?',
    kpiSuspects: 'Suspects', kpiFreezeTier: 'Freeze tier, total', kpiReview: 'Waiting for review',
    kpiFrozen: 'Frozen now', kpiAutoFrozen: 'Auto-frozen since rule-engine restart',
    tabAll: 'All suspects', tabFreeze: 'Freeze tier, still open', tabReview: 'Waiting for review', tabFrozen: 'Frozen',
    search: 'Search account number',
    colAccount: 'Account', colTier: 'Tier', colSignals: 'Signals', colStatus: 'Status',
    tier_freeze: 'Freeze tier', tier_review: 'Review',
    status_ACTIVE: 'Active', status_FROZEN: 'Frozen',
    type_CUSTOMER: 'Personal account', type_BUSINESS: 'Business account', type_SYSTEM: 'Cash vault',
    noMatch: 'No accounts match.',
    pick: 'Pick an account from the queue to see why it was flagged and where its money went.',
    loading: 'Loading…',
    balance: 'Balance',
    why: 'Signals',
    whyNone: 'No rule fired for this account.',
    rulesFired: 'Real-time rules fired', autoFreezeAt: 'Auto-freeze threshold',
    timeline: 'Money in and out', timelineHint: 'Click an incoming payment of 1,000 THB or more to follow it.',
    in: 'In', out: 'Out', follow: 'Follow',
    noEntries: 'No entries yet.',
    trail: 'Follow the money',
    trailHint: 'Click an account to open it. Each step is money that left within 2 hours of arriving. Money is fungible, so a step can include other people’s money passing through the same account.',
    legendSuspect: 'Suspect', legendPerson: 'Other account', legendFrozen: 'Frozen', legendCash: 'Cash withdrawn',
    fit: 'Fit to screen', trailCropped: 'The trail is wider than this panel: drag the graph, or use Fit to screen to see all of it.',
    community: 'Community', runLouvain: 'Find this account’s community',
    communityText: (id, size, n) => `Community ${id}: ${size} personal accounts, ${n} other suspects`,
    freeze: 'Freeze', unfreeze: 'Unfreeze', cancel: 'Cancel',
    confirmFreeze: (no) => `Freeze account ${no}? The owner can no longer send or receive money until it is unfrozen.`,
    confirmUnfreeze: (no) => `Unfreeze account ${no}? Money can move again straight away.`,
    froze: (no) => `Account ${no} is frozen.`, unfroze: (no) => `Account ${no} is active again.`,
    actionFailed: 'The bank refused the request.',
    reasonLabel: 'Reason (saved in the audit log with your name)',
    reasonPlaceholder: 'For example: victim reported the transfer to the hotline',
    history: 'Freeze history', historyNone: 'No freeze or unfreeze recorded yet.', historyMissing: 'Frozen, but no audit record was found for this freeze.',
    action_FREEZE: 'Frozen', action_UNFREEZE: 'Unfrozen',
    loginHint: 'Fraud investigators only. Every freeze and unfreeze is recorded under your name.',
    username: 'Username', password: 'Password', signIn: 'Sign in', signingIn: 'Signing in…',
    badLogin: 'Wrong username or password.', notInvestigator: 'This user is not an investigator.',
    signedInAs: (name) => `Signed in as ${name}`, signOut: 'Sign out', settings: 'Settings',
    loginTitle: 'Sign in', queue: 'Review queue', roleInvestigator: 'Fraud investigator', synthetic: 'Synthetic demo data: no real people or accounts.', overview: 'Overview',
    colAction: 'Action', noMatchQuery: (q) => `No suspect account contains "${q}".`, clearSearch: 'Clear search', backToQueue: 'Back to queue', moreSignals: (n) => `+${n}`,
    language: 'ภาษาไทย',
  },
  th: {
    title: 'ระบบสืบสวนบัญชีม้า',
    subtitle: 'บัญชีที่ต้องสงสัยว่ารับและส่งต่อเงินจากการหลอกลวง',
    refresh: 'โหลดใหม่',
    error: 'เชื่อมต่อระบบไม่ได้ ตรวจว่า core-banking, rule-engine และ graph-service เปิดอยู่',
    kpiSuspects: 'ผู้ต้องสงสัย', kpiFreezeTier: 'ระดับควรอายัดทั้งหมด', kpiReview: 'รอเจ้าหน้าที่ตรวจ',
    kpiFrozen: 'ถูกอายัดอยู่', kpiAutoFrozen: 'อายัดอัตโนมัติตั้งแต่ rule-engine เปิดล่าสุด',
    tabAll: 'ผู้ต้องสงสัยทั้งหมด', tabFreeze: 'ควรอายัด ยังไม่อายัด', tabReview: 'รอเจ้าหน้าที่ตรวจ', tabFrozen: 'อายัดแล้ว',
    search: 'ค้นหาเลขบัญชี',
    colAccount: 'เลขบัญชี', colTier: 'ระดับ', colSignals: 'สัญญาณ', colStatus: 'สถานะ',
    tier_freeze: 'ระดับอายัด', tier_review: 'รอตรวจ',
    status_ACTIVE: 'ปกติ', status_FROZEN: 'ถูกอายัด',
    type_CUSTOMER: 'บัญชีบุคคล', type_BUSINESS: 'บัญชีนิติบุคคล', type_SYSTEM: 'บัญชีเงินสดของธนาคาร',
    noMatch: 'ไม่พบบัญชีที่ตรงกัน',
    pick: 'เลือกบัญชีจากคิว เพื่อดูว่าทำไมถูกจับ และเงินไหลไปที่ไหน',
    loading: 'กำลังโหลด…',
    balance: 'ยอดเงิน',
    why: 'สัญญาณ',
    whyNone: 'บัญชีนี้ไม่ติดกฎข้อไหน',
    rulesFired: 'ติดกฎแบบ real-time', autoFreezeAt: 'เกณฑ์อายัดอัตโนมัติ',
    timeline: 'เงินเข้าและออก', timelineHint: 'คลิกเงินเข้าตั้งแต่ 1,000 บาท เพื่อตามรอยว่าเงินไปไหนต่อ',
    in: 'เข้า', out: 'ออก', follow: 'ตามรอย',
    noEntries: 'ยังไม่มีรายการ',
    trail: 'ตามรอยเงิน',
    trailHint: 'คลิกบัญชีในกราฟเพื่อเปิดดู แต่ละขั้นคือเงินที่ออกไปภายใน 2 ชั่วโมงหลังเงินเข้า เงินแต่ละบาทแยกกันไม่ได้ จึงอาจรวมเงินของคนอื่นที่ผ่านบัญชีเดียวกันด้วย',
    legendSuspect: 'ผู้ต้องสงสัย', legendPerson: 'บัญชีอื่น', legendFrozen: 'ถูกอายัด', legendCash: 'ถอนเป็นเงินสด',
    fit: 'พอดีจอ', trailCropped: 'เส้นทางกว้างกว่าแผงนี้ ลากกราฟเพื่อดูส่วนที่เหลือ หรือกดพอดีจอเพื่อดูทั้งหมด',
    community: 'กลุ่มบัญชี (community)', runLouvain: 'หากลุ่มของบัญชีนี้',
    communityText: (id, size, n) => `กลุ่ม ${id}: บัญชีบุคคล ${size} บัญชี มีผู้ต้องสงสัยอื่นอีก ${n} บัญชี`,
    freeze: 'อายัด', unfreeze: 'ปลดอายัด', cancel: 'ยกเลิก',
    confirmFreeze: (no) => `อายัดบัญชี ${no} ใช่ไหม? เจ้าของบัญชีจะโอนเข้าหรือออกไม่ได้จนกว่าจะปลดอายัด`,
    confirmUnfreeze: (no) => `ปลดอายัดบัญชี ${no} ใช่ไหม? เงินจะเคลื่อนไหวได้ทันที`,
    froze: (no) => `อายัดบัญชี ${no} แล้ว`, unfroze: (no) => `บัญชี ${no} กลับมาใช้งานได้แล้ว`,
    actionFailed: 'ธนาคารปฏิเสธคำขอ',
    reasonLabel: 'เหตุผล (บันทึกใน audit log พร้อมชื่อของคุณ)',
    reasonPlaceholder: 'เช่น ผู้เสียหายแจ้งสายด่วนว่าถูกหลอกให้โอน',
    history: 'ประวัติการอายัด', historyNone: 'ยังไม่มีบันทึกการอายัดหรือปลดอายัด', historyMissing: 'ถูกอายัดอยู่ แต่ไม่พบบันทึก audit ของการอายัดนี้',
    action_FREEZE: 'อายัด', action_UNFREEZE: 'ปลดอายัด',
    loginHint: 'สำหรับเจ้าหน้าที่ฝ่ายทุจริตเท่านั้น การอายัดและปลดอายัดทุกครั้งจะบันทึกชื่อของคุณ',
    username: 'ชื่อผู้ใช้', password: 'รหัสผ่าน', signIn: 'เข้าสู่ระบบ', signingIn: 'กำลังเข้าสู่ระบบ…',
    badLogin: 'ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง', notInvestigator: 'ผู้ใช้นี้ไม่ใช่เจ้าหน้าที่สืบสวน',
    signedInAs: (name) => `เข้าสู่ระบบในชื่อ ${name}`, signOut: 'ออกจากระบบ', settings: 'ตั้งค่า',
    loginTitle: 'เข้าสู่ระบบ', queue: 'คิวตรวจสอบ', roleInvestigator: 'เจ้าหน้าที่ฝ่ายทุจริต', synthetic: 'ข้อมูลจำลองเพื่อสาธิต ไม่มีบุคคลหรือบัญชีจริง', overview: 'ภาพรวม',
    colAction: 'การดำเนินการ', noMatchQuery: (q) => `ไม่พบบัญชีต้องสงสัยที่มีเลข "${q}"`, clearSearch: 'ล้างคำค้น', backToQueue: 'กลับไปที่คิว', moreSignals: (n) => `+${n}`,
    language: 'English',
  },
}

const signals = {
  en: {
    pass_through: ['Pass-through', 'Passed on at least 90% of a payment of 1,000 THB or more within 2 hours'],
    fan_in: ['Many senders', 'Received money from 4 or more different people within 2 hours'],
    fan_out: ['Split and sent on', 'Sent a fresh payment on to 2 or more people within an hour'],
    new_account_burst: ['New account, big money', 'Under 30 days old and received 20,000 THB or more in one day'],
    odd_hours: ['Night activity', '2 or more transfers between midnight and 5 a.m.'],
    from_suspect: ['From a suspect', 'Received money from a suspected account and passed it straight on'],
  },
  th: {
    pass_through: ['เงินผ่านมือ', 'รับเงินตั้งแต่ 1,000 บาท แล้วโอนออกไปแทบทั้งหมด (90% ขึ้นไป) ภายใน 2 ชั่วโมง'],
    fan_in: ['รับจากหลายคน', 'ได้รับเงินจากคนต่างกัน 4 คนขึ้นไปภายใน 2 ชั่วโมง'],
    fan_out: ['กระจายเงินออก', 'แบ่งเงินที่เพิ่งได้รับ โอนต่อให้ 2 คนขึ้นไปภายใน 1 ชั่วโมง'],
    new_account_burst: ['บัญชีใหม่รับเงินก้อนใหญ่', 'บัญชีเปิดไม่ถึง 30 วัน แต่รับเงินตั้งแต่ 20,000 บาทในวันเดียว'],
    odd_hours: ['ทำธุรกรรมกลางดึก', 'โอนเข้าหรือออกช่วงเที่ยงคืนถึงตี 5 ตั้งแต่ 2 ครั้ง'],
    from_suspect: ['รับเงินจากผู้ต้องสงสัย', 'รับเงินจากบัญชีต้องสงสัย แล้วส่งต่อทันที'],
  },
}

export const LangContext = createContext('en')

export function useT() {
  const lang = useContext(LangContext)
  const dict = text[lang]
  return {
    lang,
    t: (key, ...args) => (typeof dict[key] === 'function' ? dict[key](...args) : dict[key] ?? key),
    signal: (name) => signals[lang][name] ?? [name, name],
    locale: lang === 'th' ? 'th-TH' : 'en-GB',
  }
}

export function savedLang() {
  try { return localStorage.getItem('lang') === 'th' ? 'th' : 'en' } catch { return 'en' }
}

export function saveLang(lang) {
  try { localStorage.setItem('lang', lang) } catch { /* private mode: the choice just isn't remembered */ }
}
