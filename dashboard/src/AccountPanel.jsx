import { useEffect, useState } from 'react'
import { api, bangkokTime, streamRules, thb, tier } from './api.js'
import { useT } from './i18n.js'
import FreezeButton from './FreezeButton.jsx'
import Signals from './Signals.jsx'
import TrailGraph from './TrailGraph.jsx'

const LARGE_SATANG = 100_000 // 1,000 THB, the same floor the rules use

export default function AccountPanel({ no, suspects, statuses, onSelect, onStatus, onToast }) {
  const { t, signal, locale } = useT()
  const [account, setAccount] = useState(null)
  const [entries, setEntries] = useState(null)
  const [following, setFollowing] = useState(null)
  const [hops, setHops] = useState([])
  const [community, setCommunity] = useState(null)
  const [findingCommunity, setFindingCommunity] = useState(false)

  useEffect(() => {
    api.account(no).then((a) => { setAccount(a); onStatus(no, a) })
    api.statement(no).then((page) => setEntries(page.content))
  }, [no, onStatus])

  const follow = async (transferId) => {
    setFollowing(transferId)
    setHops(await api.trail(transferId))
  }

  const findCommunity = async () => {
    setFindingCommunity(true)
    try {
      const all = await api.communities()
      const members = Object.keys(all).filter((k) => all[k] === all[no])
      setCommunity({ id: all[no], size: members.length, suspects: members.filter((m) => suspects[m] && m !== no) })
    } finally {
      setFindingCommunity(false)
    }
  }

  if (!account) return <section className="panel empty">{t('loading')}</section>
  const signals = suspects[no] ?? []
  const status = statuses[no]?.status ?? account.status

  return (
    <section className="panel account">
      <div className="account-head">
        <div>
          <h2 className="mono">{no}</h2>
          <p className="muted">{t(`type_${account.type}`)}</p>
        </div>
        <div className="account-facts">
          <div><span className="muted">{t('balance')}</span><b>฿{thb(account.balanceSatang)}</b></div>
          <span className={`status big ${status}`}>{t(`status_${status}`)}</span>
          {signals.length > 0 && <span className={`badge ${tier(signals)}`}>{t(`tier_${tier(signals)}`)}</span>}
          <FreezeButton no={no} status={status} size="normal"
                        onStatus={(n, a) => { setAccount(a); onStatus(n, a) }} onToast={onToast} />
        </div>
      </div>

      <h3>{t('why')}</h3>
      {signals.length
        ? <>
            <ul className="why">
              {signals.map((s) => <li key={s}><Signals names={[s]} /> {signal(s)[1]}</li>)}
            </ul>
            <p className="muted small">{t('streamCount', streamRules(signals).length)}</p>
          </>
        : <p className="muted">{t('whyNone')}</p>}

      <h3>{t('timeline')} <span className="muted small">{t('timelineHint')}</span></h3>
      <ol className="timeline">
        {entries === null && <li className="muted">{t('loading')}</li>}
        {entries?.length === 0 && <li className="muted">{t('noEntries')}</li>}
        {entries?.map((e) => {
          const incoming = e.direction === 'CREDIT'
          const followable = incoming && e.amountSatang >= LARGE_SATANG
          return (
            <li key={e.id} className={`${incoming ? 'in' : 'out'} ${following === e.transferId ? 'current' : ''}`}>
              <span className="when">{bangkokTime(e.createdAt, locale)}</span>
              <span className="dir">{incoming ? t('in') : t('out')}</span>
              <span className="amount">{incoming ? '+' : '−'}฿{thb(e.amountSatang)}</span>
              {followable && <button className="small" onClick={() => follow(e.transferId)}>{t('follow')} →</button>}
            </li>
          )
        })}
      </ol>

      {hops.length > 0 && <>
        <h3>{t('trail')}</h3>
        <TrailGraph hops={hops} suspects={suspects} statuses={statuses} onSelect={onSelect} />
        <p className="muted small">{t('trailHint')}</p>
      </>}

      <h3>{t('community')}</h3>
      {community
        ? <p>{t('communityText', community.id, community.size, community.suspects.length)}
            {community.suspects.length > 0 && ': '}
            {community.suspects.map((m) => <button key={m} className="link mono" onClick={() => onSelect(m)}>{m}</button>)}
          </p>
        : <button onClick={findCommunity} disabled={findingCommunity}>
            {findingCommunity ? t('loading') : t('runLouvain')}
          </button>}
    </section>
  )
}
