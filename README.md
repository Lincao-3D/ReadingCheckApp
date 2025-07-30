# Reading Check App - Activity Tracker

Reading Check App is is an Android application designed to help users track their activities, mark progress, highlight important items, and receive motivational notifications. This version utilizes the Room Persistence Library for robust local data storage and populates activity data from a CSV file.

BProgress is an activity/task management application with the following main features:
1. Architecture and Components:
• Uses MVVM architecture (Model-View-ViewModel)
• Room Database for data persistence
• Fragments for modular interface
• Material Design for UI
• Support for multiple languages (English, Spanish, Japanese, and Portuguese)
2. Main Features:
a) Activity Management:
• List of activities in a RecyclerView
• Marking activities (double click)
• Highlighting important activities (press and hold)
• Interface with customizable cards
b) Theme System:
• Support for light and dark themes
• Smooth transition between themes
• Custom colors for each mode
c) Multilingual:
• Support for 4 languages
• Dynamic language switching
• Persistence of language choice
d) Achievement System:
• Tracking a 50-day streak
• Personalized congratulation dialog
• User feedback capture
3. User Interface:
• Bottom Navigation with two main sections:
• Activities (main list)
• Settings (user preferences)
• Integrated tutorial for new users
• Visual feedback through toasts and dialogs
4. Additional Features:
• Donation system (PIX and PayPal)
• Customized notifications
• Animations and visual effects
• Support for different screen sizes
5. Technical Aspects:
• Coroutines for asynchronous operations
• LiveData for data observation
• ViewBinding for view references
• Room for data persistence
• SharedPreferences for user settings
This is a well-structured application that follows the best practices of modern Android development, offering a rich user experience focused on usability and customization.
Current date: Friday, July 04, 2025, 9:39 AM -03


BProgress é um aplicativo de gerenciamento de atividades/tarefas com as seguintes características principais:
1.Arquitetura e Componentes:
•Utiliza arquitetura MVVM (Model-View-ViewModel)
•Room Database para persistência de dados
•Fragments para interface modular
•Material Design para UI
•Suporte a múltiplos idiomas (Inglês, Espanhol, Japonês e Português)
2.Funcionalidades Principais:
a) Gerenciamento de Atividades:
•Lista de atividades em um RecyclerView
•Marcação de atividades (duplo clique)
•Destaque de atividades importantes (pressionar e segurar)
•Interface com cards personalizáveis
b) Sistema de Temas:
•Suporte a tema claro e escuro
•Transição suave entre temas
•Cores personalizadas para cada modo
c) Multilíngue:
•Suporte a 4 idiomas
•Mudança dinâmica de idioma
•Persistência da escolha do idioma
d) Sistema de Conquistas:
•Rastreamento de sequência de 50 dias
•Diálogo de congratulações personalizado
•Captura de feedback do usuário
3.Interface do Usuário:
•Bottom Navigation com duas seções principais:
•Atividades (lista principal)
•Configurações (preferências do usuário)
•Tutorial integrado para novos usuários
•Feedback visual através de toasts e diálogos
4.Recursos Adicionais:
•Sistema de doações (PIX e PayPal)
•Notificações personalizadas
•Animações e efeitos visuais
•Suporte a diferentes tamanhos de tela
5.Aspectos Técnicos:
•Coroutines para operações assíncronas
•LiveData para observação de dados
•ViewBinding para referências de views
•Room para persistência de dados
•SharedPreferences para configurações do usuário
Este é um aplicativo bem estruturado que segue as melhores práticas de desenvolvimento Android moderno, oferecendo uma experiência rica ao usuário com foco em usabilidade e personalização.
Current date: Friday, July 04, 2025, 9:39 AM -03

## Features

* **Customizable Theme:** A dark grey background with white text for a clear and comfortable viewing experience, with a switch to a "Clean White" theme.
* **CSV Data Loading:** Activity data is loaded from language-specific `activities-{locale_code}.csv` files located in the `assets` folder. The order of activities in the CSV is preserved in the app.
* **Pinned Header Row:** The first row of the active language's CSV (`Activity Name`, `Details`) is displayed as a non-scrollable, pinned header at the top of the screen.
* **Activity Tracking:** Displays activities in a two-column format with an accompanying checkbox for progress marking.
* **Scrollable Interface:** Designed to handle a large number of activities with a smooth scrolling experience for the main activity list.
* **Scroll Position Persistence:** The app remembers your last scroll position in the activities list, so you can continue from where you left off.
* **Contextual Actions:** Long-press on any activity row to access a menu for:
    * **Mark a check:** Mark the activity as completed.
    * **Uncheck:** Unmark a previously checked activity (disabled if not checked).
    * **Mark as important:** Highlight the activity with a yellow marker.
