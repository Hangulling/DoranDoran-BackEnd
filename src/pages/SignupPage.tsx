import { useEffect, useState } from 'react'
import axios from 'axios'
import Button from '../components/common/Button'
import Input from '../components/common/Input'
import Agreement from '../components/common/Agreement'
import { PASSWORD_REGEX, validateEmail, validateName } from '../utils/validations'
import CommonModal from '../components/common/CommonModal'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAgreementStore } from '../stores/useAgreementStore'
import { useSignupFormStore } from '../stores/useSignupStore'
import { checkEmailExists, requestEmailVerification } from '../api/auth'
import { createUser } from '../api/user'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../constants/env'

export default function SignupPage() {
  const {
    firstName,
    lastName,
    email,
    password,
    passwordCheck,
    verifiedEmail,
    emailVerified,
    setMany,
    reset: resetForm,
  } = useSignupFormStore()

  const [firstNameError, setFirstNameError] = useState<string | null>(null)
  const [lastNameError, setLastNameError] = useState<string | null>(null)
  const [emailError, setEmailError] = useState<string | null>(null)
  const [emailSuccess, setEmailSuccess] = useState<string | null>(null)
  const [pwdTouched, setPwdTouched] = useState(false)
  const [pwdCheckTouched, setPwdCheckTouched] = useState(false)
  const [openModal, setOpenModal] = useState(false)
  const [, setSubmitError] = useState<string | null>(null)
  const [, setPwdError] = useState<string | null>(null)
  const [, setPwdCheckError] = useState<string | null>(null)

  const navigate = useNavigate()

  const agreements = useAgreementStore(s => s.value)
  const setManyAgreements = useAgreementStore(s => s.setMany)
  const resetAgreements = useAgreementStore(s => s.reset)
  const [searchParams] = useSearchParams()

  const handleFirstNameChange = (v: string) => {
    const noSpace = v.replace(/[^A-Za-z]/g, '')
    if (noSpace.length > 15) {
      setFirstNameError('Enter 1-15 characters.')
      return
    }
    setMany({ firstName: noSpace })
    if (firstNameError && validateName(noSpace) === null) setFirstNameError(null)
  }

  const handleLastNameChange = (v: string) => {
    const noSpace = v.replace(/[^A-Za-z]/g, '')
    if (noSpace.length > 15) {
      setLastNameError('Enter 1-15 characters.')
      return
    }
    setMany({ lastName: noSpace })
    if (lastNameError && validateName(noSpace) === null) setLastNameError(null)
  }

  const handleEmailChange = (v: string) => {
    const noSpace = v.replace(/\s+/g, '')
    setMany({ email: noSpace })

    if (emailError && validateEmail(noSpace) === null) setEmailError(null)
    setEmailSuccess(null)

    if (verifiedEmail && noSpace !== verifiedEmail) {
      setMany({ emailVerified: false, verifiedEmail: null })
    }

    const err = validateEmail(noSpace)
    setEmailError(err)
  }

  const handleEmailBlur = () => {
    const err = validateEmail(email)
    setEmailError(err)
    if (err) setEmailSuccess(null)
  }

  const handleEmailKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === ' ') e.preventDefault()
    if (e.nativeEvent.isComposing) return
    if (e.key === 'Enter') {
      e.preventDefault()
      if (isEmailFormatValid) handleVerify()
    }
  }

  const handlePasswordChange = (v: string) => {
    const value = v.replace(/\s+/g, '')
    setMany({ password: value })
    setPwdTouched(true)

    if (value.length === 0) setPwdError(null)
    else if (value.length < 8) setPwdError('Must be at least 8 characters.')
    else if (value.length > 20) setPwdError('Must be 20 characters or fewer.')
    else if (!PASSWORD_REGEX.test(value)) setPwdError('Please check your password format.')
    else setPwdError(null)

    if (passwordCheck.length > 0) {
      setPwdCheckTouched(true)
      setPwdCheckError(value === passwordCheck ? null : 'Passwords do not match.')
    }
  }

  const handlePasswordCheckChange = (v: string) => {
    const value = v.replace(/\s+/g, '')
    setMany({ passwordCheck: value })
    setPwdCheckTouched(true)

    const baseOk = PASSWORD_REGEX.test(password)
    if (baseOk) setPwdCheckError(password === value ? null : 'Passwords do not match.')
    else setPwdCheckError(null)
  }

  const handleVerify = async () => {
    const err = validateEmail(email) || (email.trim() === '' ? 'Please enter your email.' : null)
    if (err) {
      setEmailError(err)
      setEmailSuccess(null)
      setMany({ emailVerified: false, verifiedEmail: null })
      return
    }

    try {
      setEmailError(null)
      setEmailSuccess(null)

      const isDuplicate = await checkEmailExists(email)
      if (isDuplicate) {
        setEmailError('This email is already registered.')
        setMany({ emailVerified: false, verifiedEmail: null })
        return
      }

      const res = await requestEmailVerification({ email, firstName, lastName })
      if (import.meta.env.DEV) console.log('request-verification ok:', res)
      setEmailSuccess('Verification email sent. Please verify through your inbox.')
      setMany({ emailVerified: false, verifiedEmail: email })
    } catch (e: unknown) {
      const parsed = axios.isAxiosError(e)
        ? {
            status: e.response?.status,
            url: e.config?.url,
            message:
              e.response?.data?.message ??
              e.response?.data?.error ??
              (typeof e.response?.data === 'string' ? e.response?.data : undefined) ??
              e.message,
            data: e.response?.data,
          }
        : { message: String(e) }

      console.error('request-verification failed', parsed)
      setEmailError(parsed.message || 'Email verification failed.')
      setMany({ emailVerified: false, verifiedEmail: null })
    }
  }

  const isEmailFormatValid = email.trim() !== '' && validateEmail(email) === null
  const alreadyVerified = emailVerified && verifiedEmail === email
  const pwdFormatError = pwdTouched && password.length > 0 && !PASSWORD_REGEX.test(password)
  const pwdMatchError =
    pwdCheckTouched && passwordCheck.length > 0 && !pwdFormatError && password !== passwordCheck

  const passwordHelper = pwdFormatError
    ? 'Please check your password format.'
    : pwdMatchError
      ? 'Passwords do not match.'
      : null

  const passwordsMatch =
    pwdCheckTouched &&
    PASSWORD_REGEX.test(password) &&
    passwordCheck.length > 0 &&
    password === passwordCheck

  const requiredAgreed = agreements.service && agreements.privacy

  const isPasswordValidForSubmit =
    PASSWORD_REGEX.test(password) && passwordCheck.length > 0 && password === passwordCheck

  const isFormValid =
    firstName.trim() !== '' &&
    lastName.trim() !== '' &&
    email.trim() !== '' &&
    validateName(firstName) === null &&
    validateName(lastName) === null &&
    validateEmail(email) === null &&
    isPasswordValidForSubmit &&
    requiredAgreed &&
    emailVerified

  const handleConfirmModal = async () => {
    setOpenModal(false)
    setSubmitError(null)

    try {
      const emailErr = validateEmail(email)
      if (emailErr) {
        setEmailError(emailErr)
        return
      }
      const dup = await checkEmailExists(email)
      if (dup) {
        setEmailError('This email is already registered.')
        return
      }

      const payload = {
        email,
        firstName,
        lastName,
        name: `${firstName} ${lastName}`.trim(),
        password,
      }

      const res = await createUser(payload)
      if (import.meta.env.DEV) console.log('🎉 회원가입 성공:', res)

      // sign_up GA
      if (IS_PROD && GA_ENABLED) {
        const completeTimestamp = Math.floor(Date.now() / 1000) // UNIX 타임스탬프
        ReactGA.event('sign_up', {
          method: 'email', // 추후 확장
          user_id: res.id,
          complete_timestamp: completeTimestamp,
        })
      }

      resetForm()
      resetAgreements()
      navigate('/login', { replace: true })
    } catch (e: unknown) {
      const isAxios = axios.isAxiosError(e)
      const status = isAxios ? e.response?.status : undefined
      const code = status ?? 503
      const msg = isAxios ? e.response?.data?.message || 'Sign up failed.' : 'Sign up failed.'
      setSubmitError(msg)

      if (status && status >= 400 && status < 500) {
        return
      }
      navigate('/error', { replace: true, state: { code, from: 'signup' } })
    }
  }

  const handleOpenModal = () => {
    if (!isFormValid) return
    setOpenModal(true)
  }

  // 페이지 뷰 추가
  useEffect(() => {
    if (IS_PROD && GA_ENABLED) {
      const entryTimestamp = Math.floor(Date.now() / 1000) // UNIX 타임스탬프
      ReactGA.event('view_sign_in', {
        entry_timestamp: entryTimestamp,
      })
    }

    if (searchParams.get('clear') === '1') {
      resetForm()
      resetAgreements()
      navigate('/signup', { replace: true })
    }
  }, [])

  useEffect(() => {
    const verifiedParam = searchParams.get('verified')
    const emailParam = searchParams.get('email')
    const errorParam = searchParams.get('error')

    const firstNameParam = searchParams.get('firstName') || searchParams.get('fn')
    const lastNameParam = searchParams.get('lastName') || searchParams.get('ln')

    let touched = false
    const patch: Partial<ReturnType<typeof useSignupFormStore.getState>> = {}

    if (emailParam) {
      patch.email = emailParam
      touched = true
    }
    if (firstNameParam) {
      patch.firstName = firstNameParam
      touched = true
    }
    if (lastNameParam) {
      patch.lastName = lastNameParam
      touched = true
    }

    if (verifiedParam === 'true') {
      patch.verifiedEmail = emailParam ?? null
      patch.emailVerified = true
      setEmailSuccess('Email verified successfully!')
      setEmailError(null)
      touched = true
    } else if (verifiedParam === 'false') {
      setEmailError(errorParam || 'Email verification failed.')
      setEmailSuccess(null)
      patch.emailVerified = false
      touched = true
    }

    if (touched) {
      setMany(patch)
      navigate('/signup', { replace: true, state: { fromVerify: true } })
    }
  }, [searchParams, navigate, setMany])

  return (
    <div className="mt-4 max-w-md">
      <div className="flex flex-col justify-center items-center">
        <div>
          <Input
            type="text"
            label="First name *"
            variant={firstNameError ? 'error' : 'primary'}
            placeholder="Enter your first name (1-15 characters)"
            onChange={e => handleFirstNameChange(e.target.value)}
            onBlur={() => setFirstNameError(validateName(firstName))}
            value={firstName}
          />
          {firstNameError && <span className="text-xs text-orange-300">{firstNameError}</span>}
        </div>

        <div>
          <Input
            type="text"
            label="Last name *"
            placeholder="Enter your last name (1-15 characters)"
            variant={lastNameError ? 'error' : 'primary'}
            onChange={e => handleLastNameChange(e.target.value)}
            onBlur={() => setLastNameError(validateName(lastName))}
            value={lastName}
          />
          {lastNameError && <span className="text-xs text-orange-300">{lastNameError}</span>}
        </div>

        <div className="w-[335px]">
          <div className="flex items-end justify-between">
            <Input
              type="email"
              label="E-mail *"
              placeholder="Enter your E-mail"
              size="md"
              value={email}
              onChange={e => handleEmailChange(e.target.value)}
              onBlur={handleEmailBlur}
              onKeyDown={handleEmailKeyDown}
              variant={emailError ? 'error' : 'primary'}
            />
            <Button
              type="button"
              variant="primary"
              disabled={!isEmailFormatValid}
              className="bg-gray-800 my-2 text-subtitle"
              size="sm"
              onClick={handleVerify}
            >
              Verify
            </Button>
          </div>

          {emailError ? (
            <span className="text-xs text-orange-300">{emailError}</span>
          ) : alreadyVerified ? (
            <span className="text-xs text-blue-500">Verified successfully.</span>
          ) : emailSuccess ? (
            <span className="text-xs text-blue-500">{emailSuccess}</span>
          ) : null}
        </div>

        <div className="w-[335px]">
          <Input
            type="password"
            label="Password *"
            placeholder="Enter 8-20 characters & letters+numbers"
            value={password}
            onChange={e => handlePasswordChange(e.target.value)}
            onBlur={() => setPwdTouched(true)}
            variant={pwdFormatError || pwdMatchError ? 'error' : 'primary'}
          />
        </div>

        <div className="w-[335px]">
          <Input
            type="password"
            placeholder="Enter 8-20 characters & letters+numbers"
            value={passwordCheck}
            onChange={e => handlePasswordCheckChange(e.target.value)}
            onBlur={() => setPwdCheckTouched(true)}
            variant={pwdMatchError ? 'error' : 'primary'}
          />
          {passwordHelper ? (
            <span className="text-orange-300 text-xs">{passwordHelper}</span>
          ) : (
            passwordsMatch && <span className="text-blue-500 text-xs">Passwords match.</span>
          )}
        </div>

        <div className="my-2">
          <Agreement value={agreements} onChange={setManyAgreements} />
        </div>

        <div className="flex justify-center w-full bg-white shadow-[0_-1px_2px_0_rgba(0,0,0,0.08)]">
          <div className="m-2">
            <Button
              type="submit"
              className="bg-gray-800"
              variant="primary"
              disabled={!isFormValid}
              size="xl"
              onClick={handleOpenModal}
            >
              Sign up
            </Button>
          </div>
        </div>

        {openModal && (
          <CommonModal
            variant="signup"
            open
            title="Sign Up Complete"
            confirmText="Start"
            description="Welcome! Ready to start Chatting?"
            onCancel={() => setOpenModal(false)}
            onConfirm={handleConfirmModal}
          />
        )}
      </div>
    </div>
  )
}
