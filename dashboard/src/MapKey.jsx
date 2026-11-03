import { useT } from './i18n.js'
import { RiskContours, ViewSymbol } from './icons.jsx'

/** Shown before an account is opened: the sheet's key, so a first-time viewer can read every symbol on the page. */
export default function MapKey() {
  const { t } = useT()
  return (
    <section className="panel map-key" aria-labelledby="map-key-title">
      <h2 id="map-key-title">{t('keyTitle')}</h2>
      <p className="hint">{t('pick')}</p>
      <ul>
        <li><span className="key-mark contour"><RiskContours rules={4} size={30} /></span><span>{t('keyContours')}</span></li>
        <li><span className="key-mark"><ViewSymbol kind="freeze" size={16} /></span><span>{t('tier_freeze')}</span></li>
        <li><span className="key-mark"><ViewSymbol kind="review" size={16} /></span><span>{t('tier_review')}</span></li>
        <li><span className="key-mark"><ViewSymbol kind="frozen" size={14} /></span><span>{t('status_FROZEN')}</span></li>
        <li><span className="key-mark ok"><ViewSymbol kind="all" size={14} /></span><span>{t('status_ACTIVE')}</span></li>
        <li>
          <span className="key-mark course">
            <svg width="30" height="12" aria-hidden="true"><path d="M1 6h22" stroke="currentColor" strokeWidth="3" /><path d="m21 1 8 5-8 5Z" fill="currentColor" /></svg>
          </span>
          <span>{t('keyRoute')}</span>
        </li>
      </ul>
    </section>
  )
}
