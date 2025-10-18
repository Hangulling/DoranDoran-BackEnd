# Chat AI 인벤토리

## Agents DB

| 이름 | 목적/역할 | 입력 | 출력 | 규칙 | 예외/실패 | 호출 타이밍 | 응답 처리 흐름 | 관련 엔드포인트 | 관련 엔티티/프롬프트 | 소스 코드 | 활성 | Retry/Backoff | 최소 보장 응답 | 메트릭(토큰/지연/실패율) | 프롬프트 버전 | 변경 사유 | 적용 일시 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| IntimacyAgent | 사용자 문장의 친밀도(1~3) 분석, 교정, 피드백 제공 | chatroomId(UUID), userMessage(String) | detectedLevel(Int), correctedSentence(String), feedback(String), corrections(String[]) | 친밀도 레벨 1~3, 프롬프트 템플릿 `{level}` 치환, JSON 포맷 기대 | JSON 파싱 실패 시 기본값 반환; OpenAI 오류 시 오류 로그 | 병렬(Phase 1) | SSE `intimacy_analysis` 즉시 전송 → 진척도 업데이트 | POST /api/chat/chatrooms/{chatroomId}/messages | PromptService.getAgentPrompts("intimacy"); IntimacyProgress | service/agent/IntimacyAgent.java | true | 고정 지수 백오프(예: 0.5s,1s,2s 최대 3회) | empty JSON 시 기본값(level=1) 반환 | tokenUsage(추정), latencyMs(측정), failureRate(로그 집계) | 1 | 초기 도입 | - |
| VocabularyAgent | 어려운 단어 최대 1개 추출 | userMessage(String), userLevel(Int) | words: [{word, difficulty(1~3), context}] | 반드시 1개만 추출(기본 프롬프트), `{userLevel}` 치환 | JSON 파싱 실패 시 빈 배열; OpenAI 오류 시 빈 응답 | 병렬(Phase 1) | SSE `vocabulary_extracted` 전송 | POST /api/chat/chatrooms/{chatroomId}/messages | PromptService.getDefaultPrompts("vocabulary") | service/agent/VocabularyAgent.java | true | 고정 지수 백오프(최대 3회) | 빈 결과 허용(대화 흐름 유지) | tokenUsage, latencyMs, failureRate | 1 | 초기 도입 | - |
| TranslationAgent | Vocabulary 결과 번역/발음 제공 | words (Vocabulary 결과) | translations: [{original, english, pronunciation}] | Vocabulary 결과 없으면 빈 응답; `{input}` 치환 | JSON 파싱 실패 시 빈 배열 | 순차(Phase 2) | SSE `vocabulary_translated` 전송 | POST /api/chat/chatrooms/{chatroomId}/messages | PromptService.getDefaultPrompts("translation") | service/agent/TranslationAgent.java | true | 고정 지수 백오프(최대 3회) | 빈 결과 허용(대화 흐름 유지) | tokenUsage, latencyMs, failureRate | 1 | 초기 도입 | - |
| ConversationAgent | 자연스러운 대화 응답 스트리밍 | chatroomId(UUID), userMessage(String) | 스트림 텍스트 청크 | PromptService.buildSystemPrompt 이용; [DONE] 처리 | OpenAI 스트림 오류 시 `conversation_error` | 스트림(Phase 3) | SSE `conversation_chunk` 다수 전송 → 완료 시 `conversation_complete` 및 메시지 저장 | POST /api/chat/chatrooms/{chatroomId}/messages | PromptService.buildSystemPrompt | service/agent/ConversationAgent.java | true | 네트워크 오류 시 스트림 재연결 시도(클라이언트)/ 서버 측 재시도 없음 | 스트림 실패 시 최소 1회 `conversation_error`로 고지 | tokenUsage(추정), latencyMs(청크 간격), failureRate | 1 | 초기 도입 | - |

## SSE Events DB

