// ui.jsx — shared hi-fi components. Relies on icons.jsx + data.jsx (on window).

function Sw({ on, onClick }) {
  return (
    <div className={"sw" + (on ? " on" : "")} onClick={(e) => { e.stopPropagation(); onClick(); }} role="switch" aria-checked={on}>
      <div className="thumb" />
    </div>
  );
}

function TierChip({ tier }) {
  const m = TIER_META[tier];
  return <span className={"tier " + m.cls}><span className="pip" />{"T" + tier + " " + m.label}</span>;
}

function NewsCard({ item, onOpen }) {
  return (
    <div className="card" onClick={() => onOpen(item)}>
      {item.image && (
        <div className="card-img ph"><span className="ph-tag">press photo</span></div>
      )}
      <div className="card-pad">
        <div className="card-row">
          <TierChip tier={item.tier} />
          <span className="card-src">{item.source}</span>
          <span className="time">{item.time}</span>
        </div>
        <div className="card-head">{item.headline}</div>
        <div className="card-foot">
          <span className={"chip" + (item.cat === "Match" ? " match" : "")}>{item.cat}</span>
          {item.confirmed && <span className="chip hwg"><IcCheck s={12} /> Here we go</span>}
        </div>
      </div>
    </div>
  );
}

function RedactedCard({ item, onOpen }) {
  return (
    <div className="card redact" onClick={() => onOpen(item)}>
      <div className="redact-pad">
        <div className="redact-blur" aria-hidden="true">
          <div className="redact-row">
            <span className="tier ghost"><span className="pip" />MATCH</span>
            <span className="card-src">{item.source}</span>
          </div>
          <div className="bar" style={{ width: "94%" }} />
          <div className="bar" style={{ width: "72%" }} />
        </div>
        <div className="redact-over">
          <div className="lk"><IcLock s={19} /></div>
          <div className="ttl">Match content hidden</div>
          <div className="sub">{item.source} · tap to reveal</div>
        </div>
      </div>
    </div>
  );
}

function TopBar({ protectionOn, onToggleShield }) {
  return (
    <div className="topbar">
      <div className="brand">
        <div className="emblem"><span className="dot" /></div>
        <div>
          <div className="brand-name">A Little Bird</div>
          <div className="brand-sub">LFC transfers &amp; backroom</div>
        </div>
      </div>
      <button className={"iconbtn shieldbtn" + (protectionOn ? " on" : "")} onClick={onToggleShield} title="Spoiler protection">
        {protectionOn ? <IcShieldFill s={22} /> : <IcShield s={22} />}
      </button>
      <button className="iconbtn" title="More"><IcMore s={22} /></button>
    </div>
  );
}

function BottomNav({ tab, onTab }) {
  const items = [
    { key: "feed", label: "Feed", icon: <IcHome s={24} /> },
    { key: "settings", label: "Settings", icon: <IcTune s={24} /> },
  ];
  return (
    <div className="bnav">
      {items.map((it) => (
        <button key={it.key} className={"bnav-item" + (tab === it.key ? " on" : "")} onClick={() => onTab(it.key)}>
          <span className="bnav-ind">{it.icon}</span>
          <span className="bnav-lbl">{it.label}</span>
        </button>
      ))}
    </div>
  );
}

Object.assign(window, { Sw, TierChip, NewsCard, RedactedCard, TopBar, BottomNav });
