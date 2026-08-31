import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import Home from './pages/Home.tsx'
import DNAConsole from './pages/DnaConsole.tsx'
import DNACards from './pages/DNACards.tsx'
import DNARules from './pages/DNARules.tsx'
import DNADecisions from './pages/DNADecisions.tsx'
import DNAGoals from './pages/DNAGoals.tsx'
import OrgView from './pages/OrgView.tsx'
import AskInbox from './pages/AskInbox.tsx'
import Spawning from './pages/Spawning.tsx'
import Runs from './pages/Runs.tsx'
import Governance from './pages/Governance.tsx'
import BoardTasks from './pages/BoardTasks.tsx'
import Triggers from './pages/Triggers.tsx'
import Workspaces from './pages/Workspaces.tsx'
import Nodes from './pages/Nodes.tsx'
import RoleTemplates from './pages/RoleTemplates.tsx'
import Memory from './pages/Memory.tsx'
import Groups from './pages/Groups.tsx'
import NotFound from './pages/NotFound.tsx'
import Login from './pages/Login.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<App />}>
          <Route index element={<Home />} />
          <Route path="dna" element={<DNAConsole />} />
          <Route path="dna/cards" element={<DNACards />} />
          <Route path="dna/rules" element={<DNARules />} />
          <Route path="dna/decisions" element={<DNADecisions />} />
          <Route path="dna/goals" element={<DNAGoals />} />
          <Route path="org" element={<OrgView />} />
          <Route path="groups" element={<Groups />} />
          <Route path="asks" element={<AskInbox />} />
          <Route path="board-tasks" element={<BoardTasks />} />
          <Route path="triggers" element={<Triggers />} />
          <Route path="workspaces" element={<Workspaces />} />
          <Route path="spawn" element={<Spawning />} />
          <Route path="runs" element={<Runs />} />
          <Route path="governance" element={<Governance />} />
          <Route path="nodes" element={<Nodes />} />
          <Route path="role-templates" element={<RoleTemplates />} />
          <Route path="memory" element={<Memory />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)
