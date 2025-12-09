# Project Journey Narrative

## Setting the stage
I jumped into this API monitoring project with zero Kotlin experience and very little Spring Boot background. The dashboard was built in Next.js, the backend collector lived in Kotlin/Spring, and the data layer ran inside two MongoDB containers. My first days were spent reading every README snippet, poking through the Kotlin codebase, and cross-referencing Spring documentation just to understand how the pieces talked to one another.

## Early structure & milestones
I carved the work into milestones so the scope felt manageable:
1. **Documentation refresh** – rewrite `README.md` in a clean, recruiter-friendly format while keeping the existing sections intact.
2. **Environment sanity** – get Docker containers, PowerShell scripts, the Kotlin collector, and the Next.js dashboard all running together.
3. **Debugging & data flow** – investigate the 403s hitting `/logs`, build a reliable data seeding story, and prove the dashboard could surface alerts and issues.
4. **Demo playbook** – capture every command, URL, and script run into `instructions.txt` so I could rehearse a demo without guesswork.
5. **Polish & storytelling** – explain the “why” behind upserts, the difference between dashboard alerts and open issues, and outline cleanup strategies for stale data.

## Diving into the backend
Since Kotlin was new territory, I methodically walked through controller classes, DTOs, and security configs. I leaned heavily on official Kotlin and Spring docs to decode unfamiliar syntax (e.g., coroutine builders, data classes, annotation-driven security). I kept notes on what each endpoint expected, which JWT roles it checked, and how MongoDB collections were indexed.

## The 403 saga
Once the stack was running, every POST to `/logs` came back with HTTP 403. I traced the request path in Spring Security and realized the “admin” token lacked the `ROLE_INGESTOR` authority. That was my first “newbie in Kotlin” moment—I confirmed the behavior by reading the `SecurityConfig` and the custom JWT filter. Short-term fix: bypass the collector by seeding data straight into Mongo. Longer-term note-to-self: create a new token or relax the auth rules if I ever need to ingest through the API.

## PowerShell gymnastics & Mongo seeding
Working from Windows PowerShell meant a lot of quoting battles. I learned that `$` escapes inside double quotes and Mongo shell commands need careful quoting to avoid "Invalid regular expression flag" errors. After several failed attempts, I found a reliable pattern:
- Call `docker exec` against the logs and meta containers.
- Provide the full Mongo URI with credentials in the command.
- Wrap JSON payloads in single quotes so PowerShell leaves them alone.

With that pattern, I inserted realistic logs (`slow`, `broken`, and `rate-limited` endpoints) and then upserted matching alert documents in the meta database. Each command responded with `acknowledged: true`, which I verified by running the bundled `dump-logs.ps1` script and direct `db.alerts.find().toArray()` queries.

## Documentation marathon
I rewrote `README.md` from scratch while preserving the original sections, explaining the project, scripts, auth model, and demo flow in plain language. Then I expanded `instructions.txt` into a step-by-step playbook covering:
- Starting the stack (`start-all.ps1`).
- Running the Next.js dashboard, Kotlin collector, and Mongo containers together.
- Seeding demo data through `docker exec ... mongosh` commands.
- Visiting each dashboard panel and what story to tell recruiters.
- How to shut everything down cleanly.

## Explaining the system to myself (and recruiters)
As I practiced the demo, I documented why alerts use `updateOne(..., { upsert: true })`: it keeps a single record per endpoint/issue and updates timestamps whenever the condition resurfaces. I clarified that the dashboard’s **Open Issues** panel filters alerts that are unresolved or recently updated, while the **Alerts** feed shows the raw stream of conditions.

I also addressed stale data. Some seeded alerts had week-old timestamps, so I wrote cleanup commands (`updateMany` to bump `updatedAt`, or `deleteMany` to purge) and noted that the dashboard sorts by freshness.

## The catastrophic reset
Somewhere in the middle of all this, I ran a Git command that nuked every file in the repo (thanks, past me). That forced a hard reset of the environment. I re-cloned the project, reinstalled dependencies, and replayed every documentation edit and seeding step from scratch. Painful, but now I have muscle memory for the setup and a great story about resilience—including the reminder to double-check destructive Git commands.

## Final state & reflections
- The stack runs cleanly on Windows via PowerShell scripts and Docker.
- Mongo is seeded with demo logs/alerts that light up the dashboard.
- Both `README.md` and `instructions.txt` read like recruiter-ready documentation.
- I understand the Kotlin/Spring security flow well enough to explain the 403s and role-based auth.
- I can articulate why upserts matter, how the dashboard surfaces issues, and how to keep demo data fresh.
- I survived a full repo wipe, rebuilt everything, and documented the entire process for others.

What started as “I don’t know Kotlin” turned into a crash course in Spring security, Mongo seeding, and Windows-friendly tooling. The journey showcases persistence, fast learning, and clean communication—exactly the narrative I want recruiters to see.
