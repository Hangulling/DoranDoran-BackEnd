import type { BookmarkResponse } from '../../types/archive'

export const fakeArchiveItems: BookmarkResponse[] = [
  // Senior 채팅방
  // {
  //   id: '1',
  //   chatRoom: 'Senior',
  //   text: '오늘 회의는 3시에 시작합니다.',
  //   intimacy: 3,
  //   createdAt: '2025-09-25',
  // },
  // {
  //   id: '2',
  //   chatRoom: 'Senior',
  //   text: '내일까지 자료를 준비해 주세요.',
  //   intimacy: 2,
  //   createdAt: '2025-09-26',
  // },

  // Honey 채팅방
  {
    id: '3',
    messageId: '',
    chatroomId: 'Honey',
    botType: 'honey',
    content: '오늘 저녁은 내가 살게!',
    aiResponse: {
      intimacyLevel: 'Friendly',
    },
    createdAt: '2025-09-27',
  },
  {
    id: '4',
    messageId: '',
    chatroomId: 'Honey',
    botType: 'honey',
    content: '주말에 영화 보러 갈래?',
    aiResponse: {
      intimacyLevel: 'Casual',
    },
    createdAt: '2025-09-28',
  },
  {
    id: '9',
    messageId: '',
    chatroomId: 'Honey',
    botType: 'honey',
    content: '한강에서 치맥 하자',
    aiResponse: {
      intimacyLevel: 'Polite',
    },
    createdAt: '2025-09-27',
  },
  {
    id: '10',
    messageId: '',
    chatroomId: 'Honey',
    botType: 'honey',
    content: '우리 분좋카 가서 달달한거 먹자!',
    aiResponse: {
      intimacyLevel: 'Casual',
    },
    createdAt: '2025-09-28',
  },

  // Coworker 채팅방
  {
    id: '5',
    messageId: '',
    chatroomId: 'Coworker',
    content: '이번 프로젝트 마감일은 다음 주 금요일입니다.',
    botType: 'coworker',
    aiResponse: {
      intimacyLevel: 'Polite',
    },
    createdAt: '2025-09-29',
  },
  {
    id: '6',
    messageId: '',
    chatroomId: 'Coworker',
    content: 'API 문서 업데이트 완료했습니다.',
    botType: 'coworker',
    aiResponse: {
      intimacyLevel: 'Friendly',
    },
    createdAt: '2025-09-29',
  },

  // Senior 채팅방
  {
    id: '7',
    messageId: '',
    chatroomId: 'Senior',
    content: '견적서 전달드렸습니다. 확인 부탁드립니다.',
    botType: 'senior',
    aiResponse: {
      intimacyLevel: 'Close',
    },
    createdAt: '2025-09-28',
  },
  {
    id: '8',
    chatroomId: 'Senior',
    messageId: '',
    botType: 'senior',
    content: '계약 진행 관련 미팅 일정을 잡고 싶습니다.',
    aiResponse: {
      intimacyLevel: 'Close',
    },
    createdAt: '2025-09-30',
  },
]
