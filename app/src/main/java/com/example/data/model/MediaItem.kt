package com.example.data.model

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class SmartCategory(val displayName: String, val iconName: String) {
    ALL("الكل", "grid"),
    PEOPLE("أشخاص ووجوه", "person"),
    DOCUMENTS("مستندات ووثائق", "document"),
    ANIMALS("حيوانات أليفة", "pets"),
    NATURE("مناظر طبيعية", "landscape"),
    OBJECTS("جمادات ومقتنيات", "category")
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long, // timestamp in ms
    val dateModified: Long,
    val size: Long, // bytes
    val mimeType: String,
    val durationMs: Long = 0, // for video
    val width: Int = 0,
    val height: Int = 0,
    val bucketId: String = "",
    val bucketName: String = "",
    val mediaType: MediaType = MediaType.IMAGE,
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val smartCategory: SmartCategory = SmartCategory.OBJECTS
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }
}

data class Album(
    val id: String,
    val name: String,
    val count: Int,
    val coverUri: Uri?,
    val isVideoAlbum: Boolean = false
)

data class MediaDateGroup(
    val headerTitle: String,
    val dateTimestamp: Long,
    val items: List<MediaItem>
)

data class MediaDetails(
    val name: String,
    val pathOrUri: String,
    val dateFormatted: String,
    val timeFormatted: String,
    val sizeFormatted: String,
    val resolutionFormatted: String,
    val mimeType: String,
    val albumName: String,
    val cameraModel: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val location: String? = null
)

data class DuplicateGroup(
    val id: String,
    val items: List<MediaItem>,
    val wastedBytes: Long
) {
    val formattedWastedSize: String
        get() {
            if (wastedBytes <= 0) return "0 B"
            val kb = wastedBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
                else -> "$wastedBytes B"
            }
        }
}

data class BatchDetails(
    val totalCount: Int,
    val photoCount: Int,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val formattedTotalSize: String,
    val formattedAverageSize: String,
    val dateSpanFormatted: String
)

data class VaultMediaItem(
    val mediaId: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val mimeType: String,
    val mediaType: MediaType,
    val durationMs: Long = 0,
    val formattedSize: String
)

