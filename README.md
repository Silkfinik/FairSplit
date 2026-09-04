<p align="center">
  <h1 align="center">FairSplit</h1>
  <p align="center">
    <b>Split expenses effortlessly. Settle debts fairly.</b>
    <br/>
    A modern Android app for tracking shared expenses within groups.
  </p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" alt="Platform"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin" alt="Language"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue?logo=jetpackcompose" alt="UI"/>
  <img src="https://img.shields.io/badge/Backend-Firebase-orange?logo=firebase" alt="Backend"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-brightgreen" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/Target%20SDK-36-brightgreen" alt="Target SDK"/>
  <a href="https://github.com/Silkfinik/FairSplit/actions/workflows/android-ci.yml"><img src="https://github.com/Silkfinik/FairSplit/actions/workflows/android-ci.yml/badge.svg" alt="Android CI"/></a>
</p>

---

## Features

### Expense Management
- **Create & edit expenses** with description, amount, category, and date
- **4 split modes** — Equal, Exact amounts, Percentage-based, and Share-based
- **10 expense categories** — Groceries, Dining, Transport, Housing, Travel, Entertainment, Health, Shopping, Gifts, and Other
- **Full expense history** with change tracking

### Group Collaboration
- **Create groups** with custom names, avatars, and currency
- **Invite members** via shareable invite codes
- **Real-time balance calculation** — see who owes whom at a glance
- **Ghost members** — add people who don't have the app yet, and let them claim their account later

### Payments & Settlements
- **Record payments** between group members
- **Payment workflow** — Pending → Confirmed / Rejected status tracking
- **Smart settlement suggestions** based on calculated balances

### Authentication & Accounts
- **Multiple sign-in methods** — Email/Password, Google Sign-In, and Anonymous (guest) mode
- **Account linking** — upgrade a guest account to a full account without losing data
- **Email verification** flow
- **Custom avatars** with profile management

---

## Architecture

FairSplit follows **Clean Architecture** principles with a clear separation of concerns across three layers:

```
app/
├── app/              # Application entry point, DI setup, navigation
├── core/             # Shared infrastructure
│   ├── common/       # Utilities, auth helpers, services
│   ├── data/         # Repository implementations, mappers, sync, workers
│   ├── database/     # Room DB (entities, DAOs, converters)
│   ├── domain/       # Use cases, repository interfaces, services
│   ├── model/        # Domain models & enums
│   ├── network/      # Firebase DTOs and network models
│   └── ui/           # Shared UI components and theme
└── features/         # Feature modules
    ├── account/      # Profile & account management
    ├── auth/         # Login, registration, email verification
    ├── expenses/     # Expense creation and editing
    ├── groupdetails/ # Group detail view with balances
    ├── groups/       # Group list & creation
    ├── members/      # Member management
    └── payments/     # Payment creation & tracking
```

### Data Flow

```
UI (Compose) → ViewModel → Use Cases → Repository → Data Sources
                                                      ├── Room (offline)
                                                      └── Firestore (cloud)
```

### Offline-First & Real-Time Sync

FairSplit is designed to work **offline-first**:

- **Room Database** serves as the single source of truth for all local data
- **Firestore Listeners** receive real-time updates from the cloud and write to Room
- **Firestore Uploaders** push local changes to the cloud in the background
- **WorkManager** handles reliable background synchronization

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **DI** | Hilt (with KSP) |
| **Local Database** | Room |
| **Cloud Backend** | Firebase (Auth, Firestore, Functions, Storage, Messaging) |
| **Networking** | Firebase SDK |
| **Async** | Kotlin Coroutines + Flow |
| **Navigation** | Jetpack Navigation Compose |
| **Background Work** | WorkManager + Hilt Workers |
| **Image Loading** | Coil |
| **Serialization** | Kotlinx Serialization |
| **Credentials** | Credential Manager + Google Identity |
| **Cloud Functions** | TypeScript (Node.js) on Firebase Functions |
| **Testing** | JUnit 4, MockK, Coroutines Test, Espresso |

