export const validateName = (name: string): string | null => {
  const v = name.trim()
  if (v.length > 15) return 'You can enter up to 15 characters.'
  return null
}

export const EMAIL_REGEX_ASCII = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+$/
export const EMAIL_REGEX_UNICODE =
  /^[\p{L}\p{N}\p{M}\p{Pc}%+~](?:\.?[\p{L}\p{N}\p{M}\p{Pc}%+~])*@(?:[\p{L}\p{N}](?:[\p{L}\p{N}\p{M}\p{Pc}\p{Pd}]{0,61}[\p{L}\p{N}])?\.)+[\p{L}\p{N}]{2,63}$/u

export const validateEmail = (email: string): string | null => {
  const v = email.trim()
  if (v.length === 0) return null
  if (!(EMAIL_REGEX_ASCII.test(v) || EMAIL_REGEX_UNICODE.test(v))) {
    return 'Email error'
  }
  return null
}

export const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*\d)[A-Za-z\d]{8,20}$/

export const validatePassword = (password: string, confirm: string): string | null => {
  const v = password.trim()

  if (v.length === 0) return null
  if (v.length < 8) return 'Must be at least 8 characters.'
  if (v.length > 20) return 'Must be 20 characters or fewer.'

  if (!PASSWORD_REGEX.test(v)) {
    return 'Please check your password format.'
  }

  if (confirm.length > 0 && v !== confirm) {
    return 'Passwords do not match.'
  }

  return null
}
