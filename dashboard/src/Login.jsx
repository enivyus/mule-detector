import { useState } from 'react'
import { LoginError, login } from './api.js'
import { useT } from './i18n.js'
import { GlobeIcon, MarkIcon } from './icons.jsx'

export default function Login({ onLogin, onLoggedOut, onSwitchLang }) {
  const { t } = useT()
  const [user, setUser] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onLogin(await login(user.trim(), password, onLoggedOut))
    } catch (err) {
      setError(err instanceof LoginError ? err.message : 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <form className="login-card" onSubmit={submit}>
        <div className="login-top">
          <span className="brand-mark"><MarkIcon size={20} /></span>
          <button type="button" className="ghost small icon-text" onClick={onSwitchLang}><GlobeIcon size={14} />{t('language')}</button>
        </div>
        <div>
          <h1>{t('loginTitle')}</h1>
          <p className="muted">{t('title')}</p>
        </div>
        <p className="login-hint">{t('loginHint')}</p>
        <label className="field">
          {t('username')}
          <input autoFocus autoComplete="username" value={user} onChange={(e) => setUser(e.target.value)} required />
        </label>
        <label className="field">
          {t('password')}
          <input type="password" autoComplete="current-password" value={password}
                 onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {error && <p className="banner error" role="alert">{t(error)}</p>}
        <button className="primary wide" disabled={busy}>{busy ? t('signingIn') : t('signIn')}</button>
      </form>
    </div>
  )
}
