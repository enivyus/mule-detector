import { useEffect, useMemo, useRef, useState } from 'react'
import cytoscape from 'cytoscape'
import { groupNo, thb } from './api.js'
import { useT } from './i18n.js'
import { FitIcon } from './icons.jsx'

const VAULT = '0000000000'
const MIN_READABLE_ZOOM = 0.8
const COLUMN_GAP = 200  // the trail has the sheet's full width, so each step gets room for its labels
const VERTICAL_COLUMN_GAP = 110  // phones: one step per row, money flowing down
const VERTICAL_ROW_GAP = 150
const ROW_GAP = 110
const css = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim()

/** "Follow the money": the hops from graph-service /trail, left to right in the order money moved. */
export default function TrailGraph({ hops, suspects, statuses, onSelect }) {
  const { t, locale } = useT()
  const box = useRef(null)
  const cyRef = useRef(null)
  const [cropped, setCropped] = useState(false)
  const [drawnHeight, setDrawnHeight] = useState(null)
  const scheme = useColorScheme()

  const vertical = useNarrow()

  // Column = how many hops from where the money started; cash withdrawn always sits at the far end
  const layout = useMemo(() => {
    if (!hops.length) return { positions: new Map(), height: 0 }
    const column = new Map([[hops[0].fromAccountNo, 0]])
    for (const h of hops) {
      if (h.toAccountNo !== VAULT && !column.has(h.toAccountNo)) column.set(h.toAccountNo, h.depth + 1)
    }
    if (hops.some((h) => h.toAccountNo === VAULT)) column.set(VAULT, Math.max(...column.values()) + 1)
    const rows = new Map()
    const positions = new Map([...column].map(([no, c]) => {
      const r = rows.get(c) ?? 0
      rows.set(c, r + 1)
      return [no, vertical ? { x: r * VERTICAL_ROW_GAP, y: c * VERTICAL_COLUMN_GAP } : { x: c * COLUMN_GAP, y: r * ROW_GAP }]
    }))
    // As tall as the busiest column needs, so the trail does not float in empty space
    const deepest = Math.max(...column.values())
    return { positions, height: vertical ? deepest * VERTICAL_COLUMN_GAP + 120 : Math.max(180, (Math.max(...rows.values()) - 1) * ROW_GAP + 150) }
  }, [hops, vertical])

  useEffect(() => {
    if (!hops.length) return
    const nodes = [...layout.positions.keys()].map((no) => {
      const cls = [no === VAULT ? 'vault' : suspects[no] ? 'suspect' : 'person']
      if (statuses[no]?.status === 'FROZEN') cls.push('frozen')
      return { data: { id: no, label: no === VAULT ? t('legendCash') : groupNo(no) }, classes: cls.join(' '), position: layout.positions.get(no) }
    })
    const max = Math.max(...hops.map((h) => h.amountSatang))
    // Two transfers between the same pair draw as parallel curves; their labels go to opposite sides of the line
    const seen = new Map()
    const edges = hops.map((h) => ({
      data: {
        id: `t${h.transferId}`, source: h.fromAccountNo, target: h.toAccountNo,
        // Amount and clock time only: short enough to sit between two columns; the full time is in the timeline
        label: `฿${thb(h.amountSatang)}\n${new Date(h.at).toLocaleTimeString(locale, { timeZone: 'Asia/Bangkok', hour: '2-digit', minute: '2-digit' })}`,
        width: 1.5 + 6 * Math.sqrt(h.amountSatang / max),
        side: (() => { const k = `${h.fromAccountNo}>${h.toAccountNo}`; const n = seen.get(k) ?? 0; seen.set(k, n + 1); return n % 2 ? 1 : -1 })(),
      },
    }))

    // Cytoscape draws on a canvas and cannot read CSS variables: take the theme's values here, and redraw on a theme switch
    const text = css('--ink'), panel = css('--surface'), course = css('--course')
    const cy = cytoscape({
      container: box.current,
      elements: [...nodes, ...edges],
      layout: { name: 'preset', fit: true, padding: 30 },
      wheelSensitivity: 0.3,
      style: [
        { selector: 'node', style: {
          label: 'data(label)', 'font-size': 14, 'font-family': css('--font'),
          // Vertical (phone) routes run downward, so labels sit beside the node instead of on the edge below it
          ...(vertical ? { 'text-valign': 'center', 'text-halign': 'right', 'text-margin-x': 8 } : { 'text-valign': 'bottom', 'text-margin-y': 5 }),
          color: text, 'text-background-color': panel, 'text-background-opacity': 1, 'text-background-padding': 3,
          width: 22, height: 22, 'background-color': panel, 'border-width': 3, 'border-color': course } },
        { selector: '.suspect', style: { 'background-color': css('--danger'), 'border-color': text } },
        // Frozen accounts become squares, the map's symbol for a closed point; the vault is a muted block
        { selector: '.frozen', style: { shape: 'rectangle' } },
        { selector: '.vault', style: { shape: 'rectangle', width: 40, height: 20, 'background-color': css('--muted'), 'border-width': 0 } },
        { selector: 'edge', style: {
          label: 'data(label)', 'font-size': 12, 'font-family': css('--font'), 'text-wrap': 'wrap', ...(vertical ? { 'text-margin-x': (e) => e.data('side') * 44 } : { 'text-margin-y': (e) => e.data('side') * 16 }), color: text, 'curve-style': 'bezier', 'control-point-step-size': 46,
          'target-arrow-shape': 'triangle', width: 'data(width)', 'line-color': course, 'target-arrow-color': course,
          'text-background-color': panel, 'text-background-opacity': 0.85, 'text-background-padding': 2 } },
      ],
    })
    // On a narrow screen a long trail fits only at an unreadable size: keep labels legible and start at the
    // trail's top-left corner, where the money starts; "fit to screen" still shows the whole trail
    if (cy.zoom() < MIN_READABLE_ZOOM) cy.zoom(MIN_READABLE_ZOOM)
    // Start at the top-left corner, where the money starts, and make the canvas exactly as tall as what it draws
    const bb = cy.elements().boundingBox()
    const zoom = cy.zoom()
    // Only a trail that really runs past the edge at the readable zoom gets the pan cue
    setCropped(bb.w * zoom > cy.width() - 8)
    cy.pan({ x: Math.max(24, (cy.width() - bb.w * zoom) / 2) - bb.x1 * zoom, y: 24 - bb.y1 * zoom })
    setDrawnHeight(Math.ceil(bb.h * zoom + 48))
    cy.on('tap', 'node', (e) => e.target.id() !== VAULT && onSelect(e.target.id()))
    cyRef.current = cy
    return () => cy.destroy()
  }, [hops, layout, suspects, statuses, onSelect, t, locale, scheme, vertical])

  useEffect(() => { cyRef.current?.resize() }, [drawnHeight])

  return (
    <div className="graph-wrap">
      <div className="graph-bar">
        <span className="legend"><svg width="14" height="14" aria-hidden="true"><circle cx="7" cy="7" r="5.5" fill="var(--danger)" stroke="var(--ink)" strokeWidth="2" /></svg>{t('legendSuspect')}</span>
        <span className="legend"><svg width="14" height="14" aria-hidden="true"><circle cx="7" cy="7" r="5.5" fill="var(--surface)" stroke="var(--course)" strokeWidth="2" /></svg>{t('legendPerson')}</span>
        <span className="legend"><svg width="14" height="14" aria-hidden="true"><rect x="1.5" y="1.5" width="11" height="11" fill="var(--surface)" stroke="var(--ink)" strokeWidth="2" /></svg>{t('legendFrozen')}</span>
        <span className="legend"><svg width="20" height="12" aria-hidden="true"><rect x="0" y="1" width="20" height="10" fill="var(--muted)" /></svg>{t('legendCash')}</span>
        <span className="legend course"><svg width="22" height="12" aria-hidden="true"><path d="M1 6h16" stroke="var(--course)" strokeWidth="3" /><path d="m15 2 5 4-5 4Z" fill="var(--course)" /></svg>{t('legendRoute')}</span>
        <span className="grow" />
        <button className="small ghost icon-text" onClick={() => cyRef.current?.fit(undefined, 30)}><FitIcon size={14} />{t('fit')}</button>
      </div>
      {/* Cytoscape rewrites its container's inline style, so the height lives on a wrapper */}
      <div style={{ height: drawnHeight ?? layout.height }}><div ref={box} className="graph" /></div>
      {cropped && <p className="graph-cue">{t('trailCropped')}</p>}
    </div>
  )
}

/** 'dark' or 'light', following the OS setting live, so the canvas can redraw in the new theme. */
function useColorScheme() {
  const query = '(prefers-color-scheme: dark)'
  const [scheme, setScheme] = useState(() => (window.matchMedia(query).matches ? 'dark' : 'light'))
  useEffect(() => {
    const mq = window.matchMedia(query)
    const change = () => setScheme(mq.matches ? 'dark' : 'light')
    mq.addEventListener('change', change)
    return () => mq.removeEventListener('change', change)
  }, [])
  return scheme
}

/** True on phones (same breakpoint as the CSS), following window resizes. */
function useNarrow() {
  const query = '(max-width: 860px)'
  const [narrow, setNarrow] = useState(() => window.matchMedia(query).matches)
  useEffect(() => {
    const mq = window.matchMedia(query)
    const change = () => setNarrow(mq.matches)
    mq.addEventListener('change', change)
    return () => mq.removeEventListener('change', change)
  }, [])
  return narrow
}
