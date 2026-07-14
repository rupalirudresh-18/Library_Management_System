# 📚 SJBIT Library Management System

A full-stack Library Management System built with **Java Spring Boot**, **MySQL**, and a custom HTML/CSS/JS frontend — built as a placement-ready project demonstrating real-world backend engineering: REST APIs, relational data modeling, automated email notifications, PDF generation, and scheduled background jobs.

🔗 **Live Demo:** 
https://librarymanagementsystem-production-03c7.up.railway.app 

---

## ✨ Features

- **Role-based system** — Student, Librarian, Staff, and Admin, each with a dedicated dashboard
- **Borrow / Return workflow** — student requests a book → librarian approves/rejects → automatic due-date assignment
- **Automatic email notifications** (via Gmail SMTP) on borrow, return, and daily overdue reminders
- **Automatic fine calculation** — computed at return time and kept continuously up to date for overdue books via a scheduled daily job
- **Digital borrow slips** — a real PDF is generated on every issue, emailed to the student as an attachment, and permanently logged in the database for institutional records
- **Live admin analytics dashboard** — real-time charts (Books by Branch, Borrow Status Breakdown, Members by Role, Fines Collected vs Pending) powered by Chart.js
- **Member management** — clickable member profiles showing full borrow history, fee status, and contact details for students; basic info for staff
- **Purchase request workflow** — staff raises requests → admin approves → status tracked through Pending/Approved tabs visible to both roles
- **Inventory management** — per-copy availability tracking, automatically updated by borrow/return actions (not manual toggles)

## 🛠️ Tech Stack

**Backend:** Java 17, Spring Boot 3, Spring Data JPA (Hibernate), Spring Mail, Spring Scheduler
**Database:** MySQL
**PDF Generation:** OpenPDF
**Frontend:** HTML, CSS, vanilla JavaScript (fetch-based REST calls)
**Deployment:** Railway (app + managed MySQL)
**Build Tool:** Maven

## 🏗️ Architecture
Browser (HTML/CSS/JS)
│  REST (fetch/JSON)
▼

Spring Boot REST API
├── Controllers   → /api/auth, /api/books, /api/borrow, /api/purchase, /api/admin, /api/notifications
├── Services      → business logic (fines, emails, PDF slips, scheduling)
├── Repositories   → Spring Data JPA
└── Entities       → User, Book, BookCopy, BorrowRecord, PurchaseRequest, Notification, BorrowSlip
│▼
MySQL
## 📋 Core Workflows

**Borrowing a book:**
`Student requests → Librarian approves → Due date set + copy marked unavailable → PDF slip generated → Email sent (with PDF attached)`

**Returning a book:**
`Student/Librarian marks returned → Fine auto-calculated from days late → Copy marked available again → Email sent`

**Overdue handling:**
`Daily scheduled job recalculates fines for all overdue books still checked out → sends reminder email (once per day per book)`


## 👤 Author

Rupali 
