import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import App from './App.tsx'
import './styles/index.css'
import React from 'react'
import ReactGA from 'react-ga4'
import MaintenancePage from './pages/MaintenancePage.tsx'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { GoogleOAuthProvider } from '@react-oauth/google'
import { GOOGLE_CLIENT_ID } from './constants/env'
import NoticePage from './pages/NoticePage.tsx'

const IS_MAINTENANCE_MODE = import.meta.env.VITE_MAINTENANCE_MODE === 'true'
const IS_NOTICE_MODE = import.meta.env.VITE_NOTICE_MODE === 'true'
const GA_TRACKING_ID = import.meta.env.VITE_GA_TRACKING_ID
const GA_ENABLED = import.meta.env.VITE_GA_ENABLED === 'true'

const urlParams = new URLSearchParams(window.location.search)
const paramInternal = urlParams.get('internal') === 'true'

if (paramInternal) {
  sessionStorage.setItem('isInternalTraffic', 'true')
}

// 페이지뷰 자동 수집 제거
if (import.meta.env.PROD && GA_TRACKING_ID && GA_ENABLED) {
  const isInternal = sessionStorage.getItem('isInternalTraffic') === 'true'

  const gaConfigOptions: { traffic_type?: string; send_page_view: boolean } = {
    send_page_view: false,
  }

  if (isInternal) {
    gaConfigOptions.traffic_type = 'internal'
  }

  ReactGA.initialize(GA_TRACKING_ID, {
    gtagOptions: gaConfigOptions,
  })

  console.log(
    isInternal
      ? '[GA] Production GA Initialized (Internal Traffic)'
      : '[GA] Production GA Initialized'
  )
} else {
  console.warn('[GA] GA not initialized (Disabled, Dev mode, or no Tracking ID)')
}

const isDev = import.meta.env.DEV
const USE_MSW = import.meta.env.VITE_USE_MSW === 'true' // 환경변수에 false 변경

const prepare = async () => {
  if (isDev && USE_MSW) {
    const { worker } = await import('./mocks/browser')
    await worker.start()
  }
}

const queryClient = new QueryClient()
console.log('✅ GOOGLE_CLIENT_ID from env:', GOOGLE_CLIENT_ID)

prepare().then(() => {
  const container = document.getElementById('root')!
  const root = ReactDOM.createRoot(container)
  root.render(
    <React.StrictMode>
      <BrowserRouter>
        {IS_MAINTENANCE_MODE ? (
          <Routes>
            <Route path="*" element={<NoticePage />} />
          </Routes>
        ) : IS_NOTICE_MODE ? (
          <Routes>
            <Route path="*" element={<MaintenancePage />} />
          </Routes>
        ) : (
          <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
            <QueryClientProvider client={queryClient}>
              <App />
            </QueryClientProvider>
          </GoogleOAuthProvider>
        )}
      </BrowserRouter>
    </React.StrictMode>
  )
})
