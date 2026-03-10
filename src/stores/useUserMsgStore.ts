import { create } from 'zustand'

// 북마크 위한 사용자가 보낸 msgId, 내용 배열
interface UserMsg {
  id: string
  content: string
}

interface UserMsgState {
  userMsgList: UserMsg[]
  addUserMsg: (msg: UserMsg) => void
  clearUserMsgs: () => void
}

export const useUserMsgStore = create<UserMsgState>(set => ({
  userMsgList: [],
  addUserMsg: msg =>
    set(state => ({
      userMsgList: [...state.userMsgList, msg],
    })),
  clearUserMsgs: () =>
    set(() => ({
      userMsgList: [],
    })),
}))
