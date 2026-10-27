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

## Build release (APK da condividere)

La release è firmata con una chiave locale che **non** è nel repository:

- keystore: `~/.android/fungast-release.jks` (alias `fungast`)
- credenziali: `keystore.properties` nella root del progetto (ignorato da git)

`keystore.properties` ha questo formato:

```
storeFile=/percorso/assoluto/fungast-release.jks
storePassword=...
keyAlias=fungast
keyPassword=...
```

Per generare l'APK firmato:

```
./gradlew :composeApp:assembleRelease
```

L'output è in `composeApp/build/outputs/apk/release/composeApp-release.apk`.
Se `keystore.properties` manca, la build va lo stesso ma l'APK esce non firmato.

Note:

- **Fai un backup del keystore e di `keystore.properties`.** Se li perdi, chi ha
  già installato l'app dovrà disinstallarla (perdendo gli spot salvati) per
  installare le versioni nuove.
- Prima di distribuire una nuova versione incrementa `versionCode` (e
  `versionName`) in `composeApp/build.gradle.kts`, altrimenti l'aggiornamento
  non si installa sopra la versione precedente.
- Per installare l'APK sul telefono bisogna consentire l'installazione da
  "origini sconosciute" (Android lo chiede all'apertura del file).
- L'APK pesa ~70 MB perché include tutte le architetture e non è minificato.
