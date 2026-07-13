// ========== script.js (Java/Spring Boot backend version) ==========
// SJBIT Library Management System - same frontend, now backed by a real
// Spring Boot + MySQL REST API instead of localStorage.
// Session (who's logged in) is still kept client-side in localStorage —
// that's just UI state, not library data — everything else (books,
// borrow records, members, purchase requests, fines, notifications)
// now lives in MySQL and is fetched over HTTP.
// ================================================================

const API = ""; // same-origin; Spring Boot serves this page AND the /api/* endpoints
const LS = { SESSION: "sjbit_session_v1" };

/* ---------- tiny fetch helpers ---------- */
async function apiGet(path) {
  const res = await fetch(API + path);
  if (!res.ok) throw new Error((await safeJson(res))?.error || "Request failed");
  return safeJson(res);
}
async function apiSend(path, method, body) {
  const res = await fetch(API + path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body !== undefined ? JSON.stringify(body) : undefined
  });
  if (!res.ok) throw new Error((await safeJson(res))?.error || "Request failed");
  return safeJson(res);
}
async function safeJson(res) {
  const text = await res.text();
  if (!text) return null;
  try { return JSON.parse(text); } catch (e) { return null; }
}
const apiPost = (p, b) => apiSend(p, "POST", b);
const apiPut = (p, b) => apiSend(p, "PUT", b);
const apiDelete = (p) => apiSend(p, "DELETE");

/* ---------- generic helpers ---------- */
function fmtDate(iso){ return iso ? iso.slice(0,10) : "-"; }
function daysBetween(aIso,bIso){
  if(!aIso || !bIso) return 0;
  const a = new Date(aIso), b = new Date(bIso);
  return Math.ceil((b - a) / (1000*60*60*24));
}
const $ = (s,p=document) => p.querySelector(s);
const $$ = (s,p=document) => Array.from(p.querySelectorAll(s));
const el = (t, props={}, html="") => { const e=document.createElement(t); Object.assign(e, props); if(html) e.innerHTML = html; return e; };
function show(elm){ elm && elm.classList.remove("hidden"); }
function hide(elm){ elm && elm.classList.add("hidden"); }
async function withAlert(promise, successMsg) {
  try {
    const res = await promise;
    if (successMsg) alert(successMsg);
    return res;
  } catch (e) {
    alert(e.message || "Something went wrong");
    throw e;
  }
}

/* ---------- session (login persistence - client-side only) ---------- */
function setSession(user){
  localStorage.setItem(LS.SESSION, JSON.stringify({ id: user.id, role: (user.role||user.type||"").toLowerCase(), name: user.name }));
}
function getSession(){
  try { return JSON.parse(localStorage.getItem(LS.SESSION)); } catch(e){ return null; }
}
function clearSession(){ localStorage.removeItem(LS.SESSION); }
function currentRole(){
  const sess = getSession();
  if(sess && sess.id) return { id: sess.id, role: sess.role, name: sess.name };
  return null;
}

/* ---------- auth ---------- */
async function authLogin(){
  const role = $("#role") ? ($("#role").value || "").trim().toLowerCase() : "";
  const emailId = $("#email") ? ($("#email").value || "").trim() : "";
  const pwd = $("#password") ? ($("#password").value || "").trim() : "";

  if(!role) return alert("Select role");
  if(!emailId || !pwd) return alert("Enter library ID/email and password");

  try {
    const user = await apiPost("/api/auth/login", { id: emailId, role, password: pwd });
    setSession(user);
    if(role === "student") window.location.href = "student.html";
    else if(role === "librarian") window.location.href = "librarian.html";
    else if(role === "staff") window.location.href = "staff.html";
    else if(role === "admin") window.location.href = "admin.html";
    else window.location.href = "index.html";
  } catch(e){
    alert(e.message);
  }
}
function login(){ authLogin(); }

/* Roles that need a real email on file (for borrow/return/fine notifications).
   Extend this array later if a "faculty" role is added. */
const ROLES_REQUIRING_EMAIL = ["student"];

function onSignupRoleChange(){
  const roleElm = $("#su-role");
  const wrap = $("#su-email-wrap");
  if(!roleElm || !wrap) return;
  const role = roleElm.value.trim().toLowerCase();
  if(ROLES_REQUIRING_EMAIL.includes(role)) show(wrap);
  else { hide(wrap); const emailInput = $("#su-email"); if(emailInput) emailInput.value = ""; }
}

async function signupUser(){
  const nameElm = $("#su-name");
  const idElm = $("#su-id");
  const branchElm = $("#su-branch");
  const yearElm = $("#su-year");
  const pwdElm = $("#su-password");
  const roleElm = $("#su-role");
  const emailElm = $("#su-email");
  const semElm = $("#su-sem");
  const contactElm = $("#su-contact");

  if(!nameElm || !idElm || !pwdElm || !roleElm) return alert("Signup form not found or corrupted");
  const name = nameElm.value.trim();
  const id = idElm.value.trim();
  const branch = branchElm ? branchElm.value.trim() || "" : "";
  const year = yearElm ? yearElm.value.trim() || "" : "";
  const pwd = pwdElm.value.trim();
  const role = roleElm.value.trim().toLowerCase();
  const email = emailElm ? emailElm.value.trim() : "";
  const semester = semElm ? semElm.value.trim() : "";
  const contactNumber = contactElm ? contactElm.value.trim() : "";

  if(!name || !id || !pwd || !role) return alert("Name, ID, password and role are required.");
  const validRoles = ["student","librarian","staff","admin"];
  if(!validRoles.includes(role)) return alert("Invalid role selected.");

  if(ROLES_REQUIRING_EMAIL.includes(role) && !email){
    return alert("Please enter your email address so we can send you borrow/return notifications.");
  }

  try {
    await apiPost("/api/auth/signup", {
      id, name, branch, year: year || null, role: role.toUpperCase(), password: pwd, email: email || null,
      semester: semester || null, contactNumber: contactNumber || null
    });
    alert(`${role.charAt(0).toUpperCase()+role.slice(1)} account created. You may now login.`);
    const modal = document.getElementById("signup-modal");
    if(modal) modal.style.display = "none";
  } catch(e){
    alert(e.message);
  }
}

