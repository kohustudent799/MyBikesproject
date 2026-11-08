# My Bikes (Android, Java + XML)

Student: Kazi Obaid Hossain Utshob  |  ID: ______  |  Course: Mobile Application and Development Lab (0613-410)

Track your motorcycles: full-tank mileage, fuel history, monthly expenses, maintenance reminders, BRTA links.

## Features (mapped to the 12 mandatory lab features)
| # | Requirement | Where in My Bikes |
|---|---|---|
| 1 | 5+ screens, Intents | Splash, Login, Register, Main (4 tabs), Add/Edit Bike, Bike Detail, Add/Edit Fuel, Add/Edit Expense, Maintenance, BRTA |
| 2 | Layouts and views | ConstraintLayout / LinearLayout, TextView, ImageView, Button, CardView |
| 3 | Validation | TextInputEditText + setError() on every form |
| 4 | Model classes | model/Bike, FuelRecord, ExpenseRecord, Maintenance |
| 5 | RecyclerView | BikeAdapter, FuelAdapter, ExpenseAdapter (+ ViewHolders) |
| 6 | CRUD + dialog + FAB | AlertDialog (MaterialAlertDialogBuilder), FloatingActionButton in History and Expenses |
| 7 | Firebase Auth | Email register/login, Google Sign-In, logout, stay logged in |
| 8 | Realtime database + listener | DataStore (ValueEventListener) |
| 9 | SharedPreferences | Prefs: remember me, active bike, theme, currency, reminders |
| 10 | Photo | Android Photo Picker, preview, saved in the phone's private app storage |
| 11 | Sensor | Accelerometer: shake the phone to add fuel (MainActivity) |
| 12 | Implicit Intent | ACTION_VIEW (BRTA links), ACTION_DIAL (helpline) |

## Run the app
1. Android Studio (Koala or newer, JDK 17 is bundled) -> File > Open -> choose this folder.
2. Add your own `app/google-services.json` (Firebase setup below).
3. Let Gradle sync, then press Run.

## Firebase setup
1. console.firebase.google.com -> Add project.
2. Add Android app, package name `com.utshob.mybikes`, add your SHA-1 (see below). Download `google-services.json` into the `app/` folder.
3. Build > Authentication > Sign-in method: enable Email/Password and Google.
4. Build > Realtime Database > Create database in LOCKED mode. Paste `firebase/database.rules.json` into Rules and publish.
5. SHA-1: Gradle panel > app > Tasks > android > signingReport (debug). Add it in Project settings > your Android app, then download google-services.json again.
 Note : For SHA-1 RUN IN ANDROID STODIO  TERMINAL= gradlew signingReport
## BRTA links and helpline
The BSP link and hotline are set in `AppConfig.java`. Re-check the hotline on brta.gov.bd before submission.

## Photos
Bike photos are stored on the phone (internal app storage). Only the file name is saved in Firebase, so no Firebase Storage or Blaze plan is needed. Photos do not sync between phones.

## Build the APK
- Debug: Build > Build Bundle(s) / APK(s) > Build APK(s) -> `app/build/outputs/apk/debug/app-debug.apk`
- Release: Build > Generate Signed App Bundle / APK > APK > create a keystore (never commit it). Add the release SHA-1 to Firebase too.
- Common error: `google-services.json is missing` -> put the file in `app/`.
