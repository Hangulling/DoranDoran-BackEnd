export function getDaysDiff(dateString: string): number {
  const lastDate = new Date(dateString)
  const now = new Date()
  const diffTime = now.getTime() - lastDate.getTime()
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  return diffDays
}