/* ---------- theme / sidebar / logout ---------- */
function toggleTheme(){ document.body.classList.toggle("dark-mode"); }
function toggleSidebar(){
  const sb = document.querySelector(".sidebar");
  if(!sb) return;
  sb.classList.toggle("active");
}
function logout(){ clearSession(); window.location.href = "index.html"; }

/* ---------- panel routing ---------- */
function showPanel(id){
  $$(".panel").forEach(p=>{
    if(p.querySelector(".cards-grid")) return;
    if(p.id === id) p.classList.remove("hidden");
    else p.classList.add("hidden");
  });

  const role = currentRole();
  if(role && role.role === "student"){
    if(id === "borrow") renderStudentSearch();
    if(id === "returns") renderStudentBorrowed();
    if(id === "fines") renderStudentFines();
    if(id === "elibrary") renderStudentElib();
    if(id === "history") renderStudentHistory();
  }
  if(role && role.role === "staff"){
    if(id === "manageBooks") renderManageList();
    if(id === "availability") renderManageList();
    if(id === "purchase") renderStaffPurchase();
  }
  if(role && role.role === "librarian"){
    if(id === "issue") renderLibIssueRequests();
    if(id === "inventory") renderLibInventory();
    if(id === "members") renderLibMembers();
    if(id === "reports") renderLibFines();
  }
  if(role && role.role === "admin"){
    if(id === "overview") renderAdminOverview();
    if(id === "borrowed") renderAdminBorrowed();
    if(id === "purchase") renderAdminPurchase();
    if(id === "users") renderAdminMembers();
  }
}

/* ================= Student flows ================= */

function renderStudentSearch(){
  const panel = document.getElementById("borrow");
  if(!panel) return;
  panel.innerHTML = `
    <h2>Search & Borrow Books</h2>
    <div style="display:flex;gap:8px;margin:10px 0">
      <input id="stu-q" class="input-small" placeholder="Search by title / author / id">
      <input id="stu-ed" class="input-small" placeholder="Filter edition (optional)">
      <button class="btn btn-primary" id="stu-search-btn">Search</button>
    </div>
    <div id="stu-results"></div>
  `;
  $("#stu-search-btn").addEventListener("click", studentDoSearch);
  $("#stu-q").addEventListener("keydown", e => { if(e.key==="Enter") studentDoSearch(); });
  $("#stu-ed").addEventListener("keydown", e => { if(e.key==="Enter") studentDoSearch(); });
  $("#stu-results").innerHTML = "<p>Enter a query and click Search.</p>";
}

async function studentDoSearch(){
  const q = ($("#stu-q").value||"").trim().toLowerCase();
  const edFilter = ($("#stu-ed").value||"").trim().toLowerCase();
  const out = $("#stu-results"); out.innerHTML = "Searching...";
  if(!q) return out.innerHTML = "<p>Type something to search</p>";

  const allBooks = await apiGet("/api/books");
  const results = allBooks.filter(b => b.title.toLowerCase().includes(q) || (b.author||"").toLowerCase().includes(q) || b.id.toLowerCase().includes(q));
  out.innerHTML = "";
  if(results.length===0) return out.innerHTML = "<p>No results found</p>";
  results.forEach(b => {
    const row = el("div",{className:"book-row"});
    const meta = el("div",{className:"book-meta"}, `<strong>${b.title}</strong> <span class="badge">${b.id}</span><br><small>${b.author} • ${b.category||'General'} • ${b.branch||''}</small>`);
    const eds = el("div",{className:"book-editions"});
    (b.editions||[]).filter(c => !edFilter || (c.edition||"").toLowerCase().includes(edFilter)).forEach(c => {
      const line = el("div", {}, `<div style="font-weight:700">${c.edition} (${c.year}) - ${c.copyId}</div>`);
      const right = el("div");
      if(c.available){
        const btn = el("button",{className:"btn btn-primary"},"Request Borrow");
        btn.addEventListener("click", ()=>studentRequestBorrow(b.id));
        right.appendChild(btn);
      } else {
        right.innerHTML = `<span class="badge" style="background:#ffe6e6;color:#7f1d1d">Borrowed</span>`;
      }
      const lineRow = el("div", {}, "");
      lineRow.style.display="flex"; lineRow.style.justifyContent="space-between"; lineRow.style.alignItems="center";
      lineRow.appendChild(line); lineRow.appendChild(right);
      eds.appendChild(lineRow);
    });
    row.appendChild(meta); row.appendChild(eds);
    out.appendChild(row);
  });
}

async function studentRequestBorrow(bookId){
  const sess = getSession();
  if(!sess || sess.role!=="student") return alert("You must be logged in as Student to request");
  try {
    await apiPost("/api/borrow/request", { userId: sess.id, bookId });
    alert("Borrow request sent to librarian for approval. You'll get an email + notification once it's issued.");
    studentDoSearch();
  } catch(e){ alert(e.message); }
}

