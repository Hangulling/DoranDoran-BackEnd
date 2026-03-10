// 첫번째 글자만 대문자로
export const capitalizeFirstLetter = (str: string) => {
  if (!str) return ''
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase()
}

// 이름의 첫글자를 대문자로
export const capitalizeName = (str: string) => {
  if (!str) return ''
  return str
    .toLowerCase()
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}
