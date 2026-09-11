package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TrashEntity
import com.example.data.model.Album
import com.example.data.model.BatchDetails
import com.example.data.model.DuplicateGroup
import com.example.data.model.MediaDateGroup
import com.example.data.model.MediaDetails
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.SmartCategory
import com.example.data.model.VaultMediaItem
import com.example.data.repository.MediaRepository
import com.example.data.repository.SmartMediaAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GalleryTab {
    PHOTOS,
    ALBUMS,
    FAVORITES,
    TRASH,
    VAULT,
    DUPLICATES
}

enum class MediaFilterType {
    ALL,
    PHOTOS,
    VIDEOS
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class GalleryUiState(
    val isLoading: Boolean = true,
    val currentTab: GalleryTab = GalleryTab.PHOTOS,
    val selectedAlbum: Album? = null,
    val selectedSmartCategory: SmartCategory = SmartCategory.ALL,
    val searchQuery: String = "",
    val mediaFilter: MediaFilterType = MediaFilterType.ALL,
    val rawMediaList: List<MediaItem> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val trashIds: Set<Long> = emptySet(),
    val trashEntities: List<TrashEntity> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedMediaDetails: MediaDetails? = null,
    val isDetailsDialogOpen: Boolean = false,
    val userMessage: String? = null,
    // Multi-Selection State
    val selectedItemIds: Set<Long> = emptySet(),
    val isBatchDetailsDialogOpen: Boolean = false,
    val batchDetails: BatchDetails? = null,
    // Duplicates State
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    // Secret Vault State
    val isVaultUnlocked: Boolean = false,
    val isPinPromptOpen: Boolean = false,
    val vaultItems: List<VaultMediaItem> = emptyList()
) {
    val isSelectionMode: Boolean
        get() = selectedItemIds.isNotEmpty()

    // Media items visible in main gallery (excluding trashed items)
    val activeMediaList: List<MediaItem>
        get() {
            var list = rawMediaList.filterNot { it.id in trashIds }

            if (selectedAlbum != null) {
                list = list.filter { it.bucketName == selectedAlbum.name || it.bucketId == selectedAlbum.id }
            }

            if (selectedSmartCategory != SmartCategory.ALL) {
                list = list.filter { it.smartCategory == selectedSmartCategory }
            }

            if (mediaFilter == MediaFilterType.PHOTOS) {
                list = list.filter { it.mediaType == MediaType.IMAGE }
            } else if (mediaFilter == MediaFilterType.VIDEOS) {
                list = list.filter { it.mediaType == MediaType.VIDEO }
            }

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(q) ||
                    it.bucketName.lowercase().contains(q) ||
                    (it.mediaType == MediaType.VIDEO && (q == "video" || q == "فيديو" || q == "فيديوهات")) ||
                    (it.mediaType == MediaType.IMAGE && (q == "photo" || q == "صورة" || q == "صور"))
                }
            }

