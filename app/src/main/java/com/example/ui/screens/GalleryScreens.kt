package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.TrashEntity
import com.example.data.model.Album
import com.example.data.model.BatchDetails
import com.example.data.model.DuplicateGroup
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.SmartCategory
import com.example.data.model.VaultMediaItem
import com.example.ui.components.BatchDetailsDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaDateGrid
import com.example.ui.components.MediaDetailsDialog
import com.example.ui.components.MediaThumbnailItem
import com.example.ui.viewmodel.GalleryTab
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.MediaFilterType
import com.example.ui.viewmodel.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryHomeScreen(
    viewModel: GalleryViewModel,
    onOpenMedia: (item: MediaItem, list: List<MediaItem>) -> Unit,
    onOpenSearch: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopAppBar(
                    viewModel = viewModel,
                    uiState = uiState
                )
            } else {
                Column {
                    TopAppBar(
                        title = {
                            if (uiState.selectedAlbum != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.selectAlbum(null) }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "رجوع للألبومات"
                                        )
                                    }
                                    Text(
                                        text = uiState.selectedAlbum!!.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    text = when (uiState.currentTab) {
                                        GalleryTab.PHOTOS -> stringResource(R.string.app_name)
                                        GalleryTab.ALBUMS -> stringResource(R.string.albums_tab)
                                        GalleryTab.FAVORITES -> stringResource(R.string.favorites_tab)
                                        GalleryTab.TRASH -> stringResource(R.string.trash_tab)
                                        GalleryTab.VAULT -> stringResource(R.string.secret_vault)
                                        GalleryTab.DUPLICATES -> stringResource(R.string.duplicate_photos)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onOpenSearch, modifier = Modifier.testTag("search_button")) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.search))
                            }
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.testTag("menu_button")) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "خيارات")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("الخزنة السرية 🔒") },
                                    onClick = {
                                        showMenu = false
                                        if (uiState.isVaultUnlocked) {
                                            viewModel.setTab(GalleryTab.VAULT)
                                        } else {
                                            viewModel.openPinPrompt()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("الصور المكررة 📁") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.setTab(GalleryTab.DUPLICATES)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("توليد صور تجريبية للاختبار") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.generateDemoMedia()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (uiState.themeMode) {
                                                ThemeMode.SYSTEM -> "المظهر: تلقائي (النظام)"
                                                ThemeMode.LIGHT -> "المظهر: فاتح"
                                                ThemeMode.DARK -> "المظهر: داكن"
                                            }
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        val next = when (uiState.themeMode) {
                                            ThemeMode.SYSTEM -> ThemeMode.DARK
                                            ThemeMode.DARK -> ThemeMode.LIGHT
                                            ThemeMode.LIGHT -> ThemeMode.SYSTEM
                                        }
                                        viewModel.setThemeMode(next)
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 1.dp
                    )
                }
            }
        },
        bottomBar = {
            if (!uiState.isSelectionMode) {
                Column {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 1.dp
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        val navItemColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        NavigationBarItem(
                            selected = uiState.currentTab == GalleryTab.PHOTOS && uiState.selectedAlbum == null,
                            onClick = { viewModel.setTab(GalleryTab.PHOTOS) },
                            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.photos_tab)) },
                            label = { Text(stringResource(R.string.photos_tab), fontWeight = if (uiState.currentTab == GalleryTab.PHOTOS && uiState.selectedAlbum == null) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_photos")
                        )
                        NavigationBarItem(
                            selected = (uiState.currentTab == GalleryTab.ALBUMS || uiState.selectedAlbum != null) && uiState.currentTab != GalleryTab.DUPLICATES,
                            onClick = { viewModel.setTab(GalleryTab.ALBUMS) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.albums_tab)) },
                            label = { Text(stringResource(R.string.albums_tab), fontWeight = if (uiState.currentTab == GalleryTab.ALBUMS || uiState.selectedAlbum != null) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_albums")
                        )
                        NavigationBarItem(
                            selected = uiState.currentTab == GalleryTab.VAULT,
                            onClick = {
                                if (uiState.isVaultUnlocked) {
                                    viewModel.setTab(GalleryTab.VAULT)
                                } else {
                                    viewModel.openPinPrompt()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.secret_vault)
                                )
                            },
                            label = { Text(stringResource(R.string.secret_vault), fontWeight = if (uiState.currentTab == GalleryTab.VAULT) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_vault")
                        )
                        NavigationBarItem(
                            selected = uiState.currentTab == GalleryTab.FAVORITES,
                            onClick = { viewModel.setTab(GalleryTab.FAVORITES) },
                            icon = { Icon(Icons.Default.Star, contentDescription = stringResource(R.string.favorites_tab)) },
                            label = { Text(stringResource(R.string.favorites_tab), fontWeight = if (uiState.currentTab == GalleryTab.FAVORITES) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_favorites")
                        )
                        NavigationBarItem(
                            selected = uiState.currentTab == GalleryTab.TRASH,
                            onClick = { viewModel.setTab(GalleryTab.TRASH) },
                            icon = { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trash_tab)) },
                            label = { Text(stringResource(R.string.trash_tab), fontWeight = if (uiState.currentTab == GalleryTab.TRASH) FontWeight.Bold else FontWeight.Medium) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("tab_trash")
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.currentTab) {
                    GalleryTab.PHOTOS -> {
                        PhotosTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            onOpenMedia = onOpenMedia
                        )
                    }
                    GalleryTab.ALBUMS -> {
                        if (uiState.selectedAlbum != null) {
                            // Showing specific album items
                            val albumItems = uiState.activeMediaList
                            val grouped = viewModel.repository.groupMediaByDate(albumItems)
                            if (albumItems.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.Folder,
                                    title = "الألبوم فارغ",
                                    description = "لا توجد صور في هذا الألبوم"
                                )
                            } else {
                                MediaDateGrid(
                                    groupedMedia = grouped,
                                    selectedIds = uiState.selectedItemIds,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onItemClick = { item ->
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelection(item.id)
                                        } else {
                                            onOpenMedia(item, albumItems)
                                        }
                                    },
                                    onItemLongClick = { item ->
                                        viewModel.toggleSelection(item.id)
                                    }
                                )
                            }
                        } else {
                            AlbumsTabContent(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                        }
                    }
                    GalleryTab.FAVORITES -> {
                        FavoritesTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            onOpenMedia = onOpenMedia
                        )
                    }
                    GalleryTab.TRASH -> {
                        TrashTabContent(
                            viewModel = viewModel,
                            uiState = uiState
                        )
                    }
                    GalleryTab.VAULT -> {
                        VaultTabContent(
                            viewModel = viewModel,
                            uiState = uiState
                        )
                    }
                    GalleryTab.DUPLICATES -> {
                        DuplicatesTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            onOpenMedia = onOpenMedia
                        )
                    }
                }
            }
        }
    }

    // Media Details Dialog
    if (uiState.isDetailsDialogOpen && uiState.selectedMediaDetails != null) {
        MediaDetailsDialog(
            details = uiState.selectedMediaDetails!!,
            onDismiss = { viewModel.closeDetails() }
        )
    }

    // Batch Details Dialog
    if (uiState.isBatchDetailsDialogOpen && uiState.batchDetails != null) {
        BatchDetailsDialog(
            details = uiState.batchDetails!!,
            onDismiss = { viewModel.closeBatchDetails() }
        )
    }

    // Vault PIN Dialog
    if (uiState.isPinPromptOpen) {
        VaultPinDialog(
            onDismiss = { viewModel.closePinPrompt() },
            onUnlock = { pin -> viewModel.unlockVault(pin) }
        )
    }
}

