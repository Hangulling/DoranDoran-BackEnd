import Button from './Button'
import googleIcon from '../../assets/auth/googleLogin.svg'

type GoogleLoginButtonProps = {
  onClick?: () => void
}

export default function GoogleLoginButton({ onClick }: GoogleLoginButtonProps) {
  return (
    <Button size="full" className="bg-white border border-gray-800" onClick={onClick} type="button">
      <img src={googleIcon} alt="google" className="w-7 h-7 mr-2" />
      <span className="text-subtitle text-gray-800">Sign up with Google</span>
    </Button>
  )
}
