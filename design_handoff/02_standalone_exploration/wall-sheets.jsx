// wall-sheets.jsx — the three spoiler-wall sheet layouts (A/B/C), each
// independently interactive. Relies on wall-data / wall-shared / icons.
const { useState: useShState } = React;

function useWatched(initial = INITIAL_WATCHED) {
  const [watched, setWatched] = useShState(() => new Set(initial));
  const mark = (id) => setWatched((s) => new Set([...s, id]));
  const unmark = (id) => setWatched((s) => { const n = new Set(s); n.delete(id); return n; });
  return [watched, mark, unmark];
}
// every played game id — used to demo the "all caught up, results still visible" state
const ALL_PLAYED_IDS = FIXTURES.filter((f) => f.status !== "upcoming").map((f) => f.id);

/* =====================================================================
   OPTION A — "The Wall" timeline
   Time flows top→bottom. A glowing wall marker sits at the earliest
   unwatched kickoff; watched games (green nodes) sit above it.
   ===================================================================== */
function SheetWall() {
  const [watched, mark, unmark] = useWatched();
  const { wall, hidingCount } = deriveWall(FIXTURES, watched);
  const played = FIXTURES.filter((f) => f.status !== "upcoming");
  const upcoming = FIXTURES.filter((f) => f.status === "upcoming");

  return (
    <div className="ws-sheet ws-a" data-screen-label="A · Timeline">
      <div className="ws-grip" />
      <div className="ws-head">
        <div className="ws-htx">
          <div className="ws-title">Your match wall</div>
          <div className="ws-sub">
            {hidingCount > 0
              ? <><span className="ws-count-pill"><IcLock s={11} /> {hidingCount} GAMES</span> are hiding newer stories. Mark one watched to release the news from after its kickoff.</>
              : <>Nothing is hidden — you're all caught up.</>}
          </div>
        </div>
        <button className="iconbtn"><IcClose s={20} /></button>
      </div>

      <div className="ws-body">
        <div className="ws-grp">Recent results <span className="ln" /></div>
        <div className="ws-railg">
          {played.map((f) => {
            const st = fxState(f, watched);
            const isWall = wall && wall.id === f.id;
            const cls = "ws-fx is-" + st + (isWall ? " is-wall" : "");
            return (
              <React.Fragment key={f.id}>
                {isWall && (
                  <div className="ws-wallmark">
                    <div className="wm-top"><span className="wm-ic"><IcShieldFill s={18} /></span><span className="wm-tag">Spoiler wall</span></div>
                    <div className="wm-tx">News published after <b>{f.opp} · {f.date}, {f.ko}</b> is hidden in your feed. Watch &amp; mark this game to move the wall forward.</div>
                  </div>
                )}
                <div className={cls}>
                  <span className="ws-node" />
                  <Crest f={f} />
                  <div className="ws-fxtx">
                    <div className="ws-opp">{f.opp}</div>
                    <FxMeta f={f} />
                  </div>
                  {st === "watched"
                    ? <div style={{ display: "flex", alignItems: "center", gap: 8 }}><ScoreTrail f={f} watched /><UnmarkBtn onClick={() => unmark(f.id)} /></div>
                    : <MarkBtn kind={isWall ? "filled" : "tonal"} label="Mark watched" onClick={() => mark(f.id)} />}
                </div>
              </React.Fragment>
            );
          })}
        </div>

        <div className="ws-grp">Upcoming <span className="ln" /></div>
        <div className="ws-railg">
          {upcoming.map((f) => (
            <div className="ws-fx is-upcoming" key={f.id}>
              <span className="ws-node" />
              <Crest f={f} />
              <div className="ws-fxtx"><div className="ws-opp">{f.opp}</div><FxMeta f={f} /></div>
              <KoTrail f={f} />
            </div>
          ))}
        </div>
        <div className="ws-foot"><span className="sh"><IcShieldFill s={13} /></span> Scores never shown until you mark a game watched</div>
      </div>
    </div>
  );
}

/* =====================================================================
   OPTION B — "Results / Upcoming" segmented list
   A summary banner up top, a segmented switch, scannable card list.
   ===================================================================== */
function SheetSegmented({ initialWatched, offline }) {
  const [watched, mark, unmark] = useWatched(initialWatched);
  const [seg, setSeg] = useShState("results");
  const { wall, hidingCount } = deriveWall(FIXTURES, watched);
  const results = FIXTURES.filter((f) => f.status !== "upcoming").slice().reverse(); // most recent first
  const upcoming = FIXTURES.filter((f) => f.status === "upcoming");

  return (
    <div className="ws-sheet" data-screen-label={offline ? "B · Offline" : "B · Segmented"}>
      <div className="ws-grip" />
      <div className="ws-head">
        <div className="ws-htx">
          <div className="ws-title">Fixtures &amp; spoilers</div>
          <div className="ws-sub">
            {hidingCount > 0
              ? <><span className="ws-count-pill"><IcLock s={11} /> {hidingCount} UNWATCHED</span> {hidingCount > 1 ? "games are" : "game is"} hiding newer stories. Mark {hidingCount > 1 ? "them" : "it"} watched to catch up.</>
              : <>All caught up — nothing's hidden. Every result below is yours to see.</>}
          </div>
        </div>
        <button className="iconbtn"><IcClose s={20} /></button>
      </div>

      <div className="ws-body">
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
                    ? <div style={{ display: "flex", alignItems: "center", gap: 8 }}><ScoreTrail f={f} watched /><UnmarkBtn onClick={() => unmark(f.id)} /></div>
                    : <MarkBtn label="Mark watched" onClick={() => mark(f.id)} />}
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
                <KoTrail f={f} />
              </div>
            ))}
          </div>
        )}
        <div className="ws-foot"><span className="sh"><IcShieldFill s={13} /></span> A score is only ever shown once its game is watched</div>
      </div>
    </div>
  );
}

