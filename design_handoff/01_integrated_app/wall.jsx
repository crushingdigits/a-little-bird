// wall.jsx — Spoiler "match wall" feature for A Little Bird.
// Replaces the old bulk "reveal everything" catch-up with a per-game time wall.
//
// MODEL
//   • The WALL = kickoff of the earliest game still UNWATCHED. Match news whose
//     game is unwatched (i.e. at/after the wall) is hidden in the feed.
//   • Marking a game watched releases that game's held news and advances the wall.
//   • HARD RULE: a score/result is shown ONLY for games marked watched. It lives
//     in scoreStr()/resultWord() + the ScoreTrail component below — nowhere else.
//
// Relies on icons.jsx (IcShield, IcShieldFill, IcLock, IcEye, IcCheck, IcChevR,
// IcClose) being loaded first. Defines the few extra icons it needs here.

/* ---------- extra icons (Material outline, currentColor) ---------- */
const _wi = (s, children, fill) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill={fill || "none"}
    stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">{children}</svg>
);
const IcClock = ({ s = 24 }) => _wi(s, <><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>);
const IcWifiOff = ({ s = 24 }) => _wi(s, <><path d="M3 3l18 18" /><path d="M9 17.5a4 4 0 016 0" /><path d="M5 13a9 9 0 0110-2.2" /><path d="M2 9a14 14 0 0118-1" /></>);
const IcUndo = ({ s = 24 }) => _wi(s, <><path d="M9 14l-4-4 4-4" /><path d="M5 10h9a5 5 0 015 5v1" /></>);
const IcRefresh = ({ s = 24 }) => _wi(s, <><path d="M21 12a9 9 0 11-3-6.7" /><path d="M21 4v4h-4" /></>);
const IcCheckCircle = ({ s = 24 }) => _wi(s, <><circle cx="12" cy="12" r="9" /><path d="M8 12.5l2.6 2.6L16 9.5" /></>);

/* ---------- fixtures + wall logic ---------- */
const INITIAL_WATCHED = ["bha", "rma"]; // user is a couple of games behind

const FIXTURES = [
  { id: "bha", opp: "Brighton",     abbr: "BHA", venue: "A", compTag: "PL",  date: "Sat 17 May", ko: "15:00", status: "played",   score: { h: 2, a: 2 } },
  { id: "rma", opp: "Real Madrid",  abbr: "RMA", venue: "H", compTag: "UCL", date: "Tue 20 May", ko: "20:00", status: "played",   score: { h: 3, a: 1 } },
  { id: "eve", opp: "Everton",      abbr: "EVE", venue: "H", compTag: "PL",  date: "Sun 25 May", ko: "14:00", status: "played",   score: { h: 2, a: 0 } },
  { id: "new", opp: "Newcastle",    abbr: "NEW", venue: "A", compTag: "PL",  date: "Wed 28 May", ko: "19:45", status: "played",   score: { h: 1, a: 1 } },
  { id: "che", opp: "Chelsea",      abbr: "CHE", venue: "H", compTag: "PL",  date: "Sun 1 Jun",  ko: "16:30", status: "played",   score: { h: 2, a: 1 } },
  { id: "avl", opp: "Aston Villa",  abbr: "AVL", venue: "A", compTag: "PL",  date: "Sat 7 Jun",  ko: "12:30", status: "upcoming", score: null },
  { id: "ars", opp: "Arsenal",      abbr: "ARS", venue: "H", compTag: "PL",  date: "Sat 14 Jun", ko: "17:30", status: "upcoming", score: null },
  { id: "psg", opp: "Paris SG",     abbr: "PSG", venue: "A", compTag: "UCL", date: "Wed 18 Jun", ko: "20:00", status: "upcoming", score: null },
];
const FX = Object.fromEntries(FIXTURES.map((f) => [f.id, f]));

// earliest played game NOT in watched = the wall; plus the list still hiding news
function deriveWall(watched) {
  const played = FIXTURES.filter((f) => f.status === "played");
  const wall = played.find((f) => !watched.has(f.id)) || null;
  const hiding = played.filter((f) => !watched.has(f.id));
  return { wall, hiding, hidingCount: hiding.length };
}
function fxState(f, watched) {
  if (f.status === "upcoming") return "upcoming";
  return watched.has(f.id) ? "watched" : "unwatched";
}
// score string — call ONLY when the game is watched.
function scoreStr(f) { return f.score ? f.score.h + "\u2013" + f.score.a : ""; }
function resultWord(f) {
  if (!f.score) return "";
  const liv = f.venue === "H" ? f.score.h : f.score.a;
  const them = f.venue === "H" ? f.score.a : f.score.h;
  return liv > them ? "W" : liv < them ? "L" : "D";
}
function shortDate(d) { const p = d.split(" "); return p.length >= 3 ? p.slice(1).join(" ") : d; }

