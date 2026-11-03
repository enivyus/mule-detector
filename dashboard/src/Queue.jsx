import { streamRules, tier } from './api.js'
import { useT } from './i18n.js'
import FreezeButton from './FreezeButton.jsx'
import Signals from './Signals.jsx'
import { RiskContours, ViewSymbol } from './icons.jsx'

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
            <tr><th>{t('colAccount')}</th><th className="col-height">{t('colHeight')}</th><th className="col-wide">{t('colTier')}</th><th>{t('colSignals')}</th>
              <th className="col-wide">{t('colStatus')}</th><th className="col-wide"><span className="visually-hidden">{t('colAction')}</span></th></tr>
          </thead>
          <tbody>
            {loading && rows.length === 0 && Array.from({ length: SKELETON_ROWS }, (_, i) => (
              <tr key={i} className="skeleton-row" aria-hidden="true">
                <td><span className="skeleton" style={{ width: 92 }} /></td><td className="col-height"><span className="skeleton" style={{ width: 22 }} /></td><td className="col-wide"><span className="skeleton" style={{ width: 70 }} /></td>
                <td><span className="skeleton" style={{ width: 140 }} /></td><td className="col-wide"><span className="skeleton" style={{ width: 56 }} /></td><td className="col-wide" />
              </tr>
            ))}
            {rows.map((r) => {
              const status = statuses[r.no]?.status
              return (
                <tr key={r.no} className={r.no === selected ? 'selected' : ''} onClick={() => onSelect(r.no)}
                    tabIndex={0} aria-selected={r.no === selected} onKeyDown={(e) => e.key === 'Enter' && onSelect(r.no)}>
                  <td className="num">
                    {r.no}
                    {/* Phones drop the tier and status columns and show both here instead */}
                    <span className="row-meta"><Tier tier={r.tier} /><Status status={status} /></span>
                  </td>
                  <td className="col-height" title={t('rulesOf', streamRules(r.signals).length)}>
                    <RiskContours rules={streamRules(r.signals).length} />
                    <span className="visually-hidden">{t('rulesOf', streamRules(r.signals).length)}</span>
                  </td>
                  <td className="col-wide"><Tier tier={r.tier} /></td>
                  <td className="signals"><Signals names={r.signals} max={1} /></td>
                  <td className="col-wide"><Status status={status} /></td>
                  <td className="action col-wide"><FreezeButton no={r.no} status={status} onStatus={onStatus} onToast={onToast} /></td>
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
      <ViewSymbol kind={status === 'FROZEN' ? 'frozen' : 'all'} size={big ? 12 : 10} />
      {t(`status_${status}`)}
    </span>
  )
}

export function Tier({ tier: level }) {
  const { t } = useT()
  return <span className={`tier ${level}`}><ViewSymbol kind={level} size={11} />{t(`tier_${level}`)}</span>
}
