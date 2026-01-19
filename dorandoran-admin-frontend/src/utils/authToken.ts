export type DecodedJwt = { exp?: number; [k: string]: unknown }

export function decodeJwt(token: string): DecodedJwt | null {
  try {
    const [, payload] = token.split('.')
    if (!payload) return null
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json)
  } catch {
    return null
  }
}

export function isExpired(token: string, skewSec = 15): boolean {
  const d = decodeJwt(token)
  if (!d?.exp) return false
  const now = Math.floor(Date.now() / 1000)
  return d.exp <= now + skewSec
}

export function msUntilExpiry(token: string, skewSec = 15): number | null {
  const d = decodeJwt(token)
  if (!d?.exp) return null
  return Math.max(d.exp * 1000 - Date.now() - skewSec * 1000, 0)
}
