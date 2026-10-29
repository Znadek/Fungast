# Fungast

App per segnare i propri posti nel bosco (funghi, castagne, frutti di bosco, asparagi…), ritrovarli con
bussola e mappa offline, registrare i percorsi e condividere i posti. Build, struttura e release sono nel
`README.md`.

## Decisioni di prodotto (non riproporre alternative)

- **Tutto in locale, niente backend.** Niente account, sync cloud o server: i dati stanno nel DB Room
  sul telefono. L'unica copia fuori è il backup automatico di Android/Google, scelto dall'utente.
- **Niente Bluetooth.** La condivisione avviene tramite QR (un posto alla volta) o file `.fungast`/GPX
  tramite il menu di condivisione.
- **Offline-first.** Mappe offline con MapLibre e tile OpenFreeMap (`liberty`). Non passare a
  tile.openstreetmap.org/OpenTopoMap: le loro regole vietano il download in blocco.
- **Niente Google Play Services**: posizione con `LocationManager` (fused/GPS/network), QR con ZXing.
- **Android prima, iOS dopo.** Le parti iOS (mappa, posizione, foto, percorsi, file) sono stub con
  `TODO`: servono un Mac e Xcode. Il codice iOS deve però compilare (vedi Comandi).

## Lingua e stile

- L'utente scrive in italiano: rispondi in italiano.
- Testi dell'interfaccia in italiano; codice, commenti e messaggi di commit in inglese.
- Rispetta lo stile del codice esistente: commenti brevi che spiegano il *perché*, nomi espliciti.

## Architettura

- Kotlin Multiplatform + Compose Multiplatform. Logica, UI e dati in `commonMain`; in
  `androidMain`/`iosMain` solo ciò che dipende dalla piattaforma, dietro interfacce in `commonMain`
  (`LocationProvider`, `PhotoPicker`, `RouteRecorder`, `FileExchange`, `QrScanner`, …).
- **Servizi legati all'Activity** (permessi, fotocamera, file picker, avvio del servizio dei percorsi)
  registrano i loro launcher alla creazione di `MainActivity` e vengono chiamati dalla UI, non dai
  ViewModel (che vivono più a lungo dell'Activity). I ViewModel lavorano solo su DB e `PhotoStore`.
- Navigazione semplice: enum `Screen` in `App.kt`, nessuna libreria.
- Il DB è la fonte di verità anche per la registrazione dei percorsi: la route "aperta" (`endedAt == null`)
  è quella in registrazione. `RouteRecordingService` (servizio foreground di tipo location) si limita a
  passare le posizioni a `RouteTracker`.
- Foto: file JPEG in `filesDir/photos` (max 1600 px), nel DB ci sono solo i nomi dei file.
- Formato `.fungast`: JSON versionato, separato dalle entity Room (DTO in `FungastFile.kt`). Solo
  aggiunte compatibili: le versioni vecchie dell'app devono continuare a leggere i file nuovi
  (`ignoreUnknownKeys`; per esempio `category` viene ancora scritto accanto a `categories`).

## Database (Room)

- Ogni modifica allo schema richiede: incremento di `version`, una `AutoMigration` (con spec se si
  rinomina o elimina qualcosa) e il JSON dello schema generato in `composeApp/schemas/` da committare.
- Mai migrazioni distruttive: l'utente ha dati veri sul telefono.
- Gli aggiornamenti concorrenti (servizio + UI) usano query `UPDATE` su singola colonna, non `@Update`
  dell'intera riga.

## Comandi

```
./gradlew :composeApp:testDebugUnitTest          # test (commonTest, eseguiti su JVM)
./gradlew :composeApp:assembleDebug              # build Android
./gradlew :composeApp:compileKotlinIosArm64      # verifica che il codice iOS compili (anche su Linux)
./gradlew :composeApp:installDebug               # installa sul telefono collegato via adb
```

- Telefono di test: Motorola moto g55 via USB (`adb devices`). Sopra c'è la build **debug**.
- Installa sempre sopra la versione esistente (`installDebug`). **Non disinstallare mai l'app**: si
  cancellano i dati veri dell'utente (posti, foto, percorsi).
- Per il debug sul telefono: `adb logcat`, `adb shell run-as com.fungast.app ls files databases`.

## Modo di lavorare

- Ogni funzione nuova va provata dall'utente sul telefono **prima** del commit: build, test, iOS compile,
  installazione, poi aspetta il suo "funziona, fai il commit".
- Commit sul branch `kmp-room-spots` e push. Nessuna PR verso `main` finché l'utente non la chiede.
- Le scelte di prodotto ambigue (cosa entra in un export, dove mostrare una funzione…) vanno chieste
  all'utente, con un'opzione consigliata.
- Quando aggiungi o cambi una funzione visibile all'utente, aggiorna anche la guida
  (`ui/guide/GuideScreen.kt`).

## Lavori aperti

- I percorsi non sono ancora nell'export `.fungast` né nel GPX.
- Gli stub iOS da implementare su macOS.
- Backup automatico: limite di 25 MB per app. Le mappe offline sono escluse, le foto incluse (scelta
  dell'utente); con molte foto il backup si ferma.