* **Checkbox Tap Warning:** Tapping the checkbox directly (without long-pressing the row) will display a "Try pressing and holding" message anchored to the top-center of the screen. The checkbox itself is not directly interactive for state changes.
* **Bottom Navigation Bar:** Easily navigate between the "Activities" list and a new "Settings" screen using an icon-only bottom navigation bar.
* **Settings Screen:**
    * **Theme Changer:** A switch to toggle between "Dark Mode" (left) and "Light Mode" (right) themes.
    * **Language Changer:** A dropdown menu (Spinner) to select between English, Portuguese, Spanish, and Japanese. The entire app's text and activity data will switch accordingly.
    * **Donation Info:** Copiable text boxes for a PIX key and a PayPal email, along with developer information.
    * **50-Streak Achievement Viewer:** A button (initially hidden) that unlocks and allows viewing the user's recorded feeling after achieving a 50-check streak.
* **First-Time User Tutorial:** On the very first launch, a balloon message will appear below the header, explaining how to interact with the activities list (long-press). The screen will dim to highlight the tutorial.
* **50-Check Streak Special Event:**
    * When the user completes 50 checks, a "congrats.wav" sound plays.
    * A custom dialog appears, congratulating the user and prompting them to write down how they feel.
    * If a `congratulations_screen.jpeg` is found in assets, it will be displayed as a background within this dialog.
    * The user's feeling is saved to the database and can be viewed later from the Settings tab.
* **Progress Notifications:** Receive congratulatory notifications to celebrate your commitment:
    * After the first 7 days of consistent progress.
    * For every 10 additional checks you complete.
* **Share Progress:** Notifications include a "Share Progress" button, allowing you to share your commitment message.
* **Local Data Persistence (Room):** All activity data and user progress are securely stored offline using the Room Persistence Library, ensuring your data is always available.

## Setup and Development

This application is built using Kotlin for Android on SDK API 29 (Android 10) and managed with Gradle.
To set up the project:

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  **Update `build.gradle.kts` (app-level)** by adding `implementation("androidx.media:media:1.2.1")` for media playback.
4.  **Create `app/src/main/assets/activities-eng.csv`**, **`activities-ptbr.csv`**, **`activities-sp.csv`**, **`activities-jp.csv`** with your desired activity data (semicolon separated). Example `activities-eng.csv`:
    ```csv
    Activity Name;Details
    Morning Workout;Cardio & Strength
    Read Book;Chapter 3
    Learn Kotlin;Coroutines
    Meditate;10 minutes
    Plan Day;Prioritize tasks
    Drink Water;8 glasses
    Call Friend;Check-in
    Cook Dinner;Healthy meal
    Evening Walk;Fresh air
    Journal;Reflect on day
    ```
5.  **Create `app/src/main/res/raw/congrats.wav`** (your 0.35s sound file). Create the `raw` directory if it doesn't exist.
6.  **(Optional)** **Create `app/src/main/assets/congratulations_screen.jpeg`** (your splash screen image).
7.  Ensure your `build.gradle.kts` files (project-level and app-level) and `gradle/libs.versions.toml` include the necessary Room, KSP, and other AndroidX dependencies.
8.  **Create `res/values/themes.xml` (for light theme)**.
9.  **Move your existing `res/values/themes.xml` (dark theme) to `res/values-night/themes.xml`**.
10. **Create new string resource files:**
    * `res/values-en/strings.xml`
    * `res/values-pt-rBR/strings.xml`
    * `res/values-es/strings.xml`
    * `res/values-ja/strings.xml`
11. **Create new drawable XML files:**
    * `res/drawable/ic_list.xml`
    * `res/drawable/ic_gear.xml`
    * `res/drawable/rounded_corner_background.xml`
    * `res/drawable/rounded_white_background.xml`
    * `res/drawable/rounded_corner_background_border.xml`
    * `res/drawable/ic_copy.xml`
12. Build and run on an Android 10+ device or emulator. **Remember to clean your project and clear app data on your device/emulator after applying these changes for them to take full effect.**

## Ownership and License

**Copyright (c) 2025 Lincão.3D. All Rights Reserved.**

This work is protected by copyright. Unauthorized use, reproduction, reproduction, distribution, display, or modification of this software, in whole or in part, in any form or by any means, without the express prior written permission of Lincão.3D, is strictly prohibited except for educational purposes or similar good uses.

Lincão.3D retains all rights, title, and interest in and to the software.
