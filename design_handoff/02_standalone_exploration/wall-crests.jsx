// wall-crests.jsx — a small LOCAL set of club badges for the fixtures list.
// These are ORIGINAL, simplified kit-colour roundels (not the clubs' real
// trademarked crests) — enough to recognise at a glance by colour + motif.
// If an id isn't in the map, the UI falls back to the plain abbreviation tile.

// helper: draws full-bleed 40×40 content; the .ws-crest-img tile clips to its
// rounded-square radius, so badges just fill the square.
function Badge({ children }) {
  return (
    <svg viewBox="0 0 40 40" width="100%" height="100%" preserveAspectRatio="xMidYMid slice" style={{ display: "block" }}>
      {children}
      <rect x="0.6" y="0.6" width="38.8" height="38.8" rx="12" fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="1.2" />
    </svg>
  );
}

// each entry returns the inner <g> content (clipped to the circle)
const CRESTS = {
  liv: () => <Badge><rect width="40" height="40" fill="#C8102E" /><path d="M8 26c3 0 4-2 6-2s3 2 6 2 4-2 6-2 3 1.5 6 2" fill="none" stroke="#F6EB61" strokeWidth="2" /></Badge>,
  bha: () => <Badge>{[0,1,2,3,4,5].map(i=>(<rect key={i} x={i*7} y="0" width="3.6" height="40" fill="#0057B8" />))}<rect width="40" height="40" fill="#fff" opacity="0.001" /></Badge>,
  rma: () => <Badge><rect width="40" height="40" fill="#FEFEFE" /><circle cx="20" cy="20" r="9" fill="none" stroke="#00529F" strokeWidth="2.4" /><rect x="18.6" y="6" width="2.8" height="28" fill="#FEBE10" /></Badge>,
  eve: () => <Badge><rect width="40" height="40" fill="#003399" /><path d="M13 12h14v8a7 7 0 01-14 0z" fill="none" stroke="#fff" strokeWidth="2" /></Badge>,
  new: () => <Badge>{[0,1,2,3,4,5].map(i=>(<rect key={i} x={i*7} y="0" width="3.6" height="40" fill={i%2?"#fff":"#111"} />))}<rect width="40" height="40" fill="#111" opacity="0.001" /></Badge>,
  che: () => <Badge><rect width="40" height="40" fill="#034694" /><circle cx="20" cy="20" r="8" fill="none" stroke="#fff" strokeWidth="2.2" /><circle cx="20" cy="20" r="2.6" fill="#DBA111" /></Badge>,
  avl: () => <Badge><rect width="40" height="20" fill="#95BFE5" /><rect y="20" width="40" height="20" fill="#670E36" /><path d="M20 8l3 6h-6z" fill="#FFE600" /></Badge>,
  ars: () => <Badge><rect width="40" height="40" fill="#EF0107" /><rect y="13" width="40" height="14" fill="#fff" /><path d="M14 18h12l-6 6z" fill="#EF0107" /></Badge>,
  psg: () => <Badge><rect width="40" height="40" fill="#001E5E" /><rect x="16" width="8" height="40" fill="#E30613" /><rect x="13" width="2.5" height="40" fill="#fff" /><rect x="24.5" width="2.5" height="40" fill="#fff" /></Badge>,
};

function hasCrest(id) { return !!CRESTS[id]; }
function ClubCrest({ id }) { const C = CRESTS[id]; return C ? C() : null; }

Object.assign(window, { CRESTS, hasCrest, ClubCrest });
