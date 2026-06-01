// screens.jsx — hi-fi screens. Relies on ui.jsx / icons.jsx / data.jsx (on window).
const { useState, useEffect } = React;

/* ---------------- FEED ---------------- */
function FeedScreen({ protectionOn, onToggleProtection, revealed, onOpen, onCatchUp }) {
  const held = FEED.filter((i) => i.match && !revealed.includes(i.id)).length;
  return (
    <div className="scroll">
      <div className="scroll-pad">
        <ProtBanner on={protectionOn} onToggle={onToggleProtection} />
        {protectionOn && held > 0 && <CatchUpCard count={held} onClick={onCatchUp} />}
        {FEED.map((item) => {
          const hide = protectionOn && item.match && !revealed.includes(item.id);
          return hide
            ? <RedactedCard key={item.id} item={item} onOpen={onOpen} />
            : <NewsCard key={item.id} item={item} onOpen={onOpen} />;
        })}
      </div>
    </div>
  );
}

/* ---------------- SETTINGS (spoiler protection) ---------------- */
function SettingsScreen({ protectionOn, onToggleProtection, types, toggleType, handling, setHandling, autoWindow, setAutoWindow, onOpenAlerts }) {
  const TYPES = [
    ["scores", "Scores & results"],
    ["live", "Live match updates"],
    ["ratings", "Player ratings"],
    ["reports", "Match reports"],
    ["previews", "Previews & predicted XI"],
    ["lineups", "Lineups & team news"],
  ];
  return (
    <div className="scroll">
      <div className="screen-head">Settings<small>Spoiler protection, alerts &amp; how the feed behaves.</small></div>
      <div className="scroll-pad" style={{ paddingTop: 0 }}>
        <div className={"master-card" + (protectionOn ? " on" : "")}>
          <div className="mc-ic">{protectionOn ? <IcShieldFill s={24} /> : <IcShield s={24} />}</div>
          <div className="tx"><b>Protection {protectionOn ? "on" : "off"}</b><span>Filter match content from the feed &amp; alerts</span></div>
          <Sw on={protectionOn} onClick={onToggleProtection} />
        </div>

        <div className="set-group">
          <div className="set-label">What counts as a spoiler</div>
          <div className="set-card">
            {TYPES.map(([k, label]) => (
              <div key={k} className="set-row" onClick={() => toggleType(k)}>
                <div className="tx"><b>{label}</b></div>
                <div className={"cbx" + (types[k] ? " on" : "")}>{types[k] && <IcCheck s={16} />}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="set-group">
          <div className="set-label">When something is flagged</div>
          <div className="set-card">
            <div className="rrow" onClick={() => setHandling("blur")}>
              <div className="tx"><b>Blur inline</b><span>Stays in the feed, redacted — tap to reveal</span></div>
              <div className={"rdo" + (handling === "blur" ? " on" : "")} />
            </div>
            <div className="rrow" onClick={() => setHandling("quarantine")}>
              <div className="tx"><b>Send to Quarantine</b><span>Hidden away in a separate list for later</span></div>
              <div className={"rdo" + (handling === "quarantine" ? " on" : "")} />
            </div>
          </div>
        </div>

        <div className="set-group">
          <div className="set-label">Schedule</div>
          <div className="set-card">
            <div className="set-row" onClick={() => setAutoWindow(!autoWindow)}>
              <div className="tx"><b>Turn on around match windows</b><span>Matchday −2h until you mark it watched</span></div>
              <Sw on={autoWindow} onClick={() => setAutoWindow(!autoWindow)} />
            </div>
          </div>
        </div>

        <div className="set-group">
          <div className="set-label">Notifications</div>
          <div className="set-card">
            <div className="set-row" onClick={onOpenAlerts}>
              <span className="lead-ic"><IcBell s={22} /></span>
              <div className="tx"><b>Alerts</b><span>Choose what notifies you — and what stays hidden</span></div>
              <span style={{ color: "var(--on-dim)", display: "flex" }}><IcChevR s={20} /></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ---------------- ALERTS (notifications) ---------------- */
function AlertsScreen({ cats, toggleCat, tier, setTier, quiet, setQuiet }) {
  const CATS = [
    ["confirmed", "Confirmed signings", "“here we go” & official", true],
    ["rumours", "Transfers & rumours", null, false],
    ["coaching", "Coaching & backroom", null, false],
    ["contracts", "Contracts & academy", null, false],
  ];
  const TIERS = [
    ["t1", "Tier 1 only", "Most reliable — official + top journalists"],
    ["t12", "Tier 1 + Tier 2", "Adds established beat writers"],
    ["all", "All sources", "Includes speculative tabloids"],
  ];
  return (
    <div className="scroll">
      <div className="screen-head">Alerts<small>Fully customisable — and spoiler-aware.</small></div>
      <div className="scroll-pad" style={{ paddingTop: 0 }}>
        <div className="lockprev">
          <div className="lockprev-tag">Preview · lock screen</div>
          <div className="lockscreen">
            <div className="lock-time">9:30</div>
            <div className="lock-date">Sat 1 June</div>
            <div className="push">
              <div className="push-app"><span className="pic"><span className="d" /></span> A Little Bird · now</div>
              <div className="push-body">Here we go — defender medical booked ✓</div>
            </div>
            <div className="push">
              <div className="push-app"><span className="pic"><span className="d" /></span> A Little Bird · 2m</div>
              <div className="push-body">New set-piece coach joining the backroom staff</div>
            </div>
            <div className="push held">
              <div className="push-app"><IcShield s={11} /> Held by spoiler protection</div>
              <div className="push-body">1 match update hidden until you're caught up</div>
            </div>
          </div>
        </div>

        <div className="set-group">
          <div className="set-label">Notify me about</div>
          <div className="set-card">
            {CATS.map(([k, label, sub, star]) => (
              <div key={k} className="set-row" onClick={() => toggleCat(k)}>
                <div className="tx"><b>{star && <span style={{ color: "var(--primary)" }}><IcStar s={12} /> </span>}{label}</b>{sub && <span>{sub}</span>}</div>
                <Sw on={cats[k]} onClick={() => toggleCat(k)} />
              </div>
            ))}
          </div>
        </div>

        <div className="set-group">
          <div className="set-label">Who can notify · source tier</div>
          <div className="set-card">
            {TIERS.map(([k, label, sub]) => (
              <div key={k} className="rrow" onClick={() => setTier(k)}>
                <div className="tx"><b>{label}</b><span>{sub}</span></div>
                <div className={"rdo" + (tier === k ? " on" : "")} />
              </div>
            ))}
          </div>
        </div>

        <div className="set-group">
          <div className="set-label">Delivery</div>
          <div className="set-card">
            <div className="set-row" onClick={() => setQuiet(!quiet)}>
              <div className="tx"><b>Quiet hours</b><span>10pm – 8am</span></div>
              <Sw on={quiet} onClick={() => setQuiet(!quiet)} />
            </div>
          </div>
        </div>

        <div className="spoiler-note">
          <span className="sn-ic"><IcShieldFill s={18} /></span>
          <span>Match notifications are <b>never</b> delivered while spoiler protection is on — they wait until you mark the match <b>watched</b>.</span>
        </div>
      </div>
    </div>
  );
}

/* ---------------- DETAIL (push overlay) ---------------- */
function DetailScreen({ item, onClose, onToast }) {
  const [inn, setInn] = useState(false);
  useEffect(() => { const id = setTimeout(() => setInn(true), 30); return () => clearTimeout(id); }, []);
  const close = () => { setInn(false); setTimeout(onClose, 300); };
  const initials = item.author.split(" ").map((w) => w[0]).slice(0, 2).join("").toUpperCase();
  return (
    <div className={"overlay" + (inn ? " in" : "")}>
      <div className="detail-bar">
        <button className="iconbtn" onClick={close}><IcBack s={23} /></button>
        <span className="sp" />
        <button className="iconbtn" onClick={() => onToast("Saved to your reading list")} title="Save"><IcBookmark s={22} /></button>
        <button className="iconbtn" onClick={() => onToast("Share sheet opened")} title="Share"><IcShare s={21} /></button>
      </div>
      <div className="detail-scroll">
        <div className="detail-body">
          <div className="detail-meta">
            <TierChip tier={item.tier} />
            <span className="card-src" style={{ color: "var(--on-dim)" }}>{item.time} ago</span>
            {item.confirmed && <span className="chip hwg" style={{ marginLeft: "auto" }}><IcCheck s={12} /> Here we go</span>}
          </div>
          <div className="detail-head">{item.headline}</div>
          {item.image && (
            <div className="card-img ph" style={{ borderRadius: 18, margin: "18px 0 4px", border: "1px solid var(--outline)" }}>
              <span className="ph-tag">press photo</span>
            </div>
          )}
          <div className="detail-by">
            <div className="av">{initials}</div>
            <div>
              <div className="nm">{item.author}</div>
              <div className="rl">{item.role} · {TIER_META[item.tier].label}</div>
            </div>
          </div>
          <p className="detail-p lead">{item.lead}</p>
          {item.paras.map((p, i) => <p key={i} className="detail-p">{p}</p>)}

          {item.corrob && (
            <div className="corrob">
              <div className="corrob-h"><IcCheck s={13} /> Corroboration · {item.corrob.length} sources</div>
              {item.corrob.map((c, i) => (
                <div key={i} className="corrob-row">
                  <TierChip tier={c.tier} />
                  <span className="nm">{c.name}</span>
                  <span className="ck"><IcCheck s={17} /></span>
                </div>
              ))}
            </div>
          )}

          <div className="detail-actions">
            <button className="btn tonal" onClick={() => onToast("Saved to your reading list")}><IcBookmark s={17} /> Save</button>
            <button className="btn tonal" onClick={() => onToast("Share sheet opened")}><IcShare s={16} /> Share</button>
            <button className="btn text" onClick={() => onToast("Muted " + item.source)}><IcBellOff s={16} /> Mute</button>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ---------------- GATE (bottom sheet) ---------------- */
function GateSheet({ item, onReveal, onCancel }) {
  const [inn, setInn] = useState(false);
  useEffect(() => { const id = setTimeout(() => setInn(true), 30); return () => clearTimeout(id); }, []);
  const dismiss = (cb) => { setInn(false); setTimeout(cb, 300); };
  return (
    <>
      <div className={"scrim" + (inn ? " in" : "")} onClick={() => dismiss(onCancel)} />
      <div className={"sheet" + (inn ? " in" : "")}>
        <div className="grip" />
        <div className="gshield"><IcShieldFill s={30} /></div>
        <div className="gtitle">This one might spoil you</div>
        <div className="gsub">It's flagged as <b>match content</b> from {item.source} — likely contains a result, score or ratings. You're in delay mode.</div>
        <div className="gtag">Match content · result likely</div>
        <div className="gactions">
          <button className="btn filled" onClick={() => dismiss(onReveal)}>Reveal anyway</button>
          <button className="btn tonal" onClick={() => dismiss(onCancel)}>Keep it hidden</button>
        </div>
        <div className="gfoot">WHY WAS THIS HIDDEN?</div>
      </div>
    </>
  );
}

/* ---------------- CATCH-UP (mark as watched) ---------------- */
function CatchUpSheet({ count, onConfirm, onCancel }) {
  const [inn, setInn] = useState(false);
  useEffect(() => { const id = setTimeout(() => setInn(true), 30); return () => clearTimeout(id); }, []);
  const dismiss = (cb) => { setInn(false); setTimeout(cb, 300); };
  return (
    <>
      <div className={"scrim" + (inn ? " in" : "")} onClick={() => dismiss(onCancel)} />
      <div className={"sheet" + (inn ? " in" : "")}>
        <div className="grip" />
        <div className="gshield" style={{ background: "color-mix(in srgb, var(--t1) 18%, transparent)", color: "var(--t1)" }}><IcEye s={28} /></div>
        <div className="gtitle">All caught up?</div>
        <div className="gsub">Marking the match as watched reveals <b>{count} held update{count > 1 ? "s" : ""}</b> — scores, results and player ratings will be shown.</div>
        <div className="gactions">
          <button className="btn filled" onClick={() => dismiss(onConfirm)}>Yes, reveal {count > 1 ? "them" : "it"}</button>
          <button className="btn tonal" onClick={() => dismiss(onCancel)}>Not yet</button>
        </div>
        <div className="gfoot">FUTURE MATCHES STAY PROTECTED</div>
      </div>
    </>
  );
}

/* ---------------- TOAST ---------------- */
function Toast({ msg, action }) {
  const [inn, setInn] = useState(false);
  useEffect(() => { const id = setTimeout(() => setInn(true), 20); return () => clearTimeout(id); }, []);
  return (
    <div className={"toast" + (inn ? " in" : "")}>
      <span className="t-ic"><IcCheck s={18} /></span>
      <span className="t-msg">{msg}</span>
      {action && <button className="t-act" onClick={action.onClick}>{action.label}</button>}
    </div>
  );
}

function AlertsSubscreen({ onClose, cats, toggleCat, tier, setTier, quiet, setQuiet }) {
  const [inn, setInn] = useState(false);
  useEffect(() => { const id = setTimeout(() => setInn(true), 30); return () => clearTimeout(id); }, []);
  const close = () => { setInn(false); setTimeout(onClose, 300); };
  return (
    <div className={"overlay" + (inn ? " in" : "")}>
      <div className="detail-bar">
        <button className="iconbtn" onClick={close}><IcBack s={23} /></button>
        <span className="sp" />
      </div>
      <AlertsScreen cats={cats} toggleCat={toggleCat} tier={tier} setTier={setTier} quiet={quiet} setQuiet={setQuiet} />
    </div>
  );
}

Object.assign(window, { FeedScreen, SettingsScreen, AlertsScreen, AlertsSubscreen, DetailScreen, GateSheet, CatchUpSheet, Toast });
