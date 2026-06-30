# Work Request Module — HLD & LLD

## 1. High-Level Design (HLD)

### 1.1 Overview

The Work Request module enables a structured workflow for handling inventory shortages in the FTTH Order Management System. When a CSR encounters a port shortage while placing an order, they can raise a work request that gets routed to the Maintenance team for resolution.

### 1.2 Business Context

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────────┐
│     CSR     │──────►  │  Work Request    │──────►  │  Maintenance    │
│  (Raises)   │         │  (Orchestrates)  │         │  (Resolves)     │
└─────────────┘         └──────────────────┘         └─────────────────┘
       ▲                         │                           │
       │                         ▼                           │
       │                 ┌──────────────────┐                │
       └─────────────────│  Notification    │◄───────────────┘
                         │  Engine          │
                         └──────────────────┘
```

### 1.3 Actors & Responsibilities

| Actor | Capabilities |
|-------|-------------|
| **CSR** | Create work requests, view status, receive CLOSED notification |
| **Maintenance** | View queue, Accept, Start, Resolve, Close, Release back to queue |
| **Admin** | View-only access to all work requests (no create/modify) |

### 1.4 State Machine

```
                    ┌──────────────────┐
                    │                  │
                    ▼                  │ (Release)
                  NEW ──────► ACCEPTED ─┘
                    │              │
                    │              ▼
                    │         IN_PROGRESS
                    │              │
                    │              ▼
                    │          RESOLVED
                    │              │
                    ▼              ▼
                  CLOSED ◄────── CLOSED
```

**State Definitions:**

| State | Meaning |
|-------|---------|
| NEW | Request raised, waiting for a Maintenance user to pick up |
| ACCEPTED | A Maintenance user has claimed the task |
| IN_PROGRESS | Active work being done (adding OLT/splitters) |
| RESOLVED | Work completed, pending formal closure |
| CLOSED | Terminal state — task is done or cancelled |

### 1.5 Notification Strategy

| Event | Who Gets Notified |
|-------|-------------------|
| Work request created | All Maintenance users |
| Work request accepted by Maint1 | Notifications cleared from all other Maintenance users |
| Work request released back to queue | All Maintenance users (fresh notification) |
| Work request CLOSED | CSR who raised it |

### 1.6 Duplicate Prevention

Only one active work request (status in NEW/ACCEPTED/IN_PROGRESS) is allowed per pincode + OLT type combination. This prevents multiple CSRs from flooding the queue for the same shortage.



### 2.7 Sequence Diagrams

#### 2.7.1 CSR Reports Shortage

```
CSR                     App                     DB                  Maint Users
 │ POST /add            │                       │                       │
 │─────────────────────►│ add_customer()        │                       │
 │                      │──────────────────────►│ find_free_ont → NULL  │
 │◄─────────────────────│ shortage_info         │                       │
 │                      │                       │                       │
 │ POST /work-requests/create                   │                       │
 │─────────────────────►│ create_work_request() │                       │
 │                      │──────────────────────►│ INSERT work_requests  │
 │                      │ _notify_maintenance() │                       │
 │                      │──────────────────────►│ INSERT notifications ─┼──► 🔔
 │◄─────────────────────│ flash success         │                       │
```

#### 2.7.2 Maintenance Accepts & Resolves

```
Maint1                  App                     DB                  Maint2
 │ 🔔 click notif       │                       │                       │
 │─────────────────────►│ mark_notification_read│                       │
 │                      │──────────────────────►│ UPDATE is_read=TRUE   │
 │◄─────────────────────│ redirect to detail    │                       │
 │                      │                       │                       │
 │ POST /transition     │                       │                       │
 │ new_status=ACCEPTED  │                       │                       │
 │─────────────────────►│ update_status()       │                       │
 │                      │──────────────────────►│ UPDATE status         │
 │                      │──────────────────────►│ DELETE notifs for WR ─┼──► 🔔 cleared
 │◄─────────────────────│ flash success         │                       │
 │                      │                       │                       │
 │ (does inventory work)│                       │                       │
 │                      │                       │                       │
 │ POST /transition     │                       │                       │
 │ new_status=RESOLVED  │                       │                       │
 │─────────────────────►│ update_status()       │                       │
 │                      │──────────────────────►│ UPDATE status         │
 │◄─────────────────────│ flash success         │                       │
```

#### 2.7.3 Release Back to Queue

```
Maint1                  App                     DB                  All Maint
 │ POST /transition     │                       │                       │
 │ new_status=NEW       │                       │                       │
 │─────────────────────►│ update_status()       │                       │
 │                      │──────────────────────►│ SET assigned_to=NULL  │
 │                      │ _notify_maintenance() │                       │
 │                      │──────────────────────►│ INSERT notifications ─┼──► 🔔
 │◄─────────────────────│ flash success         │                       │
```

