# Reading Check App - Activity Tracker

Reading Check App is an Android application designed to help users track their activities, mark progress, highlight important items, and receive motivational notifications.

## Features

* **Customizable Theme:** A dark grey background with white text for a clear and comfortable viewing experience.
* **Activity Tracking:** Displays activities in a two-column format with an accompanying checkbox for progress marking.
* **Scrollable Interface:** Designed to handle a large number of activities with a smooth scrolling experience.
* **Contextual Actions:** Long-press on any activity row to access a menu for:
    * **Mark a check:** Mark the activity as completed.
    * **Uncheck:** Unmark a previously checked activity (disabled if not checked).
    * **Mark as important:** Highlight the activity with a yellow marker.
* **Progress Notifications:** Receive congratulatory notifications to celebrate your commitment:
    * After the first 7 days of consistent progress.
    * For every 10 additional checks you complete.
* **Share Progress:** Notifications include a "Share Progress" button, allowing you to share your commitment message.
* **Offline Data Persistence:** All activity data and user progress are securely stored offline using a Realm database, ensuring your data is always available.

## Setup and Development

This application is built using Kotlin for Android on SDK API 29 (Android 10) and managed with Gradle.
To set up the project:

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Ensure your `build.gradle.kts` files (project-level and app-level) include the necessary Realm Kotlin and other AndroidX dependencies as commented in the code.
4.  Build and run on an Android 10+ device or emulator.

## Ownership and License

**Copyright (c) 2025 Lincão.3D. All Rights Reserved.**

This work is protected by copyright. Unauthorized use, reproduction, or distribution of this software, or any portion of it, is strictly prohibited. For inquiries regarding licensing or use, please contact the owner, Lincão.3D.# ReadingCheckApp
# ReadingCheckApp
