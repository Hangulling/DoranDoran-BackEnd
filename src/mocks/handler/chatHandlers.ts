import { http, HttpResponse } from 'msw'
import { chatRooms as initialChatRooms } from '../db/chat'

interface DetailedChatRoom {
  roomId: number
  roomName: string
  category: string
  botId: number
  intimacy: number
}

let detailedChatRooms: DetailedChatRoom[] = initialChatRooms.map((room, index) => ({
  roomId: room.roomRouteId,
  roomName: room.roomName,
  lastMessage: room.message,
  category: ['SCHOOL', 'LOVER', 'COMPANY', 'CLIENT'][index % 4],
  botId: [1, 2, 3, 4][index % 4],
  intimacy: room.intimacy,
}))

const coachMarks: { [key: number]: boolean } = {
  1: false,
  2: false,
  3: false,
  4: false,
}

const BODY = '/api/chat'

export const chatHandlers = [
  // 채팅방 목록 조회
  http.get(`${BODY}/list`, ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1', 10)
    const limit = parseInt(url.searchParams.get('limit') || '10', 10)

    const startIndex = (page - 1) * limit
    const endIndex = startIndex + limit
    const paginatedRooms = detailedChatRooms.slice(startIndex, endIndex)

    if (paginatedRooms.length === 0) {
      return HttpResponse.json({ message: 'NO_DATA' }, { status: 404 })
    }

    const responseData = paginatedRooms.map(room => ({
      roomName: room.roomName,
      chattingRoom: room.roomId,
      category: room.category,
    }))

    return HttpResponse.json(responseData)
  }),

  // 채팅방 생성
  http.post(`${BODY}/create`, async ({ request }) => {
    const newRoomInfo = (await request.json()) as {
      roomName: string
      category: string
      botId: number
      intimacy: number
    }

    if (detailedChatRooms.some(room => room.roomName === newRoomInfo.roomName)) {
      return HttpResponse.json({ message: 'DUPLICATE_TITLE' }, { status: 409 })
    }

    const newId =
      detailedChatRooms.length > 0 ? Math.max(...detailedChatRooms.map(r => r.roomId)) + 1 : 1
    const newRoom: DetailedChatRoom = {
      roomId: newId,
      ...newRoomInfo,
    }

    detailedChatRooms.push(newRoom)
    coachMarks[newId] = true

    return HttpResponse.json(
      {
        roomId: newRoom.roomId,
        roomName: newRoom.roomName,
        category: newRoom.category,
        botId: newRoom.botId,
        intimacy: newRoom.intimacy,
      },
      { status: 201 }
    )
  }),

  // 채팅방 이름 수정
  http.patch(`${BODY}/lid/:id`, async ({ params, request }) => {
    const { id } = params
    const { roomName } = (await request.json()) as { roomName: string }
    const roomToUpdate = detailedChatRooms.find(room => room.roomId === Number(id))

    if (!roomToUpdate) {
      return HttpResponse.json({ message: 'NOT_FOUND' }, { status: 404 })
    }

    roomToUpdate.roomName = roomName

    return HttpResponse.json({ message: '이름 변경 완료' })
  }),

  // 채팅방 삭제
  http.delete(`${BODY}/lid/:id`, ({ params }) => {
    const { roomId } = params
    const initialLength = detailedChatRooms.length
    detailedChatRooms = detailedChatRooms.filter(room => room.roomId !== Number(roomId))

    if (initialLength === detailedChatRooms.length) {
      return HttpResponse.json({ message: 'ALREADY_DELETED' }, { status: 404 })
    }

    return HttpResponse.json({ message: '소프트 딜리트 완료' })
  }),

  // 코치마크 실행 여부 확인
  http.get(`${BODY}/lid/:id/mark`, ({ params }) => {
    const { id } = params
    const hasSeen = coachMarks[Number(id)] ?? true // default: true

    return HttpResponse.json({ coachMark: hasSeen })
  }),

  // 코치마크 실행 상태 변경
  http.post(`${BODY}/lid/:id/mark`, ({ params, request }) => {
    const { id } = params
    const url = new URL(request.url)
    const q = url.searchParams.get('q') === 'true'

    if (coachMarks[Number(id)] === undefined) {
      return HttpResponse.json({ message: 'NOT_FOUND' }, { status: 404 })
    }

    coachMarks[Number(id)] = q

    return HttpResponse.json({ message: '코치마크 실행 여부 변경' })
  }),

  // 메시지 전송
  // http.post('/api/chat/:chatroomId/messages', async ({ params, request }) => {
  //   const { chatroomId } = params
  //   const { content, metadata } = (await request.json()) as { content: string; metadata?: any }

  //   if (typeof chatroomId !== 'string' || !messagesByRoom[chatroomId]) {
  //     return HttpResponse.json({ message: 'Chat room not found' }, { status: 404 })
  //   }

  //   try {
  //     const roomMessages = messagesByRoom[chatroomId]
  //     const newMessage = {
  //       id: roomMessages.length + 1,
  //       content,
  //       isSender: true, // 사용자가 보낸 메시지
  //       createdAt: new Date(),
  //       metadata,
  //     }
  //     roomMessages.push(newMessage)

  //     // AI 교정 응답 시뮬레이션
  //     const aiResponse = {
  //       id: roomMessages.length + 1,
  //       content: `"${content}"에 대한 AI 교정 응답입니다.`,
  //       isSender: false, // AI가 보낸 메시지
  //       createdAt: new Date(),
  //     }
  //     roomMessages.push(aiResponse)

  //     return HttpResponse.json({
  //       savedMessage: newMessage,
  //       aiResponse: aiResponse,
  //     })
  //   } catch (error) {
  //     return HttpResponse.json({ message: 'SERVER_SAVE_FAILED' }, { status: 500 })
  //   }
  // }),

  // // AI 응답 스트림
  // http.get('/api/chat/:chatroomId/stream', async ({ params }) => {
  //   const { chatroomId } = params
  //   if (typeof chatroomId !== 'string') {
  //     return new HttpResponse(null, { status: 400 })
  //   }

  //   const stream = new ReadableStream({
  //     async start(controller) {
  //       const encoder = new TextEncoder()
  //       const send = (chunk: object) => {
  //         controller.enqueue(encoder.encode(`data: ${JSON.stringify(chunk)}\n\n`))
  //       }

  //       const aiMessage = 'AI가 코칭 중입니다...'.split(' ')

  //       for (const chunk of aiMessage) {
  //         await delay(500)
  //         send({ chunk, usage: 10 })
  //       }

  //       await delay(500)
  //       send({ done: true })
  //       controller.close()
  //     },
  //   })

  //   return new HttpResponse(stream, {
  //     headers: {
  //       'Content-Type': 'text/event-stream',
  //     },
  //   })
  // }),

  // // 메시지 히스토리 조회

  // http.get('/api/chat/:chatroomId/messages', ({ params, request }) => {
  //   const { chatroomId } = params
  //   const url = new URL(request.url)
  //   const page = parseInt(url.searchParams.get('page') || '1', 10)
  //   const limit = parseInt(url.searchParams.get('limit') || '10', 10)

  //   if (typeof chatroomId !== 'string' || !messagesByRoom[chatroomId]) {
  //     return HttpResponse.json({ message: 'Chat room not found' }, { status: 404 })
  //   }

  //   const roomMessages = messagesByRoom[chatroomId]
  //   // 최신 메시지가 위로 오도록 정렬 (실제로는 DB에서 정렬)
  //   const sortedMessages = [...roomMessages].sort(
  //     (a, b) => b.createdAt.getTime() - a.createdAt.getTime()
  //   )

  //   const startIndex = (page - 1) * limit
  //   const endIndex = startIndex + limit
  //   const paginatedMessages = sortedMessages.slice(startIndex, endIndex)

  //   return HttpResponse.json({
  //     messages: paginatedMessages,
  //     totalPages: Math.ceil(sortedMessages.length / limit),
  //     currentPage: page,
  //   })
  // }),

  // // 메시지 검색
  // http.get('/api/chat/:chatroomId/search', ({ params, request }) => {
  //   const { chatroomId } = params
  //   const url = new URL(request.url)
  //   const query = url.searchParams.get('q') || ''
  //   const date = url.searchParams.get('date')

  //   if (typeof chatroomId !== 'string' || !messagesByRoom[chatroomId]) {
  //     return HttpResponse.json({ message: 'Chat room not found' }, { status: 404 })
  //   }

  //   let searchResults = messagesByRoom[chatroomId].filter(msg =>
  //     msg.content.toLowerCase().includes(query.toLowerCase())
  //   )

  //   if (date) {
  //     searchResults = searchResults.filter(msg =>
  //       new Date(msg.createdAt).toISOString().startsWith(date)
  //     )
  //   }

  //   return HttpResponse.json(searchResults)
  // }),
]
