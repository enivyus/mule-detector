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

  const run = async (reason) => {
    setBusy(true)
    try {
      const account = await (frozen ? api.unfreeze(no, reason) : api.freeze(no, reason))
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

/** The reason is stored in core-banking's audit log next to the investigator's name. */
function Confirm({ text, action, danger, busy, onYes, onNo }) {
  const { t } = useT()
  const [reason, setReason] = useState('')
  const box = useRef(null)
  useEffect(() => { box.current?.focus() }, [])
  // Keys stop here: in the queue the dialog sits inside a row whose Enter key selects it
  const keys = (e) => {
    e.stopPropagation()
    if (e.key === 'Escape') onNo()
  }
  return (
    <div className="overlay" onClick={(e) => { e.stopPropagation(); onNo() }} onKeyDown={keys}>
      <div className="dialog" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <p>{text}</p>
        <label className="field">
          {t('reasonLabel')}
          <textarea ref={box} rows={3} maxLength={500} value={reason} placeholder={t('reasonPlaceholder')}
                    onChange={(e) => setReason(e.target.value)} />
        </label>
        <div className="dialog-actions">
          <button onClick={onNo}>{t('cancel')}</button>
          <button className={danger ? 'danger' : 'primary'} disabled={busy || !reason.trim()}
                  onClick={() => onYes(reason.trim())}>{action}</button>
        </div>
      </div>
    </div>
  )
}
