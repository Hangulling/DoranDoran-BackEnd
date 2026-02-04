import { useMatch, useNavigate } from 'react-router-dom'

export function useNavBar(onToggleSidebar?: () => void) {
  const navigate = useNavigate()

  const chatMatch = useMatch('/chat/:id')
  const closenessMatch = useMatch('/closeness/:id')
  const archiveMatch = useMatch('/archive/:id')

  const isChatPage = Boolean(chatMatch)
  const currentId = chatMatch?.params.id ?? closenessMatch?.params.id ?? archiveMatch?.params.id

  // 뒤로가기
  const goBack = () => {
    navigate(-1)
  }

  // 북마크 바로가기
  const handleBookmarkClick = () => {
    if (!currentId) {
      navigate('/archive/1')
      return
    }
    if (chatMatch || closenessMatch) {
      navigate(`/archive/${currentId}`, { state: { from: 'chat' } })
    }
  }

  // GA 포함 핸들러
  const handleHamburgerClick = () => {
    if (onToggleSidebar) {
      onToggleSidebar()
    }
  }

  return {
    goBack,
    handleBookmarkClick,
    handleHamburgerClick,
    isChatPage,
  }
}
