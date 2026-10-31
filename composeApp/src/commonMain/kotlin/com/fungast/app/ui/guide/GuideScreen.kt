package com.fungast.app.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private class GuideSection(val icon: ImageVector, val title: String, val text: String)

private val SECTIONS = listOf(
    GuideSection(
        Icons.Outlined.Forest,
        "A cosa serve",
        "Fungast è il tuo taccuino dei posti nel bosco: funghi, castagne, frutti di bosco, asparagi e " +
            "tutto quello che raccogli. Ti ricorda dove sono, cosa ci trovi e quando l'hai trovato, e " +
            "ti ci riporta. Funziona anche dove non c'è campo.",
    ),
    GuideSection(
        Icons.Filled.AddLocationAlt,
        "Salvare un posto",
        "Quando sei sul posto tocca \"Salva posto\": la posizione arriva dal GPS (all'aperto bastano " +
            "pochi secondi). Dai un nome, scegli cosa si trova (anche più di una cosa) e, se vuoi, quanto se ne " +
            "trova (poco, buono, abbondante), le date degli ultimi due ritrovamenti, delle foto e delle note.\n\n" +
            "Da casa puoi scegliere \"Link o coordinate\" e incollare un link di Google Maps o delle " +
            "coordinate. Oppure, in Google Maps, tieni premuto sul punto, tocca Condividi e scegli Fungast.\n\n" +
            "Per cambiare un posto già salvato tocca la matita accanto al posto: puoi correggere tutto, " +
            "anche la posizione (con il GPS o con un link), oppure lasciare quella attuale.",
    ),
    GuideSection(
        Icons.Outlined.Explore,
        "Ritrovare un posto",
        "I posti sono nell'elenco; con l'icona della mappa in alto li vedi sulla mappa, colorati per " +
            "categoria. Tocca la bussola accanto a un posto (\"Portami lì\"): una freccia ti indica " +
            "direzione e distanza. Tieni il telefono in piano e lontano da oggetti metallici.",
    ),
    GuideSection(
        Icons.Outlined.Download,
        "Mappa senza rete",
        "Prima di partire, con la connessione, apri la mappa, avvicinati alla zona che ti interessa e " +
            "tocca il pulsante con la freccia in basso a sinistra: quella zona resterà sul telefono e la " +
            "mappa funzionerà anche nel bosco senza campo. Il GPS funziona comunque, anche senza rete.",
    ),
    GuideSection(
        Icons.Outlined.PhotoCamera,
        "Foto",
        "Sotto ogni posto c'è un riquadro con la fotocamera: scatta una foto o scegline dalla galleria. " +
            "Tocca una foto per vederla a schermo intero, scorri per passare alle altre, e usa il cestino " +
            "per eliminarla.",
    ),
    GuideSection(
        Icons.Outlined.Route,
        "Registrare un percorso",
        "Tocca l'icona del percorso in alto: Fungast registra la strada che fai, anche a schermo spento " +
            "(lo vedi dalla notifica). Quando hai finito tocca \"Interrompi\", dai un nome al percorso e " +
            "salvalo. La linea del percorso sulla mappa è utile anche per tornare indietro, per esempio alla " +
            "macchina.\n\nI percorsi salvati sono nel menu ⋮ → \"I miei percorsi\": da lì li mostri sulla " +
            "mappa, li rinomini o li elimini.",
    ),
    GuideSection(
        Icons.Outlined.QrCode2,
        "Passare un posto a un amico",
        "Tocca l'icona del codice QR accanto a un posto. Il tuo amico, nel suo Fungast, apre il menu ⋮ → " +
            "\"Ricevi posto da QR\" e inquadra il codice. Passa tutto tranne le foto.",
    ),
    GuideSection(
        Icons.Outlined.Backup,
        "Backup e cambio di telefono",
        "Dal menu ⋮ scegli \"Esporta backup\" (o \"con foto\", più pesante) e salva o invia il file. " +
            "Sull'altro telefono usa \"Importa da file\": i posti che ci sono già vengono saltati. " +
            "\"Esporta per altre app (GPX)\" serve per le app di mappe e GPS.\n\n" +
            "In più, se è attivo sul telefono, Android salva da solo posti e foto nel backup di Google " +
            "(senza le mappe scaricate, che si possono riscaricare).",
    ),
    GuideSection(
        Icons.Outlined.Lock,
        "I tuoi posti restano tuoi",
        "Niente account e niente server: i posti, le foto e i percorsi stanno solo sul tuo telefono. " +
            "Escono solo quando li condividi tu, o nel backup di Google del tuo telefono.",
    ),
)

/** What the app is for and how to use it; opened on first launch and from the menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(firstLaunch: Boolean, onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (firstLaunch) "Benvenuto in Fungast" else "Guida") },
                navigationIcon = {
                    if (!firstLaunch) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SECTIONS.forEach { GuideCard(it) }
            Text(
                "Puoi riaprire questa guida quando vuoi dal menu ⋮ → \"Guida\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(if (firstLaunch) "Inizia" else "Chiudi")
            }
        }
    }
}

@Composable
private fun GuideCard(section: GuideSection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    section.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(section.title, style = MaterialTheme.typography.titleMedium)
            }
            Text(section.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
