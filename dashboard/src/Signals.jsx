import { useT } from './i18n.js'

/** A coloured chip per signal; hovering (or focusing) it explains the signal in plain words. */
export default function Signals({ names }) {
  const { signal } = useT()
  return names.map((n) => {
    const [label, meaning] = signal(n)
    return <span key={n} className={`chip sig-${n}`} title={meaning} tabIndex={0}>{label}</span>
  })
}
