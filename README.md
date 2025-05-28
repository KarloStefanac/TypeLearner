# TypeLearner

TypeLearner is an app which will help you learn to type faster on your phone. You will be provided with a block of text you have to type. After you're done you're provided with results, such as your WPM (Words per Minute), your time and your accuracy.

## Database
Classes & Relations
User: Basic user info (id, username, email, createdAt)
Statistics: Per-user typing stats (userId as foreign key)

One-to-one with User
Tracks WPM, accuracy, top WPM, tests finished

Test: Test templates (id, text, minAccuracy, time)

Standalone entity defining typing tests

TestResult: Individual test attempts (userId, testId as foreign keys)

Many-to-one with User (user can have multiple results)
Many-to-one with Test (test can have multiple results from different users)
Stores performance metrics and pass/fail status

The foreign key relationships are:

Statistics.userId → User.id (1:1)
TestResult.userId → User.id (1:many)
TestResult.testId → Test.id (1:many)


## MVVM
Key Changes and MVVM Implementation

    Repository:
        TypingTestRepository manages User, Statistics, Test, and TestResult data.
        Provides methods to add users, tests, and test results, update statistics, and retrieve user/statistics data.
        Uses in-memory storage (mutable lists) for simplicity; in production, this would typically use Room or a remote database.
    ViewModels:
        HomeViewModel: Minimal state for the home screen (currently just holds user data, which could be expanded for user selection).
        CustomTestViewModel: Manages state for custom test inputs (text, time limit, minimum accuracy).
        TypingViewModel: Handles typing test logic, including test initialization, word input processing, timer management, and result calculation. Stores test data and results in the repository.
        ResultsViewModel: Manages results display, including current test results and overall statistics from the repository.
    UI Components:
        Separated into distinct composables (HomeScreen, CustomTestScreen, TypingScreen, ResultsScreen).
        Each screen observes its respective ViewModel's UI state using collectAsState.
        Navigation remains unchanged, with routes for home, typing (standard/custom), custom test setup, and results.
    Data Class Integration:
        User: Used to associate tests and results with a user (a default user is created for demonstration).
        Statistics: Updated after each test with average WPM, accuracy, top WPM, and test count.
        Test: Stores test configuration (text, minimum accuracy, time limit) for both standard and custom tests.
        TestResult: Records each test's outcome (WPM, accuracy, passed status) and links to user and test IDs.
    Functionality Preservation:
        Home page with buttons for standard and custom tests.
        Custom test screen with inputs for text, time limit, and minimum accuracy.
        Typing screen with word coloring, textbox clearing on space, and time limit enforcement.
        Results screen now includes overall statistics (average WPM, average accuracy, top WPM, tests finished).
        Random 20-word selection for standard tests; custom text support for custom tests.
    Additional Features:
        The passed field in TestResult is set based on whether the accuracy meets or exceeds the minimum accuracy (if specified).
        Statistics are updated with each test, maintaining running averages and top WPM.
