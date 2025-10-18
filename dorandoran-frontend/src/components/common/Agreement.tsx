import { useMemo } from 'react'
import check from '../../assets/icon/check.svg'
import checkALL from '../../assets/icon/checkAll.svg'
import disabledCheck from '../../assets/icon/disabledCheck.svg'
import disabledCheckAll from '../../assets/icon/disabledCheckAll.svg'
import infoIcon from '../../assets/icon/info.svg'
import Button from './Button'
import { Link } from 'react-router-dom'

type TermId = 'service' | 'privacy' | 'marketing'
export type AgreementValue = Record<TermId, boolean>

interface AgreementProps {
  value: AgreementValue
  onChange: (next: AgreementValue) => void
}

export default function Agreement({ value, onChange }: AgreementProps) {
  const allChecked = useMemo(
    () => value.service && value.privacy && value.marketing,
    [value.service, value.privacy, value.marketing]
  )
  const someChecked = useMemo(
    () => value.service || value.privacy || value.marketing,
    [value.service, value.privacy, value.marketing]
  )

  const toggleAll = () => {
    const next = {
      service: !allChecked,
      privacy: !allChecked,
      marketing: !allChecked,
    }
    onChange(next)
  }

  const toggleOne = (id: TermId) => {
    onChange({ ...value, [id]: !value[id] })
  }

  const terms = [
    { id: 'all', label: 'Agree to all', required: false },
    { id: 'service', label: '(Required) Agree to Terms of Service', required: true },
    { id: 'privacy', label: '(Required) Agree to Personal Information Policy', required: true },
    { id: 'marketing', label: '(Optional) Agree to Marketing Communications', required: false },
  ] as const

  const getIconSrc = (id: 'all' | TermId, isChecked: boolean) => {
    if (id === 'all') return isChecked ? checkALL : disabledCheckAll
    return isChecked ? check : disabledCheck
  }

  return (
    <div className="flex flex-col gap-3 w-[335px] my-2">
      <button
        type="button"
        onClick={toggleAll}
        className="flex items-center gap-2"
        role="checkbox"
        aria-checked={allChecked ? 'true' : someChecked ? 'mixed' : 'false'}
      >
        <span className="relative w-6 h-6">
          <img src={getIconSrc('all', allChecked)} alt="all-check" className="w-6 h-6" />
        </span>
        <span className="text-base text-subtitle">Agree to all</span>
      </button>

      {terms.slice(1).map(t => {
        const id = t.id as TermId
        const isChecked = value[id]

        return (
          <div key={t.id} className="flex items-center justify-between">
            <button
              type="button"
              onClick={() => toggleOne(id)}
              className="flex items-center gap-2"
              role="checkbox"
              aria-checked={isChecked}
            >
              <img src={getIconSrc(id, isChecked)} alt="check" className="w-5 h-5" />
              <span
                className={`${isChecked ? 'text-green-400' : 'text-gray-500'} text-xs text-body`}
              >
                {t.label}
              </span>
            </button>

            {t.required && (
              <Link to={`/policy/${id}`}>
                <Button
                  type="button"
                  variant="text"
                  size="xs"
                  className="w-6 h-6 p-1"
                  aria-label={`${t.label} details`}
                >
                  <img src={infoIcon} alt="info" className="w-5 h-5 opacity-60" />
                </Button>
              </Link>
            )}
          </div>
        )
      })}
    </div>
  )
}
