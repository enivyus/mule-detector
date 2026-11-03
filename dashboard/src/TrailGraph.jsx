import { useEffect, useMemo, useRef, useState } from 'react'
import cytoscape from 'cytoscape'
import { bangkokTime, thb } from './api.js'
import { useT } from './i18n.js'
import { FitIcon } from './icons.jsx'

const VAULT = '0000000000'
const MIN_READABLE_ZOOM = 0.8
const COLUMN_GAP = 260
const ROW_GAP = 110
const css = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim()

/** "Follow the money": the hops from graph-service /trail, left to right in the order money moved. */
export default function TrailGraph({ hops, suspects, statuses, onSelect }) {
  const { t, locale } = useT()
  const box = useRef(null)
  const cyRef = useRef(null)
  const [cropped, setCropped] = useState(false)

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
    return { positions, height: Math.max(220, Math.max(...rows.values()) * ROW_GAP + 110) }
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

    // Cytoscape draws on a canvas and cannot read CSS variables, so take the theme's colours once
    const text = css('--ink'), line = css('--line-strong'), panel = css('--surface')
    const cy = cytoscape({
      container: box.current,
      elements: [...nodes, ...edges],
      layout: { name: 'preset', fit: true, padding: 30 },
      wheelSensitivity: 0.3,
      style: [
        { selector: 'node', style: {
          label: 'data(label)', 'font-size': 14, 'font-family': 'Inter, "Noto Sans Thai", system-ui, sans-serif',
          'text-valign': 'bottom', 'text-margin-y': 5, color: text,
          width: 28, height: 28, 'background-color': css('--person') } },
        { selector: '.suspect', style: { 'background-color': css('--danger') } },
        { selector: '.vault', style: { shape: 'round-rectangle', width: 44, 'background-color': css('--muted') } },
        { selector: '.frozen', style: { 'border-width': 5, 'border-color': text } },
        { selector: 'edge', style: {
          label: 'data(label)', 'font-size': 13, 'text-wrap': 'wrap', color: text, 'curve-style': 'bezier',
          'target-arrow-shape': 'triangle', width: 'data(width)', 'line-color': line, 'target-arrow-color': line,
          'text-background-color': panel, 'text-background-opacity': 0.85, 'text-background-padding': 2 } },
      ],
    })
    // On a narrow screen a long trail fits only at an unreadable size: keep labels legible and start at the
    // money's origin instead; "fit to screen" still shows the whole trail
    const tooWide = cy.zoom() < MIN_READABLE_ZOOM
    setCropped(tooWide)
    if (tooWide) {
      cy.zoom(MIN_READABLE_ZOOM)
      const p = cy.getElementById(hops[0].fromAccountNo).renderedPosition()
      cy.panBy({ x: 40 - p.x, y: cy.height() / 2 - p.y })
    }
    cy.on('tap', 'node', (e) => e.target.id() !== VAULT && onSelect(e.target.id()))
    cyRef.current = cy
    return () => cy.destroy()
  }, [hops, layout, suspects, statuses, onSelect, t, locale])

  return (
    <div className="graph-wrap">
      <div className="graph-bar">
        <span className="legend"><i className="dot suspect" />{t('legendSuspect')}</span>
        <span className="legend"><i className="dot person" />{t('legendPerson')}</span>
        <span className="legend"><i className="dot frozen" />{t('legendFrozen')}</span>
        <span className="legend"><i className="dot vault" />{t('legendCash')}</span>
        <span className="grow" />
        <button className="small ghost icon-text" onClick={() => cyRef.current?.fit(undefined, 30)}><FitIcon size={14} />{t('fit')}</button>
      </div>
      {/* Cytoscape rewrites its container's inline style, so the height lives on a wrapper */}
      <div style={{ height: layout.height }}><div ref={box} className="graph" /></div>
      {cropped && <p className="graph-cue">{t('trailCropped')}</p>}
    </div>
  )
}