async function renderStudentBorrowed(){
  const panel = document.getElementById("returns");
  if(!panel) return;
  const sess = getSession();
  if(!sess) return panel.innerHTML = "<h2>Return / Renew Books</h2><p>Login required</p>";
  panel.innerHTML = "<h2>Return / Renew Books</h2><p>Loading...</p>";

  const all = await apiGet(`/api/borrow/user/${sess.id}`);
  const list = all.filter(r => r.status === "APPROVED");
  if(list.length===0){ panel.innerHTML = "<h2>Return / Renew Books</h2><p>No borrowed books</p>"; return; }
  let html = `<h2>Return / Renew Books</h2>`;
  list.forEach(r => {
    html += `<div class="book-row"><div class="book-meta"><strong>${r.book.title}</strong><br><small>${r.book.author}</small></div>
      <div style="min-width:160px">Copy: ${r.copy.copyId}<br>Due: ${r.dueDate || '-'}</div>
      <div style="display:flex;flex-direction:column;gap:8px"><button class="btn btn-neutral" onclick="studentRequestReturn(${r.id})">Return</button><button class="btn btn-primary" onclick="studentRequestRenew(${r.id})">Renew</button></div></div>`;
  });
  panel.innerHTML = html;
}

async function studentRequestReturn(recordId){
  try {
    const r = await apiPost(`/api/borrow/${recordId}/return`);
    alert(r.fineAmount > 0
      ? `Book returned. A fine of ₹${r.fineAmount} applies for the delay — an email has been sent.`
      : "Book returned on time. No fine. Confirmation emailed to you.");
    renderStudentBorrowed();
  } catch(e){ alert(e.message); }
}

async function studentRequestRenew(recordId){
  try {
    const r = await apiPost(`/api/borrow/${recordId}/renew`);
    alert("Renewed! New due date: " + r.dueDate);
    renderStudentBorrowed();
  } catch(e){ alert(e.message); }
}

async function renderStudentFines(){
  const panel = document.getElementById("fines");
  if(!panel) return;
  const sess = getSession(); if(!sess) return panel.innerHTML="<h2>Fines</h2><p>Login required</p>";
  panel.innerHTML = "<h2>Fines</h2><p>Loading...</p>";

  const all = await apiGet(`/api/borrow/user/${sess.id}`);
  const borrowed = all.filter(r => r.status === "APPROVED" || r.status === "RETURNED");
  if(borrowed.length===0){ panel.innerHTML = "<h2>Fines</h2><p>No fines</p>"; return; }
  let table = `<h2>Fines</h2><table class="table"><tr><th>Book</th><th>Copy</th><th>Due</th><th>Fine</th><th>Status</th></tr>`;
  let any=false;
  borrowed.forEach(r=>{
    const fine = r.fineAmount || 0;
    const paid = r.finePaid === true;
    table += `<tr><td>${r.book.title}</td><td>${r.copy.copyId}</td><td>${r.dueDate||'-'}</td><td>₹${fine}</td><td>${paid? 'Paid' : (fine>0? 'Unpaid' : 'No fine')}</td></tr>`;
    if(fine>0 && !paid) any=true;
  });
  table += `</table>`;
  panel.innerHTML = any ? table : (table + "<p>No unpaid fines</p>");
}

async function renderStudentElib(){
  const panel = document.getElementById("elibrary");
  if(!panel) return;
  panel.innerHTML = `<h2>E-Library</h2>
    <div style="display:flex;gap:8px;margin:10px 0"><input id="elib-q" class="input-small" placeholder="Search research material / title / author"><button class="btn btn-primary" id="elib-search">Search</button></div>
    <div id="elib-results"></div>`;
  $("#elib-search").addEventListener("click", doElibSearch);
  $("#elib-q").addEventListener("keydown", e => { if(e.key==="Enter") doElibSearch(); });
}
async function doElibSearch(){
  const q = ($("#elib-q").value||"").trim().toLowerCase();
  const out = $("#elib-results"); out.innerHTML = "";
  if(!q) return out.innerHTML = "<p>Type something to search</p>";
  const allBooks = await apiGet("/api/books");
  const list = allBooks.filter(b=> b.online && (b.title.toLowerCase().includes(q) || (b.author||"").toLowerCase().includes(q) || b.id.toLowerCase().includes(q)));
  if(list.length===0) return out.innerHTML = "<p>No research materials found</p>";
  list.forEach(b => {
    const r = el("div",{className:"book-row"}, `<div class="book-meta"><strong>${b.title}</strong><br><small>${b.author}</small></div><div style="min-width:120px"><button class="btn btn-primary" onclick="simulateDownload('${b.id}')">Download</button></div>`);
    out.appendChild(r);
  });
}
function simulateDownload(id){ alert("Download simulated for " + id + "."); }

