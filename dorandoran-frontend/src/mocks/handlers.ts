import { http, passthrough } from 'msw'
import { chatHandlers } from './handler/chatHandlers'
import { userHandlers } from './handler/userHandlers'
import { authHandlers } from './handler/authHandlers'

// handler 추가
export const handlers = [
  // 서버의 루트(/) URL에 대한 GET 요청은 무시
  http.get('/', () => {
    return passthrough()
  }),
  ...chatHandlers,
  ...userHandlers,
  ...authHandlers,
]
