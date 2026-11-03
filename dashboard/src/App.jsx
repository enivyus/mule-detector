import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, bangkokTime, streamRules, thb, tier } from './api.js'
import TrailGraph from './TrailGraph.jsx'

const LARGE_SATANG = 100_000 // 1,000 THB, the same floor the rules use

export default function App() {
  const [suspects, setSuspects] = useState({})
  const [statuses, setStatuses] = useState({})
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      const s = await api.suspects()
      setSuspects(s)
      const entries = await Promise.all(Object.keys(s).map(async (no) => [no, await api.account(no)]))
      setStatuses(Object.fromEntries(entries))
    } catch (e) {
      setError(e.message)
    }
  }, [])
  useEffect(() => { load() }, [load])

  const setStatus = useCallback((no, account) => setStatuses((prev) => ({ ...prev, [no]: account })), [])

  return (
    <div className="app">
      <header>
        <h1>Mule investigator</h1>
        <button onClick={load}>Refresh</button>
      </header>
      {error && <p className="error">{error}. Are core-banking, rule-engine and graph-service running?</p>}
      <main>
        <ReviewQueue suspects={suspects} statuses={statuses} selected={selected} onSelect={setSelected}
                     onStatus={setStatus} />
        {selected
          ? <AccountPanel key={selected} no={selected} suspects={suspects} statuses={statuses}
                          onSelect={setSelected} onStatus={setStatus} />
          : <section className="panel muted">Pick an account from the queue.</section>}
      </main>
    </div>
  )
}

function ReviewQueue({ suspects, statuses, selected, onSelect, onStatus }) {
  const rows = useMemo(() => Object.entries(suspects)
    .map(([no, signals]) => ({ no, signals, tier: tier(signals) }))
    .sort((a, b) => (a.tier === b.tier ? b.signals.length - a.signals.length : a.tier === 'freeze' ? -1 : 1)),
  [suspects])
  const frozen = rows.filter((r) => statuses[r.no]?.status === 'FROZEN').length

  return (
    <section className="panel queue">
      <h2>Suspects <span className="muted">{rows.length} accounts, {frozen} frozen</span></h2>
      <table>
        <thead><tr><th>Account</th><th>Tier</th><th>Signals</th><th>Status</th><th /></tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.no} className={r.no === selected ? 'selected' : ''} onClick={() => onSelect(r.no)}>
              <td className="mono">{r.no}</td>
              <td><span className={`badge ${r.tier}`}>{r.tier === 'freeze' ? 'freeze' : 'review'}</span></td>
              <td>{r.signals.map((s) => <span key={s} className="chip">{s}</span>)}</td>
              <td>{statuses[r.no]?.status ?? '…'}</td>
              <td><FreezeButton no={r.no} status={statuses[r.no]?.status} onStatus={onStatus} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}

function FreezeButton({ no, status, onStatus }) {
  const [busy, setBusy] = useState(false)
  if (!status) return null
  const frozen = status === 'FROZEN'
  const act = async (e) => {
    e.stopPropagation()
    setBusy(true)
    try { onStatus(no, await (frozen ? api.unfreeze(no) : api.freeze(no))) } finally { setBusy(false) }
  }
  return <button disabled={busy} onClick={act}>{frozen ? 'Unfreeze' : 'Freeze'}</button>
}

function AccountPanel({ no, suspects, statuses, onSelect, onStatus }) {
  const [account, setAccount] = useState(null)
  const [incoming, setIncoming] = useState([])
  const [hops, setHops] = useState([])
  const [community, setCommunity] = useState(null)

  useEffect(() => {
    api.account(no).then((a) => { setAccount(a); onStatus(no, a) })
    api.statement(no).then((page) => setIncoming(
      page.content.filter((e) => e.direction === 'CREDIT' && e.amountSatang >= LARGE_SATANG)))
  }, [no, onStatus])

  const runLouvain = async () => {
    const all = await api.communities()
    const members = Object.keys(all).filter((k) => all[k] === all[no])
    setCommunity({ id: all[no], size: members.length, suspects: members.filter((m) => suspects[m] && m !== no) })
  }

  if (!account) return <section className="panel muted">Loading {no}…</section>
  const signals = suspects[no] ?? []
  return (
    <section className="panel">
      <h2 className="mono">{no}</h2>
      <p>
        {account.type} · <b>{account.status}</b> · balance ฿{thb(account.balanceSatang)}
        {' '}<FreezeButton no={no} status={account.status} onStatus={(n, a) => { setAccount(a); onStatus(n, a) }} />
      </p>
      <p>{signals.length
        ? <>Signals: {signals.map((s) => <span key={s} className="chip">{s}</span>)}
            {' '}({streamRules(signals).length} stream rules)</>
        : <span className="muted">No signals.</span>}</p>

      <h3>Money in, 1,000 THB or more <span className="muted">(click one to follow it)</span></h3>
      <ul className="incoming">
        {incoming.map((e) => (
          <li key={e.id}><button className="link" onClick={async () => setHops(await api.trail(e.transferId))}>
            ฿{thb(e.amountSatang)} on {bangkokTime(e.createdAt)}
          </button></li>
        ))}
        {!incoming.length && <li className="muted">None in the latest 200 entries.</li>}
      </ul>

      {hops.length > 0 && <>
        <h3>Follow the money <span className="muted">
          red: suspect · thick border: frozen · grey: cash withdrawn · click a node to open it</span></h3>
        <TrailGraph hops={hops} suspects={suspects} statuses={statuses} onSelect={onSelect} />
        <p className="muted small">Each hop left within 2 hours of the money arriving. Money is fungible, so a hop
          can carry other people's money passing through the same account.</p>
      </>}

      <h3>Community</h3>
      {community
        ? <p>Community {community.id}: {community.size} personal accounts, {community.suspects.length} other
            suspects{community.suspects.length > 0 && ': '}
            {community.suspects.map((m) => <button key={m} className="link mono" onClick={() => onSelect(m)}>{m}</button>)}
          </p>
        : <button onClick={runLouvain}>Run Louvain (a few seconds)</button>}
    </section>
  )
}
