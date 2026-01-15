# MoneyMap - Personal Finance Management App

A comprehensive personal finance management mobile application built with Kotlin and Jetpack Compose that enables users to track expenses, manage budgets, monitor debts, set savings goals, and gain financial insights through analytics and reports.

## Features

### Core Features (Implemented)
-  User Authentication (Email/Password with Firebase)
-  Transaction Management (Add, Edit, Delete)
-  Category Management (Default categories)
-  Home Dashboard with balance and recent transactions
-  Room Database for local storage
-  Budget Planning and Alerts
-  Debt Tracker
-  Recurring Transactions
-  Savings Goals
-  Category Management
-  notifications
-  Analytics with Charts
-  Biometric Authentication
-  Export Functionality (CSV, PDF)
-  Firebase Firestore Sync

## Technical Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Local Database:** Room Database
- **Cloud Backend:** Firebase (Authentication, Firestore, Storage, Cloud Messaging)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Dependency Injection:** Hilt
- **Navigation:** Jetpack Compose Navigation


### Building the Project

1. Clone the repository:
```bash
git clone <repository-url>
cd MoneyMap
```

2. Add `google-services.json` to `app/` directory

3. Open the project in Android Studio

4. Sync Gradle files

5. Build and run the app



## Architecture

The app follows MVVM (Model-View-ViewModel) architecture:

- **Model:** Data models and Room entities
- **View:** Jetpack Compose UI screens
- **ViewModel:** Business logic and state management
- **Repository:** Data access layer (Room + Firebase)


## Default Categories

### Expense Categories
- Food & Dining
- Transportation
- Shopping
- Bills & Utilities
- Entertainment
- Healthcare
- Others