async function renderStudentHistory(){
  const panel = document.getElementById("history");
  if(!panel) return;
  const sess = getSession(); if(!sess) return panel.innerHTML="<h2>Borrow History</h2><p>Login required</p>";
  panel.innerHTML = "<h2>Borrow History</h2><p>Loading...</p>";
  const all = await apiGet(`/api/borrow/user/${sess.id}`);
  if(!all.length) return panel.innerHTML = "<h2>Borrow History</h2><p>No history</p>";
  let html = `<h2>Borrow History</h2><table class="table"><tr><th>Book</th><th>Copy</th><th>Status</th><th>Borrowed</th><th>Due</th><th>Returned</th></tr>`;
  all.forEach(r => {
    html += `<tr><td>${r.book.title}</td><td>${r.copy? r.copy.copyId : '-'}</td><td>${r.status}</td><td>${fmtDate(r.borrowDate)}</td><td>${fmtDate(r.dueDate)}</td><td>${fmtDate(r.returnDate)}</td></tr>`;
  });
  html += `</table>`;
  panel.innerHTML = html;
}

/* ================= Shared: member cards + detail modal ================= */

function initials(name){
  if(!name) return "?";
  const parts = name.trim().split(/\s+/);
  return (parts[0][0] + (parts[1] ? parts[1][0] : "")).toUpperCase();
}

function emptyState(icon, text){
  return `<div class="empty-state"><div class="es-icon">${icon}</div><p>${text}</p></div>`;
}

function renderMemberGrid(members, containerSelector){
  const out = typeof containerSelector === "string" ? $(containerSelector) : containerSelector;
  if(!out) return;
  if(!members.length){ out.innerHTML = emptyState("👥", "No members yet"); return; }
  out.innerHTML = `<div class="member-grid"></div>`;
  const grid = out.querySelector(".member-grid");
  members.forEach(m => {
    const role = (m.role||"").toLowerCase();
    const card = el("div", {className:"member-card"});
    card.innerHTML = `
      <div class="member-avatar">${initials(m.name)}</div>
      <div class="member-card-info">
        <p class="m-name">${m.name}</p>
        <p class="m-sub">${m.id}${m.branch ? " • " + m.branch : ""}</p>
        <span class="role-pill role-${role}">${m.role}</span>
      </div>`;
    card.addEventListener("click", ()=> openMemberDetailModal(m.id));
    grid.appendChild(card);
  });
}

async function openMemberDetailModal(memberId){
  // remove any existing instance first
  const existing = document.getElementById("member-detail-modal");
  if(existing) existing.remove();

  const modal = el("div", {id:"member-detail-modal", className:"modal detail-modal"});
  modal.style.display = "flex";
  modal.innerHTML = `<div class="modal-content"><div class="close-btn">✕</div><div id="mdm-body">Loading...</div></div>`;
  document.body.appendChild(modal);
  modal.querySelector(".close-btn").addEventListener("click", ()=> modal.remove());
  modal.addEventListener("click", (e)=> { if(e.target === modal) modal.remove(); });

  const body = modal.querySelector("#mdm-body");
  try {
    const members = await apiGet("/api/admin/members");
    const m = members.find(x => x.id === memberId);
    if(!m){ body.innerHTML = "<p>Member not found</p>"; return; }

    const role = (m.role||"").toLowerCase();
    let html = `<div class="detail-header">
        <div class="member-avatar">${initials(m.name)}</div>
        <div><h2>${m.name}</h2><span class="role-pill role-${role}">${m.role}</span></div>
      </div>`;

    if(role === "student"){
      const today = new Date().toISOString().slice(0,10);
      const membershipActive = !m.membershipExpiry || (new Date(m.membershipExpiry) >= new Date(today));
      html += `<div class="detail-grid">
          <div class="dg-item"><label>Library ID</label>${m.id}</div>
          <div class="dg-item"><label>Branch</label>${m.branch || '-'}</div>
          <div class="dg-item"><label>Semester</label>${m.semester || '-'}</div>
          <div class="dg-item"><label>Email</label>${m.email || '-'}</div>
          <div class="dg-item"><label>Contact Number</label>${m.contactNumber || '-'}</div>
          <div class="dg-item"><label>Membership</label><span class="status-pill ${membershipActive? 'status-good':'status-bad'}">${membershipActive? 'Active':'Expired'}</span></div>
          <div class="dg-item"><label>Fees</label><span id="mdm-fees-pill" class="status-pill ${m.feesPaid? 'status-good':'status-bad'}">${m.feesPaid? 'Paid':'Unpaid'}</span></div>
        </div>`;

      const sess = getSession();
      if(sess && sess.role === "admin"){
        html += `<button class="btn btn-neutral" id="mdm-toggle-fees" style="margin-bottom:10px">${m.feesPaid ? 'Mark Fees Unpaid' : 'Mark Fees Paid'}</button>`;
      }

      html += `<div class="detail-section-title">📚 Borrow History</div><div id="mdm-history">Loading...</div>`;
      body.innerHTML = html;

      const toggleBtn = document.getElementById("mdm-toggle-fees");
      if(toggleBtn){
        toggleBtn.addEventListener("click", async ()=>{
          try {
            await apiPost(`/api/admin/members/${m.id}/fees`, { paid: !m.feesPaid });
            openMemberDetailModal(m.id); // refresh
          } catch(e){ alert(e.message); }
        });
      }

      const historyList = await apiGet(`/api/borrow/user/${m.id}`);
      const histEl = document.getElementById("mdm-history");
      if(!histEl) return;
      if(!historyList.length){ histEl.innerHTML = emptyState("📖", "No books borrowed yet"); return; }
      let table = `<table class="table"><tr><th>Book</th><th>Borrowed</th><th>Due</th><th>Returned</th><th>Fine</th></tr>`;
      historyList.forEach(r => {
        const fineText = (r.fineAmount && r.fineAmount > 0)
          ? `₹${r.fineAmount} <span class="status-pill ${r.finePaid ? 'status-good':'status-bad'}" style="margin-left:4px">${r.finePaid? 'Paid':'Unpaid'}</span>`
          : `<span class="status-pill status-neutral">None</span>`;
        table += `<tr><td>${r.book.title}<br><small style="color:var(--muted)">${r.status}</small></td><td>${fmtDate(r.borrowDate)}</td><td>${fmtDate(r.dueDate)}</td><td>${fmtDate(r.returnDate)}</td><td>${fineText}</td></tr>`;
      });
      table += `</table>`;
      histEl.innerHTML = table;

    } else {
      // librarian / staff / admin - basic details only
      html += `<div class="detail-grid">
          <div class="dg-item"><label>Library ID</label>${m.id}</div>
          <div class="dg-item"><label>Branch</label>${m.branch || '-'}</div>
          <div class="dg-item"><label>Email</label>${m.email || '-'}</div>
          <div class="dg-item"><label>Contact Number</label>${m.contactNumber || '-'}</div>
        </div>`;
      body.innerHTML = html;
    }
  } catch(e){
    body.innerHTML = `<p>Error loading member: ${e.message}</p>`;
  }
}
window.openMemberDetailModal = openMemberDetailModal;

