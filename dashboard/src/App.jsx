import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, logout, tier } from './api.js'
import { LangContext, saveLang, savedLang, useT } from './i18n.js'
import Login from './Login.jsx'
import Queue from './Queue.jsx'
import AccountPanel from './AccountPanel.jsx'

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

  const kpis = useMemo(() => {
    const all = Object.entries(suspects)
    return [
      ['kpiSuspects', all.length, ''],
      ['kpiFreezeTier', all.filter(([, s]) => tier(s) === 'freeze').length, 'freeze'],
      ['kpiReview', all.filter(([no, s]) => tier(s) === 'review' && statuses[no]?.status !== 'FROZEN').length, 'review'],
      ['kpiFrozen', all.filter(([no]) => statuses[no]?.status === 'FROZEN').length, 'frozen'],
      ['kpiAutoFrozen', autoFrozen.filter((f) => f.ok).length, ''],
    ]
  }, [suspects, statuses, autoFrozen])

  return (
    <div className="app">
      <header className="top">
        <div>
          <h1>{t('title')}</h1>
          <p className="muted">{t('subtitle')}</p>
        </div>
        <div className="top-actions">
          <span className="muted small">{t('signedInAs', me.username)}</span>
          <button onClick={load} disabled={loading}>{loading ? t('loading') : t('refresh')}</button>
          <button className="lang" onClick={onSwitchLang} aria-label="Switch language">{t('language')}</button>
          <button onClick={onSignOut}>{t('signOut')}</button>
        </div>
      </header>

      {error && <p className="banner error" role="alert">{t('error')}</p>}

      <section className="kpis" aria-label="Overview">
        {kpis.map(([key, value, tone]) => (
          <div key={key} className={`kpi ${tone}`}>
            <span className="kpi-value">{loading && !value ? '–' : value}</span>
            <span className="kpi-label">{t(key)}</span>
          </div>
        ))}
      </section>

      <main className="layout">
        <Queue suspects={suspects} statuses={statuses} selected={selected} onSelect={setSelected}
               onStatus={setStatus} onToast={setToast} loading={loading} />
        {selected
          ? <AccountPanel key={selected} no={selected} suspects={suspects} statuses={statuses}
                          onSelect={setSelected} onStatus={setStatus} onToast={setToast} />
          : <section className="panel empty">{t('pick')}</section>}
      </main>

      {toast && <div className={`toast ${toast.tone}`} role="status">{toast.text}</div>}
    </div>
  )
}
