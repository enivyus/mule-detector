import { useEffect, useRef, useState } from 'react'
import { api, bangkokTime, streamRules, thb, tier, FREEZE_THRESHOLD } from './api.js'
import { useT } from './i18n.js'
import FreezeButton from './FreezeButton.jsx'
import Signals from './Signals.jsx'
import TrailGraph from './TrailGraph.jsx'
import { Status } from './Queue.jsx'
import { ArrowLeftIcon, ArrowRightIcon } from './icons.jsx'

const LARGE_SATANG = 100_000 // 1,000 THB, the same floor the rules use

export default function AccountPanel({ no, suspects, statuses, onSelect, onStatus, onToast }) {
  const { t, signal, locale } = useT()
  const [account, setAccount] = useState(null)
  const [entries, setEntries] = useState(null)
  const [following, setFollowing] = useState(null)
  const [hops, setHops] = useState([])
  const [community, setCommunity] = useState(null)
  const [findingCommunity, setFindingCommunity] = useState(false)
  const [audit, setAudit] = useState(null)
  const liveStatus = statuses[no]?.status
  const panel = useRef(null)
  // When the detail stacks under the queue, bring it into view once loaded (the skeleton is too short to scroll to)
  const loaded = account !== null
  useEffect(() => {
    if (loaded && window.matchMedia('(max-width: 1180px)').matches) panel.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [loaded])

  useEffect(() => {
    api.account(no).then((a) => { setAccount(a); onStatus(no, a) })
    api.statement(no).then((page) => setEntries(page.content))
  }, [no, onStatus])
  // Reload whenever the status changes, from this panel or from the queue
  useEffect(() => { api.audit(no).then(setAudit) }, [no, liveStatus])

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

  if (!account) {
    return (
      <section className="panel account" aria-busy="true" ref={panel}>
        <span className="skeleton" style={{ width: 180, height: 24 }} />
        <span className="skeleton" style={{ width: 260, marginTop: 12 }} />
        <span className="skeleton" style={{ width: '100%', height: 120, marginTop: 24 }} />
      </section>
    )
  }
  const signals = suspects[no] ?? []
  const status = liveStatus ?? account.status
  const fired = streamRules(signals).length

  return (
    <section className="panel account" ref={panel}>
      <button className="back ghost small icon-text" onClick={() => onSelect(null)}><ArrowLeftIcon size={14} />{t('backToQueue')}</button>
      <header className="account-head">
        <div className="account-id">
          <h2 className="num">{no}</h2>
          <div className="account-tags">
            <span className="muted">{t(`type_${account.type}`)}</span>
            <Status status={status} big />
            {signals.length > 0 && <span className={`tier ${tier(signals)}`}>{t(`tier_${tier(signals)}`)}</span>}
          </div>
        </div>
        <FreezeButton no={no} status={status} size="normal"
                      onStatus={(n, a) => { setAccount(a); onStatus(n, a) }} onToast={onToast} />
      </header>

      <dl className="facts">
        <div><dt>{t('balance')}</dt><dd className="num">฿{thb(account.balanceSatang)}</dd></div>
        <div><dt>{t('rulesFired')}</dt><dd className="num">{fired} / 5</dd></div>
        <div><dt>{t('autoFreezeAt')}</dt><dd className="num">{FREEZE_THRESHOLD} / 5</dd></div>
      </dl>

      <section className="block">
        <h3>{t('why')}</h3>
        {signals.length
          ? <ul className="why">
              {signals.map((s) => <li key={s}><Signals names={[s]} /><span>{signal(s)[1]}</span></li>)}
            </ul>
          : <p className="muted">{t('whyNone')}</p>}
      </section>

      <section className="block">
        <h3>{t('history')}</h3>
        {audit?.length
          ? <ol className="audit">
              {audit.map((a, i) => (
                <li key={i}>
                  <span className="when">{bangkokTime(a.at, locale)}</span>
                  <span className={`audit-action ${a.action}`}>{t(`action_${a.action}`)}</span>
                  <span><b>{a.actor}</b> · {a.reason}</span>
                </li>
              ))}
            </ol>
          : <p className="muted small">{!audit ? t('loading') : status === 'FROZEN' ? t('historyMissing') : t('historyNone')}</p>}
      </section>

      <section className="block">
        <h3>{t('timeline')}</h3>
        <p className="hint">{t('timelineHint')}</p>
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
                <span className="amount num">{incoming ? '+' : '−'}฿{thb(e.amountSatang)}</span>
                {followable
                  ? <button className="small ghost" onClick={() => follow(e.transferId)}>{t('follow')}<ArrowRightIcon size={14} /></button>
                  : <span />}
              </li>
            )
          })}
        </ol>
      </section>

      {hops.length > 0 && (
        <section className="block">
          <h3>{t('trail')}</h3>
          <TrailGraph hops={hops} suspects={suspects} statuses={statuses} onSelect={onSelect} />
          <p className="hint">{t('trailHint')}</p>
        </section>
      )}

      <section className="block">
        <h3>{t('community')}</h3>
        {community
          ? <p>{t('communityText', community.id, community.size, community.suspects.length)}
              {community.suspects.length > 0 && ': '}
              {community.suspects.map((m) => <button key={m} className="link num" onClick={() => onSelect(m)}>{m}</button>)}
            </p>
          : <button onClick={findCommunity} disabled={findingCommunity}>
              {findingCommunity ? t('loading') : t('runLouvain')}
            </button>}
      </section>
    </section>
  )
}
