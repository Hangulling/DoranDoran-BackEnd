import Button from '../components/common/Button'
import Notice from '/public/notice.png'

const NoticePage = () => {
  const endDate = import.meta.env.VITE_NOTICE_DATE || '2026.MM.DD'
  const appleStoreLink = import.meta.env.VITE_APPLE_STORE_LINK

  const handleAppleClick = () => {
    if (appleStoreLink) {
      window.open(appleStoreLink, '_blank')
    } else {
      alert('The iOS app is currently under review and will be available soon.')
    }
  }

  return (
    <div className="flex flex-col items-center h-full text-center mb-[91px]">
      <img src={Notice} />
      <h2 className="text-[24px] text-title text-[#2C2A2C] mb-6">Service Closure Notice</h2>
      <p className="text-[14px] text-[#78757a] mb-5">
        DoranDoran is getting an upgrade in Koach.
        <br />
        DoranDoran Web ends on {endDate}.
        <br />
        Keep using the service in the Koach app.
      </p>
      <p className="text-[14px] text-[#6c51f0] mb-10">
        Your account and data will move with you.
        <br />
        No new sign-up needed.
      </p>
      <Button variant="notice" size="notice" onClick={handleAppleClick}>
        Go to Apple App Store
      </Button>
      <Button
        disabled
        variant="disabledNotice"
        size="notice"
        className="
        cursor-not-allowed pointer-events-none mt-3
      "
      >
        Google Play version is coming soon
      </Button>
      <p className="mt-5 text-[#aba9ad] text-[12px]">
        Our Privacy Policy and Terms
        <br />
        will be updated due to this change.
      </p>
    </div>
  )
}

export default NoticePage
