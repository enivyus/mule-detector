import { useEffect, useRef, useState } from 'react'
import { useT } from './i18n.js'
import { GearIcon, GlobeIcon, SignOutIcon } from './icons.jsx'

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
        <GearIcon size={18} />
      </button>
      {open && (
        <div className="menu" role="menu">
          <button role="menuitem" onClick={() => { onSwitchLang(); setOpen(false) }}><GlobeIcon />{t('language')}</button>
          <button role="menuitem" className="sign-out" onClick={onSignOut}><SignOutIcon />{t('signOut')}</button>
        </div>
      )}
    </div>
  )
}
