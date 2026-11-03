import { useEffect, useRef } from 'react'
import cytoscape from 'cytoscape'
import { bangkokTime, thb } from './api.js'
import { useT } from './i18n.js'

const VAULT = '0000000000'
const MIN_READABLE_ZOOM = 0.8
const css = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim()

/** "Follow the money": the hops from graph-service /trail, left to right in the order money moved. */
export default function TrailGraph({ hops, suspects, statuses, onSelect }) {
  const { t, locale } = useT()
  const box = useRef(null)
  const cyRef = useRef(null)

  useEffect(() => {
    if (!hops.length) return
    // Column = how many hops from where the money started; cash withdrawn always sits at the far right
    const column = new Map([[hops[0].fromAccountNo, 0]])
    for (const h of hops) {
      if (h.toAccountNo !== VAULT && !column.has(h.toAccountNo)) column.set(h.toAccountNo, h.depth + 1)
    }
    if (hops.some((h) => h.toAccountNo === VAULT)) column.set(VAULT, Math.max(...column.values()) + 1)
    const rows = new Map()
    const position = (no) => {
      const c = column.get(no)
      const r = rows.get(c) ?? 0
      rows.set(c, r + 1)
      return { x: c * 230, y: r * 95 }
    }

    const nodes = [...column.keys()].map((no) => {
      const cls = [no === VAULT ? 'vault' : suspects[no] ? 'suspect' : 'person']
      if (statuses[no]?.status === 'FROZEN') cls.push('frozen')
      return { data: { id: no, label: no === VAULT ? t('legendCash') : no }, classes: cls.join(' '), position: position(no) }
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
    const text = css('--text'), line = css('--line-strong'), panel = css('--panel')
    const cy = cytoscape({
      container: box.current,
      elements: [...nodes, ...edges],
      layout: { name: 'preset', fit: true, padding: 30 },
      wheelSensitivity: 0.3,
      style: [
        { selector: 'node', style: {
          label: 'data(label)', 'font-size': 10, 'font-family': 'ui-monospace, Consolas, monospace',
          'text-valign': 'bottom', 'text-margin-y': 5, color: text,
          width: 28, height: 28, 'background-color': css('--person') } },
        { selector: '.suspect', style: { 'background-color': css('--freeze') } },
        { selector: '.vault', style: { shape: 'round-rectangle', width: 44, 'background-color': css('--muted'),
          'font-family': 'system-ui, sans-serif' } },
        { selector: '.frozen', style: { 'border-width': 5, 'border-color': text } },
        { selector: 'edge', style: {
          label: 'data(label)', 'font-size': 8, 'text-wrap': 'wrap', color: text, 'curve-style': 'bezier',
          'target-arrow-shape': 'triangle', width: 'data(width)', 'line-color': line, 'target-arrow-color': line,
          'text-background-color': panel, 'text-background-opacity': 0.85, 'text-background-padding': 2 } },
      ],
    })
    // On a narrow screen a long trail fits only at an unreadable size: keep labels legible and start at the
    // money's origin instead; "fit to screen" still shows the whole trail
    if (cy.zoom() < MIN_READABLE_ZOOM) {
      cy.zoom(MIN_READABLE_ZOOM)
      const p = cy.getElementById(hops[0].fromAccountNo).renderedPosition()
      cy.panBy({ x: 40 - p.x, y: cy.height() / 2 - p.y })
    }
    cy.on('tap', 'node', (e) => e.target.id() !== VAULT && onSelect(e.target.id()))
    cyRef.current = cy
    return () => cy.destroy()
  }, [hops, suspects, statuses, onSelect, t, locale])

  return (
    <div className="graph-wrap">
      <div className="graph-bar">
        <span className="legend"><i className="dot suspect" />{t('legendSuspect')}</span>
        <span className="legend"><i className="dot person" />{t('legendPerson')}</span>
        <span className="legend"><i className="dot frozen" />{t('legendFrozen')}</span>
        <span className="legend"><i className="dot vault" />{t('legendCash')}</span>
        <span className="muted small grow">{t('openNode')}</span>
        <button className="small" onClick={() => cyRef.current?.fit(undefined, 30)}>{t('fit')}</button>
      </div>
      <div ref={box} className="graph" />
    </div>
  )
}
