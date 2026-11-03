import { useEffect, useRef } from 'react'
import cytoscape from 'cytoscape'
import { bangkokTime, thb } from './api.js'

const VAULT = '0000000000'

/** "Follow the money": the hops returned by graph-service /trail, drawn left to right. */
export default function TrailGraph({ hops, suspects, statuses, onSelect }) {
  const box = useRef(null)

  useEffect(() => {
    if (!hops.length) return
    const nodes = new Map()
    const addNode = (no) => {
      if (nodes.has(no)) return
      const cls = [no === VAULT ? 'vault' : suspects[no] ? 'suspect' : 'person']
      if (statuses[no]?.status === 'FROZEN') cls.push('frozen')
      nodes.set(no, { data: { id: no, label: no === VAULT ? 'cash out' : no }, classes: cls.join(' ') })
    }
    const edges = hops.map((h) => {
      addNode(h.fromAccountNo)
      addNode(h.toAccountNo)
      return {
        data: {
          id: `t${h.transferId}`, source: h.fromAccountNo, target: h.toAccountNo,
          label: `฿${thb(h.amountSatang)}\n${bangkokTime(h.at)}`,
        },
      }
    })

    const cy = cytoscape({
      container: box.current,
      elements: [...nodes.values(), ...edges],
      layout: { name: 'breadthfirst', directed: true, roots: [hops[0].fromAccountNo], spacingFactor: 1.4 },
      style: [
        { selector: 'node', style: {
          label: 'data(label)', 'font-size': 10, 'text-valign': 'bottom', 'text-margin-y': 4,
          color: '#1d2127', width: 26, height: 26, 'background-color': '#4a7bd0' } },
        { selector: '.suspect', style: { 'background-color': '#d0453c' } },
        { selector: '.vault', style: { shape: 'round-rectangle', 'background-color': '#8a8f98', width: 34 } },
        { selector: '.frozen', style: { 'border-width': 4, 'border-color': '#111' } },
        { selector: 'edge', style: {
          label: 'data(label)', 'font-size': 8, 'text-wrap': 'wrap', 'curve-style': 'bezier',
          'target-arrow-shape': 'triangle', width: 1.5, 'line-color': '#9aa3ad', 'target-arrow-color': '#9aa3ad',
          'text-background-color': '#fff', 'text-background-opacity': 0.8, 'text-background-padding': 2 } },
      ],
    })
    cy.on('tap', 'node', (e) => e.target.id() !== VAULT && onSelect(e.target.id()))
    return () => cy.destroy()
  }, [hops, suspects, statuses, onSelect])

  return <div ref={box} className="graph" />
}
