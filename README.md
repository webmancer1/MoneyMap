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
- **Charts:** Vico (planned)

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or higher
- Android SDK with API 24 (Android 7.0) minimum
- Firebase project (for authentication and cloud sync)

### Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project:
   - Package name: `com.example.moneymap`
   - Download `google-services.json`
   - Place it in `app/` directory
3. Enable Authentication:
   - Go to Authentication > Sign-in method
   - Enable Email/Password authentication
4. Enable Firestore (for future sync):
   - Go to Firestore Database
   - Create database in test mode (for development)
   - Update security rules as needed

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

## Project Structure

```
app/src/main/java/com/example/moneymap/
├── data/
│   ├── database/          # Room database, DAOs, converters
│   ├── model/             # Data models (Transaction, Category, etc.)
│   ├── repository/        # Repository implementations
│   └── util/              # Utility classes (DefaultCategories)
├── di/                    # Hilt dependency injection modules
├── navigation/            # Navigation setup and routes
├── ui/
│   ├── screen/            # UI screens (auth, home, transactions, etc.)
│   ├── theme/             # Material 3 theme configuration
│   └── viewmodel/         # ViewModels for each screen
└── MainActivity.kt        # Main activity
```

## Architecture

The app follows MVVM (Model-View-ViewModel) architecture:

- **Model:** Data models and Room entities
- **View:** Jetpack Compose UI screens
- **ViewModel:** Business logic and state management
- **Repository:** Data access layer (Room + Firebase)

## Database Schema

### Tables
- `transactions` - Stores income and expense transactions
- `categories` - Stores transaction categories
- `budgets` - Stores budget information
- `goals` - Stores savings goals
- `debts` - Stores debt information

## Default Categories

### Expense Categories
- Food & Dining
- Transportation
- Shopping
- Bills & Utilities
- Entertainment
- Healthcare
- Education
- Personal Care
- Travel
- Other



