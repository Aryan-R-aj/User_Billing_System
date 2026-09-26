# Usage & Billing System for a Resource

A Java desktop application (Swing UI + embedded SQLite database) that manages
shared, capacity-limited resources (meeting rooms, gym equipment, paid
workstations, etc.), tracks usage sessions, enforces capacity limits, and
generates bills based on configurable hourly pricing rules.

Built for the "Assignment - Usage & Billing System for a Resource" spec:
manual logic only, no frameworks or external libraries beyond a JDBC driver
for the database.

---

## 1. Data Structures Used

The system is organized in four layers: **model → dao → service → ui**.

### Model classes (`com.ubs.model`)

| Class | Represents | Key fields |
|---|---|---|
| `Resource` | A shared facility/entity with fixed capacity | `id, name, category, capacity, createdAt`, plus a computed `activeCount` (not stored — derived live from active sessions) |
| `ServicePlan` | A pricing plan attached to a resource | `id, resourceId, serviceName, firstHourPrice, additionalHourPrice` |
| `UsageSession` | One occupancy of a resource by a user, start→stop | `id, resourceId, serviceId, userName, startTime, endTime, status (ACTIVE/COMPLETED)` |
| `Bill` | An immutable, generated invoice for a completed session | `id, sessionId, resourceName, serviceName, userName, startTime, endTime, durationMinutes, billedHours, amount, generatedAt` |

These mirror exactly how the same information would be normalized in a
relational database (see schema below) — each model is a plain Java object
(no ORM), and the DAO layer hand-writes the SQL to move data between the
objects and the tables.

### Database schema (SQLite, created automatically on first run)

```
resources(id PK, name UNIQUE, category, capacity, created_at)
services(id PK, resource_id FK -> resources, service_name, first_hour_price, additional_hour_price)
usage_sessions(id PK, resource_id FK, service_id FK, user_name, start_time, end_time, status)
bills(id PK, session_id FK, resource_name, service_name, user_name, start_time, end_time,
      duration_minutes, billed_hours, amount, generated_at)
```

* A resource's **current occupancy** is never stored as a counter column;
  it is always computed on the fly as
  `COUNT(*) FROM usage_sessions WHERE resource_id = ? AND status = 'ACTIVE'`.
  This avoids any risk of a stored counter drifting out of sync with reality.
* `bills` intentionally denormalizes resource/service names (rather than only
  storing foreign keys) so that historical bills remain fully readable even
  if a resource or service is later renamed or deleted.
* Foreign keys use `ON DELETE CASCADE` so deleting a resource cleanly removes
  its services/sessions/bills (the UI asks for confirmation first).

### In-memory structures (runtime only)

* `UsagePanel` keeps a `Map<sessionId, UsageSession>` of currently active
  sessions and a `Map<serviceId, ServicePlan>` pricing cache, refreshed from
  the DB on every action and used by a 1-second Swing `Timer` to render a
  **live elapsed-time stopwatch and running cost estimate** per active
  session without hitting the database every tick.

---

## 2. Logic and Approach

### Capacity enforcement (start usage)
`BillingService.startUsage()` runs the capacity check and the session insert
inside a **single JDBC transaction** (`setAutoCommit(false)` + `commit()`/
`rollback()`), and the method itself is `synchronized`. This guarantees that
two "start usage" requests can never both slip through once a resource is
already at full capacity — the check and the write are atomic.

Flow: look up the resource → count its currently `ACTIVE` sessions → if
`activeCount >= capacity`, roll back and throw `BillingException` (shown to
the user as a rejection message) → otherwise insert a new `ACTIVE`
`usage_sessions` row and commit.

### Billing calculation (stop usage)
`BillingService.stopUsage()`:
1. Loads the active session; rejects if it isn't active.
2. Records `endTime = now()` and marks the session `COMPLETED` (this
   immediately frees the resource slot for new requests).
3. Computes `Duration.between(startTime, endTime)`.
4. **Rounds up to the next full hour**: any usage beyond a whole hour counts
   as another full billed hour (`Math.ceil(totalSeconds / 3600.0)`), exactly
   as specified. A session of 1h00m00s bills as 1 hour; 1h00m01s bills as 2
   hours; a session of a few seconds still bills a minimum of 1 hour (there
   is no "0-hour" tier defined in the pricing rules).
5. `amount = firstHourPrice + max(0, billedHours - 1) * additionalHourPrice`.
6. Persists an immutable `Bill` row and returns it to the UI, which shows it
   in a summary dialog.

This matches the worked example in the assignment exactly:
**10:00–11:20 (1h20m) → rounded up to 2h → ₹30 + ₹10 = ₹40**, which is
covered by an automated check (see Testing below).

### Layering
* `dao/` — one class per table, plain JDBC (`PreparedStatement`), no ORM.
* `service/BillingService` — the only place business rules live; DAOs never
  enforce capacity or pricing themselves, so the rules are defined once.
* `ui/` — five tabs (Dashboard, Resources, Services & Pricing, Start/Stop
  Usage, Bill History), each a self-contained `JPanel` that calls the
  service/DAO layer and re-renders itself; `MainFrame.refreshAll()` keeps
  every tab in sync after any change.

---

## 3. "Plus point" additions beyond the minimum spec

* **Full Swing UI** across 5 tabs, with a shared theme (colors, spacing,
  button styles) rather than raw default Swing look.
* **SQLite database** (not just in-memory) — persists between runs.
* **Live dashboard**: total resources, current occupancy, bills generated,
  total revenue, plus a per-resource occupancy card with a progress bar.
* **Live "Active Usage" view**: a ticking stopwatch and a running cost
  estimate (recomputed every second client-side) for every session still in
  progress, so a user can see the bill growing in real time before they stop
  it.
