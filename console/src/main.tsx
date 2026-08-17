import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import Home from './pages/Home.tsx'
import DnaConsole from './pages/DnaConsole.tsx'
import OrgView from './pages/OrgView.tsx'
import AskInbox from './pages/AskInbox.tsx'
import Governance from './pages/Governance.tsx'
import NotFound from './pages/NotFound.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App />}>
          <Route index element={<Home />} />
          <Route path="dna" element={<DnaConsole />} />
          <Route path="org" element={<OrgView />} />
          <Route path="asks" element={<AskInbox />} />
          <Route path="governance" element={<Governance />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)
