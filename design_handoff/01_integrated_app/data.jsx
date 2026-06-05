// data.jsx — feed content for A Little Bird (LFC transfer & backroom news)
const FEED = [
  {
    id: "mid", tier: 1, source: "The Athletic", author: "David Ornstein", role: "The Athletic",
    cat: "Transfer", time: "8m", confirmed: true, match: false, image: true,
    headline: "Liverpool agree personal terms with target midfielder",
    lead: "Liverpool have reached full agreement on personal terms with their priority midfield target, with the player set to sign a five-year contract.",
    paras: [
      "Talks between the two clubs over a structured fee have advanced quickly in the last 48 hours, and sources indicate the framework of a deal is now in place pending the exchange of final documents.",
      "A medical is expected to be scheduled before the end of the week. The midfielder has already informed his current club of his desire to move to Anfield.",
    ],
    corrob: [
      { tier: 1, name: "Fabrizio Romano" },
      { tier: 2, name: "Liverpool Echo" },
      { tier: 2, name: "This Is Anfield" },
    ],
  },
  {
    id: "ft", tier: 1, source: "Liverpool FC", author: "Liverpool FC", role: "Official club",
    cat: "Match", time: "14m", confirmed: false, match: true, game: "che", image: false,
    headline: "Full-time report: official match summary now live",
    lead: "The club's official full-time report covering the result, goals and key moments is now available.",
    paras: ["This article contains the final score and a breakdown of how the match unfolded."],
    corrob: null,
  },
  {
    id: "rb", tier: 1, source: "Fabrizio Romano", author: "Fabrizio Romano", role: "Transfer reporting",
    cat: "Transfer", time: "19m", confirmed: true, match: false, image: true,
    headline: "Right-back deal agreed in full — medical booked for Monday",
    lead: "A full agreement has been struck for the right-back, with the medical scheduled early next week ahead of an announcement.",
    paras: [
      "Personal terms were never an issue and the clubs have now settled on a fee structure including add-ons.",
      "The player will travel for his medical on Monday, with the club hoping to confirm the signing within days.",
    ],
    corrob: [
      { tier: 1, name: "David Ornstein" },
      { tier: 2, name: "Liverpool Echo" },
    ],
  },
  {
    id: "match1", tier: 2, source: "BBC Sport", author: "BBC Sport", role: "Match coverage",
    cat: "Match", time: "22m", confirmed: false, match: true, game: "eve", image: false,
    headline: "Match report: full-time ratings & tactical breakdown",
    lead: "A full-time report covering the result, key moments and player performances.",
    paras: ["This article contains the final score and match details."],
    corrob: null,
  },
  {
    id: "live", tier: 2, source: "Liverpool Echo", author: "Liverpool Echo", role: "Live coverage",
    cat: "Match", time: "28m", confirmed: false, match: true, game: "new", image: false,
    headline: "Live: second-half flashpoints and key incidents",
    lead: "Rolling live coverage of the second half, including chances, bookings and tactical changes.",
    paras: ["This live blog reveals the current scoreline and how the game is developing."],
    corrob: null,
  },
  {
    id: "capt", tier: 1, source: "The Athletic", author: "David Ornstein", role: "The Athletic",
    cat: "Contract", time: "33m", confirmed: true, match: false, image: false,
    headline: "Captain on brink of new extension — here we go",
    lead: "The club captain is closing in on a new long-term contract, with all parties aligned and an announcement imminent.",
    paras: [
      "Negotiations have been straightforward, reflecting a mutual desire to continue, and only the final formalities remain.",
    ],
    corrob: [
      { tier: 1, name: "Fabrizio Romano" },
      { tier: 2, name: "This Is Anfield" },
    ],
  },
  {
    id: "coach", tier: 2, source: "This Is Anfield", author: "This Is Anfield", role: "Beat reporting",
    cat: "Coaching", time: "41m", confirmed: false, match: false, image: false,
    headline: "New set-piece coach expected to join the backroom staff",
    lead: "Liverpool are finalising the appointment of a dedicated set-piece coach as part of an ongoing reshape of the first-team backroom.",
    paras: [
      "The role has grown in importance across the Premier League, and the club have identified a specialist with a strong record of improving dead-ball returns.",
      "An announcement is anticipated once the wider staff structure for the season is confirmed.",
    ],
    corrob: null,
  },
  {
    id: "sd", tier: 1, source: "Liverpool FC", author: "Liverpool FC", role: "Official club",
    cat: "Backroom", time: "52m", confirmed: true, match: false, image: false,
    headline: "Club confirms new sporting director appointment",
    lead: "Liverpool have officially announced the appointment of a new sporting director to lead football operations.",
    paras: [
      "The appointment follows an extensive search and is expected to shape recruitment strategy for the seasons ahead.",
    ],
    corrob: [
      { tier: 1, name: "Liverpool FC (Official)" },
    ],
  },
  {
    id: "contract", tier: 1, source: "Liverpool FC", author: "Liverpool FC", role: "Official club",
    cat: "Contract", time: "1h", confirmed: true, match: false, image: false,
    headline: "Academy graduate signs new long-term deal",
    lead: "The club has officially confirmed that one of its brightest academy graduates has committed his future by signing a new long-term contract.",
    paras: [
      "The 19-year-old has progressed through every age group and made his senior debut last season. The new deal rewards a breakthrough year and reflects the club's confidence in his development.",
    ],
    corrob: [
      { tier: 1, name: "Liverpool FC (Official)" },
      { tier: 2, name: "Liverpool Echo" },
    ],
  },
  {
    id: "match2", tier: 2, source: "The Athletic", author: "The Athletic", role: "Match coverage",
    cat: "Match", time: "1h", confirmed: false, match: true, game: "che", image: false,
    headline: "Player ratings: who shone and who struggled",
    lead: "Individual performance ratings from the latest fixture.",
    paras: ["This article reveals how each player performed and implies the result."],
    corrob: null,
  },
  {
    id: "keeper", tier: 2, source: "Liverpool Echo", author: "Liverpool Echo", role: "Beat reporting",
    cat: "Transfer", time: "1h", confirmed: false, match: false, image: false,
    headline: "Echo: Reds open talks over experienced backup keeper",
    lead: "Liverpool have opened early discussions over an experienced goalkeeper to provide senior cover next season.",
    paras: [
      "The club are keen to add depth in the position and view the target as a reliable, low-risk option.",
    ],
    corrob: null,
  },
  {
    id: "asst", tier: 2, source: "This Is Anfield", author: "This Is Anfield", role: "Beat reporting",
    cat: "Coaching", time: "2h", confirmed: false, match: false, image: false,
    headline: "Assistant boss agrees to extend his Anfield stay",
    lead: "A key member of the coaching team is set to extend his stay after agreeing fresh terms with the club.",
    paras: [
      "The continuity is seen as important to the manager as preparations for the new campaign continue.",
    ],
    corrob: null,
  },
  {
    id: "rumor", tier: 3, source: "Daily Mirror", author: "Daily Mirror", role: "Aggregated speculation",
    cat: "Transfer", time: "2h", confirmed: false, match: false, image: false,
    headline: "Reds 'eyeing' shock swoop for La Liga forward, claim reports",
    lead: "Speculation links Liverpool with a surprise move for a forward currently playing in Spain.",
    paras: [
      "The report cites unnamed sources and offers no figures or timeline. There has been no corroboration from reliable journalists, and the story should be treated as speculative.",
    ],
    corrob: null,
  },
  {
    id: "fitness", tier: 2, source: "Liverpool Echo", author: "Liverpool Echo", role: "Beat reporting",
    cat: "Backroom", time: "3h", confirmed: false, match: false, image: false,
    headline: "New head of fitness lined up in staff reshuffle",
    lead: "The club is reshaping its sports-science department, with a new head of fitness expected to be confirmed.",
    paras: ["The reshuffle follows a review of the physical-performance setup carried out over the summer."],
    corrob: null,
  },
  {
    id: "u21", tier: 2, source: "This Is Anfield", author: "This Is Anfield", role: "Academy reporting",
    cat: "Academy", time: "3h", confirmed: false, match: false, image: false,
    headline: "U21 playmaker rewarded with first professional deal",
    lead: "A standout U21 playmaker has signed his first professional contract after an impressive year in the youth ranks.",
    paras: ["The deal caps a strong season and keeps one of the academy's brightest talents on the books."],
    corrob: null,
  },
  {
    id: "academy", tier: 2, source: "This Is Anfield", author: "This Is Anfield", role: "Beat reporting",
    cat: "Academy", time: "4h", confirmed: false, match: false, image: false,
    headline: "Highly-rated U18 striker promoted to first-team training",
    lead: "A standout young striker has been promoted to train with the senior squad after a prolific run at youth level.",
    paras: ["Coaches have been impressed by his finishing and movement, and the step up is seen as a reward for his form."],
    corrob: null,
  },
  {
    id: "winger", tier: 3, source: "TuttoMercato", author: "TuttoMercato", role: "Aggregated speculation",
    cat: "Transfer", time: "5h", confirmed: false, match: false, image: false,
    headline: "Italian outlet claims surprise interest in Serie A winger",
    lead: "An Italian outlet suggests Liverpool are among clubs monitoring a Serie A winger, though details are thin.",
    paras: [
      "No fee, timeline or corroboration accompanies the claim, and it should be treated as early-window speculation.",
    ],
    corrob: null,
  },
];

