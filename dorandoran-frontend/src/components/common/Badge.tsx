export type BadgeVariant = 'Friendly' | 'Polite' | 'Casual'

interface BadgeProps {
  variant: BadgeVariant
}

const VARIANT_STYLES: Record<BadgeVariant, string> = {
  Friendly: 'bg-label-pink',
  Polite: 'bg-label-blue',
  Casual: 'bg-label-yellow',
}

export default function Badge({ variant }: BadgeProps) {
  return (
    <div
      className={`my-1 px-2 py-1 h-[19px] leading-none text-subtitle text-[10px] text-white rounded-sm  ${VARIANT_STYLES[variant]}`}
    >
      {variant}
    </div>
  )
}