/* ================= Librarian flows ================= */

async function renderLibIssueRequests(){
  const panel = document.getElementById("issue"); if(!panel) return;
  const out = $("#issue-requests");
  if(!out) return;
  out.innerHTML = "Loading...";
  const list = await apiGet("/api/borrow/status/PENDING");
  if(list.length===0){ out.innerHTML = emptyState("📋", "No pending issue requests"); return; }
  let html = `<div>`;
  list.forEach(r=>{
    html += `<div class="book-row"><div class="book-meta"><strong>${r.book.title}</strong><div style="font-size:13px;color:#555">Copy: ${r.copy.copyId}</div></div>
      <div style="min-width:220px">${r.user.name||r.user.id}</div>
      <div style="min-width:160px"><button class="btn btn-primary" onclick="approveRequest(${r.id})">Issue</button> <button class="btn btn-neutral" onclick="rejectRequest(${r.id})">Reject</button></div></div>`;
  });
  html += `</div>`;
  out.innerHTML = html;
}

/* approveRequest: backend sets due date, marks copy unavailable, generates
   the real PDF borrow slip, saves the digital record, and emails the student
   automatically. Nothing else to do here except refresh the list. */
async function approveRequest(recordId){
  try {
    await apiPost(`/api/borrow/${recordId}/approve`);
    alert("Issued. Borrow slip generated and emailed to the student automatically.");
    renderLibIssueRequests();
  } catch(e){ alert(e.message); }
}

async function rejectRequest(recordId){
  try {
    await apiPost(`/api/borrow/${recordId}/reject`);
    renderLibIssueRequests();
  } catch(e){ alert(e.message); }
}

async function renderLibInventory(){
  const panel = document.getElementById("inventory"); if(!panel) return;
  const view = panel.querySelector("#inventory-view");
  if(view) view.innerHTML = "Loading...";
  const list = await apiGet("/api/books");
  if(!list.length){ if(view) view.innerHTML = emptyState("📚", "No books in system"); return; }
  const groups = {};
  list.forEach(b => {
    const br = (b.branch || "General").toUpperCase();
    groups[br] = groups[br] || [];
    groups[br].push(b);
  });
  let html = "";
  Object.keys(groups).sort().forEach(br => {
    html += `<div class="panel"><h3 style="margin:0 0 8px">${br}</h3>`;
    html += `<table class="table"><tr><th>Title</th><th>Author</th><th>Edition(s)</th><th>Quantity (available)</th></tr>`;
    groups[br].forEach(b=>{
      const editions = (b.editions||[]).map(e=>`${e.edition} (${e.copyId})${e.available? '' : ' [Borrowed]'}`).join("<br>");
      const qty = (b.editions||[]).filter(e=>e.available).length;
      html += `<tr><td>${b.title}</td><td>${b.author}</td><td>${editions}</td><td>${qty}</td></tr>`;
    });
    html += `</table></div>`;
  });
  if(view) view.innerHTML = html;
}

async function renderLibMembers(){
  const panel = document.getElementById("members");
  if(!panel) return;
  const view = panel.querySelector("#members-view");
  if(view) view.innerHTML = "Loading...";
  const mems = await apiGet("/api/admin/members");
  renderMemberGrid(mems, view);
}

async function renderLibFines(){
  const panel = document.getElementById("reports"); if(!panel) return;
  panel.innerHTML = "<div id='reports-view'>Loading...</div>";
  const arr = await apiGet("/api/borrow/status/APPROVED");
  const returnedWithFines = (await apiGet("/api/borrow/status/RETURNED")).filter(r => (r.fineAmount||0) > 0);
  const unpaid = [];
  const paid = [];
  [...arr, ...returnedWithFines].forEach(r=>{
    const fine = r.fineAmount || 0;
    const item = { id:r.id, name: r.user.name||r.user.id, bookTitle:r.book.title, copyId:r.copy? r.copy.copyId : '-', dueDate: r.dueDate, amount: fine, paid: !!r.finePaid };
    if(item.amount>0 && !item.paid) unpaid.push(item);
    if(item.amount>0 && item.paid) paid.push(item);
  });
  let html = `<h3>Unpaid Fines</h3>`;
  if(unpaid.length===0) html += "<p>No unpaid fines</p>";
  else {
    html += `<table class="table"><tr><th>Name</th><th>Book</th><th>Copy</th><th>Due</th><th>Amount</th><th>Action</th></tr>`;
    unpaid.forEach(u => html += `<tr><td>${u.name}</td><td>${u.bookTitle}</td><td>${u.copyId}</td><td>${u.dueDate}</td><td>₹${u.amount}</td><td><button class="btn btn-primary" onclick="markFinePaid(${u.id})">Mark Paid</button></td></tr>`);
    html += `</table>`;
  }
  html += `<h3>Paid Fines</h3>`;
  if(paid.length===0) html += "<p>No paid fines</p>";
  else {
    html += `<table class="table"><tr><th>Name</th><th>Book</th><th>Copy</th><th>Due</th><th>Amount</th></tr>`;
    paid.forEach(u => html += `<tr><td>${u.name}</td><td>${u.bookTitle}</td><td>${u.copyId}</td><td>${u.dueDate}</td><td>₹${u.amount}</td></tr>`);
    html += `</table>`;
  }
  const view = document.getElementById("reports-view");
  if(view) view.innerHTML = html;
}

