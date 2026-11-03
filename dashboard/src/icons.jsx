// One icon set in the survey-map hand: 24px grid, 2px stroke, square caps and mitred corners, so the icons read as
// map symbols set beside Chakra Petch rather than soft UI glyphs.
const Icon = ({ size = 16, children, ...rest }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
       strokeLinecap="square" strokeLinejoin="miter" aria-hidden="true" {...rest}>
    {children}
  </svg>
)

/** Settings: an eight-toothed gear cut from straight segments. */
export const GearIcon = (p) => (
  <Icon {...p}>
    <path d="M10 2h4l.6 3 2.2 1.3 2.9-1 2 3.4-2.3 2v2.6l2.3 2-2 3.4-2.9-1-2.2 1.3-.6 3h-4l-.6-3-2.2-1.3-2.9 1-2-3.4 2.3-2v-2.6l-2.3-2 2-3.4 2.9 1 2.2-1.3Z" />
    <rect x="9" y="9" width="6" height="6" />
  </Icon>
)
export const LockIcon = (p) => (
  <Icon {...p}><rect x="4" y="11" width="16" height="10" /><path d="M8 11V7h8v4" /></Icon>
)
export const UnlockIcon = (p) => (
  <Icon {...p}><rect x="4" y="11" width="16" height="10" /><path d="M8 11V7h8V5" /></Icon>
)
export const SearchIcon = (p) => (
  <Icon {...p}><rect x="4" y="4" width="11" height="11" /><path d="m15 15 5 5" /></Icon>
)
export const RefreshIcon = (p) => (
  <Icon {...p}><path d="M20 12a8 8 0 1 1-3-6.2" /><path d="M20 3v5h-5" /></Icon>
)
export const ArrowRightIcon = (p) => (
  <Icon {...p}><path d="M4 12h15" /><path d="m13 6 6 6-6 6" /></Icon>
)
export const ArrowLeftIcon = (p) => (
  <Icon {...p}><path d="M20 12H5" /><path d="m11 6-6 6 6 6" /></Icon>
)
/** Language: a graticule, the grid of a map sheet. */
export const GlobeIcon = (p) => (
  <Icon {...p}><rect x="3" y="3" width="18" height="18" /><path d="M3 9h18M3 15h18M9 3v18M15 3v18" /></Icon>
)
export const SignOutIcon = (p) => (
  <Icon {...p}><path d="M13 4H4v16h9" /><path d="M10 12h10" /><path d="m16 8 4 4-4 4" /></Icon>
)
export const FitIcon = (p) => (
  <Icon {...p}><path d="M3 8V3h5M21 8V3h-5M3 16v5h5M21 16v5h-5" /></Icon>
)

/** Product mark: a north arrow over a contour, the reading point of every survey sheet. */
export const MarkIcon = (p) => (
  <Icon {...p}><path d="M12 2 17 14 12 11 7 14Z" fill="currentColor" /><path d="M3 19c3-2 6-2 9 0s6 2 9 0" /></Icon>
)

/**
 * Map-legend symbols for the queue views and states: open circle (all), filled triangle (freeze tier, still open),
 * outlined triangle (waiting for review), filled square (frozen).
 */
export function ViewSymbol({ kind, size = 14 }) {
  const shape = {
    all: <circle cx="8" cy="8" r="5.5" fill="none" stroke="currentColor" strokeWidth="2" />,
    freeze: <path d="M8 1.5 15 14H1Z" fill="currentColor" />,
    review: <path d="M8 3.5 13.5 13h-11Z" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="miter" />,
    frozen: <rect x="2" y="2" width="12" height="12" fill="currentColor" />,
  }[kind]
  return <svg width={size} height={size} viewBox="0 0 16 16" aria-hidden="true" className={`symbol ${kind}`}>{shape}</svg>
}

/** Risk as elevation: one contour ring per real-time rule fired, up to five, with a summit dot at the top. */
export function RiskContours({ rules, size = 22 }) {
  const rings = Math.max(0, Math.min(5, rules))
  return (
    <svg width={size} height={size} viewBox="0 0 22 22" aria-hidden="true" className="contours">
      {Array.from({ length: rings }, (_, i) => (
        <circle key={i} cx="11" cy="11" r={10 - i * 2} fill="none" stroke="currentColor" strokeWidth="1.2" />
      ))}
      {rings >= 3 && <circle cx="11" cy="11" r="1.4" fill="currentColor" />}
    </svg>
  )
}
