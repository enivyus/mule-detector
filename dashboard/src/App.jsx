import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, logout, bangkokDate } from './api.js'
import { LangContext, saveLang, savedLang, useT } from './i18n.js'
import Login from './Login.jsx'
import Queue, { VIEWS, queueRows } from './Queue.jsx'
import SettingsMenu from './SettingsMenu.jsx'
import AccountPanel from './AccountPanel.jsx'
import MapKey from './MapKey.jsx'
import { MarkIcon, RefreshIcon, SearchIcon, ViewSymbol } from './icons.jsx'

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
    return rows.filter((r) => keep(r, statuses[r.no]?.status) && r.no.includes(query.replace(/\s/g, '')))
  }, [rows, statuses, view, query])

  // The freeze tier leads: it is the number that says how much money is in danger right now
  const overview = [
    ['kpiFreezeTier', rows.filter((r) => r.tier === 'freeze').length, 'freeze lead'],
    ['kpiSuspects', counts.all, ''],
    ['kpiReview', counts.review, 'review'],
    ['kpiFrozen', counts.frozen, 'frozen'],
    ['kpiAutoFrozen', autoFrozen.filter((f) => f.ok).length, ''],
  ]
  const viewIndex = VIEWS.findIndex(([id]) => id === view)
  const viewTitle = VIEWS[viewIndex][1]

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
              <ViewSymbol kind={id} />
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
        <div className="ticks" aria-hidden="true" />
        <header className="topbar">
          <div className="page-title">
            <h1>{t(viewTitle)}</h1>
            <p>{t('subtitle')}</p>
          </div>
          <dl className="sheet-meta">
            <div><dt>{t('sheetLabel')}</dt><dd>{viewIndex + 1} / {VIEWS.length}</dd></div>
            <div><dt>{t('dateLabel')}</dt><dd>{bangkokDate(lang)}</dd></div>
          </dl>
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
              <span className="stat-value">
                <svg className="benchmark" width="12" height="12" viewBox="0 0 12 12" aria-hidden="true"><path d="M6 1 11 10H1Z" fill="none" stroke="currentColor" strokeWidth="1.5" /><circle cx="6" cy="7" r="1.3" fill="currentColor" /></svg>
                {loading ? <span className="skeleton num" /> : value}
              </span>
            </div>
          ))}
        </section>

        <main className={`work ${selected ? 'has-selection' : ''}`}>
          <Queue rows={shown} statuses={statuses} selected={selected} onSelect={setSelected}
                 onStatus={setStatus} onToast={setToast} loading={loading} query={query} onClearQuery={() => setQuery('')} />
          {selected
            ? <AccountPanel key={selected} no={selected} suspects={suspects} statuses={statuses}
                            onSelect={setSelected} onStatus={setStatus} onToast={setToast} />
            : <MapKey />}
        </main>
      </div>

      {toast && <div className={`toast ${toast.tone}`} role="status">{toast.text}</div>}
    </div>
  )
}
