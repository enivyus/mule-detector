import { tier } from './api.js'
import { useT } from './i18n.js'
import FreezeButton from './FreezeButton.jsx'
import Signals from './Signals.jsx'
import { LockIcon } from './icons.jsx'

/** Queue views shown in the sidebar: id, label key, which rows belong. Frozen accounts leave the work views. */
export const VIEWS = [
  ['all', 'tabAll', () => true],
  ['freeze', 'tabFreeze', (r, st) => r.tier === 'freeze' && st !== 'FROZEN'],
  ['review', 'tabReview', (r, st) => r.tier === 'review' && st !== 'FROZEN'],
  ['frozen', 'tabFrozen', (r, st) => st === 'FROZEN'],
]

/** Freeze tier first, then the accounts that broke the most rules. */
export const queueRows = (suspects) => Object.entries(suspects)
  .map(([no, signals]) => ({ no, signals, tier: tier(signals) }))
  .sort((a, b) => (a.tier === b.tier ? b.signals.length - a.signals.length : a.tier === 'freeze' ? -1 : 1))

const SKELETON_ROWS = 8

export default function Queue({ rows, statuses, selected, onSelect, onStatus, onToast, loading, query, onClearQuery }) {
  const { t } = useT()

  return (
    <section className="panel queue">
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>{t('colAccount')}</th><th>{t('colTier')}</th><th>{t('colSignals')}</th><th>{t('colStatus')}</th>
              <th><span className="visually-hidden">{t('colAction')}</span></th></tr>
          </thead>
          <tbody>
            {loading && rows.length === 0 && Array.from({ length: SKELETON_ROWS }, (_, i) => (
              <tr key={i} className="skeleton-row" aria-hidden="true">
                <td><span className="skeleton" style={{ width: 92 }} /></td><td><span className="skeleton" style={{ width: 70 }} /></td>
                <td><span className="skeleton" style={{ width: 140 }} /></td><td><span className="skeleton" style={{ width: 56 }} /></td><td />
              </tr>
            ))}
            {rows.map((r) => {
              const status = statuses[r.no]?.status
              return (
                <tr key={r.no} className={r.no === selected ? 'selected' : ''} onClick={() => onSelect(r.no)}
                    tabIndex={0} aria-selected={r.no === selected} onKeyDown={(e) => e.key === 'Enter' && onSelect(r.no)}>
                  <td className="num">{r.no}</td>
                  <td><span className={`tier ${r.tier}`}>{t(`tier_${r.tier}`)}</span></td>
                  <td className="signals"><Signals names={r.signals} /></td>
                  <td><Status status={status} /></td>
                  <td className="action"><FreezeButton no={r.no} status={status} onStatus={onStatus} onToast={onToast} /></td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {!loading && rows.length === 0 && (
          <div className="empty-table">
            <p>{query ? t('noMatchQuery', query) : t('noMatch')}</p>
            {query && <button className="small" onClick={onClearQuery}>{t('clearSearch')}</button>}
          </div>
        )}
      </div>
    </section>
  )
}

export function Status({ status, big }) {
  const { t } = useT()
  if (!status) return <span className="status pending">…</span>
  return (
    <span className={`status ${status} ${big ? 'big' : ''}`}>
      {status === 'FROZEN' ? <LockIcon size={big ? 14 : 12} /> : <span className="status-dot" aria-hidden="true" />}
      {t(`status_${status}`)}
    </span>
  )
}