---

## Firebase Cloud Functions

The backend logic runs on **Firebase Cloud Functions** (TypeScript, `europe-west1`):

| Function | Type | Description |
|---|---|---|
| `onExpenseWrite` | Firestore Trigger | Reacts to expense document changes |
| `onUserUpdate` | Firestore Trigger | Syncs user profile changes across groups |
| `onGroupWrite` | Firestore Trigger | Handles group document lifecycle events |
| `createInviteCode` | Callable | Generates a unique invite code for a group |
| `joinByInviteCode` | Callable | Allows a user to join a group via invite code |
| `claimGhost` | Callable | Links a ghost member to a real user account |

---

## Testing

FairSplit follows the **Testing Pyramid** methodology, providing comprehensive test coverage across all architectural layers with **77 automated tests** in total:

```
        / \
       /   \        System / E2E Tests (4)
      / UI  \       Compose UI flows, user navigation, device verification
     /-------\
    /         \     Integration Tests (15)
   / Workflows \    Multi-component workflows, offline caching, balance settlement
  /-------------\
 /               \  Unit Tests (58)
/  Domain & Data  \ Domain use cases, ViewModels, business logic, mappers
-------------------
```

### Test Stack
- **JUnit 4** — test runner and assertions
- **MockK** — Kotlin-first mocking framework
- **Kotlinx Coroutines Test** — `runTest`, `advanceUntilIdle`, `UnconfinedTestDispatcher`
- **Turbine** — testing Kotlin Coroutine `Flow` and `StateFlow`
- **AndroidX Test & Jetpack Compose UI Testing** — `createAndroidComposeRule`, Compose semantics, end-to-end screen interactions
- **MainDispatcherRule** — custom JUnit rule for replacing the main dispatcher in ViewModel tests

---

### Test Pyramid Breakdown

#### 1. Unit Tests (58 tests)
Fast, isolated tests verifying core business logic, domain use cases, ViewModels, and data mappers on the JVM without requiring an Android runtime:

| Layer | Test Class | Tests | What's Covered |
|---|---|---|---|
| **Domain Use Cases** | `CalculateGroupBalanceUseCaseTest` | 7 | Comprehensive balance calculation, multi-currency splits, debt simplification algorithm |
| **Domain Use Cases** | `ValidateEmailUseCaseTest` | 6 | Email format validation (valid, empty, no @, no domain, invalid domain, leading @) |
| **Domain Use Cases** | `ValidatePasswordUseCaseTest` | 5 | Password strength rules (too short, no uppercase, no lowercase, no digit, valid) |
| **Domain Service** | `MemberResolutionServiceTest` | 5 | Ghost member resolution, account linking, display name fallback rules |
| **ViewModel** | `CreateExpenseViewModelTest` | 10 | Expense creation, split recalculation (Equal/Exact/Percent/Shares), validation |
| **ViewModel** | `GroupDetailsViewModelTest` | 4 | Sync initialization, screen data loading, ghost member creation, expense deletion |
| **ViewModel** | `AccountViewModelTest` | 5 | Email account linking with full validation (blank fields, invalid email, password mismatch, short password, valid flow) |
| **ViewModel** | `GroupsViewModelTest` | 1 | Group list loading and UI state emission |
| **Data Mappers** | `ExpenseMapperTest` | 4 | Domain `Expense` <-> Firestore / Room DTO transformations |
| **Data Mappers** | `GroupMapperTest` | 3 | Domain `Group` <-> Firestore / Room DTO transformations |
| **Models & Enums** | `ExpenseCategoryTest` | 3 | Category attributes, default icons, and localized labels |
| **Common Utils** | `CollectionExtTest` | 4 | Safe collections and map extensions |
| **Sanity** | `ExampleUnitTest` | 1 | Basic JVM execution sanity check |

#### 2. Integration Tests (15 tests)
Verify interactions between multiple architectural layers (UseCases, Repositories, Offline Storage, Caching, and Business Workflows) working together:

| Integration Suite | Tests | What's Covered |
|---|---|---|
| `ExpenseWorkflowIntegrationTest` | 4 | Full expense addition workflow, balance recomputation across members, multi-payer transactions, rollback on failure |
| `PaymentWorkflowIntegrationTest` | 4 | Debt settlement flow, multi-currency payment handling, debt reduction verification, overpayment edge cases |
| `OfflineRepositoryIntegrationTest` | 4 | Offline-first sync behavior, Room local caching, dirty flag resolution, conflict management |
| `GroupBalanceIntegrationTest` | 3 | End-to-end balance calculation with complex group expenses, ghost members, and debt simplification |

#### 3. System / End-to-End Tests (4 tests)
Run directly on physical Android devices or emulators using Jetpack Compose UI Testing to validate real user journeys, navigation graphs, and UI states:

| Test Class | Tests | What's Covered |
|---|---|---|
| `AuthNavigationSystemTest` | 2 | Navigation from Login to Registration screen and back, UI element rendering, password visibility toggle |
| `RegistrationSystemTest` | 1 | Complete registration user journey: inputting name, email, password, and triggering account creation |
| `ExampleInstrumentedTest` | 1 | Application context, package name, and Android runtime initialization check |

---

### Running Tests

```bash
# Run all JVM tests (Unit + Integration tests)
./gradlew test

# Run a specific test class
./gradlew test --tests "com.silkfinik.fairsplit.integration.ExpenseWorkflowIntegrationTest"

# Run all connected System / UI tests (requires connected device or running emulator)
./gradlew connectedAndroidTest

# Run a specific connected system test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.silkfinik.fairsplit.system.AuthNavigationSystemTest
```

---

## Continuous Integration (CI/CD)

The project includes an automated **GitHub Actions** pipeline ([`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml)) running on every push and pull request to `main`, `master`, and `develop`.

### Pipeline Highlights
- **Build Environment**: Ubuntu runner with **JDK 21** and built-in Gradle build cache.
- **Secure Secret Handling**:
  - `google-services.json` is kept strictly out of git version control.
  - Decodes the real configuration from the repository secret during CI runs.
  - Automatically falls back to generating a valid mock `google-services.json` (with OAuth client support) if the secret is absent, allowing external pull requests and forks to compile and pass all tests seamlessly.
- **Automated Quality Gates**:
  - Runs all **Unit and Integration tests** (`./gradlew test`).
  - Assembles the debug APK (`./gradlew assembleDebug`).
- **Artifacts**:
  - Automatically publishes HTML test reports (`test-reports`).
  - Generates downloadable `fairsplit-debug-apk` on successful builds.

---

## Getting Started

### Prerequisites
- **Android Studio** Ladybug or later
- **JDK 11+**
- A **Firebase project** with Authentication, Firestore, Cloud Functions, Storage, and Messaging enabled
- **Node.js 18+** (for deploying Cloud Functions)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/silkfinik/FairSplit.git
   cd FairSplit
   ```

2. **Configure Firebase**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Download `google-services.json` and place it in `app/`
   - Enable Email/Password and Google sign-in providers in Firebase Auth
   - Create a Firestore database

3. **Deploy Cloud Functions**
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

4. **Build & Run**
   - Open the project in Android Studio
   - Sync Gradle
   - Run on an emulator or device (API 24+)

---

## Domain Models

```kotlin
Group(id, name, currency, inviteCode?, avatarUrl?)
Expense(id, groupId, description, amount, currency, date, creatorId, payers, splits, splitType, ...)
Payment(id, groupId, payerId, receiverId, amount, currency, status, ...)
Member(id, groupId, name, photoUrl?, isGhost, mergedWithUid?, ...)
User(id, email?, displayName?, photoUrl?, isAnonymous, linkedGhostIds, fcmToken?, ...)
```

---

## License

This project is developed as a personal/educational project.
