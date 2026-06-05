// wall-shared.jsx — shared bits for the spoiler-wall sheet explorations:
// faux feed backdrop, fixture sub-components, banners, and edge states.
// Relies on wall-data.jsx + icons.jsx + wall-icons.jsx on window.
const { useState: useSState } = React;

const INITIAL_WATCHED = ["bha", "rma"]; // user is two games behind

/* ---------- fixture sub-components (shared by all layouts) ---------- */
function Crest({ f }) {
  const withImg = hasCrest(f.id);
  return (
    <div className={"ws-crest" + (withImg ? " has-img" : "")}>
      {withImg ? <span className="ws-crest-img"><ClubCrest id={f.id} /></span> : <span>{f.abbr}</span>}
      <span className="ws-ven">{f.venue}</span>
    </div>
  );
}

function shortDate(d) { const p = d.split(" "); return p.length >= 3 ? p.slice(1).join(" ") : d; } // "Sun 25 May" → "25 May"
function FxMeta({ f, showResultHint }) {
  return (
    <div className="ws-meta">
      <span className={"ws-comp" + (f.compTag === "UCL" ? " ucl" : "")}>{f.compTag}</span>
      <span className="dot" />
      <span>{f.venue === "H" ? "Home" : "Away"}</span>
      <span className="dot" />
      <span>{shortDate(f.date)}</span>
      {showResultHint && (<><span className="dot" /><span style={{ color: "var(--primary)", fontWeight: 600 }}>hidden</span></>)}
    </div>
  );
}

// HARD RULE lives here: only render a score when the game is watched.
function ScoreTrail({ f, watched }) {
  if (!watched) return null;
  const r = resultWord(f);
  return (
    <div className="ws-trail">
      <span className="ws-score"><span className={"ws-rw " + r}>{r}</span>{scoreStr(f)}</span>
      <span className="ws-watchtag"><span className="ck"><IcCheck s={11} /></span> Watched</span>
    </div>
  );
}

function HiddenChip() {
  return <span className="ws-hidden"><span className="lk"><IcLock s={11} /></span> Result hidden</span>;
}

function KoTrail({ f }) {
  return (
    <div className="ws-ko">
      <div className="t">{f.ko}</div>
      <div className="d">{f.date.split(" ").slice(0, 1)}</div>
    </div>
  );
}

function MarkBtn({ kind = "filled", label = "Mark watched", onClick }) {
  return (
    <button className={"ws-mark" + (kind === "tonal" ? " tonal" : kind === "ghost" ? " ghost" : "")} onClick={onClick}>
      <IcEye s={15} /> {label}
    </button>
  );
}
function UnmarkBtn({ onClick }) {
  return <button className="ws-unmark" onClick={onClick} title="Un-mark — re-hide this result"><IcUndo s={16} /></button>;
}

