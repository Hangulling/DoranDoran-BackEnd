import { useEffect, useState } from 'react'
import axios from 'axios'
import Button from '../components/common/Button'
import Input from '../components/common/Input'
import Agreement from '../components/common/Agreement'
import { PASSWORD_REGEX, validateEmail, validateName } from '../utils/validations'
import CommonModal from '../components/common/CommonModal'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAgreementStore } from '../stores/useAgreementStore'
import { useSignupFormStore } from '../stores/useSignupStore'
import { checkEmailExists } from '../api/auth'
import { createUser } from '../api/user'

export default function SignupPage() {
  const {
    firstName,
    lastName,
    email,
    password,
    passwordCheck,
    setMany,
    reset: resetForm,
  } = useSignupFormStore()

  const [firstNameError, setFirstNameError] = useState<string | null>(null)
  const [lastNameError, setLastNameError] = useState<string | null>(null)
  const [emailError, setEmailError] = useState<string | null>(null)
  const [emailSuccess, setEmailSuccess] = useState<string | null>(null)
  const [emailVerified, setEmailVerified] = useState(false)
  const [pwdTouched, setPwdTouched] = useState(false)
  const [pwdCheckTouched, setPwdCheckTouched] = useState(false)
  const [openModal, setOpenModal] = useState(false)
  const [, setSubmitError] = useState<string | null>(null)
  const [verifyLoading, setVerifyLoading] = useState(false)
  const [, setPwdError] = useState<string | null>(null)
  const [, setPwdCheckError] = useState<string | null>(null)

  const navigate = useNavigate()
  const location = useLocation() as { state?: { fromPolicy?: boolean } }

  const agreements = useAgreementStore(s => s.value)
  const setManyAgreements = useAgreementStore(s => s.setMany)
  const resetAgreements = useAgreementStore(s => s.reset)

  const handleFirstNameChange = (v: string) => {
    const noSpace = v.replace(/\s+/g, '')
    if (noSpace.length > 15) {
      setFirstNameError('You can enter up to 15 characters.')
      return
    }
    setMany({ firstName: noSpace })
    if (firstNameError && validateName(noSpace) === null) setFirstNameError(null)
  }

  const handleLastNameChange = (v: string) => {
    const noSpace = v.replace(/\s+/g, '')
    if (noSpace.length > 15) {
      setLastNameError('You can enter up to 15 characters.')
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
    setEmailVerified(false)
    const err = validateEmail(noSpace)
    setEmailError(err)
    if (err) {
      setEmailSuccess(null)
      setEmailVerified(false)
    }
  }

  const handleEmailBlur = () => {
    const err = validateEmail(email)
    setEmailError(err)
    if (err) {
      setEmailSuccess(null)
      setEmailVerified(false)
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
      setEmailVerified(false)
      return
    }

    try {
      setVerifyLoading(true)
      setEmailError(null)
      setEmailSuccess(null)

      const isDuplicate = await checkEmailExists(email)
      if (isDuplicate) {
        setEmailError('This email is already registered.')
        setEmailVerified(false)
      } else {
        setEmailSuccess('Verified successfully.')
        setEmailVerified(true)
      }
    } catch (e: unknown) {
      const msg = axios.isAxiosError(e)
        ? e.response?.data?.message || 'Email verification failed.'
        : 'Email verification failed.'
      setEmailError(msg)
      setEmailVerified(false)
    } finally {
      setVerifyLoading(false)
    }
  }

  const isEmailFormatValid = email.trim() !== '' && validateEmail(email) === null

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
        setEmailVerified(false)
        return
      }
      const dup = await checkEmailExists(email)
      if (dup) {
        setEmailError('This email is already registered.')
        setEmailVerified(false)
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

      resetForm()
      resetAgreements()
      navigate('/login', { replace: true })
    } catch (e: unknown) {
      const msg = axios.isAxiosError(e)
        ? e.response?.data?.message || 'Sign up failed.'
        : 'Sign up failed.'
      setSubmitError(msg)
    }
  }

  const handleOpenModal = () => {
    if (!isFormValid) return
    setOpenModal(true)
  }

  useEffect(() => {
    if (!location.state?.fromPolicy) {
      resetForm()
      resetAgreements()
    }
    navigate('.', { replace: true, state: null })
  }, [])

  return (
    <div className="mt-4 max-w-md">
      <div className="flex flex-col justify-center items-center">
        <div>
          <Input
            type="text"
            label="First name *"
            variant={firstNameError ? 'error' : 'primary'}
            placeholder="Enter 1-15 characters"
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
            placeholder="Enter 1-15 characters"
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
              onKeyDown={e => {
                if (e.key === ' ') e.preventDefault()
              }}
              variant={emailError ? 'error' : 'primary'}
            />
            <Button
              variant="primary"
              disabled={!isEmailFormatValid || verifyLoading}
              className="bg-gray-800 my-2 text-subtitle"
              size="sm"
              onClick={handleVerify}
            >
              {verifyLoading ? 'Checking…' : 'Verify'}
            </Button>
          </div>
          {emailError ? (
            <span className="text-xs text-orange-300">{emailError}</span>
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