async function markFinePaid(recordId){
  try {
    await apiPost(`/api/borrow/${recordId}/mark-fine-paid`);
    alert("Fine marked paid");
    renderLibFines();
  } catch(e){ alert(e.message); }
}

/* ================= Staff flows ================= */

async function renderManageList(){
  const out = $("#manage-list"); if(!out) return;
  out.innerHTML = "Loading...";
  const qInput = $("#m-search-q");
  const q = qInput ? qInput.value.trim().toLowerCase() : "";
  const allBooks = await apiGet("/api/books");
  const list = allBooks.filter(b=> !q || b.title.toLowerCase().includes(q) || (b.author||"").toLowerCase().includes(q) || b.id.toLowerCase().includes(q));
  if(list.length===0) return out.innerHTML = emptyState("📚", "No books found");
  out.innerHTML = "";
  list.forEach(b=>{
    const copiesAvailable = (b.editions||[]).filter(e=>e.available).length;
    const availText = copiesAvailable > 0 ? `${copiesAvailable} available` : "All borrowed";
    const chips = (b.editions||[]).map(e=>
      `<span class="copy-chip ${e.available ? 'available' : 'taken'}">${e.copyId} — ${e.available ? 'Available' : 'Borrowed'}</span>`
    ).join("");
    const div = el("div",{className:"book-row"});
    div.innerHTML = `<div class="book-meta"><strong>${b.title}</strong> <span class="badge">${b.id}</span><br><small>${b.author} • ${b.category||''} • ${b.branch||''}</small></div>
      <div style="min-width:160px"><span class="status-pill ${copiesAvailable>0 ? 'status-good':'status-bad'}">${availText}</span></div>
      <div style="min-width:240px">${chips}</div>
      <div style="display:flex;flex-direction:column;gap:8px">
        <button class="btn" style="background:#ef4444;color:#fff;border:none;padding:8px;border-radius:8px" onclick='staffRemoveBook("${b.id}")'>Delete</button>
      </div>`;
    out.appendChild(div);
  });
}

async function staffAddBook(){
  const title = $("#m-title").value.trim(); const author = $("#m-author").value.trim(); const id = $("#m-id").value.trim();
  const ed = $("#m-ed") ? $("#m-ed").value.trim() : "1st";
  const year = $("#m-year") ? $("#m-year").value.trim() : new Date().getFullYear();
  const cat = $("#m-cat") ? $("#m-cat").value.trim() : "General";
  const branch = $("#m-branch") ? $("#m-branch").value.trim() : "CSE";
  if(!title || !id) return alert("Title & ID required");

  try {
    await apiPost("/api/books", {
      id, title, author, year: Number(year)||new Date().getFullYear(),
      category: cat||"General", branch: branch||"", online:false,
      editions: [{ copyId: id+"-1", edition: ed||"1st", year: Number(year)||new Date().getFullYear(), available:true }]
    });
    alert("Book added");
    renderManageList();
  } catch(e){ alert(e.message); }
}

async function staffRemoveBook(bookId){
  if(!confirm("Delete this book permanently?")) return;
  try {
    await apiDelete(`/api/books/${bookId}`);
    renderManageList();
  } catch(e){ alert(e.message); }
}

/** Shared tabbed Pending/Approved purchase view - used by both Staff and Admin.
    canApprove=true shows the Approve button on pending cards (admin only). */
