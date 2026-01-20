// 채팅 응답 시간 측정 스크립트 (최종 수정 버전)
(function() {
  let startTime = null;
  
  console.log('🔧 스크립트 초기화 중...');
  
  const originalLog = console.log;
  
  // 1. XMLHttpRequest 가로채기 (axios가 내부적으로 사용)
  const originalXHROpen = XMLHttpRequest.prototype.open;
  
  XMLHttpRequest.prototype.open = function(method, url, ...rest) {
    this._method = method;
    this._url = url;
    
    if (typeof url === 'string' && 
        url.includes('/api/chat/chatrooms/') && 
        url.includes('/messages') && 
        method.toUpperCase() === 'POST') {
      startTime = performance.now();
      originalLog('📤 [측정 시작] 메시지 전송 요청:', url, new Date().toISOString());
    }
    
    return originalXHROpen.apply(this, [method, url, ...rest]);
  };
  
  // 2. fetch도 가로채기 (혹시 사용되는 경우)
  const originalFetch = window.fetch;
  window.fetch = function(...args) {
    const url = args[0];
    const options = args[1] || {};
    
    if (typeof url === 'string' && 
        url.includes('/api/chat/chatrooms/') && 
        url.includes('/messages') && 
        (options.method === 'POST' || (!options.method && typeof url === 'string'))) {
      startTime = performance.now();
      originalLog('📤 [측정 시작] Fetch 요청:', url, new Date().toISOString());
    }
    
    return originalFetch.apply(this, args);
  };
  
  // 3. console.log에서 SSE 이벤트 감지
  console.log = function(...args) {
    const message = args.join(' ');
    
    // conversation_complete 이벤트 감지
    if (message.includes('[SSE Event: conversation_complete]')) {
      if (startTime !== null) {
        const endTime = performance.now();
        const duration = endTime - startTime;
        const durationSeconds = (duration / 1000).toFixed(2);
        
        originalLog('✅ [측정 완료] 응답 수신:', new Date().toISOString());
        originalLog('⏱️ [총 소요 시간]', durationSeconds, '초 (', duration.toFixed(0), 'ms)');
        originalLog('📊 [상세 정보]', {
          시작: new Date(startTime + performance.timeOrigin).toISOString(),
          종료: new Date(endTime + performance.timeOrigin).toISOString(),
          경과시간: durationSeconds + '초'
        });
        
        startTime = null;
      }
    }
    
    originalLog.apply(console, args);
  };
  
  // 4. Performance Observer로 네트워크 요청 추적 (백업 방법)
  if ('PerformanceObserver' in window) {
    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.name && 
              entry.name.includes('/api/chat/chatrooms/') && 
              entry.name.includes('/messages')) {
            const resourceType = entry.initiatorType || '';
            if ((resourceType === 'xmlhttprequest' || resourceType === 'fetch') && startTime === null) {
              startTime = entry.startTime;
              originalLog('📤 [측정 시작] Performance API 감지:', entry.name, new Date().toISOString());
            }
          }
        }
      });
      
      observer.observe({ entryTypes: ['resource'] });
      originalLog('✅ Performance Observer 활성화됨');
    } catch (e) {
      console.error('Performance Observer 오류:', e);
    }
  }
  
  originalLog('✅ 채팅 응답 시간 측정 스크립트가 활성화되었습니다.');
  originalLog('💡 이제 채팅을 전송하면 자동으로 시간이 측정됩니다.');
  originalLog('📌 감지 방법: XHR, Fetch, Performance API, Console 로그');
})();










