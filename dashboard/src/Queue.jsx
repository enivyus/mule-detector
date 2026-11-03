import { useMemo, useState } from 'react'
import { tier } from './api.js'
import { useT } from './i18n.js'
import FreezeButton from './FreezeButton.jsx'
import Signals from './Signals.jsx'

const TABS = [
  ['all', 'tabAll', () => true],
  ['freeze', 'tabFreeze', (r, st) => r.tier === 'freeze' && st !== 'FROZEN'],
  ['review', 'tabReview', (r, st) => r.tier === 'review' && st !== 'FROZEN'],
  ['frozen', 'tabFrozen', (r, st) => st === 'FROZEN'],
]

export default function Queue({ suspects, statuses, selected, onSelect, onStatus, onToast, loading }) {
  const { t } = useT()
  const [tab, setTab] = useState('all')
  const [query, setQuery] = useState('')

  const rows = useMemo(() => Object.entries(suspects)
    .map(([no, signals]) => ({ no, signals, tier: tier(signals) }))
    .sort((a, b) => (a.tier === b.tier ? b.signals.length - a.signals.length : a.tier === 'freeze' ? -1 : 1)),
  [suspects])
  const counts = Object.fromEntries(TABS.map(([id, , keep]) => [id, rows.filter((r) => keep(r, statuses[r.no]?.status)).length]))
  const keep = TABS.find(([id]) => id === tab)[2]
  const shown = rows.filter((r) => keep(r, statuses[r.no]?.status) && r.no.includes(query.trim()))

  return (
    <section className="panel queue">
      <div className="queue-head">
        <div className="tabs" role="tablist">
          {TABS.map(([id, key]) => (
            <button key={id} role="tab" aria-selected={tab === id} className={tab === id ? 'tab active' : 'tab'}
                    onClick={() => setTab(id)}>
              {t(key)} <span className="count">{counts[id]}</span>
            </button>
          ))}
        </div>
        <input type="search" placeholder={t('search')} aria-label={t('search')} value={query}
               onChange={(e) => setQuery(e.target.value)} inputMode="numeric" />
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>{t('colAccount')}</th><th>{t('colTier')}</th><th>{t('colSignals')}</th><th>{t('colStatus')}</th><th /></tr>
          </thead>
          <tbody>
            {shown.map((r) => {
              const status = statuses[r.no]?.status
              return (
                <tr key={r.no} className={r.no === selected ? 'selected' : ''} onClick={() => onSelect(r.no)}
                    tabIndex={0} onKeyDown={(e) => e.key === 'Enter' && onSelect(r.no)}>
                  <td className="mono">{r.no}</td>
                  <td><span className={`badge ${r.tier}`}>{t(`tier_${r.tier}`)}</span></td>
                  <td className="signals"><Signals names={r.signals} /></td>
                  <td><span className={`status ${status}`}>{status ? t(`status_${status}`) : '…'}</span></td>
                  <td><FreezeButton no={r.no} status={status} onStatus={onStatus} onToast={onToast} /></td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {!loading && shown.length === 0 && <p className="muted center">{t('noMatch')}</p>}
      </div>
    </section>
  )
}
