# Quick Start Guide

BHEL HRM System — 3-Tier Distributed Architecture with Fault Tolerance

## System Components

1. **Database Server** (Tier 3) — Manages all CSV data, port 1098
2. **Application Server** (Tier 2) — Business logic, authentication, port 1099
3. **Client GUI** (Tier 1) — User interface (Swing)

---
## Test Accounts

| Username        | Password   | Role          |
|-----------------|------------|---------------|
| `admin`     | `admin123` | Administrator |
| `hr1`       | `hr1234`   | HR Staff      |
| `testemp1`  | `emp123`   | Employee      |

### Quick Test

1. Login as `testemp1` / `emp123`
   - View your profile
   - Apply for leave
   - Add family member

2. Logout, then login as `hr1` / `hr1234`
   - Approve the leave application
   - Register new employee
   - Generate yearly report

3. Logout, then login as `admin` / `admin123`
   - Manage user accounts
   - View system audit logs



## Single Laptop Setup
### Step 1: Open terminal in the BHEL_HRM directory
```batch
cd C:\path\to\BHEL_HRM
```
### Step 2: Compile (first time or after code changes)

```batch
.\compile.bat
```
### Step 3: Run Everything
```batch
.\run-all.bat
```

This opens 3 new windows:
- **Database Server** window (port 1098)
- **Application Server** window (port 1099)
- **Client GUI** window

### Running Components Individually ###
```batch
.\run-database.bat        # Terminal 1: Database Server
.\run-server.bat          # Terminal 2: Application Server
.\run-client.bat          # Terminal 3: Client GUI
```

---

## Multi-Laptop Setup (Fault Tolerance Demo) ##
This is where the system demonstrates **primary-backup fault tolerance**. You need 2 or 3 laptops on the **same WiFi network**.
### What Each Laptop Does
| Laptop| Role                    | What it runs                                         | Bat file              |
|-------|-------------------------|------------------------------------------------------|----------             |
| **A** | Primary                 | Primary DB + Primary App Server + Client (optional)  | `run-all-primary.bat` |
| **B** | Backup                  | Backup DB + Backup App Server + Client with failover | `run-backup.bat`      |
| **C** | Extra client (optional) | Just a client, no server                             | `run-client-only.bat` |
### How It Works
```
Laptop A (Primary)                    Laptop B (Backup)
┌─────────────────────┐               ┌─────────────────────┐
│ Database:1098       │── replicates─→│ Database:2098       │
│ App Server:1099     │               │ App Server:2099     │
└─────────────────────┘               │ Client (failover)   │
                                      └─────────────────────┘

Laptop C (optional)
┌─────────────────────┐
│ Client (plain)      │── connects to Laptop A
└─────────────────────┘
```
- Laptop A's database **replicates every write** to Laptop B's database in real-time
- Laptop B's client connects to Laptop A first, but **automatically switches** to the local backup if Laptop A goes down
- Laptop C is a plain client with no failover (for demonstrating the contrast)

---

##### Step-by-Step Setup #####
#### Step 0: Find Your IP Addresses
On **each laptop**, open Command Prompt (CMD) and run:
```batch
ipconfig
```

Look for this section:
```
Wireless LAN adapter Wi-Fi:
   IPv4 Address. . . . . . . : 192.168.X.X
```

Write down both IPs. For example:
- Laptop A: `192.168.100.5`
- Laptop B: `192.168.100.10`

**Important:** Both laptops MUST be on the same WiFi network.
#### Step 1: Edit the IP Addresses in the Bat Files
This is the only thing you need to change. Open each bat file in a text editor (Notepad, VS Code, etc.) and change the IP on the line that says `EDIT THIS`.

**On Laptop A** — open `run-all-primary.bat`, find this line:
```batch
set BACKUP_IP=192.168.100.10
```

Change `192.168.100.10` to **Laptop B's actual IP address**.
**On Laptop B** — open `run-backup.bat`, find this line:
```batch
set PRIMARY_IP=192.168.100.5
```
Change `192.168.100.5` to **Laptop A's actual IP address**.

**On Laptop C** (if using) — open `run-client-only.bat`, find this line:
```batch
set SERVER_IP=192.168.100.5
```
Change `192.168.100.5` to **Laptop A's actual IP address**.

#### Step 2: Copy the Project to All Laptops
Copy the entire `BHEL_HRM` folder to every laptop. The code must be identical on all machines.

#### Step 3: Compile on All Laptops
On each laptop, open a terminal in the `BHEL_HRM` folder and run:
```batch
.\compile.bat
```

#### Step 4: Start the Backup First (Laptop B)
On Laptop B, double-click `run-backup.bat` (or run in terminal):
```batch
.\run-backup.bat
```

3 windows open. Wait until you see in the Database Server window:
```
Database Server listening on port 2098
```
And in the Application Server window:
```
Server is running on port 2099
```

#### Step 5: Start the Primary (Laptop A)
On Laptop A, double-click `run-all-primary.bat` (or run in terminal):
```batch
.\run-all-primary.bat
```

