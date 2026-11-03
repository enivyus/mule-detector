import { useEffect, useRef, useState } from 'react'
import { useT } from './i18n.js'

/** Gear button with the language switch and sign-out. Closes on a click outside or Escape. */
export default function SettingsMenu({ onSwitchLang, onSignOut }) {
  const { t } = useT()
  const [open, setOpen] = useState(false)
  const box = useRef(null)

  useEffect(() => {
    if (!open) return
    const close = (e) => {
      if (e.type === 'keydown' ? e.key === 'Escape' : !box.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', close)
    return () => {
      document.removeEventListener('mousedown', close)
      document.removeEventListener('keydown', close)
    }
  }, [open])

  return (
    <div className="settings" ref={box}>
      <button className="icon" aria-label={t('settings')} title={t('settings')} aria-haspopup="menu" aria-expanded={open}
              onClick={() => setOpen((o) => !o)}>
        {/* currentColor instead of the file's white stroke, so the icon follows light and dark mode */}
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"
             strokeMiterlimit="10" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M12 15C13.6569 15 15 13.6569 15 12C15 10.3431 13.6569 9 12 9C10.3431 9 9 10.3431 9 12C9 13.6569 10.3431 15 12 15Z" />
          <path d="M2 12.8794V11.1194C2 10.0794 2.85 9.21945 3.9 9.21945C5.71 9.21945 6.45 7.93945 5.54 6.36945C5.02 5.46945 5.33 4.29945 6.24 3.77945L7.97 2.78945C8.76 2.31945 9.78 2.59945 10.25 3.38945L10.36 3.57945C11.26 5.14945 12.74 5.14945 13.65 3.57945L13.76 3.38945C14.23 2.59945 15.25 2.31945 16.04 2.78945L17.77 3.77945C18.68 4.29945 18.99 5.46945 18.47 6.36945C17.56 7.93945 18.3 9.21945 20.11 9.21945C21.15 9.21945 22.01 10.0694 22.01 11.1194V12.8794C22.01 13.9194 21.16 14.7794 20.11 14.7794C18.3 14.7794 17.56 16.0594 18.47 17.6294C18.99 18.5394 18.68 19.6994 17.77 20.2194L16.04 21.2094C15.25 21.6794 14.23 21.3994 13.76 20.6094L13.65 20.4194C12.75 18.8494 11.27 18.8494 10.36 20.4194L10.25 20.6094C9.78 21.3994 8.76 21.6794 7.97 21.2094L6.24 20.2194C5.33 19.6994 5.02 18.5294 5.54 17.6294C6.45 16.0594 5.71 14.7794 3.9 14.7794C2.85 14.7794 2 13.9194 2 12.8794Z" />
        </svg>
      </button>
      {open && (
        <div className="menu" role="menu">
          <button role="menuitem" onClick={() => { onSwitchLang(); setOpen(false) }}>{t('language')}</button>
          <button role="menuitem" className="sign-out" onClick={onSignOut}>{t('signOut')}</button>
        </div>
      )}
    </div>
  )
}