/* ---------- club crest badges (placeholders — see handoff README) ---------- */
function Badge({ children }) {
  return (
    <svg viewBox="0 0 40 40" width="100%" height="100%" preserveAspectRatio="xMidYMid slice" style={{ display: "block" }}>
      {children}
      <rect x="0.6" y="0.6" width="38.8" height="38.8" rx="12" fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="1.2" />
    </svg>
  );
}
const CRESTS = {
  bha: () => <Badge>{[0,1,2,3,4,5].map(i=>(<rect key={i} x={i*7} width="3.6" height="40" fill="#0057B8" />))}</Badge>,
  rma: () => <Badge><rect width="40" height="40" fill="#FEFEFE" /><circle cx="20" cy="20" r="9" fill="none" stroke="#00529F" strokeWidth="2.4" /><rect x="18.6" y="6" width="2.8" height="28" fill="#FEBE10" /></Badge>,
  eve: () => <Badge><rect width="40" height="40" fill="#003399" /><path d="M13 12h14v8a7 7 0 01-14 0z" fill="none" stroke="#fff" strokeWidth="2" /></Badge>,
  new: () => <Badge>{[0,1,2,3,4,5].map(i=>(<rect key={i} x={i*7} width="3.6" height="40" fill={i%2?"#fff":"#111"} />))}</Badge>,
  che: () => <Badge><rect width="40" height="40" fill="#034694" /><circle cx="20" cy="20" r="8" fill="none" stroke="#fff" strokeWidth="2.2" /><circle cx="20" cy="20" r="2.6" fill="#DBA111" /></Badge>,
  avl: () => <Badge><rect width="40" height="20" fill="#95BFE5" /><rect y="20" width="40" height="20" fill="#670E36" /><path d="M20 8l3 6h-6z" fill="#FFE600" /></Badge>,
  ars: () => <Badge><rect width="40" height="40" fill="#EF0107" /><rect y="13" width="40" height="14" fill="#fff" /><path d="M14 18h12l-6 6z" fill="#EF0107" /></Badge>,
  psg: () => <Badge><rect width="40" height="40" fill="#001E5E" /><rect x="16" width="8" height="40" fill="#E30613" /><rect x="13" width="2.5" height="40" fill="#fff" /><rect x="24.5" width="2.5" height="40" fill="#fff" /></Badge>,
};

function Crest({ f }) {
  const C = CRESTS[f.id];
  return (
    <div className={"ws-crest" + (C ? " has-img" : "")}>
      {C ? <span className="ws-crest-img">{C()}</span> : <span>{f.abbr}</span>}
      <span className="ws-ven">{f.venue}</span>
    </div>
  );
}
function FxMeta({ f }) {
  return (
    <div className="ws-meta">
      <span className={"ws-comp" + (f.compTag === "UCL" ? " ucl" : "")}>{f.compTag}</span>
      <span className="dot" /><span>{f.venue === "H" ? "Home" : "Away"}</span>
      <span className="dot" /><span>{shortDate(f.date)}</span>
    </div>
  );
}
// HARD RULE gate: renders a score only when watched.
function ScoreTrail({ f }) {
  const r = resultWord(f);
  return (
    <div className="ws-trail">
      <span className="ws-score"><span className={"ws-rw " + r}>{r}</span>{scoreStr(f)}</span>
      <span className="ws-watchtag"><span className="ck"><IcCheck s={11} /></span> Watched</span>
    </div>
  );
}

