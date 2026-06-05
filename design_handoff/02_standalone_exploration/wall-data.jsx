// wall-data.jsx — Liverpool fixtures for the spoiler-wall bottom sheet.
//
// Spoiler model (replaces the old bulk "reveal everything"):
//   • The WALL = kickoff of the EARLIEST game still marked UNWATCHED that has
//     already kicked off. News published after that instant is hidden.
//   • Marking a game watched moves the wall forward to the next unwatched
//     kickoff and releases the news in between.
//   • HARD RULE: a score/result may ONLY ever be shown for a game whose state
//     is "watched". Unwatched (played) games show "result hidden" — never a
//     number, anywhere.
//
// status: "watched" | "unwatched" (played, behind/at the wall) | "upcoming"
// Each played game carries a real result, but the UI must gate it on watched.

const FIXTURES = [
  {
    id: "bha", opp: "Brighton", abbr: "BHA", venue: "A",
    comp: "Premier League", compTag: "PL", date: "Sat 17 May",
    ko: "15:00", status: "watched",
    score: { h: 2, a: 2 }, note: "Draw at the Amex",
  },
  {
    id: "rma", opp: "Real Madrid", abbr: "RMA", venue: "H",
    comp: "Champions League", compTag: "UCL", date: "Tue 20 May",
    ko: "20:00", status: "watched",
    score: { h: 3, a: 1 }, note: "Anfield night",
  },
  {
    // ---- THE WALL: earliest unwatched game that has kicked off ----
    id: "eve", opp: "Everton", abbr: "EVE", venue: "H",
    comp: "Premier League", compTag: "PL", date: "Sun 25 May",
    ko: "14:00", status: "unwatched",
    score: { h: 2, a: 0 }, // gated — never rendered while unwatched
  },
  {
    id: "new", opp: "Newcastle", abbr: "NEW", venue: "A",
    comp: "Premier League", compTag: "PL", date: "Wed 28 May",
    ko: "19:45", status: "unwatched",
    score: { h: 1, a: 1 },
  },
  {
    id: "che", opp: "Chelsea", abbr: "CHE", venue: "H",
    comp: "Premier League", compTag: "PL", date: "Sun 1 Jun",
    ko: "16:30", status: "unwatched",
    score: { h: 2, a: 1 },
  },
  {
    id: "avl", opp: "Aston Villa", abbr: "AVL", venue: "A",
    comp: "Premier League", compTag: "PL", date: "Sat 7 Jun",
    ko: "12:30", status: "upcoming",
    score: null,
  },
  {
    id: "ars", opp: "Arsenal", abbr: "ARS", venue: "H",
    comp: "Premier League", compTag: "PL", date: "Sat 14 Jun",
    ko: "17:30", status: "upcoming",
    score: null,
  },
  {
    id: "psg", opp: "Paris SG", abbr: "PSG", venue: "A",
    comp: "Champions League", compTag: "UCL", date: "Wed 18 Jun",
    ko: "20:00", status: "upcoming",
    score: null,
  },
];

// ---- helpers — every layout shares these so the hard rule lives in one place.

// Given a list of fixtures + a Set of watched ids, derive everything the UI
// needs. The wall = earliest (oldest) played game NOT in watched.
function deriveWall(fixtures, watchedIds) {
  const played = fixtures.filter((f) => f.status !== "upcoming");
  const wall = played.find((f) => !watchedIds.has(f.id)) || null;
  // games hiding news = played games at/after the wall that are unwatched
  const hiding = played.filter((f) => !watchedIds.has(f.id));
  return { wall, hiding, hidingCount: hiding.length };
}

// state of a single fixture for rendering
function fxState(f, watchedIds) {
  if (f.status === "upcoming") return "upcoming";
  return watchedIds.has(f.id) ? "watched" : "unwatched";
}

// score string — ONLY call when you have verified the game is watched.
function scoreStr(f) {
  if (!f.score) return "";
  return f.score.h + "\u2013" + f.score.a; // en dash
}
// result word for a watched LFC game (Liverpool is always one side)
function resultWord(f) {
  if (!f.score) return "";
  const liv = f.venue === "H" ? f.score.h : f.score.a;
  const them = f.venue === "H" ? f.score.a : f.score.h;
  if (liv > them) return "W";
  if (liv < them) return "L";
  return "D";
}

Object.assign(window, { FIXTURES, deriveWall, fxState, scoreStr, resultWord });
