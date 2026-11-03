import { useEffect, useMemo, useRef, useState } from 'react'
import cytoscape from 'cytoscape'
import { bangkokTime, thb } from './api.js'
import { useT } from './i18n.js'
import { FitIcon } from './icons.jsx'

const VAULT = '0000000000'
const MIN_READABLE_ZOOM = 0.8
const COLUMN_GAP = 220  // three columns fit a ~460px detail column at the zoom floor
const ROW_GAP = 110
const css = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim()

/** "Follow the money": the hops from graph-service /trail, left to right in the order money moved. */
export default function TrailGraph({ hops, suspects, statuses, onSelect }) {
  const { t, locale } = useT()
  const box = useRef(null)
  const cyRef = useRef(null)
  const [cropped, setCropped] = useState(false)
  const scheme = useColorScheme()

  // Column = how many hops from where the money started; cash withdrawn always sits at the far right
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
      return [no, { x: c * COLUMN_GAP, y: r * ROW_GAP }]
    }))
    // As tall as the busiest column needs, so the trail does not float in empty space
    return { positions, height: Math.max(180, (Math.max(...rows.values()) - 1) * ROW_GAP + 150) }
  }, [hops])

  useEffect(() => {
    if (!hops.length) return
    const nodes = [...layout.positions.keys()].map((no) => {
      const cls = [no === VAULT ? 'vault' : suspects[no] ? 'suspect' : 'person']
      if (statuses[no]?.status === 'FROZEN') cls.push('frozen')
      return { data: { id: no, label: no === VAULT ? t('legendCash') : no }, classes: cls.join(' '), position: layout.positions.get(no) }
    })
    const max = Math.max(...hops.map((h) => h.amountSatang))
    const edges = hops.map((h) => ({
      data: {
        id: `t${h.transferId}`, source: h.fromAccountNo, target: h.toAccountNo,
        label: `฿${thb(h.amountSatang)}\n${bangkokTime(h.at, locale)}`,
        width: 1.5 + 6 * Math.sqrt(h.amountSatang / max),
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
          'text-valign': 'bottom', 'text-margin-y': 5, color: text,
          width: 26, height: 26, 'background-color': panel, 'border-width': 3, 'border-color': course } },
        { selector: '.suspect', style: { 'background-color': css('--danger'), 'border-color': text } },
        // Frozen accounts become squares, the map's symbol for a closed point; the vault is a muted block
        { selector: '.frozen', style: { shape: 'rectangle' } },
        { selector: '.vault', style: { shape: 'rectangle', width: 40, height: 20, 'background-color': css('--muted'), 'border-width': 0 } },
        { selector: 'edge', style: {
          label: 'data(label)', 'font-size': 12, 'font-family': css('--font'), 'text-wrap': 'wrap', color: text, 'curve-style': 'bezier',
          'target-arrow-shape': 'triangle', width: 'data(width)', 'line-color': course, 'target-arrow-color': course,
          'text-background-color': panel, 'text-background-opacity': 0.85, 'text-background-padding': 2 } },
      ],
    })
    // On a narrow screen a long trail fits only at an unreadable size: keep labels legible and start at the
    // trail's top-left corner, where the money starts; "fit to screen" still shows the whole trail
    const tooWide = cy.zoom() < MIN_READABLE_ZOOM
    setCropped(tooWide)
    if (tooWide) {
      cy.zoom(MIN_READABLE_ZOOM)
      const bb = cy.elements().boundingBox()
      cy.pan({ x: 30 - bb.x1 * MIN_READABLE_ZOOM, y: 30 - bb.y1 * MIN_READABLE_ZOOM })
    }
    cy.on('tap', 'node', (e) => e.target.id() !== VAULT && onSelect(e.target.id()))
    cyRef.current = cy
    return () => cy.destroy()
  }, [hops, layout, suspects, statuses, onSelect, t, locale, scheme])

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
      <div style={{ height: layout.height }}><div ref={box} className="graph" /></div>
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