const TIER_META = {
  1: { label: "RELIABLE", cls: "t1" },
  2: { label: "BEAT", cls: "t2" },
  3: { label: "SPECULATIVE", cls: "t3" },
};

// News-type groups — used by every filter pattern (tabs, chips, sheet).
// "all" is the catch-all feed; the rest map to one or more raw categories.
const NEWS_TYPES = [
  { key: "all", label: "All news", short: "All", cats: null },
  { key: "transfer", label: "Transfers", short: "Transfers", cats: ["Transfer"] },
  { key: "match", label: "Match news", short: "Match", cats: ["Match"] },
  { key: "staff", label: "Squad & staff", short: "Squad & staff", cats: ["Coaching", "Backroom"] },
  { key: "contract", label: "Contracts", short: "Contracts", cats: ["Contract"] },
  { key: "academy", label: "Academy", short: "Academy", cats: ["Academy"] },
];

// Tier filter modes shared by all patterns.
const TIER_MODES = [
  { key: "all", label: "All tiers", tiers: [1, 2, 3] },
  { key: "t12", label: "T1 · T2", tiers: [1, 2] },
  { key: "t1", label: "T1 only", tiers: [1] },
];

Object.assign(window, { FEED, TIER_META, NEWS_TYPES, TIER_MODES });
