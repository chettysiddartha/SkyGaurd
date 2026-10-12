# ✈️ SkyGuard – Aviation Operations & Air Safety Management System

A full-stack, enterprise-grade aviation operations and safety management platform built using **Java (Spring Boot 3.4.4)**, **Spring Data JPA**, **Spring Security (BCrypt)**, **MySQL 8.0**, and a modern **Aviation Command Center UI (Bootstrap 5 & Chart.js)**.

---

## 📌 Features & Implemented Modules

### 1. 🛡️ User Authentication & Role-Based Access Control
- **BCrypt password encryption** for all stored user credentials.
- **Three user privilege levels**:
  - `ROLE_ADMIN`: Full administrative control, user account management, fleet deletion privileges.
  - `ROLE_STAFF`: Flight scheduling, aircraft status updating, maintenance logging, incident management.
  - `ROLE_VIEWER`: Read-only operational radar telemetry, flight tracking, and safety alerts.
- **Pre-configured Demo Accounts**:
  - **Administrator**: `admin` / `admin123`
  - **Operations Staff**: `staff` / `staff123`
  - **Pilot / Viewer**: `pilot` / `pilot123`
- Session-backed authentication with REST endpoints (`/api/auth/login`, `/api/auth/register`, `/api/auth/me`, `/api/auth/logout`).

### 2. ✈️ Aircraft Fleet Management
- Full CRUD operations with duplicate registration prevention (`findByRegistration`).
- Tracks tail registration, aircraft model, manufacturer, passenger capacity, base station, airframe flight hours, and scheduled maintenance windows.
- State machine: `AVAILABLE`, `IN_FLIGHT`, `MAINTENANCE`, `GROUNDED`.
- Active flight safety constraint: prevents deleting or modifying aircraft currently assigned to active flights.

### 3. 🛫 Flight Operations & Scheduling
- Schedule flights with route mapping (`Origin ➔ Destination`), scheduled departure & arrival times, terminals, gates, and passenger manifests.
- Aircraft airworthiness safety check: automatically rejects flight scheduling if assigned aircraft is `MAINTENANCE` or `GROUNDED`.
- Real-time flight status tracker: `SCHEDULED`, `BOARDING`, `IN_FLIGHT`, `LANDED`, `DELAYED`, `CANCELLED`.
- Automated fleet synchronization: landing or cancelling a flight automatically releases the aircraft back to `AVAILABLE`.

### 4. 🔧 Maintenance Tracking & Airworthiness Engine
- Track routine A-Checks, C-Checks, structural inspections, avionics diagnostics, and engine overhauls.
- Priority assignment: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- Automatically transitions aircraft to `MAINTENANCE` when high/critical work orders are opened.
- Single-click completion workflow: marking a work order as `COMPLETED` automatically logs the completion timestamp and restores the aircraft's status to `AVAILABLE`.

### 5. ⚠️ Incident Reporting & Safety Hazards
- ICAO Annex 13 compliant safety hazard and occurrence logging.
- Categorization: `BIRD_STRIKE`, `WEATHER`, `MECHANICAL`, `SYSTEM_FAILURE`, `SECURITY`, `RUNWAY_INCURSION`, `MEDICAL`, `OTHER`.
- Automatic incident tracking number generation (e.g. `INC-2026-0104`).
- Investigation workflow: `OPEN` ➔ `INVESTIGATING` ➔ `RESOLVED` ➔ `CLOSED` with forensic investigation notes.

### 6. 🚨 Rule-Based Real-Time Operational Alerts
Automated background rules engine evaluating fleet risk:
- **CRITICAL**: Overdue maintenance tasks, grounded aircraft, open critical safety incidents.
- **HIGH**: Aircraft currently in service bays, flight departure delays exceeding operational limits.
- **MEDIUM**: Scheduled inspections due within 7 days, cancelled flights.
- Visual alerts console with priority badges and dynamic banner ticker.

### 7. 🤖 AI Operations Copilot
- Integrated conversational assistant for flight dispatchers and safety officers.
- Built-in Aviation Knowledge Engine: provides instant decision support for bird strikes, weather diversions, C-check intervals, and turnaround optimization without needing an external API key.
- Optional Cloud AI: set `AI_API_KEY` to connect to live OpenAI / Gemini / Ollama endpoints.

---

## 🛠️ Technology Stack

- **Backend**: Java 17/21/25, Spring Boot 3.4.4, Spring Web, Spring Data JPA, Spring Security, Hibernate 6.
- **Database**: MySQL 8.0 (with automatic table schema generation and sample data seeding via `DataInitializer`).
- **Frontend**: HTML5, CSS3, JavaScript (ES6+), Bootstrap 5.3, Bootstrap Icons, Chart.js 4.4.
- **Build Tool**: Maven with included Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **Testing**: JUnit 5, Spring Boot Test, MockMvc, Spring Security Test.

---

## 🚀 Step-by-Step Instructions to Run on Windows (VS Code)

### Step 1: Open the Project in VS Code
Open Visual Studio Code and open the project directory:
```
File -> Open Folder -> Select "SkyGuard"
```

### Step 2: Configure Database (MySQL 8.0)
The application connects to MySQL on `localhost:3306` with database `aviation_db`.
If not already created, run in PowerShell:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot -e "CREATE DATABASE IF NOT EXISTS aviation_db;"
```
*(If your MySQL password is not `root`, adjust `spring.datasource.password` in `src/main/resources/application.properties` or set the `DB_PASSWORD` environment variable).*

### Step 3: Run Automated Tests
Open the VS Code Terminal (`Ctrl + ~`) in PowerShell and run:
```powershell
.\mvnw.cmd clean test
```
*All 10 tests verify context loading, REST APIs, safety validation rules, and authentication.*

### Step 4: Start the Application
Run the Spring Boot application using the Maven wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```
The application will compile and launch the embedded Tomcat server on port **8080**.

### Step 5: Open in Your Browser
Open your browser and navigate to:
```
http://localhost:8080
```

### Step 6: Log In & Explore
1. Click **Sign In** in the top right corner.
2. Click **Admin** under *Quick 1-Click Demo Logins* (or enter `admin` / `admin123`).
3. You can now register aircraft, schedule flights, manage maintenance, inspect incidents, view alerts, and converse with the **AI Operations Copilot**!

---

## 📡 REST API Documentation

| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/summary` | `GET` | Public | Comprehensive operational KPI statistics |
| `/api/alerts` | `GET` | Public | Real-time rule-based safety alerts |
| `/api/aircraft` | `GET` / `POST` | Public / Staff | List all aircraft or register new aircraft |
| `/api/aircraft/{id}` | `PUT` / `DELETE` | Staff / Admin | Update aircraft or remove from fleet |
| `/api/flights` | `GET` / `POST` | Public / Staff | List flights or schedule a new flight |
| `/api/flights/{id}/status` | `PATCH` | Staff | Quick status update (`IN_FLIGHT`, `LANDED`, etc.) |
| `/api/maintenance` | `GET` / `POST` | Public / Staff | List work orders or create new maintenance order |
| `/api/maintenance/{id}/status` | `PATCH` | Staff | Complete or update maintenance order status |
| `/api/incidents` | `GET` / `POST` | Public / Staff | List safety incidents or report a new incident |
| `/api/ai/ask` | `POST` | Public | Query the AI Operations Copilot |
| `/api/auth/login` | `POST` | Public | Authenticate user session |
| `/api/auth/register` | `POST` | Public | Register new staff/pilot account |
| `/api/auth/me` | `GET` | Authenticated | Get current authenticated user profile |
| `/api/users` | `GET` | Admin | List all registered user accounts |
