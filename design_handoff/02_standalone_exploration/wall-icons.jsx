// wall-icons.jsx — extra icons for the spoiler-wall sheet. Relies on icons.jsx
// being loaded first (shares the currentColor / stroke conventions).
const _wsv = (s, children, fill) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill={fill || "none"}
    stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">{children}</svg>
);

const IcCalendar = ({ s = 24 }) => _wsv(s, <><rect x="3.5" y="5" width="17" height="16" rx="2.5" /><path d="M3.5 9.5h17" /><path d="M8 3v4" /><path d="M16 3v4" /></>);
const IcClock = ({ s = 24 }) => _wsv(s, <><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>);
const IcWifiOff = ({ s = 24 }) => _wsv(s, <><path d="M3 3l18 18" /><path d="M9 17.5a4 4 0 016 0" /><path d="M5 13a9 9 0 0110-2.2" /><path d="M2 9a14 14 0 0118-1" /><path d="M19 13.5l-.5.4" /></>);
const IcUndo = ({ s = 24 }) => _wsv(s, <><path d="M9 14l-4-4 4-4" /><path d="M5 10h9a5 5 0 015 5v1" /></>);
const IcRefresh = ({ s = 24 }) => _wsv(s, <><path d="M21 12a9 9 0 11-3-6.7" /><path d="M21 4v4h-4" /></>);
const IcArrowDown = ({ s = 24 }) => _wsv(s, <><path d="M12 5v14" /><path d="M6 13l6 6 6-6" /></>);
const IcMinus = ({ s = 24 }) => _wsv(s, <path d="M6 12h12" />);
const IcWhistle = ({ s = 24 }) => _wsv(s, <><circle cx="9" cy="14" r="6" /><path d="M14.5 11.5L22 9" /><path d="M9 8V5h4" /></>);
const IcCheckCircle = ({ s = 24 }) => _wsv(s, <><circle cx="12" cy="12" r="9" /><path d="M8 12.5l2.6 2.6L16 9.5" /></>);

Object.assign(window, {
  IcCalendar, IcClock, IcWifiOff, IcUndo, IcRefresh, IcArrowDown, IcMinus, IcWhistle, IcCheckCircle,
});