            return list.map { it.copy(isFavorite = it.id in favoriteIds) }
        }

    // Favorite items
    val favoriteMediaList: List<MediaItem>
        get() {
            return rawMediaList
                .filter { it.id in favoriteIds && it.id !in trashIds }
                .map { it.copy(isFavorite = true) }
        }
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MediaRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        // Collect favorites and trash IDs from Room
        viewModelScope.launch {
            repository.favoriteIds.collect { favs ->
                _uiState.update { it.copy(favoriteIds = favs.toSet()) }
            }
        }

        viewModelScope.launch {
            repository.trashIds.collect { trashed ->
                _uiState.update { it.copy(trashIds = trashed.toSet()) }
            }
        }

        viewModelScope.launch {
            repository.trashEntities.collect { entities ->
                _uiState.update { it.copy(trashEntities = entities) }
            }
        }

        viewModelScope.launch {
            repository.vaultItems.collect { vaultMedia ->
                _uiState.update { it.copy(vaultItems = vaultMedia) }
            }
        }

        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val media = repository.loadAllMedia()
            
            // If device has 0 media, generate demo photos for seamless instant exploration!
            if (media.isEmpty()) {
                repository.generateDemoMediaIfEmpty()
                val refreshed = repository.loadAllMedia()
                val duplicates = SmartMediaAnalyzer.findDuplicates(refreshed)
                _uiState.update { it.copy(rawMediaList = refreshed, duplicateGroups = duplicates, isLoading = false) }
            } else {
                val duplicates = SmartMediaAnalyzer.findDuplicates(media)
                _uiState.update { it.copy(rawMediaList = media, duplicateGroups = duplicates, isLoading = false) }
            }
        }
    }

    fun setTab(tab: GalleryTab) {
        _uiState.update { it.copy(currentTab = tab, selectedAlbum = null, selectedSmartCategory = SmartCategory.ALL) }
    }

    fun selectAlbum(album: Album?) {
        _uiState.update { it.copy(selectedAlbum = album) }
    }

    fun setSmartCategory(category: SmartCategory) {
        _uiState.update { it.copy(selectedSmartCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setMediaFilter(filter: MediaFilterType) {
        _uiState.update { it.copy(mediaFilter = filter) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val isFav = item.id in _uiState.value.favoriteIds
            repository.toggleFavorite(item, isFav)
        }
    }

    fun moveToTrash(item: MediaItem) {
        viewModelScope.launch {
            repository.moveToTrash(item)
            showMessage("تم نقل العنصر إلى سلة المحذوفات")
        }
    }

    fun restoreFromTrash(mediaId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(mediaId)
            showMessage("تمت استعادة العنصر")
        }
    }

    fun deletePermanently(mediaId: Long) {
        viewModelScope.launch {
            repository.deletePermanently(mediaId)
            showMessage("تم الحذف نهائياً")
        }
    }

    fun clearAllTrash() {
        viewModelScope.launch {
            repository.clearTrash()
            showMessage("تم إفراغ سلة المحذوفات")
        }
    }

    fun openDetails(item: MediaItem) {
        viewModelScope.launch {
            val details = repository.getMediaDetails(item, isArabicLocale = true)
            _uiState.update { it.copy(selectedMediaDetails = details, isDetailsDialogOpen = true) }
        }
    }

    fun closeDetails() {
        _uiState.update { it.copy(isDetailsDialogOpen = false, selectedMediaDetails = null) }
    }

    fun shareMedia(item: MediaItem) {
        repository.shareMedia(item)
    }

    // ==========================================
    // Multi-Selection Operations
    // ==========================================

    fun toggleSelection(mediaId: Long) {
        _uiState.update { state ->
            val current = state.selectedItemIds.toMutableSet()
            if (current.contains(mediaId)) {
                current.remove(mediaId)
            } else {
                current.add(mediaId)
            }
            state.copy(selectedItemIds = current)
        }
    }

    fun selectAll(items: List<MediaItem>) {
        _uiState.update { it.copy(selectedItemIds = items.map { item -> item.id }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItemIds = emptySet(), isBatchDetailsDialogOpen = false) }
    }

    fun deleteSelected() {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        val itemsToDelete = _uiState.value.rawMediaList.filter { it.id in selectedIds }
        viewModelScope.launch {
            for (item in itemsToDelete) {
                repository.moveToTrash(item)
            }
            clearSelection()
            showMessage("تم نقل ${itemsToDelete.size} عناصر إلى سلة المحذوفات")
        }
    }

    fun favoriteSelected() {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        val items = _uiState.value.rawMediaList.filter { it.id in selectedIds }
        viewModelScope.launch {
            for (item in items) {
                val isFav = item.id in _uiState.value.favoriteIds
                if (!isFav) {
                    repository.toggleFavorite(item, false)
                }
            }
            clearSelection()
            showMessage("تمت إضافة ${items.size} عناصر إلى المفضلة")
        }
    }

    fun shareSelected() {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        val itemsToShare = _uiState.value.rawMediaList.filter { it.id in selectedIds }
        repository.shareMultipleMedia(itemsToShare)
    }

    fun moveToVaultSelected() {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        val items = _uiState.value.rawMediaList.filter { it.id in selectedIds }
        viewModelScope.launch {
            repository.moveToVault(items)
            clearSelection()
            loadMedia()
            showMessage("تم تأمين ونقل ${items.size} عناصر إلى الخزنة السرية 🔒")
        }
    }

    fun openBatchDetails() {
        val selectedIds = _uiState.value.selectedItemIds
        val selectedItems = _uiState.value.rawMediaList.filter { it.id in selectedIds }
        if (selectedItems.isEmpty()) return
        val details = SmartMediaAnalyzer.computeBatchDetails(selectedItems, isArabicLocale = true)
        _uiState.update { it.copy(batchDetails = details, isBatchDetailsDialogOpen = true) }
    }

    fun closeBatchDetails() {
        _uiState.update { it.copy(isBatchDetailsDialogOpen = false, batchDetails = null) }
    }

    // ==========================================
    // Secret Vault (الخزنة السرية)
    // ==========================================

    fun openPinPrompt() {
        _uiState.update { it.copy(isPinPromptOpen = true) }
    }

    fun closePinPrompt() {
        _uiState.update { it.copy(isPinPromptOpen = false) }
    }

    fun unlockVault(enteredPin: String): Boolean {
        val actualPin = repository.getVaultPin()
        return if (enteredPin == actualPin) {
            _uiState.update { it.copy(isVaultUnlocked = true, isPinPromptOpen = false, currentTab = GalleryTab.VAULT) }
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _uiState.update { it.copy(isVaultUnlocked = false, currentTab = GalleryTab.PHOTOS) }
    }

    fun restoreFromVault(mediaId: Long) {
        viewModelScope.launch {
            repository.restoreFromVault(mediaId)
            loadMedia()
            showMessage("تمت استعادة العنصر إلى المعرض")
        }
    }

    fun deleteFromVaultPermanently(mediaId: Long) {
        viewModelScope.launch {
            repository.deleteFromVaultPermanently(mediaId)
            showMessage("تم حذف العنصر من الخزنة نهائياً")
        }
    }

    fun changeVaultPin(oldPin: String, newPin: String): Boolean {
        val actualPin = repository.getVaultPin()
        if (oldPin != actualPin) return false
        repository.setVaultPin(newPin)
        showMessage("تم تغيير رمز قفل الخزنة بنجاح")
        return true
    }

    // ==========================================
    // Duplicate Photos Management
    // ==========================================

    fun cleanDuplicateGroup(group: DuplicateGroup) {
        viewModelScope.launch {
            // Keep the first item, trash all other duplicate copies
            val extraCopies = group.items.drop(1)
            for (copy in extraCopies) {
                repository.moveToTrash(copy)
            }
            loadMedia()
            showMessage("تم تنظيف النسخ المكررة وتوفير المساحة")
        }
    }

    fun cleanAllDuplicates() {
        viewModelScope.launch {
            val groups = _uiState.value.duplicateGroups
            var cleanedCount = 0
            for (group in groups) {
                val extraCopies = group.items.drop(1)
                for (copy in extraCopies) {
                    repository.moveToTrash(copy)
                    cleanedCount++
                }
            }
            loadMedia()
            showMessage("تم تنظيف $cleanedCount ملف مكرر بنجاح")
        }
    }

    fun generateDemoMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.generateDemoMediaIfEmpty()
            val media = repository.loadAllMedia()
            val duplicates = SmartMediaAnalyzer.findDuplicates(media)
            _uiState.update { it.copy(rawMediaList = media, duplicateGroups = duplicates, isLoading = false) }
            showMessage("تمت إضافة وسائط تجريبية للمعرض بنجاح")
        }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(userMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
