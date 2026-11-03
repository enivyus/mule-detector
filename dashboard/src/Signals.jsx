import { useT } from './i18n.js'

/** A chip per signal; hovering (or focusing) it explains the signal in plain words. With max, the rest fold into "+N". */
export default function Signals({ names, max = names.length }) {
  const { signal, t } = useT()
  const hidden = names.slice(max)
  return <>
    {names.slice(0, max).map((n) => {
      const [label, meaning] = signal(n)
      return <span key={n} className={`chip sig-${n}`} title={meaning} tabIndex={0}>{label}</span>
    })}
    {hidden.length > 0 && (
      <span className="chip more" tabIndex={0} title={hidden.map((n) => signal(n)[0]).join(', ')}
            aria-label={hidden.map((n) => signal(n)[0]).join(', ')}>{t('moreSignals', hidden.length)}</span>
    )}
  </>
}
