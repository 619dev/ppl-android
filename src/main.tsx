import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

// Capability-based Android styling covers Samsung Galaxy S/A/Z devices without
// brittle model-name checks (including cover displays and unfolded Fold screens).
if (/Android/i.test(navigator.userAgent)) {
  document.documentElement.dataset.platform = 'android'
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
