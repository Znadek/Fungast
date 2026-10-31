package com.fungast.app.ui.spots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fungast.app.photos.LocalPhotoStore

/** What the user can do with a spot's photos; without it the photos are only shown. */
class PhotoActions(
    val onTakePhoto: () -> Unit,
    val onPickPhotos: () -> Unit,
    val onRemovePhoto: (String) -> Unit,
)

/** Row of thumbnails; tapping one opens it full screen. */
@Composable
fun SpotPhotos(
    photos: List<String>,
    actions: PhotoActions?,
    modifier: Modifier = Modifier,
    thumbnailSize: Dp = 64.dp,
) {
    if (photos.isEmpty() && actions == null) return
    var openedIndex by remember { mutableStateOf<Int?>(null) }

    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(photos, key = { it }) { photo ->
            Photo(
                name = photo,
                maxSize = THUMBNAIL_PX,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(thumbnailSize)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { openedIndex = photos.indexOf(photo) },
            )
        }
        if (actions != null) {
            item { AddPhotoTile(actions, thumbnailSize) }
        }
    }

    openedIndex?.let { index ->
        PhotoViewer(
            photos = photos,
            initialIndex = index,
            onRemove = actions?.onRemovePhoto,
            onClose = { openedIndex = null },
        )
    }
}

@Composable
private fun AddPhotoTile(actions: PhotoActions, size: Dp) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable { menuOpen = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AddAPhoto, contentDescription = "Aggiungi foto")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Scatta foto") },
                leadingIcon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    actions.onTakePhoto()
                },
            )
            DropdownMenuItem(
                text = { Text("Scegli dalla galleria") },
                leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    actions.onPickPhotos()
                },
            )
        }
    }
}

/** Full-screen photos, swiping between them. */
@Composable
private fun PhotoViewer(
    photos: List<String>,
    initialIndex: Int,
    onRemove: ((String) -> Unit)?,
    onClose: () -> Unit,
) {
    // Opened from a thumbnail, so only empty if the photos changed while it was open
    if (photos.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(photos.indices)) { photos.size }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, key = { photos[it] }, modifier = Modifier.fillMaxSize()) { page ->
                Photo(
                    name = photos[page],
                    maxSize = FULL_SCREEN_PX,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().systemBarsPadding().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconColors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                IconButton(onClick = onClose, colors = iconColors) {
                    Icon(Icons.Filled.Close, contentDescription = "Chiudi")
                }
                Text(
                    "${pagerState.currentPage + 1} di ${photos.size}",
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                if (onRemove != null) {
                    IconButton(onClick = { confirmDelete = true }, colors = iconColors) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Elimina foto")
                    }
                }
            }
        }
    }

    if (confirmDelete && onRemove != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare la foto?") },
            text = { Text("La foto verrà cancellata definitivamente.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onRemove(photos[pagerState.currentPage])
                    onClose()
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun Photo(name: String, maxSize: Int, contentScale: ContentScale, modifier: Modifier) {
    val store = LocalPhotoStore.current
    var loading by remember(name, maxSize) { mutableStateOf(true) }
    val bitmap by produceState<ImageBitmap?>(null, name, maxSize) {
        value = store?.load(name, maxSize)
        loading = false
    }
    val image = bitmap
    if (image != null) {
        Image(image, contentDescription = "Foto", contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.BrokenImage, contentDescription = "Foto non disponibile")
            }
        }
    }
}

/** Twice the largest thumbnail size, so thumbnails stay sharp on high-density screens. */
private const val THUMBNAIL_PX = 256
private const val FULL_SCREEN_PX = 1600
