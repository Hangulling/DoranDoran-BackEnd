import { http, HttpResponse } from 'msw'
import { userProfile } from '../db/user'

const BODY = '/api/users'

export const userHandlers = [
  //내 프로필 조회
  http.get(`${BODY}/me`, () => {
    return HttpResponse.json({
      id: userProfile.id,
      email: userProfile.email,
      name: userProfile.name,
      firstName: userProfile.firstName,
      lastName: userProfile.lastName,
      createdAt: userProfile.createdAt.toISOString(),
      updatedAt: userProfile.updatedAt.toISOString(),
    })
  }),

  // 이름 및 설정 수정 (추가 기능)
  http.put(`${BODY}/me`, async ({ request }) => {
    const { name } = (await request.json()) as {
      name: string
    }

    // 사용자 프로필 데이터 업데이트
    userProfile.name = name
    userProfile.updatedAt = new Date()

    return HttpResponse.json({ message: '업데이트 완료' })
  }),

  // 이메일 변경 (추가 기능)
  http.patch(`${BODY}/me/email`, async ({ request }) => {
    const { newEmail } = (await request.json()) as { newEmail: string }
    userProfile.email = newEmail
    userProfile.updatedAt = new Date()

    return HttpResponse.json({ message: '요청 완료(재인증 필요)' })
  }),

  //비밀번호 변경 (추가 기능)
  http.patch(`${BODY}/me/password`, async ({ request }) => {
    const { currentPassword, newPassword } = (await request.json()) as {
      currentPassword: string
      newPassword: string
    }

    // 현재 비밀번호 유효성 검사
    if (currentPassword !== 'qwer1234') {
      return HttpResponse.json({ message: 'PASSWORD_INVALID' }, { status: 400 })
    }

    // 새 비밀번호로 변경
    console.log(`비밀번호가 ${newPassword}(으)로 변경되었습니다.`)
    userProfile.updatedAt = new Date()

    return HttpResponse.json({ message: '변경 완료' })
  }),
]
