import { useState } from 'react'
import axios from 'axios'
import loginCharacter from '../assets/auth/loginCharacter.svg'
import doran from '../assets/auth/doranText.svg'
import Button from '../components/common/Button'
import Input from '../components/common/Input'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleLogin = async () => {
    if (loading) return
    setError(null)

    if (!email && !password) {
      setError('both')
      return
    }
    if (!email) {
      setError('email')
      return
    }
    if (!password) {
      setError('password')
      return
    }

    setLoading(true)
    try {
      const res = await login({ email, password })
      // res = { success, message, data: {...} }
      if (!res?.success) {
        setError(res?.message || '로그인 실패')
        return
      }

      const accessToken = res?.data?.accessToken as string | undefined
      const user = res?.data?.user

      console.log('🎉 로그인 성공!', { user, accessToken })

      // if (accessToken) {
      //   localStorage.setItem('accessToken', accessToken)
      // }

      navigate('/')
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || '네트워크 오류가 발생했습니다.')
        console.error('🚨 로그인 에러:', err.response?.data || err)
      } else {
        setError('알 수 없는 오류가 발생했습니다.')
        console.error('🚨 로그인 에러(unknown):', err)
      }
    } finally {
      setLoading(false)
    }
  }

  const onKeyDown: React.KeyboardEventHandler<HTMLInputElement> = e => {
    if (e.key === 'Enter') handleLogin()
  }

  return (
    <div className="flex justify-center items-center h-screen overflow-y-hidden">
      <div className="w-full bg-white rounded-lg">
        <div className="flex flex-col justify-center items-center mb-12">
          <img src={doran} alt="DoranDoran" className="mb-4" />
          <span className="text-gray-700 text-sm text-subtitle">
            Chat your way to real-life Korean
          </span>
        </div>
        <div className="flex justify-center">
          <img src={loginCharacter} alt="login character" />
        </div>

        <div className="flex justify-center items-center">
          <div className="mt-4">
            <Input
              type="email"
              variant={error === 'email' || error === 'both' ? 'error' : 'primary'}
              placeholder="E-mail"
              value={email}
              onChange={e => setEmail(e.target.value)}
              onKeyDown={onKeyDown}
            />
            <Input
              type="password"
              variant={error === 'password' || error === 'both' ? 'error' : 'primary'}
              placeholder="Password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              onKeyDown={onKeyDown}
            />

            {error && (
              <span className="mt-1 block text-xs text-orange-500 text-body">
                {error === 'email' && 'Email error'}
                {error === 'password' && 'Password error'}
                {error === 'both' && 'Email error + Password error'}
              </span>
            )}

            <Button
              variant="primary"
              size="xl"
              className="bg-gray-800 my-4 w-full text-subtitle"
              onClick={handleLogin}
              // disabled={loading}
            >
              Login
              {/* {loading ? 'Logging in…' : 'Login'} */}
            </Button>
          </div>
        </div>

        <div className="flex justify-center items-center gap-2 my-4 text-sm">
          <span className="text-gray-700 text-body">Don't have an account yet?</span>
          <Link to="/signup" className="underline underline-offset-4 text-title text-gray-800">
            Sign up
          </Link>
        </div>
      </div>
    </div>
  )
}
