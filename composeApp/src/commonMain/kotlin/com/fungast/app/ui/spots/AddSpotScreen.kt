package com.fungast.app.ui.spots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.fungast.app.data.SpotCategory
import com.fungast.app.location.GeoPoint
import com.fungast.app.location.LinkResolver
import com.fungast.app.location.LocationProvider
import com.fungast.app.location.MapLinkResult
import com.fungast.app.location.locationFromMapLink
import com.fungast.app.photos.PhotoPicker
import com.fungast.app.ui.PlatformBackHandler
import com.fungast.app.ui.formatDate
import com.fungast.app.ui.formatLatLon
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

sealed interface LocationState {
    data object Searching : LocationState
    data object PermissionDenied : LocationState
    data object Unavailable : LocationState
    data class Found(val point: GeoPoint) : LocationState
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSpotScreen(
    locationProvider: LocationProvider,
    linkResolver: LinkResolver,
    /** When set, the screen starts in link mode with this text (shared from another app). */
    initialLinkText: String?,
    photoPicker: PhotoPicker,
    onSave: (
        name: String,
        categories: Set<SpotCategory>,
        notes: String,
        findDates: List<LocalDate>,
        photos: List<String>,
        location: GeoPoint,
    ) -> Unit,
    /** Photos are stored as soon as they are taken: these were removed or left unsaved and can be deleted. */
    onDiscardPhotos: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var categories by rememberSaveable { mutableStateOf(setOf(SpotCategory.MUSHROOMS)) }
    var notes by rememberSaveable { mutableStateOf("") }
    // Kept as the date picker's UTC millis: LocalDate cannot be saved in the instance state
    var lastFoundMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var previousFoundMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var photos by rememberSaveable { mutableStateOf(listOf<String>()) }
    var source by rememberSaveable {
        mutableStateOf(if (initialLinkText != null) LocationSource.LINK else LocationSource.GPS)
    }
    var locationState by remember { mutableStateOf<LocationState>(LocationState.Searching) }
    var linkText by rememberSaveable { mutableStateOf(initialLinkText.orEmpty()) }
    var linkState by remember { mutableStateOf<LinkState>(LinkState.Empty) }
    val scope = rememberCoroutineScope()

    suspend fun locate() {
        locationState = LocationState.Searching
        locationState = when {
            !locationProvider.requestPermission() -> LocationState.PermissionDenied
            else -> locationProvider.currentLocation()?.let { LocationState.Found(it) }
                ?: LocationState.Unavailable
        }
    }

    // Switching to the link cancels a GPS search still running
    LaunchedEffect(source) {
        if (source == LocationSource.GPS && locationState !is LocationState.Found) locate()
    }

    LaunchedEffect(linkText) {
        if (linkText.isBlank()) {
            linkState = LinkState.Empty
            return@LaunchedEffect
        }
        // Wait for the user to stop typing; a paste arrives in one go
        delay(LINK_DEBOUNCE_MS)
        linkState = LinkState.Reading
        linkState = LinkState.Done(locationFromMapLink(linkText, linkResolver))
    }

    val location = when (source) {
        LocationSource.GPS -> (locationState as? LocationState.Found)?.point
        LocationSource.LINK -> ((linkState as? LinkState.Done)?.result as? MapLinkResult.Found)?.point
    }
    val canSave = name.isNotBlank() && categories.isNotEmpty() && location != null
    val leave = {
        onDiscardPhotos(photos)
        onBack()
    }
    PlatformBackHandler(onBack = leave)
    val photoActions = PhotoActions(
        onTakePhoto = { scope.launch { photoPicker.takePhoto()?.let { photos = photos + it } } },
        onPickPhotos = { scope.launch { photos = photos + photoPicker.pickPhotos() } },
        onRemovePhoto = { photo ->
            photos = photos - photo
            onDiscardPhotos(listOf(photo))
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo posto") },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LocationCard(
                source = source,
                onSourceChange = { source = it },
                gpsState = locationState,
                onRetry = { scope.launch { locate() } },
                linkText = linkText,
                onLinkTextChange = { linkText = it },
                linkState = linkState,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                placeholder = { Text("Es. Faggeta sopra il rifugio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text("Cosa si trova", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (categories.isEmpty()) "Scegli almeno una voce" else "Puoi sceglierne più di una",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (categories.isEmpty()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpotCategory.entries.forEach { option ->
                    FilterChip(
                        selected = option in categories,
                        onClick = { categories = if (option in categories) categories - option else categories + option },
                        label = { Text(option.label) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ritrovamenti (facoltativo)", style = MaterialTheme.typography.titleSmall)
                FindDateField(
                    label = "Ultimo ritrovamento",
                    utcMillis = lastFoundMillis,
                    onChange = { lastFoundMillis = it },
                )
                FindDateField(
                    label = "Penultimo ritrovamento",
                    utcMillis = previousFoundMillis,
                    onChange = { previousFoundMillis = it },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Foto (facoltativo)", style = MaterialTheme.typography.titleSmall)
                SpotPhotos(photos = photos, actions = photoActions, thumbnailSize = 72.dp)
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Note") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    location?.let {
                        val findDates = listOfNotNull(lastFoundMillis, previousFoundMillis).map(::utcMillisToDate)
                        onSave(name, categories, notes, findDates, photos, it)
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salva")
            }
        }
    }
}

private enum class LocationSource(val label: String) { GPS("GPS"), LINK("Link o coordinate") }

private sealed interface LinkState {
    data object Empty : LinkState
    data object Reading : LinkState
    data class Done(val result: MapLinkResult) : LinkState
}

private const val LINK_DEBOUNCE_MS = 400L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationCard(
    source: LocationSource,
    onSourceChange: (LocationSource) -> Unit,
    gpsState: LocationState,
    onRetry: () -> Unit,
    linkText: String,
    onLinkTextChange: (String) -> Unit,
    linkState: LinkState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Posizione", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                LocationSource.entries.forEach { option ->
                    SegmentedButton(
                        selected = option == source,
                        onClick = { onSourceChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(option.ordinal, LocationSource.entries.size),
                    ) { Text(option.label) }
                }
            }
            when (source) {
                LocationSource.GPS -> GpsLocation(gpsState, onRetry)
                LocationSource.LINK -> LinkLocation(linkText, onLinkTextChange, linkState)
            }
        }
    }
}

@Composable
private fun GpsLocation(state: LocationState, onRetry: () -> Unit) {
    when (state) {
        LocationState.Searching -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Ricerca del segnale GPS…")
        }
        LocationState.PermissionDenied ->
            Text("Permesso di posizione negato. Concedilo per salvare il posto.")
        LocationState.Unavailable ->
            Text("Posizione non disponibile. Controlla che il GPS sia attivo e riprova all'aperto.")
        is LocationState.Found -> FoundPoint(state.point)
    }
    if (state != LocationState.Searching) {
        OutlinedButton(onClick = onRetry) {
            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Aggiorna posizione", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun LinkLocation(text: String, onTextChange: (String) -> Unit, state: LinkState) {
    val clipboard = LocalClipboardManager.current
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text("Link Google Maps o coordinate") },
        placeholder = { Text("https://maps.app.goo.gl/… o 45.4642, 9.19") },
        maxLines = 3,
        trailingIcon = {
            if (text.isEmpty()) {
                IconButton(onClick = { clipboard.getText()?.text?.let(onTextChange) }) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = "Incolla")
                }
            } else {
                IconButton(onClick = { onTextChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Cancella")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    when (state) {
        LinkState.Empty -> Text(
            "Su Google Maps tieni premuto sul punto, tocca Condividi e copia il link.",
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
        )
        LinkState.Reading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Lettura del link…")
        }
        is LinkState.Done -> when (state.result) {
            is MapLinkResult.Found -> FoundPoint(state.result.point)
            MapLinkResult.NotFound -> Text(
                "Nessuna posizione nel link. Su Google Maps tieni premuto sul punto esatto " +
                    "(compare una puntina rossa), poi Condividi.",
            )
            MapLinkResult.NetworkError -> Text(
                "Per leggere i link brevi (maps.app.goo.gl) serve la connessione. " +
                    "Senza rete puoi incollare le coordinate.",
            )
        }
    }
}

@Composable
private fun FoundPoint(point: GeoPoint) {
    Text(formatLatLon(point.latitude, point.longitude), style = MaterialTheme.typography.bodyLarge)
    val details = listOfNotNull(
        point.accuracyMeters?.let { "precisione ±${it.roundToInt()} m" },
        point.altitude?.let { "altitudine ${it.roundToInt()} m" },
    )
    if (details.isNotEmpty()) {
        Text(
            details.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A date the user can pick or leave empty; [utcMillis] is midnight UTC, as used by the date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindDateField(label: String, utcMillis: Long?, onChange: (Long?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                "$label: ${utcMillis?.let { formatDate(utcMillisToDate(it)) } ?: "non indicato"}",
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
        }
        if (utcMillis != null) {
            IconButton(onClick = { onChange(null) }) {
                Icon(Icons.Filled.Clear, contentDescription = "Togli data")
            }
        }
    }
    if (picking) {
        val todayMillis = remember {
            Clock.System.todayIn(TimeZone.currentSystemDefault()).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = utcMillis,
            selectableDates = object : SelectableDates {
                // A find cannot be in the future
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.selectedDateMillis)
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text("Annulla") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun utcMillisToDate(utcMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date
