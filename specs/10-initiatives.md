# SPEC-10 — Initiatives

Source: PLAN.md §5.1.

## Structure

- **INT-001** — An initiative is the execution spine linking goal → work: title, optional
  `goal_ref` and `decision_ref`, sponsor (pinned human, refused at write if agent, viewer,
  or non-active), lead
  (any member but an ephemeral worker; viewer and non-active members refused at write),
  optional deadline, optional business budget (display-only until CFG-110), status
  `proposed|active|paused|closed`, optional `depends_on`.
- **INT-002** — DNA first: the directive lands as a decision record and (usually) a goal
  through the normal write path; the initiative references them.
- **INT-010** — Ephemeral-origin initiatives are refused at write: a bounded worker's
  directive-deserving output folds back to its spawner; a human or persistent agent opens
  the initiative.
- **INT-011** — A new initiative's `goal_ref` names a live goal at write — an initiative is
  never born pointed at history; the only way it comes to address a terminal row is the goal
  dying under it, which is the case the direction ask exists for.

## Transitions and authority

- **INT-020** — `proposed` → `active` is the sponsor's acceptance: an initiative opened by
  anyone other than its sponsor routes an activation ask (kind `approval`, tier `standard`)
  to the sponsor with expiry `deny` —
  a directive that never won its authority never executes; the denied initiative stays
  `proposed`, inert (no bindings, no runs, no escalations) until its sponsor or lead closes
  it; org state is never silently evaporated. A sponsor's own opens active.
- **INT-021** — Activation re-validates its respond-time assumptions (ASK-041): an accept
  against a goal that died mid-wait is audit-only with a re-point successor ask; an accept
  landing after a close is audit-only — terminal beats activation.
- **INT-022** — Pause and resume belong to the lead or the sponsor; an admin holds both as
  emergency backstop. Close belongs to either (and to a lead or sponsor shutting down a
  `proposed` directive that never won activation) and always runs the CLC-020 dependency
  check; close unbinds workspaces with the goal slice re-deriving at once.
- **INT-023** — `closed` is terminal — initiatives never reopen; revival opens a new
  initiative referencing the old one's decision (`decision_ref`), so burndown and history
  survive as themselves.

## Pause semantics

- **INT-030** — Pause is explicit and total: it suspends the stalled-work escalation and
  freezes the board slice (no new runs or spawns launch under it), while filing and editing
  board tasks stays open as planning — pause stops execution, not deliberation.
- **INT-031** — The freeze is a launch gate, not a mid-run kill: runs already in flight
  complete onto the paused slice; stopping work mid-flight stays suspend/retire's job
  (CLC-020).
- **INT-032** — Pause retains workspace bindings frozen: linked goals keep injecting
  (context, not execution), the delegation's own window stays the bound — a window ending
  under pause still files the sponsor's direction ask, its escalation suspended with the
  stall clock (INT-050) — and resume unfreezes in place. Close, not pause, unbinds.
- **INT-033** — Schedules elapsing under pause coalesce per SUB-051 and play their catch-up
  run on resume; pause defers timetables, never drops them.

## Close semantics

- **INT-040** — Closing runs the same dependency check as retiring an agent over the
  initiative's durable state: open asks and tasks resolved or reassigned; pending spawn
  requests archived with their template pins drained (a terminal act leaves no waiters);
  triggers and playbook schedules whose runs launch under it re-pointed to a successor
  initiative or disabled with an audit note.
- **INT-041** — In-flight execution drains, never truncates: runs already launched complete
  with artifacts landing on the closed slice as history; new runs, spawns, and task filings
  under the initiative are refused (a task that still needs filing belongs to a successor
  initiative); ephemeral workers finish their bounded task and fold back. A sponsor needing
  mid-flight stops suspends or retires the specific agents (CLC-010).
- **INT-042** — When an initiative transitions to `closed`, the close event shall file a
  retrospective ask (kind `question`, tier `bulk`, expiry `escalate`) to the lead —
  the sponsor when the lead is non-active (a lead is required at write, INT-001; the walks
  re-point or close the post before it can sit unheld) — directing DNA proposals through the
  normal write path (DWP-010): a decision record of the outcome, plus the lessons worth
  keeping. The ask's resolution is the filing; the §1 loop closes.

## Direction asks (goal linkage)

- **INT-050** — The goal-end direction ask fires in every non-closed state when the linked
  goal's window ends or the goal goes terminal: window-end offers extend/re-target/close;
  `met` offers close or re-target; `missed`/`retired` offer re-base/re-target/close. Under
  pause its escalation is suspended with the stall clock (playing on resume); under
  `proposed` it joins the activation ask on the sponsor's desk.
- **INT-051** — The answered choice moves the linkage atomically with the answer: extend
  re-windows the same goal row; re-base re-issues the ended objective as a new goal row
  (statement carried, window fresh) and re-target swaps to a different goal — both moving
  `goal_ref` — with the goal slice re-deriving at once; the swap's target rides the same
  respond-time liveness check — a re-point onto a goal that died while the ask waited is
  audit-only, the successor ask carrying a live choice.
- **INT-052** — Direction asks are questions: unanswered, they escalate sponsor → admin and
  stay pending in every digest (ASK-110) — never a silent no.

## Stall detection

- **INT-060** — A stalled initiative — deadline passed with open work — raises an ask
  (kind `question`, expiry `escalate`) to its
  sponsor (then admin) reusing the escalation machinery.
- **INT-061** — The stall clock is defined without a deadline: keyed to the linked goal's
  window when there is one, else a bulk-tier staleness line in the sponsor's digest after a
  configurable window (default 30 days, CFG-011).
- **INT-062** — Clock state coverage: it runs while `active` and while `proposed` (a
  directive that never won activation is decay in its purest form — the activation deny ends
  the wait, not the watch; the bulk line keeps rendering); `paused` suspends it with resume
  restarting it; `closed` stops it for good.
- **INT-063** — A deadline passed with *no* open work raises a close-out ask (kind
  `question`, tier `bulk`, expiry `escalate`) to the
  sponsor — close, or extend if more is coming — so a finished initiative cannot linger
  holding workspace bindings and injecting goals nobody is driving.

## Dependencies

- **INT-070** — `depends_on` is a cross-initiative DAG: cycles are refused at write; edges
  name non-closed rows — a dependency on a closed initiative is refused at write; the only
  way an edge addresses a terminal row is the upstream closing under it, exactly the case
  the close-ask exists for.
- **INT-071** — Closing an upstream initiative with active dependents raises an ask (kind
  `question`, expiry `escalate`) to each
  dependent's sponsor — proceed, re-base (the dependency edge re-pointed), or pause: a
  coordination signal, not a block.

## Launch gates

- **INT-080** — Only `active` initiatives launch runs and spawns: a spawn filed under a
  non-active initiative is refused at request; `proposed` has no bindings to offer;
  `paused` freezes execution; `closed` refuses new work.
- **INT-081** — Board tasks join runs and spawns in the closed-slice refusal; `proposed` and
  `paused` keep task-filing open as planning.
