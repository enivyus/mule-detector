import { useState } from 'react'
import { LoginError, login } from './api.js'
import { useT } from './i18n.js'

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
      <form className="panel" onSubmit={submit}>
        <button type="button" className="lang small" onClick={onSwitchLang} aria-label="Switch language">{t('language')}</button>
        <div>
          <h1>{t('title')}</h1>
          <p className="muted small">{t('loginHint')}</p>
        </div>
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
        <button className="primary" disabled={busy}>{busy ? t('signingIn') : t('signIn')}</button>
      </form>
    </div>
  )
}