async function renderPurchaseTabbed(container, canApprove){
  const out = typeof container === "string" ? $(container) : container;
  if(!out) return;
  out.innerHTML = "Loading...";

  const [pending, approved] = await Promise.all([
    apiGet("/api/purchase/status/PENDING"),
    apiGet("/api/purchase/status/APPROVED")
  ]);

  out.innerHTML = `
    <div class="tab-bar">
      <button class="tab-btn active" data-tab="pending">⏳ Pending <span class="purchase-count">${pending.length}</span></button>
      <button class="tab-btn" data-tab="approved">✅ Approved <span class="purchase-count">${approved.length}</span></button>
    </div>
    <div id="ptab-pending" class="tab-panel"></div>
    <div id="ptab-approved" class="tab-panel hidden"></div>
  `;

  const pendingEl = out.querySelector("#ptab-pending");
  const approvedEl = out.querySelector("#ptab-approved");

  if(!pending.length) pendingEl.innerHTML = emptyState("✅", "Nothing waiting on approval right now");
  else pending.forEach(p => {
    const div = el("div", {className:"purchase-card"});
    div.innerHTML = `<div class="pc-info"><strong>${p.title}</strong><small>${p.author||''}${p.edition ? ' • '+p.edition+' edition' : ''}${p.requestedBy? ' • requested by '+p.requestedBy : ''}</small></div>`;
    if(canApprove){
      const btn = el("button", {className:"btn btn-primary"}, "Approve");
      btn.addEventListener("click", ()=> approvePurchase(p.id));
      div.appendChild(btn);
    } else {
      const btn = el("button", {className:"btn btn-neutral"}, "Withdraw");
      btn.addEventListener("click", ()=> removePurchase(p.id));
      div.appendChild(btn);
    }
    pendingEl.appendChild(div);
  });

  if(!approved.length) approvedEl.innerHTML = emptyState("📦", "No approved purchases yet");
  else approved.forEach(p => {
    approvedEl.innerHTML += `<div class="purchase-card approved">
      <div class="pc-info"><strong>${p.title}</strong><small>${p.author||''}${p.edition ? ' • '+p.edition+' edition' : ''}${p.requestedBy? ' • requested by '+p.requestedBy : ''}</small></div>
      <span class="status-pill status-good">Approved</span>
    </div>`;
  });

  out.querySelectorAll(".tab-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      out.querySelectorAll(".tab-btn").forEach(b=>b.classList.remove("active"));
      btn.classList.add("active");
      const tab = btn.dataset.tab;
      pendingEl.classList.toggle("hidden", tab !== "pending");
      approvedEl.classList.toggle("hidden", tab !== "approved");
    });
  });
}

async function renderStaffPurchase(){
  await renderPurchaseTabbed("#purchase-list-view", false);

  const addBtn = $("#p-add");
  if(addBtn && !addBtn._bound){
    addBtn.addEventListener("click", staffAddPurchase);
    addBtn._bound = true;
  }
}

async function staffAddPurchase(){
  const title = $("#p-title").value.trim(); const author = $("#p-author").value.trim(); const ed = $("#p-ed").value.trim();
  if(!title) return alert("Title required");
  const sess = getSession();
  try {
    await apiPost("/api/purchase", { title, author: author||'', edition: ed||'', requestedBy: (sess && sess.id) || "staff" });
    $("#p-title").value = ""; $("#p-author").value = ""; $("#p-ed").value = "";
    renderStaffPurchase();
  } catch(e){ alert(e.message); }
}

async function removePurchase(id){
  try {
    await apiDelete(`/api/purchase/${id}`);
    renderStaffPurchase();
  } catch(e){ alert(e.message); }
}

/* ================= Admin flows ================= */

let _adminCharts = {};
function destroyChart(key){ if(_adminCharts[key]){ _adminCharts[key].destroy(); delete _adminCharts[key]; } }

const CHART_COLORS = ["#2563eb","#ff7b00","#10b981","#7c3aed","#ef4444","#06b6d4","#f59e0b","#ec4899"];

async function renderAdminOverview(){
  const panel = document.getElementById("overview"); if(!panel) return;
  panel.innerHTML = "<h2>Library Overview</h2><p>Loading real-time stats...</p>";
  const stats = await apiGet("/api/admin/overview");

  panel.innerHTML = `
    <h2>Library Overview</h2>
    <div class="cards-grid">
      <div class="card blue"><h3>${stats.totalBooks}</h3><p>Total Books</p></div>
      <div class="card purple"><h3>${stats.totalMembers}</h3><p>Total Members</p></div>
      <div class="card orange"><h3>${stats.borrowed}</h3><p>Currently Borrowed</p></div>
      <div class="card"><h3>${stats.pending}</h3><p>Pending Requests</p></div>
      <div class="card red"><h3>${stats.damagedOrLost}</h3><p>Damaged / Lost</p></div>
      <div class="card red"><h3>${stats.unpaidFinesCount}</h3><p>Unpaid Fines</p></div>
    </div>

    <div class="member-grid" style="grid-template-columns:repeat(auto-fit, minmax(320px,1fr))">
      <div class="panel">
        <h3 style="margin-top:0">📚 Books by Branch</h3>
        <canvas id="chart-books-branch" height="220"></canvas>
      </div>
      <div class="panel">
        <h3 style="margin-top:0">🔄 Borrow Status Breakdown</h3>
        <canvas id="chart-borrow-status" height="220"></canvas>
      </div>
      <div class="panel">
        <h3 style="margin-top:0">👥 Members by Role</h3>
        <canvas id="chart-members-role" height="220"></canvas>
      </div>
      <div class="panel">
        <h3 style="margin-top:0">💰 Fines: Collected vs Pending</h3>
        <canvas id="chart-fines" height="220"></canvas>
      </div>
    </div>
  `;

  if(typeof Chart === "undefined"){
    // Chart.js failed to load (e.g. no internet) - degrade gracefully, numbers above still work
    return;
  }

  const branchLabels = Object.keys(stats.booksByBranch || {});
  const branchData = Object.values(stats.booksByBranch || {});
  destroyChart("branch");
  _adminCharts.branch = new Chart(document.getElementById("chart-books-branch"), {
    type: "bar",
    data: { labels: branchLabels, datasets: [{ label: "Books", data: branchData, backgroundColor: CHART_COLORS }] },
    options: { plugins:{legend:{display:false}}, scales:{ y:{ beginAtZero:true, ticks:{precision:0} } } }
  });

  const statusLabels = Object.keys(stats.borrowStatusCounts || {});
  const statusData = Object.values(stats.borrowStatusCounts || {});
  destroyChart("status");
  _adminCharts.status = new Chart(document.getElementById("chart-borrow-status"), {
    type: "doughnut",
    data: { labels: statusLabels, datasets: [{ data: statusData, backgroundColor: CHART_COLORS }] },
    options: { plugins:{legend:{position:"bottom"}} }
  });

  const roleLabels = Object.keys(stats.membersByRole || {});
  const roleData = Object.values(stats.membersByRole || {});
  destroyChart("role");
  _adminCharts.role = new Chart(document.getElementById("chart-members-role"), {
    type: "pie",
    data: { labels: roleLabels, datasets: [{ data: roleData, backgroundColor: CHART_COLORS }] },
    options: { plugins:{legend:{position:"bottom"}} }
  });

  destroyChart("fines");
  _adminCharts.fines = new Chart(document.getElementById("chart-fines"), {
    type: "bar",
    data: { labels: ["Collected", "Pending"], datasets: [{ label: "₹", data: [stats.finesCollected||0, stats.finesPending||0], backgroundColor: ["#10b981","#ef4444"] }] },
    options: { indexAxis: "y", plugins:{legend:{display:false}}, scales:{ x:{ beginAtZero:true } } }
  });
}