/* ---------- faux feed backdrop (dimmed behind the sheet) ---------- */
function FauxFeed() {
  return (
    <div className="ws-bg">
      <div className="topbar">
        <div className="brand">
          <div className="emblem"><span className="dot" /></div>
          <div>
            <div className="brand-name">A Little Bird</div>
            <div className="brand-sub">LFC transfers &amp; backroom</div>
          </div>
        </div>
        <button className="iconbtn shieldbtn on"><IcShieldFill s={22} /></button>
        <button className="iconbtn"><IcMore s={22} /></button>
      </div>
      <div className="scroll">
        <div className="scroll-pad">
          <div className="protban on">
            <div className="protban-ic"><IcShieldFill s={22} /></div>
            <div className="protban-tx"><b>Spoiler protection</b><span>News after the Everton kickoff is hidden</span></div>
            <div className="sw on"><div className="thumb" /></div>
          </div>
          <div className="card">
            <div className="card-pad">
              <div className="card-row"><span className="tier t1"><span className="pip" />T1 RELIABLE</span><span className="card-src">The Athletic</span><span className="time">8m</span></div>
              <div className="card-head">Liverpool agree personal terms with target midfielder</div>
            </div>
          </div>
          <div className="card redact">
            <div className="redact-pad">
              <div className="redact-blur"><div className="redact-row"><span className="tier ghost"><span className="pip" />MATCH</span></div><div className="bar" style={{ width: "92%" }} /><div className="bar" style={{ width: "70%" }} /></div>
              <div className="redact-over"><div className="lk"><IcLock s={19} /></div><div className="ttl">Match content hidden</div><div className="sub">tap to reveal</div></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* phone stage: dimmed feed + scrim + docked sheet child */
function PhoneStage({ children, label }) {
  return (
    <div className="ws-screen" data-screen-label={label}>
      <FauxFeed />
      <div className="ws-dim" />
      {children}
    </div>
  );
}

/* =====================================================================
   FEED ENTRY — the protection card IS the entry point.
   Master on/off toggle on top; a tappable action row below opens the sheet.
   The action row adapts to whether anything is currently hidden.
   ===================================================================== */
function FeedTopBar() {
  return (
    <div className="topbar">
      <div className="brand">
        <div className="emblem"><span className="dot" /></div>
        <div><div className="brand-name">A Little Bird</div><div className="brand-sub">LFC transfers &amp; backroom</div></div>
      </div>
      <button className="iconbtn shieldbtn on"><IcShieldFill s={22} /></button>
    </div>
  );
}
function FeedCardSample() {
  return (
    <div className="card">
      <div className="card-pad">
        <div className="card-row"><span className="tier t1"><span className="pip" />T1 RELIABLE</span><span className="card-src">Fabrizio Romano</span><span className="time">19m</span></div>
        <div className="card-head">Right-back deal agreed in full — medical booked</div>
      </div>
    </div>
  );
}

function BannerStage({ count }) {
  const hiding = count > 0;
  return (
    <div className="ws-screen" style={{ height: "auto", minHeight: 0, borderRadius: 30 }}>
      <div style={{ position: "relative" }}>
        <FeedTopBar />
        <div className="scroll-pad" style={{ paddingTop: 8, paddingBottom: 20, gap: 12, display: "flex", flexDirection: "column" }}>
          <div className="ws-protcard">
            <div className="pc-top">
              <span className="pc-ic"><IcShieldFill s={22} /></span>
              <div className="pc-tx">
                <b>Spoiler protection</b>
                <span>{hiding ? "On · hiding news after the Everton kickoff" : "On · nothing to hide right now"}</span>
              </div>
              <div className="sw on"><div className="thumb" /></div>
            </div>
            <div className="pc-div" />
            {hiding ? (
              <button className="pc-action">
                <span className="pa-lk"><IcLock s={14} /></span>
                <span className="pa-tx"><b>{count} unwatched {count > 1 ? "games are" : "game is"}</b> hiding newer stories</span>
                <span className="pa-cta">Manage <IcChevR s={13} /></span>
              </button>
            ) : (
              <button className="pc-action">
                <span className="pa-lk ok"><IcCheck s={14} /></span>
                <span className="pa-tx"><b>You're all caught up</b> — every game watched</span>
                <span className="pa-cta">Fixtures <IcChevR s={13} /></span>
              </button>
            )}
          </div>

          <FeedCardSample />
        </div>
      </div>
    </div>
  );
}

/* =====================================================================
   EDGE STATES — empty / loading / offline
   ===================================================================== */
function StateEmptySheet() {
  const next = FIXTURES.find((f) => f.status === "upcoming");
  return (
    <div className="ws-sheet">
      <div className="ws-grip" />
      <div className="ws-head">
        <div className="ws-htx"><div className="ws-title">All caught up</div><div className="ws-sub">No games are hiding your news. The feed is showing everything.</div></div>
        <button className="iconbtn"><IcClose s={20} /></button>
      </div>
      <div className="ws-body">
        <div className="ws-state" style={{ paddingTop: 18, paddingBottom: 10 }}>
          <div className="st-ic ok"><IcCheckCircle s={32} /></div>
          <div className="st-h">You're all watched up</div>
          <div className="st-s">Protection moves forward automatically when your next game kicks off.</div>
        </div>
        <div className="ws-grp">Next fixture <span className="ln" /></div>
        <div className="ws-next-card">
          <Crest f={next} />
          <div className="ws-fxtx"><div className="ws-opp">{next.opp}</div><FxMeta f={next} /></div>
          <KoTrail f={next} />
        </div>
        <div className="ws-foot"><span className="sh"><IcShieldFill s={13} /></span> Protection re-arms at {next.ko} on {next.date}</div>
      </div>
    </div>
  );
}

function StateLoadingSheet() {
  return (
    <div className="ws-sheet">
      <div className="ws-grip" />
      <div className="ws-head">
        <div className="ws-htx"><div className="ws-title">Fixtures &amp; spoilers</div><div className="ws-sub">Loading your Liverpool fixtures…</div></div>
        <button className="iconbtn"><IcClose s={20} /></button>
      </div>
      <div className="ws-body">
        <div style={{ height: 70, borderRadius: 16, marginTop: 6 }} className="ws-sk" />
        <div className="ws-grp"><span className="ws-sk line" style={{ width: 90 }} /><span className="ln" /></div>
        {[0, 1, 2, 3].map((i) => (
          <div className="ws-skel-row" key={i}>
            <div className="ws-sk crest" />
            <div style={{ display: "flex", flexDirection: "column", gap: 7 }}>
              <div className="ws-sk line" style={{ width: i % 2 ? "55%" : "70%" }} />
              <div className="ws-sk line" style={{ width: "42%", height: 9 }} />
            </div>
            <div className="ws-sk pill" />
          </div>
        ))}
      </div>
    </div>
  );
}

function StateOfflineSheet() {
  return (
    <div className="ws-sheet">
      <div className="ws-grip" />
      <div className="ws-head">
        <div className="ws-htx"><div className="ws-title">Fixtures &amp; spoilers</div><div className="ws-sub">You're offline — showing your saved fixtures.</div></div>
        <button className="iconbtn"><IcClose s={20} /></button>
      </div>
      <div className="ws-body">
        <div className="ws-offline-note">
          <span className="on-ic"><IcCheckCircle s={18} /></span>
          <span className="on-tx"><b>Marking still works offline.</b> Your watched games are saved on this device and the wall moves right away — no connection needed.</span>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  INITIAL_WATCHED, Crest, FxMeta, ScoreTrail, HiddenChip, KoTrail, MarkBtn, UnmarkBtn,
  FauxFeed, PhoneStage, BannerStage, StateLoadingSheet,
});
