# RYZORA AI — Android Application Source Project

**Application ID:** `com.ryzora.aiagent`  
**App Name:** RYZORA AI  
**Brand Tagline:** *Smart Choice. Better Life.*  
**Website:** [https://ryzorabd.com](https://ryzorabd.com) | **Facebook:** [https://www.facebook.com/ryzorabd](https://www.facebook.com/ryzorabd)  
**Location:** Bangladesh | **Currency:** BDT (৳)

---

### Project Structure

```
├── app/
│   ├── build.gradle.kts              # Application Gradle build configuration
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Manifest with permissions and launcher intent
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt   # Scaffold & Navigation Tabs (Overview, Inbox, Orders, Catalog, Simulator, Setup)
│       │   │   ├── data/
│       │   │   │   └── RyzoraRepository.kt  # Repository state flows & e-commerce logic
│       │   │   ├── model/
│       │   │   │   └── Models.kt     # Products, Orders, Chat, and Settings data classes
│       │   │   ├── ui/
│       │   │   │   ├── components/
│       │   │   │   │   └── RyzoraComponents.kt # Material 3 Cards, Metric cards, Status badges
│       │   │   │   ├── screens/
│       │   │   │   │   ├── DashboardScreen.kt        # Executive KPIs, sales in BDT ৳, live alerts
│       │   │   │   │   ├── ConversationsScreen.kt    # Messenger Inbox with Human Takeover controls
│       │   │   │   │   ├── OrdersScreen.kt           # Order management & status lifecycle
│       │   │   │   │   ├── ProductsScreen.kt         # Catalog with stock toggle & price editor
│       │   │   │   │   ├── WebhookSimulatorScreen.kt # Meta Webhook & AI reasoning testbench
│       │   │   │   │   └── AgentSettingsScreen.kt    # Persona & Bangladesh delivery rules
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt  # Obsidian dark tech palette with cyan/emerald accents
│       │   │   │       ├── Theme.kt  # RyzoraTheme (M3 DarkColorScheme)
│       │   │   │       └── Type.kt   # Typography tokens
│       │   └── res/
│       │       ├── drawable/         # RYZORA logos, monogram, and adaptive icon layers
│       │       ├── mipmap-*/         # High-resolution launcher and round icons
│       │       └── values/           # strings.xml, colors.xml, themes.xml
├── gradle/
│   ├── libs.versions.toml            # Version Catalog with all dependencies
│   └── wrapper/                      # Gradle wrapper configuration and jar
├── gradlew                           # Unix Gradle wrapper executable
├── gradlew.bat                       # Windows Gradle wrapper executable
├── build.gradle.kts                  # Root project build configuration
├── settings.gradle.kts               # Project name ("RYZORA AI") and module inclusions
└── gradle.properties                 # JVM and AndroidX build flags
```

---

### How to Open & Build in Android Studio

1. **Extract Archive:**
   Unzip `ryzora-android-source.zip` into any directory on your computer.

2. **Open in Android Studio:**
   - Launch Android Studio (Ladybug / Koala / Hedgehog or newer).
   - Click **File > Open...** and select the unzipped project folder.
   - Android Studio will automatically recognize Gradle and sync the dependencies using `gradle/libs.versions.toml`.

3. **Build APK via Command Line:**
   - **Linux / macOS:**
     ```bash
     ./gradlew assembleDebug
     ```
   - **Windows:**
     ```cmd
     gradlew.bat assembleDebug
     ```
   - The compiled APK will be at:
     `app/build/outputs/apk/debug/app-debug.apk`

4. **Run on Device or Emulator:**
   - Connect an Android phone with USB Debugging enabled, or start an Android Virtual Device (AVD).
   - Press **Run 'app'** (`Shift + F10`) in Android Studio.