/* ---------- the sheet ---------- */
function MatchWallSheet({ watched, onMark, onUnmark, offline, onClose }) {
  const [inn, setInn] = React.useState(false);
  const [seg, setSeg] = React.useState("results");
  React.useEffect(() => { const id = setTimeout(() => setInn(true), 30); return () => clearTimeout(id); }, []);
  const dismiss = () => { setInn(false); setTimeout(onClose, 300); };

  const { wall, hidingCount } = deriveWall(watched);
  const results = FIXTURES.filter((f) => f.status === "played").slice().reverse();
  const upcoming = FIXTURES.filter((f) => f.status === "upcoming");

  return (
    <React.Fragment>
      <div className={"scrim" + (inn ? " in" : "")} onClick={dismiss} />
      <div className={"sheet left" + (inn ? " in" : "")}>
        <div className="grip" />
        <div className="sheet-head">
          <div className="sh-title">Fixtures &amp; spoilers</div>
          <button className="iconbtn" onClick={dismiss}><IcClose s={20} /></button>
        </div>
        <div className="sheet-scroll">
          <div className="ws-sub" style={{ padding: "0 0 12px" }}>
            {hidingCount > 0
              ? <><span className="ws-count-pill"><IcLock s={11} /> {hidingCount} UNWATCHED</span> {hidingCount > 1 ? "games are" : "game is"} hiding newer stories. Mark {hidingCount > 1 ? "them" : "it"} watched to catch up.</>
              : <>All caught up — nothing's hidden. Every result below is yours to see.</>}
          </div>

          <div className={"ws-statusbar" + (offline ? " offline" : "")}>
            <span className="sb-ic">{offline ? <IcWifiOff s={16} /> : <IcClock s={15} />}</span>
            <span className="sb-tx">{offline ? <><b>No connection</b> · updated 2 Jun, 21:30</> : <>Updated 2 Jun, 21:30</>}</span>
            <button className="sb-btn"><IcRefresh s={13} /> {offline ? "Try again" : "Refresh"}</button>
          </div>
          {offline && (
            <div className="ws-offline-note" style={{ marginTop: 0, marginBottom: 4 }}>
              <span className="on-ic"><IcCheckCircle s={18} /></span>
              <span className="on-tx"><b>Marking still works offline.</b> Your watched games are saved on this device and the wall moves right away — no connection needed.</span>
            </div>
          )}

          <div className="ws-seg">
            <button className={seg === "results" ? "on" : ""} onClick={() => setSeg("results")}>Results</button>
            <button className={seg === "upcoming" ? "on" : ""} onClick={() => setSeg("upcoming")}>Upcoming</button>
          </div>

          {seg === "results" ? (
            <div className="ws-list">
              {results.map((f) => {
                const w = watched.has(f.id);
                return (
                  <div className="ws-fx" key={f.id}>
                    <Crest f={f} />
                    <div className="ws-fxtx"><div className="ws-opp">{f.opp}</div><FxMeta f={f} /></div>
                    {w
                      ? <div style={{ display: "flex", alignItems: "center", gap: 8 }}><ScoreTrail f={f} /><button className="ws-unmark" onClick={() => onUnmark(f.id)} title="Un-mark — re-hide this result"><IcUndo s={16} /></button></div>
                      : <button className="ws-mark" onClick={() => onMark(f.id)}><IcEye s={15} /> Mark watched</button>}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="ws-list">
              {upcoming.map((f) => (
                <div className="ws-fx" key={f.id}>
                  <Crest f={f} />
                  <div className="ws-fxtx"><div className="ws-opp">{f.opp}</div><FxMeta f={f} /></div>
                  <div className="ws-ko"><div className="t">{f.ko}</div><div className="d">{f.date.split(" ")[0]}</div></div>
                </div>
              ))}
            </div>
          )}
          <div className="ws-foot"><span className="sh"><IcShieldFill s={13} /></span> A score is only ever shown once its game is watched</div>
        </div>
      </div>
    </React.Fragment>
  );
}

/* ---------- the feed entry point — merged into the protection card ---------- */
function ProtectionCard({ on, onToggle, hidingCount, onOpen }) {
  if (!on) {
    return (
      <div className="protban" onClick={onToggle}>
        <div className="protban-ic"><IcShield s={22} /></div>
        <div className="protban-tx"><b>Spoiler protection</b><span>Off · showing everything, including match content</span></div>
        <Sw on={false} onClick={onToggle} />
      </div>
    );
  }
  const hiding = hidingCount > 0;
  return (
    <div className="ws-protcard">
      <div className="pc-top">
        <span className="pc-ic"><IcShieldFill s={22} /></span>
        <div className="pc-tx">
          <b>Spoiler protection</b>
          <span>{hiding ? "On · hiding news from games you haven't watched" : "On · nothing to hide right now"}</span>
        </div>
        <Sw on={true} onClick={onToggle} />
      </div>
      <div className="pc-div" />
      {hiding ? (
        <button className="pc-action" onClick={onOpen}>
          <span className="pa-lk"><IcLock s={14} /></span>
          <span className="pa-tx"><b>{hidingCount} unwatched {hidingCount > 1 ? "games are" : "game is"}</b> hiding newer stories</span>
          <span className="pa-cta">Manage <IcChevR s={13} /></span>
        </button>
      ) : (
        <button className="pc-action" onClick={onOpen}>
          <span className="pa-lk ok"><IcCheck s={14} /></span>
          <span className="pa-tx"><b>You're all caught up</b> — every game watched</span>
          <span className="pa-cta">Fixtures <IcChevR s={13} /></span>
        </button>
      )}
    </div>
  );
}

Object.assign(window, {
  FIXTURES, FX, INITIAL_WATCHED, deriveWall, fxState, scoreStr, resultWord,
  MatchWallSheet, ProtectionCard,
});
