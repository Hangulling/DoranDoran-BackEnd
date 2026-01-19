/**
 * Admin 전용 로그인 페이지
 * 기존 LoginPage와 분리하여 관리
 */
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import { adminLogin } from '../../api/admin/auth'

export default function AdminLoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleLogin = async () => {
    setError('')
    setLoading(true)

    if (!email || !password) {
      setError('이메일과 비밀번호를 입력해주세요.')
      setLoading(false)
      return
    }

    try {
      await adminLogin({ email, password })
      // adminLogin에서 이미 role 체크를 수행하므로, 성공 시 admin 페이지로 이동
      navigate('/', { replace: true })
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const status = err.response?.status
        const data = (err.response?.data ?? {}) as {
          errorCode?: string
          message?: string
        }

        if (status === 401 || status === 403) {
          setError('관리자 권한이 필요합니다.')
        } else if (status === 404) {
          setError('이메일 또는 비밀번호가 올바르지 않습니다.')
        } else if (status && status >= 500) {
          setError('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
        } else {
          setError(data.message || '로그인에 실패했습니다.')
        }
      } else if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('로그인에 실패했습니다.')
      }
    } finally {
      setLoading(false)
    }
  }

  const onKeyDown: React.KeyboardEventHandler<HTMLInputElement> = e => {
    if (e.key === 'Enter' && !loading) {
      handleLogin()
    }
  }

  return (
    <div className="flex justify-center items-center min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-lg p-10">
        <div className="flex flex-col justify-center items-center mb-10">
          <h1 className="text-3xl font-bold text-gray-800 mb-3">Koach Admin</h1>
          <span className="text-gray-600 text-base">관리자 로그인</span>
        </div>

        <div className="space-y-6">
          <div>
            <Input
              type="email"
              variant={error && !email ? 'error' : 'primary'}
              placeholder="이메일을 입력하세요"
              value={email}
              onChange={e => setEmail(e.target.value)}
              onKeyDown={onKeyDown}
              size="lg"
            />
          </div>
          <div>
            <Input
              type="password"
              variant={error && !password ? 'error' : 'primary'}
              placeholder="비밀번호를 입력하세요"
              value={password}
              onChange={e => setPassword(e.target.value)}
              onKeyDown={onKeyDown}
              size="lg"
            />
          </div>

          {error && (
            <div className="mt-2 p-4 bg-red-50 border border-red-200 rounded-lg">
              <span className="text-base text-red-600">{error}</span>
            </div>
          )}

          <Button
            variant="primary"
            size="xl"
            className="bg-blue-600 hover:bg-blue-700 my-6 w-full py-4 text-base font-semibold rounded-lg shadow-sm"
            onClick={handleLogin}
            disabled={loading}
          >
            {loading ? '로그인 중...' : '로그인'}
          </Button>
        </div>
      </div>
    </div>
  )
}
