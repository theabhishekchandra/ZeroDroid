# Trending Playbook — ZeroDroid

A concrete, sourced plan to get ZeroDroid onto github.com/trending — built from how the ranking actually works, not folklore.

**Snapshot:** 32 stars · 5 forks · created Mar 2026 · last push Aug 27 · 14 topics set

## How trending actually works

The algorithm itself is closed-source — everything below is inference from public behavior and community write-ups, not a documented spec. Treat thresholds as ballpark, not guarantees.

1. Trending ranks by **stars gained inside a window** — daily, weekly, or monthly — not lifetime total. A repo that jumps from 30→90 stars today outranks one steadily sitting at 5,000. *(OSS Insight; dev.to — Yvonnick Frin)*
2. The page is **filtered by language**. "Kotlin, daily" is a much smaller pond than "All languages, daily" — same star count, wildly different odds. *(GitHub Community Discussion #163970)*
3. It's believed to weigh velocity against a repo's **own recent baseline**, so a quiet repo suddenly spiking counts for more than an already-viral one gaining the same amount. *(GitHub Community Discussion #163970)*
4. Once you land on trending, it's **self-reinforcing** — the page itself is a discovery surface developers browse daily, so visibility compounds beyond the launch push that got you there. *(dev.to — pottekkat; AFFiNE case study)*

## Realistic targets

| Target | Threshold | Notes |
|---|---|---|
| **Kotlin / Android — daily** | ~50–100 stars / 24h | The realistic first target. Far less competition than "All languages," and ZeroDroid's 32-star base makes this jump plausible with one coordinated push. |
| All languages — daily | Several 100s / 24h | The harder bar — what a front-page Show HN or a very strong Product Hunt run can produce. Treat as a stretch outcome, not the plan. |

## The plan

### Phase 0 — Foundation (before you pick a date)

The README already has badges, a demo GIF, and screenshots — that groundwork is done. What's left is closing the gaps that make a first-time visitor bounce instead of starring.

- [x] Badges, demo GIF, screenshots in README — already shipped
- [x] Add a 2-line "why ZeroDroid" hook above the fold — added: "Replaces Termux + a dozen single-purpose scanner apps with one native, offline, permission-scoped toolkit."
- [x] Set the repo's homepage URL field (currently blank) — set to the [latest release page](https://github.com/theabhishekchandra/ZeroDroid/releases/latest)
- [x] Extend topics beyond the current 14 with discovery terms — added `android-app`, `rf-analysis`, `osint`, `pentest-tools` (18 topics total)
- [x] Tag 3–5 "good first issue" items — opened [#25](https://github.com/theabhishekchandra/ZeroDroid/issues/25), [#26](https://github.com/theabhishekchandra/ZeroDroid/issues/26), [#27](https://github.com/theabhishekchandra/ZeroDroid/issues/27) (unit tests for untested pure-logic classes) and [#28](https://github.com/theabhishekchandra/ZeroDroid/issues/28) (localization, from the existing roadmap)
- [x] Confirm the release APK link on the README works end-to-end — `HEAD` on the v1.2.1 asset URL resolves `302 → 200`, link is live

### Phase 1 — Pre-launch (T-7 to T-1)

Everything that needs to exist before launch morning, written and ready to paste — not drafted live under pressure.

- [ ] Pick launch day: a US weekday, Tue–Thu, post 8–10am ET (when Hacker News' front page moves fastest)
- [ ] Draft the Show HN post: *"Show HN: ZeroDroid – turn an Android phone into a portable RF/security lab (29 tools, open source)"* + a first-comment "why I built this"
- [ ] Draft the Product Hunt listing: tagline, gallery (demo GIF first), maker's first comment
- [ ] Draft per-subreddit posts for r/androiddev, r/AndroidApps, r/opensource, r/AskNetsec, r/hacking — read each sub's self-promo rule before posting
- [ ] Line up 5–10 real people (not bots, not purchased stars — see caution below) to check out the repo in the first hour
- [ ] Cut a 20–30s demo clip for X/Twitter and the PH gallery — same GIF as README works fine

### Phase 2 — Launch day (T-0, one 24–48h window)

The whole point is compression: every channel fires inside the same day so star velocity spikes at once instead of trickling across weeks.

- [ ] 8–10am ET — post Show HN
- [ ] Same morning — publish the Product Hunt listing
- [ ] Stagger Reddit posts ~1–2h apart across subs to avoid cross-post spam flags
- [ ] Post to X/Twitter with the demo clip, tag relevant Android/security accounts and communities
- [ ] Stay online — answer every comment and issue within minutes; momentum dies on unanswered questions
- [ ] Watch star velocity against the ~50–100/24h Kotlin target — that's the number that puts you on trending

### Phase 3 — Sustain (T+1 to T+7)

Trending is a spike; contributors and durable listings are what's left after it fades.

- [ ] If it hits trending, screenshot it — becomes social proof for the next wave and future README/PH copy
- [ ] Write a short retro post on what worked — feeds a second traffic wave a week later
- [ ] Triage new issues/PRs fast — converting launch-day visitors into contributors outlasts the star count itself
- [ ] Submit to curated lists (awesome-android, awesome-security) — a durable discovery channel that keeps working after launch day

## Caution

**Don't buy stars or use star-farming services.** GitHub actively detects and strips fake stars from repos, and a caught spike can get a repo flagged or delisted from trending entirely — the opposite of the goal. Every tactic above drives *real* people to star the repo themselves.

## Sources

- OSS Insight — ["We Built a GitHub Trending Page That Actually Uses Data"](https://ossinsight.io/blog/introducing-trending-page)
- GitHub Community Discussions — ["Curious about trending repos calculations" #163970](https://github.com/orgs/community/discussions/163970)
- dev.to / Yvonnick Frin — ["I wonder how GitHub's trending algorithm works"](https://dev.to/yvonnickfrin/i-wonder-how-github-s-trending-algorithm-works-any-clue-3ebf)
- dev.to / pottekkat — ["How we made it to #1 Trending repository in GitHub"](https://dev.to/pottekkat/how-we-made-it-to-1-trending-repository-in-github-291a)
- AFFiNE 33k→60k stars case study — [dev.to / iris1031](https://dev.to/iris1031/how-to-get-more-github-stars-the-definitive-guide-33k-stars-case-study-2kjo)
- HackerNoon — ["The Ultimate Playbook for Getting More GitHub Stars"](https://hackernoon.com/the-ultimate-playbook-for-getting-more-github-stars)
