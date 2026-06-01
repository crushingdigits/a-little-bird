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
    id: "match1", tier: 2, source: "BBC Sport", author: "BBC Sport", role: "Match coverage",
    cat: "Match", time: "22m", confirmed: false, match: true, image: false,
    headline: "Match report: full-time ratings & tactical breakdown",
    lead: "A full-time report covering the result, key moments and player performances.",
    paras: ["This article contains the final score and match details."],
    corrob: null,
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
    cat: "Match", time: "1h", confirmed: false, match: true, image: false,
    headline: "Player ratings: who shone and who struggled",
    lead: "Individual performance ratings from the latest fixture.",
    paras: ["This article reveals how each player performed and implies the result."],
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
    id: "academy", tier: 2, source: "This Is Anfield", author: "This Is Anfield", role: "Beat reporting",
    cat: "Academy", time: "4h", confirmed: false, match: false, image: false,
    headline: "Highly-rated U18 striker promoted to first-team training",
    lead: "A standout young striker has been promoted to train with the senior squad after a prolific run at youth level.",
    paras: ["Coaches have been impressed by his finishing and movement, and the step up is seen as a reward for his form."],
    corrob: null,
  },
];

const TIER_META = {
  1: { label: "RELIABLE", cls: "t1" },
  2: { label: "BEAT", cls: "t2" },
  3: { label: "SPECULATIVE", cls: "t3" },
};

Object.assign(window, { FEED, TIER_META });