@Composable
private fun PhotosTabContent(
    viewModel: GalleryViewModel,
    uiState: com.example.ui.viewmodel.GalleryUiState,
    onOpenMedia: (item: MediaItem, list: List<MediaItem>) -> Unit
) {
    val items = uiState.activeMediaList
    val grouped = viewModel.repository.groupMediaByDate(items)

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips Row (All, Photos, Videos)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val chipBorder = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            FilterChip(
                selected = uiState.mediaFilter == MediaFilterType.ALL,
                onClick = { viewModel.setMediaFilter(MediaFilterType.ALL) },
                label = { Text(stringResource(R.string.all_media), fontWeight = if (uiState.mediaFilter == MediaFilterType.ALL) FontWeight.SemiBold else FontWeight.Normal) },
                shape = RoundedCornerShape(8.dp),
                colors = chipColors,
                border = chipBorder
            )
            FilterChip(
                selected = uiState.mediaFilter == MediaFilterType.PHOTOS,
                onClick = { viewModel.setMediaFilter(MediaFilterType.PHOTOS) },
                label = { Text(stringResource(R.string.photos_only), fontWeight = if (uiState.mediaFilter == MediaFilterType.PHOTOS) FontWeight.SemiBold else FontWeight.Normal) },
                shape = RoundedCornerShape(8.dp),
                colors = chipColors,
                border = chipBorder
            )
            FilterChip(
                selected = uiState.mediaFilter == MediaFilterType.VIDEOS,
                onClick = { viewModel.setMediaFilter(MediaFilterType.VIDEOS) },
                label = { Text(stringResource(R.string.videos_only), fontWeight = if (uiState.mediaFilter == MediaFilterType.VIDEOS) FontWeight.SemiBold else FontWeight.Normal) },
                shape = RoundedCornerShape(8.dp),
                colors = chipColors,
                border = chipBorder
            )
        }

        // Smart AI Classification Categories Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartCategoryChip(
                title = stringResource(R.string.category_all),
                icon = Icons.Default.AllInclusive,
                isSelected = uiState.selectedSmartCategory == SmartCategory.ALL,
                onClick = { viewModel.setSmartCategory(SmartCategory.ALL) }
            )
            SmartCategoryChip(
                title = stringResource(R.string.category_people),
                icon = Icons.Default.Face,
                isSelected = uiState.selectedSmartCategory == SmartCategory.PEOPLE,
                onClick = { viewModel.setSmartCategory(SmartCategory.PEOPLE) }
            )
            SmartCategoryChip(
                title = stringResource(R.string.category_documents),
                icon = Icons.Default.Description,
                isSelected = uiState.selectedSmartCategory == SmartCategory.DOCUMENTS,
                onClick = { viewModel.setSmartCategory(SmartCategory.DOCUMENTS) }
            )
            SmartCategoryChip(
                title = stringResource(R.string.category_nature),
                icon = Icons.Default.Landscape,
                isSelected = uiState.selectedSmartCategory == SmartCategory.NATURE,
                onClick = { viewModel.setSmartCategory(SmartCategory.NATURE) }
            )
            SmartCategoryChip(
                title = stringResource(R.string.category_animals),
                icon = Icons.Default.Pets,
                isSelected = uiState.selectedSmartCategory == SmartCategory.ANIMALS,
                onClick = { viewModel.setSmartCategory(SmartCategory.ANIMALS) }
            )
            SmartCategoryChip(
                title = stringResource(R.string.category_objects),
                icon = Icons.Default.Category,
                isSelected = uiState.selectedSmartCategory == SmartCategory.OBJECTS,
                onClick = { viewModel.setSmartCategory(SmartCategory.OBJECTS) }
            )
        }

        if (items.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.PhotoLibrary,
                title = stringResource(R.string.empty_gallery_title),
                description = stringResource(R.string.empty_gallery_desc),
                actionButtonText = stringResource(R.string.generate_demo_media),
                onActionClick = { viewModel.generateDemoMedia() }
            )
        } else {
            MediaDateGrid(
                groupedMedia = grouped,
                selectedIds = uiState.selectedItemIds,
                isSelectionMode = uiState.isSelectionMode,
                onItemClick = { item ->
                    if (uiState.isSelectionMode) {
                        viewModel.toggleSelection(item.id)
                    } else {
                        onOpenMedia(item, items)
                    }
                },
                onItemLongClick = { item ->
                    viewModel.toggleSelection(item.id)
                }
            )
        }
    }
}

