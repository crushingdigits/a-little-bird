// icons.jsx — simple Material-style outline icons (UI only). Color = currentColor.
const _sv = (s, children, fill) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill={fill || "none"}
    stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">{children}</svg>
);

const IcShield = ({ s = 24 }) => _sv(s, <path d="M12 3l7 3v5c0 4.4-3 7.6-7 9-4-1.4-7-4.6-7-9V6l7-3z" />);
const IcShieldFill = ({ s = 24 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="currentColor" stroke="none">
    <path d="M12 2.6l7.4 3.2V11c0 4.7-3.2 8.2-7.4 9.7C7.8 19.2 4.6 15.7 4.6 11V5.8L12 2.6z" />
    <path d="M10.6 13.4l-1.9-1.9-1.3 1.3 3.2 3.2 5-5-1.3-1.3-3.7 3.7z" fill="var(--on-primary)" />
  </svg>
);
const IcBack = ({ s = 24 }) => _sv(s, <><path d="M19 12H5" /><path d="M12 19l-7-7 7-7" /></>);
const IcClose = ({ s = 24 }) => _sv(s, <><path d="M18 6L6 18" /><path d="M6 6l12 12" /></>);
const IcHome = ({ s = 24 }) => _sv(s, <path d="M4 11l8-7 8 7v8a1 1 0 01-1 1h-4v-6h-6v6H5a1 1 0 01-1-1v-8z" />);
const IcBell = ({ s = 24 }) => _sv(s, <><path d="M18 8a6 6 0 10-12 0c0 7-3 8-3 8h18s-3-1-3-8" /><path d="M13.7 21a2 2 0 01-3.4 0" /></>);
const IcBellOff = ({ s = 24 }) => _sv(s, <><path d="M13.7 21a2 2 0 01-3.4 0" /><path d="M18 8a6 6 0 00-9-5" /><path d="M6 8c0 7-3 8-3 8h13" /><path d="M3 3l18 18" /></>);
const IcTune = ({ s = 24 }) => _sv(s, <><path d="M4 8h10" /><path d="M18 8h2" /><circle cx="16" cy="8" r="2" /><path d="M4 16h2" /><path d="M10 16h10" /><circle cx="8" cy="16" r="2" /></>);
const IcMore = ({ s = 24 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="currentColor" stroke="none">
    <circle cx="12" cy="5" r="1.8" /><circle cx="12" cy="12" r="1.8" /><circle cx="12" cy="19" r="1.8" />
  </svg>
);
const IcBookmark = ({ s = 24 }) => _sv(s, <path d="M6 4h12v16l-6-4-6 4V4z" />);
const IcShare = ({ s = 24 }) => _sv(s, <><circle cx="18" cy="5" r="2.4" /><circle cx="6" cy="12" r="2.4" /><circle cx="18" cy="19" r="2.4" /><path d="M8.1 13.1l7.8 4.5" /><path d="M15.9 6.4L8.1 10.9" /></>);
const IcCheck = ({ s = 24 }) => _sv(s, <path d="M5 12.5l4.5 4.5L19 7" />);
const IcLock = ({ s = 24 }) => _sv(s, <><rect x="5" y="11" width="14" height="9" rx="2" /><path d="M8 11V8a4 4 0 018 0v3" /></>);
const IcChevR = ({ s = 24 }) => _sv(s, <path d="M9 6l6 6-6 6" />);
const IcStar = ({ s = 24 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="currentColor" stroke="none">
    <path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9L12 3z" />
  </svg>
);
const IcInfo = ({ s = 24 }) => _sv(s, <><circle cx="12" cy="12" r="9" /><path d="M12 11v5" /><path d="M12 8h.01" /></>);
const IcEye = ({ s = 24 }) => _sv(s, <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z" /><circle cx="12" cy="12" r="3" /></>);

Object.assign(window, {
  IcShield, IcShieldFill, IcBack, IcClose, IcHome, IcBell, IcBellOff, IcTune,
  IcMore, IcBookmark, IcShare, IcCheck, IcLock, IcChevR, IcStar, IcInfo, IcEye,
});
