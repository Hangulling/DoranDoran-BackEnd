import { http, HttpResponse } from 'msw'
import { userProfile } from '../db/user'
import type { SignupRequest } from '../../types/auth'

export const loginHandlers = [
  http.post('http://localhost:8080/api/auth/login', async ({ request }) => {
    const { email, password } = (await request.json()) as { email: string; password: string }

    if (email !== userProfile.email || password !== userProfile.password) {
      return HttpResponse.json(
        { success: false, message: '이메일 또는 비밀번호가 올바르지 않습니다.', data: null },
        { status: 401 }
      )
    }

    const accessToken = `mock.${btoa(email)}.${Date.now()}`
    const refreshToken = `mock.refresh.${Date.now()}`

    return HttpResponse.json(
      {
        success: true,
        message: '로그인 성공(Mock)',
        data: {
          accessToken,
          refreshToken,
          user: {
            email: userProfile.email,
            firstName: userProfile.firstName,
            lastName: userProfile.lastName,
            name: userProfile.name,
          },
        },
      },
      { status: 200 }
    )
  }),
]

export const signupHandlers = [
  http.post('http://localhost:8080/api/users', async ({ request }) => {
    const { firstName, lastName, email, password } = (await request.json()) as SignupRequest

    if (!firstName || !lastName || !email || !password) {
      return HttpResponse.json(
        { success: false, message: '모든 필드를 입력해주세요.', data: null },
        { status: 400 }
      )
    }

    return HttpResponse.json(
      {
        success: true,
        message: '회원가입 성공(Mock)',
        data: {
          id: 'mock-user-id',
          email,
          firstName,
          lastName,
          name: `${firstName} ${lastName}`,
        },
      },
      { status: 201 }
    )
  }),
]

export const emailUserHandlers = [
  http.get('http://localhost:8080/api/users/email/:email', ({ params }) => {
    const { email } = params
    if (email === 'taken@example.com') {
      return HttpResponse.json({ data: { id: 'u1', email } }, { status: 200 })
    }
    return HttpResponse.json({ message: 'Not found' }, { status: 404 })
  }),
]

export const authHandlers = [...loginHandlers, ...signupHandlers, ...emailUserHandlers]