/* =====================================================================
   OPTION C — "Next to watch" focus / hero
   Surfaces the single action that moves the wall, with the rest tucked
   below. The hero advances as you mark games watched.
   ===================================================================== */
function SheetFocus() {
  const [watched, mark, unmark] = useWatched();
  const { wall, hiding, hidingCount } = deriveWall(FIXTURES, watched);
  const upcomingNext = FIXTURES.find((f) => f.status === "upcoming");
  const watchedCount = FIXTURES.filter((f) => f.status !== "upcoming" && watched.has(f.id)).length;
  const alsoWaiting = hiding.filter((f) => !wall || f.id !== wall.id);

  return (
    <div className="ws-sheet" data-screen-label="C · Focus">
      <div className="ws-grip" />
      <div className="ws-head">
        <div className="ws-htx">
          <div className="ws-title">Catch up to watch</div>
          <div className="ws-sub">{hidingCount > 0 ? <>One step at a time — clear the oldest game first.</> : <>You're all caught up.</>}</div>
        </div>
        <button className="iconbtn"><IcClose s={20} /></button>
      </div>

      <div className="ws-body pad-lg">
        {wall ? (
          <div className="ws-hero">
            <div className="h-eyebrow"><IcArrowDown s={13} /> Next to catch up · {hidingCount} hiding news</div>
            <div className="h-row ws-fx hero-crest" style={{ padding: 0, border: "none", display: "grid" }}>
              <Crest f={wall} />
              <div><div className="h-opp">{wall.opp}</div><div className="h-meta"><span className={"ws-comp" + (wall.compTag === "UCL" ? " ucl" : "")}>{wall.compTag}</span><span className="dot" />{wall.venue === "H" ? "Home" : "Away"}<span className="dot" />{wall.date} · {wall.ko}</div></div>
            </div>
            <div className="h-note">Marking this watched moves the wall to your next game and releases every story published after the <b>{wall.ko}</b> kickoff.</div>
            <button className="btn filled h-cta" onClick={() => mark(wall.id)}><IcEye s={17} /> Mark {wall.opp} watched</button>
            <button className="h-skip"><IcLock s={14} /> I haven't watched it — keep hidden</button>
          </div>
        ) : (
          <div className="ws-hero" style={{ background: "linear-gradient(165deg, color-mix(in srgb, var(--green) 16%, var(--sc)) 0%, var(--sc) 75%)", borderColor: "color-mix(in srgb, var(--green) 30%, transparent)" }}>
            <div className="h-eyebrow" style={{ color: "var(--green)" }}><IcCheckCircle s={14} /> All caught up</div>
            <div className="h-note" style={{ marginTop: 12 }}>Every game is watched and nothing is hidden. Protection re-arms automatically at your next kickoff.</div>
            {upcomingNext && (
              <div className="ws-mini" style={{ marginTop: 4 }}>
                <Crest f={upcomingNext} />
                <div><div className="m-opp">{upcomingNext.opp}</div><div className="m-meta">Next up · {upcomingNext.date}</div></div>
                <KoTrail f={upcomingNext} />
              </div>
            )}
          </div>
        )}

        {alsoWaiting.length > 0 && (
          <React.Fragment>
            <div className="ws-strip-lbl">Also waiting · {alsoWaiting.length}</div>
            {alsoWaiting.map((f) => (
              <div className="ws-mini" key={f.id}>
                <Crest f={f} />
                <div><div className="m-opp">{f.opp}</div><div className="m-meta">{f.compTag} · {f.date} · result hidden</div></div>
                <HiddenChip />
              </div>
            ))}
          </React.Fragment>
        )}

        {watchedCount > 0 && (
          <div className="ws-foot" style={{ justifyContent: "space-between", padding: "16px 2px 2px" }}>
            <span><span className="sh" style={{ color: "var(--green)", marginRight: 6 }}><IcCheck s={12} /></span>{watchedCount} game{watchedCount > 1 ? "s" : ""} watched</span>
            {watchedCount > 0 && wall && <button className="ws-mark ghost" style={{ height: 30, fontSize: 11 }} onClick={() => unmark([...watched].filter((id) => FIXTURES.some((f) => f.id === id && f.status !== "upcoming")).pop())}><IcUndo s={13} /> Undo last</button>}
          </div>
        )}
      </div>
    </div>
  );
}

Object.assign(window, { SheetWall, SheetSegmented, SheetFocus, ALL_PLAYED_IDS });