| 이벤트명 | 페이로드 스키마 | 발신 주체 | 트리거 타이밍 | 관련 채팅방/메시지 | 관련 에이전트 | 에러 처리 | 비고 | Retry/Backoff | 전송 성공률 | 지연(ms) |
|---|---|---|---|---|---|---|---|---|---|---|
| intimacy_analysis | { detectedLevel:Int, correctedSentence:String, feedback:String, corrections:String[] } | Orchestrator | Phase 1 완료 시 | chatroomId 기준 broadcast | IntimacyAgent | 실패 시 로그만, 재시도 없음 | IntimacyProgress 업데이트 수행 | 없음(서버) | 집계로 산출 | SSE 전송 시간 측정 |
| vocabulary_extracted | { words:[{word, difficulty, context}] } | Orchestrator | Phase 1 완료 시 | chatroomId 기준 | VocabularyAgent | 실패 시 로그만 | 최대 1개 단어 지향 | 없음(서버) | 집계로 산출 | 측정 |
| vocabulary_translated | { translations:[{original, english, pronunciation}] } | Orchestrator | Phase 2 완료 시 | chatroomId 기준 | TranslationAgent | 실패 시 로그만 | Vocabulary 결과 없으면 미발행 가능 | 없음(서버) | 집계로 산출 | 측정 |
| conversation_chunk | String (텍스트 청크) | Orchestrator | Phase 3 스트림 중 | chatroomId 기준 | ConversationAgent | 전송 실패 시 emitter 제거 | 빈 청크 필터링 후 전송 | 없음(서버) | 집계로 산출 | 청크 간격/전송 지연 측정 |
| conversation_complete | { messageId:UUID, content:String } | Orchestrator | Phase 3 수집 완료 | chatroomId 기준 | ConversationAgent | 저장 실패 시 `conversation_error` | 저장 성공 시 메시지 기록 | 없음(서버) | 집계로 산출 | 측정 |
| aggregated_complete | { intimacy:{detectedLevel, correctedSentence, feedback}, vocabulary:{words:Int} } | Orchestrator | Phase 4 집계 완료 | chatroomId 기준 | Orchestrator | 실패 시 `agent_error` | Intimacy/Vocab zip 결과 | 없음(서버) | 집계로 산출 | 측정 |
| conversation_error | { error:String } | Orchestrator | Conv 오류/저장 실패 | chatroomId 기준 | ConversationAgent | 사용자 알림 가능 | 오류 메시지 간단화 | 없음(서버) | 집계로 산출 | 측정 |
| agent_error | String (오류 메시지) | Orchestrator | 집계/에이전트 오류 | chatroomId 기준 | Orchestrator | 사용자 알림 가능 | 공통 오류 채널 | 없음(서버) | 집계로 산출 | 측정 |

## API Endpoints DB

| 메서드/경로 | 설명 | 요청 DTO | 요청 헤더 | 응답 DTO | 인증 | 호출 시나리오 | 관련 에이전트 | SSE 연계 | 에러 코드 | 소스 코드 |
|---|---|---|---|---|---|---|---|---|---|---|
| POST /api/chat/chatrooms | 채팅방 생성/조회 | ChatRoomCreateRequest | Bearer 토큰 | ChatRoomResponse | Bearer | 최초 방 생성 시 Greeting 전송 | - | - | 400 | controller/ChatController.java |
| GET /api/chat/chatrooms | 채팅방 목록 | - (query page,size,userId) | Bearer | Page<ChatRoomResponse> | Bearer | 리스트 조회 | - | - | 400, 401 | controller/ChatController.java |
| GET /api/chat/chatrooms/{chatroomId}/messages | 메시지 목록 | - (query page,size,userId) | Bearer | Page<MessageResponse> | Bearer | 대화 기록 조회 | - | - | 400,403 | controller/ChatController.java |
| POST /api/chat/chatrooms/{chatroomId}/messages | 메시지 전송(사용자) → Multi-Agent 실행 | MessageSendRequest | Bearer, X-User-Id(optional) | MessageResponse | Bearer | 사용자 메시지 저장 후 Orchestrator 구동 | 모든 Agent | intimacy_analysis, vocabulary_extracted, vocabulary_translated, conversation_chunk, conversation_complete, aggregated_complete | 400,403 | controller/ChatController.java |
| GET /api/chat/stream/{chatroomId} | SSE 구독 | - (query userId optional) | Bearer | SseEmitter | Bearer | 실시간 이벤트 수신 | - | 모든 이벤트 | 400,403 | controller/SSEController.java |
| POST /api/chat/chatbots/prompt | 챗봇 프롬프트 수정 | ChatbotUpdateRequest | Bearer | {success, message,...} | Bearer | 운영/툴링 | - | - | 400,404 | controller/ChatController.java |
| POST /api/chat/chatbots/reset | 챗봇 프롬프트 리셋 | query chatbotId,agentType | Bearer | {success,...} | Bearer | 운영/툴링 | - | - | 400,404 | controller/ChatController.java |
| GET /api/chat/chatbots/{chatbotId} | 챗봇 조회 | - | Bearer | {success, chatbot} | Bearer | 구성 조회 | - | - | 404 | controller/ChatController.java |
| GET /api/chat/chatbots/{chatbotId}/agents/{agentType} | Agent 프롬프트 조회 | - | Bearer | {success, prompts} | Bearer | 프롬프트 확인 | - | - | 404 | controller/ChatController.java |

## DTOs DB

