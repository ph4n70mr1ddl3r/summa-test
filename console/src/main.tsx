import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import Home from './pages/Home.tsx'
import DnaConsole from './pages/DnaConsole.tsx'
import DNACards from './pages/DNACards.tsx'
import DNARules from './pages/DNARules.tsx'
import DNADecisions from './pages/DNADecisions.tsx'
import OrgView from './pages/OrgView.tsx'
import AskInbox from './pages/AskInbox.tsx'
import Spawning from './pages/Spawning.tsx'
import Runs from './pages/Runs.tsx'
import Governance from './pages/Governance.tsx'
import NotFound from './pages/NotFound.tsx'
import Login from './pages/Login.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App />}>
          <Route index element={<Home />} />
          <Route path="dna" element={<DnaConsole />} />
          <Route path="dna/cards" element={<DNACards />} />
          <Route path="dna/rules" element={<DNARules />} />
          <Route path="dna/decisions" element={<DNADecisions />} />
          <Route path="org" element={<OrgView />} />
          <Route path="asks" element={<AskInbox />} />
          <Route path="spawn" element={<Spawning />} />
          <Route path="runs" element={<Runs />} />
          <Route path="governance" element={<Governance />} />
          <Route path="login" element={<Login />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)
