# SJBIT Library Management System — Java (Spring Boot) Edition

This is your original frontend (index/student/librarian/staff/admin HTML,
same CSS, same design) now wired to a **real Java backend**:

- **Spring Boot** (REST API)
- **MySQL** (real relational database via Spring Data JPA/Hibernate)
- **Spring Mail** — real emails via Gmail SMTP for borrow / return / overdue events
- **OpenPDF** — generates a real PDF borrow slip on every issue, saved to disk
  and recorded permanently in the `borrow_slips` table (digital college record)
- **Spring `@Scheduled`** — a daily job that automatically recalculates fines
  for overdue books and sends reminder emails

The frontend files (`index.html`, `student.html`, etc., `css/style.css`,
`assets/*.jpg`) are **byte-for-byte the same design** — only `js/script.js`
changed, because it now calls the REST API instead of `localStorage`.

---

## 1. Prerequisites

- **JDK 17+** — check with `java -version`
- **Maven 3.8+** — check with `mvn -version` (or use your IDE's bundled Maven)
- **MySQL 8+** running locally (or update the URL in `application.properties`
  to point at any MySQL server you have)
- A **Gmail account** for sending real emails

## 2. Set up MySQL

You don't need to manually create tables — Hibernate does that for you
(`spring.jpa.hibernate.ddl-auto=update`). You only need the database itself
to exist. Open a MySQL shell / Workbench and run:

```sql
CREATE DATABASE IF NOT EXISTS sjbit_library;
```

Or just leave it — `application.properties` already has
`createDatabaseIfNotExist=true` in the JDBC URL, so it'll create it for you
on first connect, as long as the MySQL **server** itself is running and your
username/password are correct.

## 3. Configure `src/main/resources/application.properties`

Open the file and fill in **three things**:

```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD      # <-- your real MySQL password

spring.mail.username=your_email@gmail.com            # <-- your Gmail address
spring.mail.password=your_16_char_app_password       # <-- Gmail App Password (see step 4)
library.mail.from=your_email@gmail.com
```

## 4. Generate a Gmail App Password (for real email sending)

Gmail no longer allows sending via plain passwords from apps. You need an
"App Password":

1. Go to your Google Account → **Security**
2. Turn on **2-Step Verification** if it isn't already on
3. Go to https://myaccount.google.com/apppasswords
4. Create an app password (name it e.g. "SJBIT Library"), select "Mail" as
   the app
5. Google gives you a **16-character password** like `abcd efgh ijkl mnop`
   — copy it **without spaces** into `spring.mail.password` above

If you skip this step, set `library.mail.enabled=false` in
`application.properties` — emails will just be printed to the console
instead of actually sent, so the rest of the app still works while you sort
out Gmail.

## 5. Run it

From the `library/` folder (where `pom.xml` is):

```bash
mvn clean install
mvn spring-boot:run
```

Or build a runnable jar and run that directly:

```bash
mvn clean package
java -jar target/library.jar
```

Then open: **http://localhost:8080**

That's it — Spring Boot serves both the frontend (static HTML/CSS/JS) *and*
the REST API from the same port, so there's no separate frontend server and
no CORS issues.

## 6. Demo data

On first run, the app auto-seeds the same 5 demo books (Algorithms, Digital
Logic Design, Fluid Mechanics, Structural Analysis, Database Systems) and 3
demo purchase requests your original localStorage version had — so it looks
identical the first time you open it. No demo *members* are seeded (same as
before) — sign up your own accounts via the "Create Account" modal.

**Important:** when signing up, you'll be asked for a real email address —
that's where borrow/return/overdue emails actually get sent, so use an
email you can check.

---

## What's now real (vs. the old localStorage version)

| Feature | Before | Now |
|---|---|---|
| Data storage | Browser `localStorage` | MySQL (persists across devices/browsers) |
| Borrow confirmation | none | Real email via Gmail SMTP |
| Return confirmation | none | Real email via Gmail SMTP |
| Overdue reminders | none | Daily scheduled job + email |
| Fine calculation | Recomputed on page view only | Calculated at return time **and** automatically kept up-to-date daily for books still out |
| Borrow slip | Print-only popup, nothing saved | Real PDF saved to disk (`/slips` folder) **and** a permanent DB row (`borrow_slips` table) — satisfies both the student's copy and the college's record |
| Admin overview | Read from localStorage | Live query against the real DB (`/api/admin/overview`) |

## Project structure

```
library/
├── pom.xml
├── application.properties setup is inside src/main/resources
├── src/main/java/com/sjbit/library/
│   ├── entity/        JPA entities (User, Book, BookCopy, BorrowRecord, PurchaseRequest, Notification, BorrowSlip)
│   ├── repository/    Spring Data JPA repositories
│   ├── service/        business logic (borrow/return/fine/email/PDF slip generation)
│   ├── controller/     REST endpoints (/api/auth, /api/books, /api/borrow, /api/purchase, /api/admin, /api/notifications)
│   ├── scheduler/       FineScheduler — daily overdue job
│   └── config/          DataSeeder (demo data), GlobalExceptionHandler
├── src/main/resources/
│   ├── application.properties
│   └── static/          index.html, student.html, librarian.html, staff.html, admin.html, css/, js/, assets/
└── slips/                generated borrow-slip PDFs land here at runtime
```

## Notes / things worth knowing for your viva / placement round

- **Fine rate & borrow duration** are configurable in `application.properties`
  (`library.fine.per-day`, `library.borrow.duration-days`) — not hardcoded,
  good talking point ("externalized configuration").
- **Password storage is plaintext** in this version, same as the original —
  fine for a college project demo, but if asked, mention that a production
  system would hash passwords with BCrypt (`spring-boot-starter-security`
  gives you `BCryptPasswordEncoder` for free).
- **Session/login** is still tracked client-side (in the browser, just to
  remember who's logged in for the UI) — the actual data (books, borrow
  records, fines) all lives server-side in MySQL now. If you want proper
  server-side sessions or JWT-based auth next, that's a natural "Phase 2"
  extension to mention if asked about scaling it further.
- **Borrow request → approval flow** is intentionally two-step (student
  requests, librarian approves) — matches a real library workflow and gives
  you something to explain about state machines (`PENDING → APPROVED →
  RETURNED`).