| 이름 | 위치 | 필드·타입 | 사용처 | 검증 규칙 | 비고 |
|---|---|---|---|---|---|
| MessageSendRequest | service/dto/MessageSendRequest.java | senderType:String, content:String, contentType:String | POST /messages | senderType ∈ {user,bot,system}; content 1~10000; contentType ∈ {text,code,system} | API에서는 user만 허용 |
| MessageResponse | service/dto/MessageResponse.java | id(UUID), chatroomId(UUID), senderType, senderId(UUID), content, contentType, sequenceNumber(Long), isEdited(Boolean), isDeleted(Boolean), createdAt | 메시지 응답 | - | - |
| ChatRoomCreateRequest | service/dto/ChatRoomCreateRequest.java | userId(UUID), chatbotId(UUID), name(String), concept(Enum), intimacyLevel(Int 1~3) | POST /chatrooms | name 1~100; intimacyLevel 1~3 | concept 필수 |
| ChatRoomResponse | service/dto/ChatRoomResponse.java | id, userId, chatbotId, name, description, concept, intimacyLevel, lastMessageAt, lastMessageId, isArchived, isDeleted, createdAt, updatedAt | 방 조회 응답 | - | - |
| ChatbotUpdateRequest | service/dto/ChatbotUpdateRequest.java | chatbotId, agentType, prompts... | POST /chatbots/prompt | - | 파일 내 상세 참조 |
| IntimacyUpdateRequest | service/dto/IntimacyUpdateRequest.java | intimacyLevel(Int 1~3) | PATCH /chatrooms/{id}/intimacy | 1~3 | 파일 내 상세 참조 |

## Error Catalog DB

| 이름 | 소스 | 예외 타입 | 메시지/코드 | 대응 방안 | 사용자 노출 | 관련 엔드포인트 | 관련 이벤트 |
|---|---|---|---|---|---|---|---|
| SSE 권한 없음 | SSEController | 403 | FORBIDDEN | 접근 권한 확인 후 종료 | 가능 | GET /api/chat/stream/{id} | - |
| 잘못된 요청(유저 없음) | SSEController | 400 | BAD_REQUEST | userId 요구 | 가능 | GET /api/chat/stream/{id} | - |
| 메시지 전송 권한 없음 | ChatController | 403 | FORBIDDEN | 채팅방 권한 검증 | 가능 | POST /messages | - |
| Intimacy 파싱 실패 | IntimacyAgent | Exception | "분석 중 오류가 발생했습니다." | 기본값으로 대체 | 제한 | - | agent_error |
| Vocabulary 파싱 실패 | VocabularyAgent | Exception | 빈 배열 | 로깅 | 제한 | - | agent_error |
| Translation 파싱 실패 | TranslationAgent | Exception | 빈 배열 | 로깅 | 제한 | - | agent_error |
| Conversation 생성 오류 | Orchestrator | Exception | conversation_error | 오류 이벤트 전송 | 가능 | POST /messages | conversation_error |
| 집계(zip) 오류 | Orchestrator | Exception | agent_error | 오류 이벤트 전송 | 제한 | POST /messages | agent_error |

## Flow Steps DB

| 단계명 | 설명 | 트리거 | 입력 | 출력 | 다음 단계 | 관련 이벤트 | 관련 엔드포인트 |
|---|---|---|---|---|---|---|---|
| Phase 1 (병렬) | Intimacy, Vocabulary, Conversation 동시 시작 | 사용자 메시지 저장 후 Orchestrator | chatroomId, userId, content | 각 에이전트 중간 결과 | Phase 2,3 | intimacy_analysis, vocabulary_extracted, conversation_chunk | POST /messages |
| Phase 2 (순차) | Vocabulary → Translation | Vocabulary 결과 | words | translations | Phase 3 | vocabulary_translated | POST /messages |
| Phase 3 (스트림) | Conversation 스트리밍 및 저장 | 사용자 메시지 | 텍스트 청크 | 최종 메시지 | Phase 4 | conversation_chunk, conversation_complete, conversation_error | POST /messages |
| Phase 4 (집계) | Intimacy + Vocabulary 집계 | intimacyMono+vocabularyMono | aggregated_result | 완료 | - | aggregated_complete, agent_error | POST /messages |

## Chatbot Config DB 

| 이름 | bot_type | model_name | system_prompt | intimacy_system_prompt | intimacy_user_prompt | vocabulary_system_prompt | vocabulary_user_prompt | translation_system_prompt | translation_user_prompt | capabilities | settings | intimacy_level | 활성 | 소스 | 프롬프트 버전 | 변경 사유 | 적용 일시 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 기본 챗봇 | gpt | gpt-4o 등 | TEXT | TEXT | TEXT | TEXT | TEXT | TEXT | TEXT | JSON | JSON | 1~3 | true | entity/Chatbot.java | 1 | 초기 도입 | - |

## Intimacy Progress DB

| chatroom_id | user_id | intimacy_level | total_corrections | last_feedback | progress_data | last_updated |
|---|---|---|---|---|---|---|
| UUID | UUID | 1~3 | Number | String | JSON | Timestamp |


