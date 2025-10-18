import { useParams, useNavigate, useLocation } from 'react-router-dom'
import Button from '../components/common/Button'
import { useAgreementStore, type TermId } from '../stores/useAgreementStore'
import PrivacyPolicy from '../components/policy/PrivacyPolicy'
import ServicePolicy from '../components/policy/ServicePolicy'

export default function PolicyPage() {
  const { id } = useParams<{ id: TermId }>()
  const navigate = useNavigate()

  const location = useLocation()
  const path = location.pathname
  const setOne = useAgreementStore(s => s.setOne)

  const handleConfirm = () => {
    if (id) setOne(id, true)
    navigate('/signup', { replace: true, state: { fromPolicy: true } })
  }

  const search = new URLSearchParams(location.search)
  const hideViaQuery = search.get('hideConfirm') === '1'
  const hideConfirm = Boolean(location.state?.hideConfirm || hideViaQuery)

  return (
    <div className="bg-white">
      <div className="max-w-4xl mx-auto px-6 py-10 pb-28 text-gray-800 leading-relaxed">
        {path === '/policy/privacy' ? <PrivacyPolicy /> : <ServicePolicy />}
      </div>

      {!hideConfirm && (
        <div className="fixed inset-x-0 bottom-0 z-10 flex justify-center">
          <div className="w-full max-w-md bg-white px-4 py-3 shadow-[0_-1px_2px_0_rgba(0,0,0,0.08)]">
            <Button
              onClick={handleConfirm}
              variant="primary"
              size="xl"
              className="bg-gray-800 w-full"
            >
              Confirm
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
