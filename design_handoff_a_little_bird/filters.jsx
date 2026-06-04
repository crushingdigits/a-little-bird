// filters.jsx — filter chrome + logic for A Little Bird.
//
// Model:
//   • TYPE = single-select feed switcher (the "what am I reading about")
//   • TIER = single cumulative reliability LEVEL (the "how much do I trust it"):
//            level N shows every tier from 1 (most reliable) down to N.
//            T1 = reliable only · T2 = +beat writers · T3 = +speculative.
//   • Three switchable SURFACES for picking type: tabs / chips / feed-sheet.
//   • Pin a default feed, persist across sessions.
// Relies on data.jsx (NEWS_TYPES, TIER_META, FEED) + icons.jsx on window.
const { useState: useFState, useEffect: useFEffect } = React;

const TYPE_GROUPS = NEWS_TYPES.filter((t) => t.cats); // exclude "all"
const ALL_TIERS = [1, 2, 3];

// ---- core filter ----
function filterFeed(feed, f) {
  let items = feed.filter((i) => i.tier <= f.tierMax);
  const g = NEWS_TYPES.find((t) => t.key === f.type);
  if (g && g.cats) items = items.filter((i) => g.cats.includes(i.cat));
  return items;
}

// count in the FULL feed for a type group, honouring the current reliability level
function typeCount(typeKey, tierMax) {
  const g = NEWS_TYPES.find((t) => t.key === typeKey);
  return FEED.filter((i) => i.tier <= tierMax && (!g.cats || g.cats.includes(i.cat))).length;
}

/* ---------------- TIER — single cumulative reliability level ---------------- */
function TierLevel({ level, onSet, showLabel }) {
  return (
    <div className="tiertog">
      {showLabel && <span className="ctrl-lbl">Reliability</span>}
      <div className="tierladder" role="radiogroup" aria-label="Reliability level">
        {ALL_TIERS.map((n) => {
          const inc = n <= level; // every tier above the chosen level is included
          return (
            <button key={n} className={"tstep t" + n + (inc ? " inc" : "") + (n === level ? " lvl" : "")}
              onClick={() => onSet(n)} role="radio" aria-checked={n === level} title={TIER_META[n].label}>
              <span className="pip" />T{n}
            </button>
          );
        })}
      </div>
    </div>
  );
}

/* ---------------- PIN — set current feed as default ---------------- */
function PinBtn({ active, onToggle }) {
  return (
    <button className={"pinbtn" + (active ? " on" : "")} onClick={onToggle}
      title={active ? "Default feed — tap to unpin" : "Pin as default feed"}>
      {active ? <IcPinFill s={18} /> : <IcPin s={18} />}
    </button>
  );
}

/* ---------------- SURFACE A: type tabs ---------------- */
function TypeTabs({ type, onType, tierMax, pinned }) {
  return (
    <div className="tabs">
      {NEWS_TYPES.map((t) => {
        const on = type === t.key;
        const n = typeCount(t.key, tierMax);
        return (
          <button key={t.key} className={"tab" + (on ? " on" : "") + (n === 0 ? " muted" : "")} onClick={() => onType(t.key)}>
            {pinned === t.key && <span className="tab-pin"><IcPinFill s={11} /></span>}
            {t.short}<span className="tcount">{n}</span>
          </button>
        );
      })}
    </div>
  );
}

/* ---------------- SURFACE B: chips bar (tier ladder + type chips) ---------------- */
function ChipBar({ type, onType, tierMax, onSetTier, pinned, onPin }) {
  return (
    <div className="fbar">
      <TierLevel level={tierMax} onSet={onSetTier} />
      <div className="fdiv" />
      <div className="fchips">
        {NEWS_TYPES.map((t) => {
          const on = type === t.key;
          const n = typeCount(t.key, tierMax);
          return (
            <button key={t.key} className={"fchip" + (on ? " on" : "")} onClick={() => onType(t.key)}>
              {pinned === t.key && <span className="fchip-pin"><IcPinFill s={11} /></span>}
              {t.short}<span className="fcount">{n}</span>
            </button>
          );
        })}
      </div>
      <div className="fdiv" />
      <PinBtn active={pinned === type} onToggle={onPin} />
    </div>
  );
}

/* ---------------- SURFACE C: feed button + sheet ---------------- */
function FeedButton({ type, onOpen }) {
  const g = NEWS_TYPES.find((t) => t.key === type);
  return (
    <button className="feedbtn" onClick={onOpen}>
      <span className="fb-ic"><IcList s={18} /></span>
      <span className="fb-tx"><span className="fb-lbl">Feed</span><span className="fb-val">{g.label}</span></span>
      <span className="fb-chev"><IcChevR s={18} /></span>
    </button>
  );
}

function FeedSheet({ type, tierMax, onPick, pinned, onPin, onClose }) {
  const [inn, setInn] = useFState(false);
  useFEffect(() => { const id = setTimeout(() => setInn(true), 30); return () => clearTimeout(id); }, []);
  const dismiss = (cb) => { setInn(false); setTimeout(cb, 300); };
  return (
    <>
      <div className={"scrim" + (inn ? " in" : "")} onClick={() => dismiss(onClose)} />
      <div className={"sheet left" + (inn ? " in" : "")}>
        <div className="grip" />
        <div className="sheet-head">
          <div className="sh-title">Choose a feed</div>
          <button className="iconbtn" onClick={() => dismiss(onClose)}><IcClose s={20} /></button>
        </div>
        <div className="sheet-scroll">
          <div className="set-card feedlist">
            {NEWS_TYPES.map((t) => {
              const on = type === t.key;
              const n = typeCount(t.key, tierMax);
              return (
                <div key={t.key} className={"feedrow" + (on ? " on" : "")} onClick={() => dismiss(() => onPick(t.key))}>
                  <div className="tx"><b>{t.label}</b><span>{n} {n === 1 ? "story" : "stories"} at your reliability level</span></div>
                  {pinned === t.key && <span className="feedrow-pin" onClick={(e) => { e.stopPropagation(); onPin(t.key); }}><IcPinFill s={16} /></span>}
                  {on && <span className="feedrow-ck"><IcCheck s={18} /></span>}
                </div>
              );
            })}
          </div>
          <button className="pin-current" onClick={() => onPin(type)}>
            {pinned === type ? <><IcPinFill s={16} /> Default feed pinned</> : <><IcPin s={16} /> Pin “{NEWS_TYPES.find((t) => t.key === type).label}” as default</>}
          </button>
        </div>
      </div>
    </>
  );
}

/* ---------------- empty state ---------------- */
function FilterEmpty({ onReset }) {
  return (
    <div className="empty">
      <div className="eic"><IcInfo s={26} /></div>
      <div className="eh">Nothing here right now</div>
      <div className="es">No stories in this feed at your current reliability level. Try a lower tier or another feed.</div>
      <button className="btn tonal" onClick={onReset}>Show all tiers</button>
    </div>
  );
}

Object.assign(window, {
  filterFeed, typeCount,
  TierLevel, PinBtn, TypeTabs, ChipBar, FeedButton, FeedSheet, FilterEmpty,
});
