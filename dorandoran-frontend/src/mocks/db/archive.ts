import type { ExpressionItem } from '../../types/archive'

export const fakeArchiveItems: ExpressionItem[] = [
  // Senior 채팅방
  // {
  //   id: '1',
  //   chatRoom: 'Senior',
  //   text: '오늘 회의는 3시에 시작합니다.',
  //   intimacy: 3,
  //   savedAt: '2025-09-25',
  // },
  // {
  //   id: '2',
  //   chatRoom: 'Senior',
  //   text: '내일까지 자료를 준비해 주세요.',
  //   intimacy: 2,
  //   savedAt: '2025-09-26',
  // },

  // Honey 채팅방
  {
    id: '3',
    chatRoom: 'Honey',
    text: '오늘 저녁은 내가 살게!',
    intimacy: 1,
    savedAt: '2025-09-27',
  },
  {
    id: '4',
    chatRoom: 'Honey',
    text: '주말에 영화 보러 갈래?',
    intimacy: 2,
    savedAt: '2025-09-28',
  },
  {
    id: '9',
    chatRoom: 'Honey',
    text: '한강에서 치맥 하자',
    intimacy: 3,
    savedAt: '2025-09-27',
  },
  {
    id: '10',
    chatRoom: 'Honey',
    text: '우리 분좋카 가서 달달한거 먹자!',
    intimacy: 3,
    savedAt: '2025-09-28',
  },

  // Coworker 채팅방
  {
    id: '5',
    chatRoom: 'Coworker',
    text: '이번 프로젝트 마감일은 다음 주 금요일입니다.',
    intimacy: 2,
    savedAt: '2025-09-29',
  },
  {
    id: '6',
    chatRoom: 'Coworker',
    text: 'API 문서 업데이트 완료했습니다.',
    intimacy: 1,
    savedAt: '2025-09-29',
  },

  // Client 채팅방
  {
    id: '7',
    chatRoom: 'Client',
    text: '견적서 전달드렸습니다. 확인 부탁드립니다.',
    intimacy: 1,
    savedAt: '2025-09-28',
  },
  {
    id: '8',
    chatRoom: 'Client',
    text: '계약 진행 관련 미팅 일정을 잡고 싶습니다.',
    intimacy: 2,
    savedAt: '2025-09-30',
  },
]