* **Search/filter and CSV export** on the Bill History tab.
* **Transactional capacity enforcement** (see above) to close a race
  condition that a naive "check-then-insert" implementation would have.
* Input validation and confirmation dialogs throughout (duplicate resource
  names rejected, delete confirmations, clear rejection messages when a
  resource is full or a service/plan is missing).

---

## 4. How to Run

### Requirements
* Java 17 or newer. The `dist/usage-billing-system.jar` is compiled targeting
  Java 17 bytecode specifically, so it runs on Java 17 through the latest
  release without needing to locate a newer JDK.
* No installation of Maven/Gradle needed — everything is pre-built into a
  runnable jar. No internet connection is required to run it.

### Option A — Run the pre-built jar (fastest)
```
cd dist
java -jar usage-billing-system.jar
```
or double-click `run.bat` (Windows) / run `./run.sh` (Linux/macOS).

The `lib/` folder next to the jar contains the only two dependencies used:
`sqlite-jdbc` (the JDBC driver needed to talk to the SQLite file — see
"Technical assumptions" below) and its logging dependency `slf4j`. The jar's
manifest already points to them (`Class-Path: lib/...`), so nothing else
needs to be installed.

A file named `usage_billing.db` will be created in the current directory on
first launch — this **is** the database; delete it to reset all data.

### Option B — Compile from source
```
cd src
javac -d ../build -cp "../lib/sqlite-jdbc.jar" $(find . -name "*.java")
cd ..
java -cp "build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar:lib/slf4j-simple.jar" com.ubs.Main
```
(On Windows, replace `:` with `;` in `-cp` and use `dir /s /b *.java` in
place of `find`.)

### Using the app
1. **Resources tab** — add a resource (e.g. "Meeting Room A", capacity 2).
2. **Services & Pricing tab** — add a pricing plan for it (e.g. "Hourly
   Usage", first hour ₹30, additional hour ₹10).
3. **Start / Stop Usage tab** — pick the resource + service, enter a user
   name, click **Start Usage**. Repeat until the resource is full — a further
   attempt is rejected with a clear message.
4. Select an active row and click **Stop Selected & Generate Bill** — a bill
   summary dialog appears and the slot is freed immediately.
5. **Bill History tab** — see every generated bill, search by resource /
   service / user, and export the currently visible rows to CSV.
6. **Dashboard tab** — live totals and per-resource occupancy at a glance.

---

## 5. Testing

A standalone smoke test (`SmokeTest.java`, not part of the shipped app) was
used during development to verify the business rules end-to-end against a
real SQLite database:

* Creates a resource with capacity 2 and a plan (₹30 first hour / ₹10 each
  additional hour).
* Starts 2 sessions successfully, a 3rd is correctly **rejected** as the
  resource is full.
* Backdates a session's start time by 80 minutes and stops it →
  **confirms the bill is exactly ₹40 with 2 billed hours**, matching the
  assignment's worked example precisely.
* Stops the second session immediately → confirms it bills the **minimum of
  1 hour (₹30)**.
* Confirms the resource becomes available again (a new session can start)
  as soon as a slot is freed.

To re-run it yourself:
```
javac -d /tmp -cp "build:lib/sqlite-jdbc.jar" SmokeTest.java
java -cp "/tmp:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar:lib/slf4j-simple.jar" SmokeTest
```
Beyond that, the full flow (add resource → add service → start usage →
capacity rejection → stop usage → bill generated → CSV export) was manually
exercised through the UI described above.

---

## 6. Important Technical Assumptions

* **JDBC driver as the sole dependency.** The assignment forbids frameworks
  and external libraries, but also explicitly encourages using a real
  database as a "plus point." Java has no built-in embedded SQL database, so
  a minimal JDBC driver (`sqlite-jdbc`, plus its `slf4j` logging dependency)
  is used purely as the *connector* between Java and SQLite — no ORM,
  query-builder, or framework of any kind is used; every SQL statement in
  `dao/` is written by hand with `PreparedStatement`. If a stricter
  "zero-dependency" interpretation is required, the same `dao` interfaces
  could be re-implemented over an in-memory `HashMap`-based store with no
  code changes needed elsewhere (`BillingService` and the UI depend only on
  the DAO method signatures, not on JDBC directly).
* **Billing rounds up to a minimum of 1 hour.** The spec doesn't define a
  price for usage under 1 hour, so any recorded usage — even a few seconds
  — is billed as a full first hour, consistent with "any usage beyond a full
  hour must be rounded up."
  * A resource cannot be used before it has at least one pricing plan
    defined; the Start Usage form makes this explicit rather than allowing
    an ambiguous free session.
* **Single JVM, single database file.** Concurrent access is handled with
  Java-level `synchronized` methods plus SQLite transactions, which is
  correct for a single running instance of the app (as intended for this
  assignment) but would need a client-server database for true multi-process
  concurrency.
* **Time zone / clock**: all timestamps use the machine's local
  `LocalDateTime.now()`; no timezone conversion is performed.
* **Resource names must be unique** (case-insensitive) — enforced both at
  the UI level and with a `UNIQUE` DB constraint.

---

## 7. Project Structure

```
src/com/ubs/
  Main.java                      - entry point
  model/                         - Resource, ServicePlan, UsageSession, Bill
  dao/                           - DatabaseManager, ResourceDAO, ServiceDAO,
                                    UsageSessionDAO, BillDAO
  service/                       - BillingService, BillingException
  ui/                            - MainFrame, DashboardPanel, ResourcePanel,
                                    ServicePanel, UsagePanel, BillHistoryPanel,
                                    UITheme
lib/                              - sqlite-jdbc.jar, slf4j-api.jar, slf4j-simple.jar
dist/                             - pre-built runnable jar + run.sh / run.bat
```
