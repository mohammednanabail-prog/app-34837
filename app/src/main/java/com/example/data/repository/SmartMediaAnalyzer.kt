package com.example.data.repository

import com.example.data.model.BatchDetails
import com.example.data.model.DuplicateGroup
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.SmartCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmartMediaAnalyzer {

    fun classifyMedia(item: MediaItem): SmartCategory {
        val lowerName = item.name.lowercase(Locale.ROOT)
        val lowerBucket = item.bucketName.lowercase(Locale.ROOT)

        // 1. Documents & Receipts & Screenshots of text
        val docKeywords = listOf(
            "doc", "document", "scan", "receipt", "invoice", "فاتورة", "مستند",
            "وثيقة", "pdf", "sheet", "note", "paper", "screenshot", "لقطة"
        )
        if (docKeywords.any { lowerName.contains(it) || lowerBucket.contains(it) }) {
            return SmartCategory.DOCUMENTS
        }

        // 2. Animals & Pets
        val animalKeywords = listOf(
            "cat", "dog", "pet", "animal", "قطة", "كلب", "حيوان", "طائر",
            "bird", "horse", "kitten", "puppy", "lion", "أسد", "فرس"
        )
        if (animalKeywords.any { lowerName.contains(it) || lowerBucket.contains(it) }) {
            return SmartCategory.ANIMALS
        }

        // 3. Nature, Landscapes & Outdoors
        val natureKeywords = listOf(
            "sunset", "mountain", "beach", "sea", "sky", "flower", "forest",
            "nature", "شمس", "غروب", "جبل", "بحر", "طبيعة", "سماء", "حديقة",
            "شاطئ", "زهور", "نهر", "lake", "ocean", "tree", "شجر", "خريف", "ربيع"
        )
        if (natureKeywords.any { lowerName.contains(it) || lowerBucket.contains(it) }) {
            return SmartCategory.NATURE
        }

        // 4. People, Portraits, Selfies & Faces
        val peopleKeywords = listOf(
            "selfie", "portrait", "person", "face", "people", "سيلفي",
            "بورتريه", "شخص", "أشخاص", "وجه", "طفل", "baby", "friend", "صديق",
            "family", "عائلة", "profile", "avatar"
        )
        if (peopleKeywords.any { lowerName.contains(it) || lowerBucket.contains(it) }) {
            return SmartCategory.PEOPLE
        }

        // Heuristic: If taken by camera and vertical ratio, likely portrait or person
        if (item.width > 0 && item.height > 0) {
            val ratio = item.height.toFloat() / item.width.toFloat()
            if (ratio in 1.25f..1.5f && lowerBucket.contains("camera")) {
                return SmartCategory.PEOPLE
            }
        }

        // 5. Default fallback to Objects & Vehicles & Artifacts
        return SmartCategory.OBJECTS
    }

    fun findDuplicates(items: List<MediaItem>): List<DuplicateGroup> {
        val nonTrash = items.filter { !it.isTrash }
        val groups = mutableListOf<DuplicateGroup>()

        // 1. Exact size match (high confidence duplicate)
        val bySize = nonTrash.filter { it.size > 0 }.groupBy { it.size }
        for ((size, list) in bySize) {
            if (list.size > 1) {
                val wasted = size * (list.size - 1)
                groups.add(
                    DuplicateGroup(
                        id = "size_${size}",
                        items = list.sortedByDescending { it.dateModified },
                        wastedBytes = wasted
                    )
                )
            }
        }

        // 2. Name similarity matching for copies (e.g., photo_1.jpg, photo_copy.jpg) where sizes are close
        val seenIds = groups.flatMap { g -> g.items.map { it.id } }.toSet()
        val remaining = nonTrash.filter { it.id !in seenIds }

        val byBaseName = remaining.groupBy { item ->
            normalizeFileName(item.name)
        }

        for ((baseName, list) in byBaseName) {
            if (list.size > 1 && baseName.length >= 4) {
                val wasted = list.drop(1).sumOf { it.size }
                groups.add(
                    DuplicateGroup(
                        id = "name_${baseName}",
                        items = list.sortedByDescending { it.dateModified },
                        wastedBytes = wasted
                    )
                )
            }
        }

        return groups.sortedByDescending { it.wastedBytes }
    }

    private fun normalizeFileName(name: String): String {
        val base = name.substringBeforeLast(".")
        return base.replace(Regex("[ _\\-(0-9)]+"), "").lowercase(Locale.ROOT)
    }

    fun computeBatchDetails(items: List<MediaItem>, isArabicLocale: Boolean = true): BatchDetails {
        val count = items.size
        val photoCount = items.count { it.mediaType == MediaType.IMAGE }
        val videoCount = items.count { it.mediaType == MediaType.VIDEO }
        val totalSize = items.sumOf { it.size }

        val avgSize = if (count > 0) totalSize / count else 0L

        val locale = if (isArabicLocale) Locale("ar") else Locale.getDefault()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", locale)

        val minDate = items.minOfOrNull { it.dateTaken } ?: 0L
        val maxDate = items.maxOfOrNull { it.dateTaken } ?: 0L

        val dateSpan = if (minDate > 0 && maxDate > 0) {
            if (dateFormat.format(Date(minDate)) == dateFormat.format(Date(maxDate))) {
                dateFormat.format(Date(minDate))
            } else {
                "${dateFormat.format(Date(minDate))} — ${dateFormat.format(Date(maxDate))}"
            }
        } else {
            "—"
        }

        return BatchDetails(
            totalCount = count,
            photoCount = photoCount,
            videoCount = videoCount,
            totalSizeBytes = totalSize,
            formattedTotalSize = formatBytes(totalSize),
            formattedAverageSize = formatBytes(avgSize),
            dateSpanFormatted = dateSpan
        )
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