3 windows open. In the Database Server window, you should see:
```
[REPLICATION] Connected to backup!
[REPLICATION] All writes now replicated
```

#### Step 6: Start Extra Client (Laptop C — Optional)
On Laptop C, double-click `run-client-only.bat`:
```batch
.\run-client-only.bat
```
This opens a client that connects directly to Laptop A.

#### Step 7: Test Normal Operations
1. Log in on Laptop B's client as `hr1 / hr1234`
2. Register a new employee or approve leave
3. Watch Laptop A's Database Server window — you'll see the operation
4. Watch Laptop B's Database Server window — you'll see `[REPLICATION] APPEND -> employees.csv`
5. The data is now on both laptops

#### Step 8: Test Failover (The Demo)
1. **Kill Laptop A** — close all its command prompt windows (or just close the lid)
2. **On Laptop B's client** — click any button (refresh employees, apply for leave, etc.)
3. A popup appears: *"Primary server is down. Switched to backup. Please login again."*
4. Log in again — you're now running on the backup server
5. The title bar shows **[BACKUP SERVER]** to confirm
6. All data you created before the failover is still there

**On Laptop C** (if using) — click anything and it shows a connection error. This client has no failover configured, which demonstrates the contrast.

---
## Security Features
- **Audit Logging** — Every action is logged (who, what, when)
- **SSL/TLS** — Encrypted communication (optional, see below)
- **Session Tokens** — Secure authentication per login
- **Password Hashing** — SHA-256 with salt, passwords never stored in plain text
- **Role-Based Access Control** — Employee, HR, and Admin roles with enforced permissions
---

## Data Files

All data is stored in CSV format in the `data/` directory:

| File                     | Contents                |
|--------------------------|-------------------------|
| `users.csv`              | Login accounts          |
| `employees.csv`          | Employee records        |
| `leave_applications.csv` | Leave applications      |
| `leave_balances.csv`     | Leave balances per year |
| `family_members.csv`     | Employee family info    |
| `payroll_records.csv`    | Salary records          |
| `profile_updates.csv`    | Profile change requests |
| `audit_log.csv`          | Complete activity log   |

View the audit log:

```batch
type data\audit_log.csv
```

---
## Using SSL/TLS (Encrypted Communication)
### Step 1: Generate SSL certificates (one-time)
```batch
.\setup-ssl.bat
```

### Step 2: Start with SSL
```batch
.\run-all.bat /ssl
```

Or individually:
```batch
.\run-database.bat 1098 data /ssl
.\run-server.bat 1099 localhost 1098 /ssl
.\run-client.bat localhost 1099 /ssl
```

---

## Architecture 
### Single Laptop

```
YOUR COMPUTER
├── Database Server (port 1098)
│   └── data/ (CSV files)
│
├── Application Server (port 1099)
│   └── connects to Database Server
│
└── Client GUI
    └── connects to Application Server
```

### Multi-Laptop
```
LAPTOP A (Primary)                      LAPTOP B (Backup)
├── Database Server (1098)              ├── Database Server (2098)
│   └── data/                           │   └── data_backup/
│   └── replicates every write ──────→  │
│                                       │
├── Application Server (1099)           ├── Application Server (2099)
│                                       │
└── Client (optional)                   └── Client (failover)
                                            └── connects to A:1099 first
                                            └── switches to localhost:2099 if A dies

LAPTOP C (optional)
└── Client (plain, no failover)
    └── connects to A:1099
```
---
## Stopping Everything
Press `Ctrl+C` in each command prompt window to stop, in this order:
1. Client GUI
2. Application Server
3. Database Server

---
## Troubleshooting
**"Build failed" error when running bat files**
Make sure you are inside the `BHEL_HRM` folder where `compile.bat` is located.
**"Connection refused" error**
Make sure both laptops are on the same WiFi network. Check IPs with `ipconfig`.
**"Backup not available. Retrying in 5s..."**
The primary cannot reach the backup. Either the backup is not running yet, or the IP address in `run-all-primary.bat` is wrong.
**Client shows connection error but no failover popup**
The client was started without failover (only 2 arguments). Use `run-backup.bat` on Laptop B for failover support, or pass 4 arguments: `java -cp out/ client.ClientMain primaryHost primaryPort backupHost backupPort`
**PowerShell says "not recognized as a cmdlet"**
Add `.\` before the bat file name: `.\run-all-primary.bat` instead of `run-all-primary.bat`
---

## Quick Reference

```batch
.\compile.bat                # Compile code
.\run-all.bat                # Run all 3 tiers (single laptop)
.\run-all.bat /ssl           # Same but with SSL encryption
.\run-all-primary.bat        # Primary laptop (with replication)
.\run-backup.bat             # Backup laptop (with failover client)
.\run-client-only.bat        # Plain client for extra laptops
.\run-database.bat           # Database Server only
.\run-server.bat             # Application Server only
.\run-client.bat             # Client GUI only
.\setup-ssl.bat              # Generate SSL certificates
```