@Composable
private fun AlbumsTabContent(
    viewModel: GalleryViewModel,
    uiState: com.example.ui.viewmodel.GalleryUiState
) {
    val albums = viewModel.repository.extractAlbums(uiState.rawMediaList.filterNot { it.id in uiState.trashIds })

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Special Utility Folders (Secret Vault & Duplicates)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpecialFeatureCard(
                title = stringResource(R.string.secret_vault),
                subtitle = "${uiState.vaultItems.size} عناصر محمية",
                icon = Icons.Default.Lock,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                iconTint = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (uiState.isVaultUnlocked) {
                        viewModel.setTab(GalleryTab.VAULT)
                    } else {
                        viewModel.openPinPrompt()
                    }
                }
            )

            SpecialFeatureCard(
                title = stringResource(R.string.duplicate_photos),
                subtitle = "${uiState.duplicateGroups.size} مجموعات مكررة",
                icon = Icons.Default.FileCopy,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                iconTint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.setTab(GalleryTab.DUPLICATES)
                }
            )
        }

        if (albums.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Folder,
                title = "لا توجد ألبومات إضافية",
                description = "سيتم تجميع ألبومات الكاميرا والتنزيلات تلقائياً هنا."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { viewModel.selectAlbum(album) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("album_${album.name}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.coverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.items_count, album.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FavoritesTabContent(
    viewModel: GalleryViewModel,
    uiState: com.example.ui.viewmodel.GalleryUiState,
    onOpenMedia: (item: MediaItem, list: List<MediaItem>) -> Unit
) {
    val items = uiState.favoriteMediaList
    val grouped = viewModel.repository.groupMediaByDate(items)

    if (items.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.StarBorder,
            title = stringResource(R.string.empty_favorites_title),
            description = stringResource(R.string.empty_favorites_desc)
        )
    } else {
        MediaDateGrid(
            groupedMedia = grouped,
            selectedIds = uiState.selectedItemIds,
            isSelectionMode = uiState.isSelectionMode,
            onItemClick = { item ->
                if (uiState.isSelectionMode) {
                    viewModel.toggleSelection(item.id)
                } else {
                    onOpenMedia(item, items)
                }
            },
            onItemLongClick = { item ->
                viewModel.toggleSelection(item.id)
            }
        )
    }
}

@Composable
private fun TrashTabContent(
    viewModel: GalleryViewModel,
    uiState: com.example.ui.viewmodel.GalleryUiState
) {
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.trashEntities.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.trashEntities.size} عنصر في السلة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showEmptyConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.empty_trash_button), fontSize = 12.sp)
                }
            }
        }

        if (uiState.trashEntities.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Delete,
                title = stringResource(R.string.empty_trash_title),
                description = stringResource(R.string.empty_trash_desc)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.trashEntities, key = { it.mediaId }) { item ->
                    TrashItemRow(
                        item = item,
                        onRestore = { viewModel.restoreFromTrash(item.mediaId) },
                        onPermanentDelete = { viewModel.deletePermanently(item.mediaId) }
                    )
                }
            }
        }
    }

    if (showEmptyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirmDialog = false },
            title = { Text(stringResource(R.string.confirm_empty_trash_title)) },
            text = { Text(stringResource(R.string.confirm_empty_trash_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyConfirmDialog = false
                        viewModel.clearAllTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun TrashItemRow(
    item: TrashEntity,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale("ar")).format(Date(item.deletedAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = item.uriString,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "حُذف في: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = stringResource(R.string.restore),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onPermanentDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = stringResource(R.string.delete_permanently),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onOpenMedia: (item: MediaItem, list: List<MediaItem>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf(uiState.searchQuery) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                viewModel.setSearchQuery(it)
                            },
                            placeholder = { Text(stringResource(R.string.search_hint), fontSize = 14.sp) },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchText = ""
                                        viewModel.setSearchQuery("")
                                    }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .testTag("search_text_field"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.setSearchQuery("")
                            onBack()
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 1.dp
                )
            }
        }
    ) { paddingValues ->
        val results = uiState.activeMediaList
        val grouped = viewModel.repository.groupMediaByDate(results)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (results.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = "لا توجد نتائج",
                    description = "جرب البحث باسم صورة، ألبوم، أو صيغة أخرى."
                )
            } else {
                MediaDateGrid(
                    groupedMedia = grouped,
                    onItemClick = { item -> onOpenMedia(item, results) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    viewModel: GalleryViewModel,
    uiState: com.example.ui.viewmodel.GalleryUiState
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.selected_count, uiState.selectedItemIds.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.clearSelection() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء التحديد")
                }
            },
            actions = {
                // Select All
                IconButton(
                    onClick = { viewModel.selectAll(uiState.activeMediaList) },
                    modifier = Modifier.testTag("select_all_button")
                ) {
                    Icon(imageVector = Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
                }
                // Batch Details (Total size, dates, count)
                IconButton(
                    onClick = { viewModel.openBatchDetails() },
                    modifier = Modifier.testTag("batch_details_button")
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = stringResource(R.string.info))
                }
                // Share
                IconButton(
                    onClick = { viewModel.shareSelected() },
                    modifier = Modifier.testTag("batch_share_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(R.string.share))
                }
                // Move to Secret Vault
                IconButton(
                    onClick = { viewModel.moveToVaultSelected() },
                    modifier = Modifier.testTag("batch_vault_button")
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = stringResource(R.string.secret_vault))
                }
                // Toggle Favorites
                IconButton(
                    onClick = { viewModel.favoriteSelected() },
                    modifier = Modifier.testTag("batch_favorite_button")
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = stringResource(R.string.favorite))
                }
                // Delete
                IconButton(
                    onClick = { viewModel.deleteSelected() },
                    modifier = Modifier.testTag("batch_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}

@Composable
fun SmartCategoryChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SpecialFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VaultTabContent(
    viewModel: GalleryViewModel,
    uiState: com.example.ui.viewmodel.GalleryUiState
) {
    var showChangePinDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Vault Security Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "الخزنة مفتوحة ومحمية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.vaultItems.size} ملفات مخفية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showChangePinDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تغيير PIN", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.lockVault() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قفل الخزنة", fontSize = 12.sp)
                    }
                }
            }
        }

        if (uiState.vaultItems.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Lock,
                title = "الخزنة فارغة",
                description = "يمكنك حماية صورك وفيديوهاتك الحساسة هنا! قم بتحديد أي صور من المعرض ثم اضغط أيقونة القفل لنقلها إلى الخزنة المشفرة."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.vaultItems, key = { it.mediaId }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "محمية في الخزنة • ${item.formattedSize}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.restoreFromVault(item.mediaId) },
                                modifier = Modifier.testTag("restore_vault_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "استعادة للمعرض",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteFromVaultPermanently(item.mediaId) },
                                modifier = Modifier.testTag("delete_vault_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "حذف نهائي",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onChangePin = { oldPin, newPin ->
                viewModel.changeVaultPin(oldPin, newPin)
                showChangePinDialog = false
            }
        )
    }
}