async function renderAdminBorrowed(){
  const panel = document.getElementById("borrowed"); if(!panel) return;
  panel.innerHTML = "<h2>Borrowed / Damaged</h2><p>Loading...</p>";
  const arr = await apiGet("/api/borrow/status/APPROVED");
  if(!arr.length) return panel.innerHTML = "<h2>Borrowed / Damaged</h2><p>No records</p>";
  let html = `<h2>Borrowed / Damaged</h2>`;
  arr.forEach(r => { html += `<div class="panel"><strong>${r.book.title}</strong> — ${r.copy.copyId} — Due: ${r.dueDate} — By: ${r.user.id}</div>`; });
  panel.innerHTML = html;
}

async function renderAdminPurchase(){
  const panel = document.getElementById("purchase"); if(!panel) return;
  panel.innerHTML = "<h2>Purchase Requests</h2><div id='admin-purchase-view'></div>";
  await renderPurchaseTabbed("#admin-purchase-view", true);
}

async function approvePurchase(id){
  try {
    await apiPost(`/api/purchase/${id}/approve`);
    renderAdminPurchase();
  } catch(e){ alert(e.message); }
}

async function renderAdminMembers(){
  const panel = document.getElementById("users"); if(!panel) return;
  panel.innerHTML = "<h2>Members</h2><div id='admin-members-view'>Loading...</div>";
  const arr = await apiGet("/api/admin/members");
  renderMemberGrid(arr, "#admin-members-view");
}

async function renewMembership(memberId, newExpiry){
  if(!memberId) return alert("Member ID required");
  try {
    await apiPost(`/api/admin/members/${memberId}/renew`, { expiry: newExpiry || null });
    alert("Membership renewed");
    renderAdminMembers();
  } catch(e){ alert(e.message); }
}

function demoAddPurchaseRequest(){
  apiPost("/api/purchase", { title:"Cloud Computing", author:"Jane Doe", edition:"3rd", requestedBy:"staff" })
    .then(()=> alert("Demo purchase request added"))
    .catch(e=> alert(e.message));
}

/* ---------- expose functions used by inline onclick= in the HTML ---------- */
window.login = login;
window.authLogin = authLogin;
window.toggleTheme = toggleTheme;
window.toggleSidebar = toggleSidebar;
window.showPanel = showPanel;
window.logout = logout;
window.studentRequestBorrow = studentRequestBorrow;
window.studentRequestReturn = studentRequestReturn;
window.studentRequestRenew = studentRequestRenew;
window.approveRequest = approveRequest;
window.rejectRequest = rejectRequest;
window.staffRemoveBook = staffRemoveBook;
window.demoAddPurchaseRequest = demoAddPurchaseRequest;
window.showSignup = function(){ const m=document.getElementById("signup-modal"); if(m) m.style.display="flex"; };
window.closeSignup = function(){ const m=document.getElementById("signup-modal"); if(m) m.style.display="none"; };
window.signupUser = signupUser;
window.onSignupRoleChange = onSignupRoleChange;
window.signupStudent = signupUser;
window.simulateDownload = simulateDownload;
window.markFinePaid = markFinePaid;
window.renewMembership = renewMembership;
window.staffAddBook = staffAddBook;
window.renderManageList = renderManageList;
window.renderStaffPurchase = renderStaffPurchase;
window.staffAddPurchase = staffAddPurchase;
window.removePurchase = removePurchase;
window.approvePurchase = approvePurchase;

/* ---------- on page load ---------- */
document.addEventListener("DOMContentLoaded", ()=> {
  const p = location.pathname.split("/").pop();
  if(p === "staff.html"){
    if(document.getElementById("purchase-list-view")) renderStaffPurchase();
    if(document.getElementById("manage-list")) renderManageList();
  }
  if(p === "librarian.html"){
    const inv = document.getElementById("inventory-view");
    if(inv) inv.innerHTML = "<p>Open Inventory card to view grouped inventory.</p>";
    const memv = document.getElementById("members-view");
    if(memv) memv.innerHTML = "<p>Open Members card to view members.</p>";
    const issueOut = document.getElementById("issue-requests");
    if(issueOut) issueOut.innerHTML = "<p>Open Issue / Return to view requests.</p>";
  }
});
