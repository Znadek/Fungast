# Fungast
App open for trck your location for mushroom, Chestnuts or other forest's product

## Struttura

Progetto Kotlin Multiplatform con Compose Multiplatform (Android ora, iOS in futuro).

- `composeApp/src/commonMain` – codice condiviso (UI, logica, dati)
- `composeApp/src/androidMain` – codice e risorse specifici Android (`MainActivity`, manifest)
- `composeApp/src/iosMain` – entry point iOS (`MainViewController`)

## Build

```
./gradlew :composeApp:assembleDebug
```

Richiede l'Android SDK con `platforms;android-35` (impostare `sdk.dir` in `local.properties`).
I target iOS si compilano solo su macOS con Xcode.
