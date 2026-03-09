/**
 * 이메일 인증 완료 딥링크 URL 파싱
 * 백엔드 리디렉트: dorandoran://email-verified?email=...&verified=true|false&firstName=...&lastName=...&error=...
 */
export interface EmailVerifiedPayload {
  email: string
  verified: boolean
  firstName?: string
  lastName?: string
  error?: string
}

export function parseEmailVerifiedUrl(url: string): EmailVerifiedPayload | null {
  try {
    if (!url.includes('email-verified')) return null
    const parsed = new URL(url)
    const email = parsed.searchParams.get('email')
    if (!email) return null
    const verified = parsed.searchParams.get('verified') === 'true'
    return {
      email: decodeURIComponent(email),
      verified,
      firstName: parsed.searchParams.get('firstName')?.length
        ? decodeURIComponent(parsed.searchParams.get('firstName')!)
        : undefined,
      lastName: parsed.searchParams.get('lastName')?.length
        ? decodeURIComponent(parsed.searchParams.get('lastName')!)
        : undefined,
      error: parsed.searchParams.get('error')?.length
        ? decodeURIComponent(parsed.searchParams.get('error')!)
        : undefined,
    }
  } catch {
    return null
  }
}