@Composable
fun VaultPinDialog(
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "فتح الخزنة السرية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "أدخل رقم PIN السري للوصول للصور المشفرة (الرمز الافتراضي: 1234)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 8) {
                            pinText = it
                            hasError = false
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    label = { Text("رمز الـ PIN") },
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text("رمز PIN غير صحيح", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_pin_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (pinText.isNotBlank()) {
                                onUnlock(pinText)
                            } else {
                                hasError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_unlock_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("فتح")
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onChangePin: (String, String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "تغيير رمز PIN الخزنة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    label = { Text("رمز الـ PIN الحالي") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    label = { Text("رمز الـ PIN الجديد") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (oldPin.isNotBlank() && newPin.isNotBlank()) {
                                onChangePin(oldPin, newPin)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ")
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicatesTabContent(
    viewModel: GalleryViewModel,
    uiState: com.example.ui.viewmodel.GalleryUiState,
    onOpenMedia: (item: MediaItem, list: List<MediaItem>) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.duplicateGroups.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تم العثور على ${uiState.duplicateGroups.size} مجموعات مكررة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val totalWasted = uiState.duplicateGroups.sumOf { it.wastedBytes }
                        Text(
                            text = "يمكنك توفير ${com.example.data.repository.SmartMediaAnalyzer.formatBytes(totalWasted)} من المساحة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.cleanAllDuplicates() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تنظيف الكل", fontSize = 12.sp)
                    }
                }
            }
        }

        if (uiState.duplicateGroups.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.CheckCircle,
                title = "لا توجد صور مكررة",
                description = "معرضك منظم تماماً ولا يحتوي على صور أو ملفات مكررة تهدر مساحة التخزين!"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.duplicateGroups, key = { it.id }) { group ->
                    DuplicateGroupCard(
                        group = group,
                        onClean = { viewModel.cleanDuplicateGroup(group) },
                        onOpenMedia = { item -> onOpenMedia(item, group.items) }
                    )
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    onClean: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${group.items.size} صور متطابقة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الحجم: ${group.formattedWastedSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onClean,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("إبقاء نسخة واحدة", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(group.items, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenMedia(item) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
