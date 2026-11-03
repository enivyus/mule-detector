import { useEffect, useRef, useState } from 'react'
import { api } from './api.js'
import { useT } from './i18n.js'

/** Freeze / unfreeze with a confirmation step: freezing a genuine customer is the costly mistake. */
export default function FreezeButton({ no, status, onStatus, onToast, size = 'small' }) {
  const { t } = useT()
  const [asking, setAsking] = useState(false)
  const [busy, setBusy] = useState(false)
  if (!status) return null
  const frozen = status === 'FROZEN'

  const run = async () => {
    setBusy(true)
    try {
      const account = await (frozen ? api.unfreeze(no) : api.freeze(no))
      onStatus(no, account)
      onToast({ tone: frozen ? 'ok' : 'warn', text: frozen ? t('unfroze', no) : t('froze', no) })
    } catch {
      onToast({ tone: 'bad', text: t('actionFailed') })
    } finally {
      setBusy(false)
      setAsking(false)
    }
  }

  return (
    <>
      <button className={`${frozen ? '' : 'danger'} ${size}`} disabled={busy}
              onClick={(e) => { e.stopPropagation(); setAsking(true) }}>
        {frozen ? t('unfreeze') : t('freeze')}
      </button>
      {asking && <Confirm text={frozen ? t('confirmUnfreeze', no) : t('confirmFreeze', no)}
                          action={frozen ? t('unfreeze') : t('freeze')} danger={!frozen} busy={busy}
                          onYes={run} onNo={() => setAsking(false)} />}
    </>
  )
}

function Confirm({ text, action, danger, busy, onYes, onNo }) {
  const { t } = useT()
  const yes = useRef(null)
  useEffect(() => {
    yes.current?.focus()
    const esc = (e) => e.key === 'Escape' && onNo()
    window.addEventListener('keydown', esc)
    return () => window.removeEventListener('keydown', esc)
  }, [onNo])
  return (
    <div className="overlay" onClick={(e) => { e.stopPropagation(); onNo() }}>
      <div className="dialog" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <p>{text}</p>
        <div className="dialog-actions">
          <button onClick={onNo}>{t('cancel')}</button>
          <button ref={yes} className={danger ? 'danger' : 'primary'} disabled={busy} onClick={onYes}>{action}</button>
        </div>
      </div>
    </div>
  )
}
