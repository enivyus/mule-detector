import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, logout } from './api.js'
import { LangContext, saveLang, savedLang, useT } from './i18n.js'
import Login from './Login.jsx'
import Queue, { VIEWS, queueRows } from './Queue.jsx'
import SettingsMenu from './SettingsMenu.jsx'
import AccountPanel from './AccountPanel.jsx'
import { MarkIcon, RefreshIcon, SearchIcon } from './icons.jsx'

export default function App() {
  const [lang, setLang] = useState(savedLang)
  const [me, setMe] = useState(null)
  const switchLang = () => setLang((l) => { const next = l === 'en' ? 'th' : 'en'; saveLang(next); return next })
  const signOut = useCallback(() => { logout(); setMe(null) }, [])
  return (
    <LangContext.Provider value={lang}>
      {me
        ? <Dashboard me={me} onSignOut={signOut} onSwitchLang={switchLang} />
        : <Login onLogin={setMe} onLoggedOut={signOut} onSwitchLang={switchLang} />}
    </LangContext.Provider>
  )
}

function Dashboard({ me, onSignOut, onSwitchLang }) {
  const { t, lang } = useT()
  const [suspects, setSuspects] = useState({})
  const [statuses, setStatuses] = useState({})
  const [autoFrozen, setAutoFrozen] = useState([])
  const [selected, setSelected] = useState(null)
  const [view, setView] = useState('all')
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [toast, setToast] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setError(false)
      const [s, f] = await Promise.all([api.suspects(), api.freezes()])
      setSuspects(s)
      setAutoFrozen(f)
      const entries = await Promise.all(Object.keys(s).map(async (no) => [no, await api.account(no)]))
      setStatuses(Object.fromEntries(entries))
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [])
  useEffect(() => { load() }, [load])
  useEffect(() => { document.documentElement.lang = lang }, [lang])
  useEffect(() => {
    if (!toast) return
    const id = setTimeout(() => setToast(null), 3500)
    return () => clearTimeout(id)
  }, [toast])

  const setStatus = useCallback((no, account) => setStatuses((prev) => ({ ...prev, [no]: account })), [])

  const rows = useMemo(() => queueRows(suspects), [suspects])
  const counts = useMemo(() => Object.fromEntries(VIEWS.map(([id, , keep]) =>
    [id, rows.filter((r) => keep(r, statuses[r.no]?.status)).length])), [rows, statuses])
  const shown = useMemo(() => {
    const keep = VIEWS.find(([id]) => id === view)[2]
    return rows.filter((r) => keep(r, statuses[r.no]?.status) && r.no.includes(query.trim()))
  }, [rows, statuses, view, query])

  const overview = [
    ['kpiSuspects', counts.all, ''],
    ['kpiFreezeTier', rows.filter((r) => r.tier === 'freeze').length, 'freeze'],
    ['kpiReview', counts.review, 'review'],
    ['kpiFrozen', counts.frozen, 'frozen'],
    ['kpiAutoFrozen', autoFrozen.filter((f) => f.ok).length, ''],
  ]
  const viewTitle = VIEWS.find(([id]) => id === view)[1]

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark"><MarkIcon size={18} /></span>
          <span className="brand-name">{t('title')}</span>
        </div>
        <nav className="views" aria-label={t('queue')}>
          <span className="nav-label">{t('queue')}</span>
          {VIEWS.map(([id, key]) => (
            <button key={id} className={`view ${view === id ? 'active' : ''}`} aria-current={view === id ? 'page' : undefined}
                    onClick={() => setView(id)}>
              <span className={`view-dot ${id}`} aria-hidden="true" />
              <span className="view-name">{t(key)}</span>
              <span className="view-count">{loading ? '–' : counts[id]}</span>
            </button>
          ))}
        </nav>
        <p className="synthetic">{t('synthetic')}</p>
        <div className="account-menu">
          <span className="avatar" aria-hidden="true">{me.username[0].toUpperCase()}</span>
          <span className="who"><b>{me.username}</b><span>{t('roleInvestigator')}</span></span>
          <SettingsMenu onSwitchLang={onSwitchLang} onSignOut={onSignOut} />
        </div>
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="page-title">
            <h1>{t(viewTitle)}</h1>
            <p>{t('subtitle')}</p>
          </div>
          <label className="search">
            <SearchIcon size={16} />
            <input type="search" placeholder={t('search')} aria-label={t('search')} value={query}
                   onChange={(e) => setQuery(e.target.value)} inputMode="numeric" />
          </label>
          <button className="icon-text" onClick={load} disabled={loading}>
            <RefreshIcon size={16} className={loading ? 'spin' : ''} />{loading ? t('loading') : t('refresh')}
          </button>
        </header>

        {error && <p className="banner error" role="alert">{t('error')}</p>}

        <section className="overview" aria-label={t('overview')}>
          {overview.map(([key, value, tone]) => (
            <div key={key} className={`stat ${tone}`}>
              <span className="stat-label">{t(key)}</span>
              <span className="stat-value">{loading ? <span className="skeleton num" /> : value}</span>
            </div>
          ))}
        </section>

        <main className="work">
          <Queue rows={shown} statuses={statuses} selected={selected} onSelect={setSelected}
                 onStatus={setStatus} onToast={setToast} loading={loading} query={query} onClearQuery={() => setQuery('')} />
          {selected
            ? <AccountPanel key={selected} no={selected} suspects={suspects} statuses={statuses}
                            onSelect={setSelected} onStatus={setStatus} onToast={setToast} />
            : <section className="panel empty"><p>{t('pick')}</p></section>}
        </main>
      </div>

      {toast && <div className={`toast ${toast.tone}`} role="status">{toast.text}</div>}
    </div>
  )
}
