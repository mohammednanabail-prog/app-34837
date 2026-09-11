package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.TrashEntity
import com.example.data.local.VaultEntity
import com.example.data.model.Album
import com.example.data.model.BatchDetails
import com.example.data.model.DuplicateGroup
import com.example.data.model.MediaDateGroup
import com.example.data.model.MediaDetails
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.SmartCategory
import com.example.data.model.VaultMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MediaRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val favoriteDao = db.favoriteDao()
    private val trashDao = db.trashDao()
    private val vaultDao = db.vaultDao()

    val favoriteIds: Flow<List<Long>> = favoriteDao.getFavoriteIds()
    val trashIds: Flow<List<Long>> = trashDao.getTrashIds()
    val trashEntities: Flow<List<TrashEntity>> = trashDao.getAllTrash()
    val vaultMediaIds: Flow<List<Long>> = vaultDao.getVaultMediaIds()

    val vaultItems: Flow<List<VaultMediaItem>> = vaultDao.getAllVaultItems().map { list ->
        list.map { entity ->
            val file = File(entity.vaultFilePath)
            val uri = if (file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.parse(entity.originalUriString)
            }
            VaultMediaItem(
                mediaId = entity.mediaId,
                uri = uri,
                name = entity.name,
                dateTaken = entity.dateTaken,
                size = entity.size,
                mimeType = entity.mimeType,
                mediaType = if (entity.mediaType == "VIDEO") MediaType.VIDEO else MediaType.IMAGE,
                durationMs = entity.durationMs,
                formattedSize = formatBytes(entity.size)
            )
        }
    }

    /**
     * Query all images and videos from MediaStore.
     */
    suspend fun loadAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        // 1. Fetch Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateModCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "IMG_$id.jpg"
                    var dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) * 1000L else 0L
                    if (dateTaken <= 0L) dateTaken = dateMod
                    if (dateTaken <= 0L) dateTaken = System.currentTimeMillis()

                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "camera" else "camera"
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Camera" else "Camera"

                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = name,
                            dateTaken = dateTaken,
                            dateModified = dateMod,
                            size = size,
                            mimeType = mime,
                            width = width,
                            height = height,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            mediaType = MediaType.IMAGE
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fetch Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                val dateModCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "VID_$id.mp4"
                    var dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) * 1000L else 0L
                    if (dateTaken <= 0L) dateTaken = dateMod
                    if (dateTaken <= 0L) dateTaken = System.currentTimeMillis()

                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "videos" else "videos"
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Videos" else "Videos"

                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = name,
                            dateTaken = dateTaken,
                            dateModified = dateMod,
                            size = size,
                            mimeType = mime,
                            durationMs = duration,
                            width = width,
                            height = height,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            mediaType = MediaType.VIDEO
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also check app private pictures directory for saved edits
        val appPhotos = loadAppInternalPhotos()
        mediaList.addAll(appPhotos)

        // Exclude items currently secured in the Secret Vault
        val vaultIds = try {
            vaultDao.getVaultMediaIds().firstOrNull()?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }

        val filtered = mediaList.filter { it.id !in vaultIds }

        // Classify each media item with on-device Smart Category
        val classified = filtered.map { item ->
            item.copy(smartCategory = SmartMediaAnalyzer.classifyMedia(item))
        }

        // Sort descending by dateTaken
        classified.sortedByDescending { it.dateTaken }
    }

    private fun loadAppInternalPhotos(): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        try {
            val dir = File(context.filesDir, "gallery_edits")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png"))) {
                        val uri = Uri.fromFile(file)
                        val id = file.name.hashCode().toLong()
                        list.add(
                            MediaItem(
                                id = id,
                                uri = uri,
                                name = file.name,
                                dateTaken = file.lastModified(),
                                dateModified = file.lastModified(),
                                size = file.length(),
                                mimeType = if (file.name.endsWith(".png")) "image/png" else "image/jpeg",
                                width = 1080,
                                height = 1080,
                                bucketId = "edits",
                                bucketName = "معرضي Edits",
                                mediaType = MediaType.IMAGE
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Group items by human-readable Date (Today, Yesterday, or Localized date like 31 أغسطس 2026)
     */
    fun groupMediaByDate(items: List<MediaItem>, isArabicLocale: Boolean = true): List<MediaDateGroup> {
        if (items.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yestYear = calendar.get(Calendar.YEAR)
        val yestDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        val locale = if (isArabicLocale) Locale("ar") else Locale.getDefault()
        val sameYearFormat = SimpleDateFormat("d MMMM", locale)
        val otherYearFormat = SimpleDateFormat("d MMMM yyyy", locale)

        val itemCal = Calendar.getInstance()
        val groups = linkedMapOf<String, MutableList<MediaItem>>()
        val groupTimestamps = linkedMapOf<String, Long>()

        for (item in items) {
            itemCal.timeInMillis = item.dateTaken
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemDayOfYear = itemCal.get(Calendar.DAY_OF_YEAR)

            val header = when {
                itemYear == todayYear && itemDayOfYear == todayDayOfYear -> if (isArabicLocale) "اليوم" else "Today"
                itemYear == yestYear && itemDayOfYear == yestDayOfYear -> if (isArabicLocale) "أمس" else "Yesterday"
                itemYear == todayYear -> sameYearFormat.format(Date(item.dateTaken))
                else -> otherYearFormat.format(Date(item.dateTaken))
            }

            if (!groups.containsKey(header)) {
                groups[header] = mutableListOf()
                groupTimestamps[header] = item.dateTaken
            }
            groups[header]?.add(item)
        }

        return groups.map { (header, list) ->
            MediaDateGroup(
                headerTitle = header,
                dateTimestamp = groupTimestamps[header] ?: 0L,
                items = list
            )
        }
    }

    /**
     * Group media by Albums / Buckets
     */
    fun extractAlbums(items: List<MediaItem>): List<Album> {
        val albumMap = linkedMapOf<String, MutableList<MediaItem>>()

        for (item in items) {
            val key = item.bucketName.ifEmpty { "Other" }
            if (!albumMap.containsKey(key)) {
                albumMap[key] = mutableListOf()
            }
            albumMap[key]?.add(item)
        }

        return albumMap.map { (name, list) ->
            Album(
                id = list.firstOrNull()?.bucketId ?: name,
                name = name,
                count = list.size,
                coverUri = list.firstOrNull()?.uri,
                isVideoAlbum = list.all { it.mediaType == MediaType.VIDEO }
            )
        }.sortedByDescending { it.count }
    }

    /**
     * Read EXIF and file details
     */
    suspend fun getMediaDetails(item: MediaItem, isArabicLocale: Boolean = true): MediaDetails = withContext(Dispatchers.IO) {
        val locale = if (isArabicLocale) Locale("ar") else Locale.getDefault()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", locale)
        val timeFormat = SimpleDateFormat("hh:mm a", locale)

        var camera: String? = null
        var aperture: String? = null
        var exposure: String? = null
        var iso: String? = null
        var focal: String? = null
        var location: String? = null

        if (item.mediaType == MediaType.IMAGE) {
            try {
                val inputStream: InputStream? = if (item.uri.scheme == "file") {
                    File(item.uri.path ?: "").inputStream()
                } else {
                    context.contentResolver.openInputStream(item.uri)
                }

                inputStream?.use { stream ->
                    val exif = ExifInterface(stream)
                    val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                    if (!model.isNullOrBlank()) {
                        camera = if (!make.isNullOrBlank() && !model.contains(make, ignoreCase = true)) {
                            "$make $model"
                        } else {
                            model
                        }
                    }

                    val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                    if (!fNumber.isNullOrBlank()) {
                        aperture = "f/$fNumber"
                    }

                    val expTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                    if (!expTime.isNullOrBlank()) {
                        exposure = "${expTime}s"
                    }

                    val isoSpeed = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    if (!isoSpeed.isNullOrBlank()) {
                        iso = "ISO $isoSpeed"
                    }

                    val focalLen = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                    if (!focalLen.isNullOrBlank()) {
                        focal = "${focalLen}mm"
                    }

                    val latLong = exif.latLong
                    if (latLong != null) {
                        location = String.format(Locale.US, "%.4f, %.4f", latLong[0], latLong[1])
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val resolutionStr = if (item.width > 0 && item.height > 0) {
            val mp = (item.width.toLong() * item.height.toLong()) / 1_000_000.0
            if (mp >= 0.5) {
                "${item.width} × ${item.height} (${String.format(Locale.US, "%.1f", mp)} MP)"
            } else {
                "${item.width} × ${item.height}"
            }
        } else {
            "—"
        }

        MediaDetails(
            name = item.name,
            pathOrUri = item.uri.toString(),
            dateFormatted = dateFormat.format(Date(item.dateTaken)),
            timeFormatted = timeFormat.format(Date(item.dateTaken)),
            sizeFormatted = item.formattedSize,
            resolutionFormatted = resolutionStr,
            mimeType = item.mimeType,
            albumName = item.bucketName.ifEmpty { if (item.mediaType == MediaType.VIDEO) "Videos" else "Photos" },
            cameraModel = camera,
            aperture = aperture,
            exposureTime = exposure,
            iso = iso,
            focalLength = focal,
            location = location
        )
    }

    /**
     * Toggle favorite
     */
    suspend fun toggleFavorite(item: MediaItem, isFav: Boolean) = withContext(Dispatchers.IO) {
        if (isFav) {
            favoriteDao.removeFavorite(item.id)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(item.id, item.uri.toString()))
        }
    }

    /**
     * Move item to local trash bin
     */
    suspend fun moveToTrash(item: MediaItem) = withContext(Dispatchers.IO) {
        trashDao.moveToTrash(
            TrashEntity(
                mediaId = item.id,
                uriString = item.uri.toString(),
                name = item.name,
                dateTaken = item.dateTaken,
                size = item.size,
                mimeType = item.mimeType,
                mediaType = item.mediaType.name
            )
        )
    }

    /**
     * Restore item from trash
     */
    suspend fun restoreFromTrash(mediaId: Long) = withContext(Dispatchers.IO) {
        trashDao.restoreFromTrash(mediaId)
    }

    /**
     * Permanently delete from trash
     */
    suspend fun deletePermanently(mediaId: Long) = withContext(Dispatchers.IO) {
        trashDao.deletePermanently(mediaId)
    }

    /**
     * Empty entire trash
     */
    suspend fun clearTrash() = withContext(Dispatchers.IO) {
        trashDao.clearTrash()
    }

    /**
     * Save edited bitmap to local storage / MediaStore
     */
    suspend fun saveEditedBitmap(
        bitmap: Bitmap,
        originalName: String,
        saveAsCopy: Boolean = true
    ): Uri? = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = if (saveAsCopy) {
            val baseName = originalName.substringBeforeLast(".")
            "EDIT_${baseName}_${timestamp}.jpg"
        } else {
            originalName
        }

        try {
            // Save to app internal edits directory first
            val dir = File(context.filesDir, "gallery_edits")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            // Also try inserting into system MediaStore Pictures directory so other apps / system gallery can see it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/معرضي")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                    return@withContext uri
                }
            }

            return@withContext Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback return file URI
            val dir = File(context.filesDir, "gallery_edits")
            val file = File(dir, fileName)
            if (file.exists()) Uri.fromFile(file) else null
        }
    }

    /**
     * Share media item via Android Intent using FileProvider or content Uri
     */
    fun shareMedia(item: MediaItem) {
        try {
            val shareUri = getShareableUri(item)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifBlank { if (item.mediaType == MediaType.VIDEO) "video/*" else "image/*" }
                putExtra(Intent.EXTRA_STREAM, shareUri)
                clipData = android.content.ClipData.newUri(context.contentResolver, item.name, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة الوسائط").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "تعذر مشاركة الوسائط: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Share multiple media items at once
     */
    fun shareMultipleMedia(items: List<MediaItem>) {
        if (items.isEmpty()) return
        try {
            val uris = ArrayList<Uri>()
            items.forEach { item ->
                uris.add(getShareableUri(item))
            }
            val mimeType = if (items.all { it.mediaType == MediaType.IMAGE }) "image/*"
            else if (items.all { it.mediaType == MediaType.VIDEO }) "video/*"
            else "*/*"

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                if (uris.isNotEmpty()) {
                    val clip = android.content.ClipData.newUri(context.contentResolver, "Media", uris[0])
                    for (i in 1 until uris.size) {
                        clip.addItem(android.content.ClipData.Item(uris[i]))
                    }
                    clipData = clip
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة ${items.size} عناصر").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "تعذر مشاركة العناصر: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getShareableUri(item: MediaItem): Uri {
        return try {
            if (item.uri.scheme == "file") {
                val file = File(item.uri.path ?: "")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else if (item.uri.scheme == "content" && item.uri.authority == "${context.packageName}.fileprovider") {
                item.uri
            } else if (item.uri.scheme == "android.resource") {
                val cacheDir = File(context.cacheDir, "shared_media")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val ext = if (item.mediaType == MediaType.VIDEO) ".mp4" else ".jpg"
                val tempFile = File(cacheDir, "share_${item.id}$ext")
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            } else {
                item.uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            item.uri
        }
    }

    // ==========================================
    // Secret Vault (الخزنة السرية) Methods
    // ==========================================

    suspend fun moveToVault(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val vaultDir = File(context.filesDir, "secret_vault")
        if (!vaultDir.exists()) vaultDir.mkdirs()

        for (item in items) {
            val ext = if (item.mediaType == MediaType.VIDEO) ".mp4" else ".jpg"
            val destFile = File(vaultDir, "VAULT_${item.id}$ext")

            try {
                if (item.uri.scheme == "file") {
                    val srcFile = File(item.uri.path ?: "")
                    if (srcFile.exists()) {
                        srcFile.copyTo(destFile, overwrite = true)
                    }
                } else {
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            vaultDao.addToVault(
                VaultEntity(
                    mediaId = item.id,
                    originalUriString = item.uri.toString(),
                    vaultFilePath = destFile.absolutePath,
                    name = item.name,
                    dateTaken = item.dateTaken,
                    size = if (destFile.exists()) destFile.length() else item.size,
                    mimeType = item.mimeType,
                    mediaType = item.mediaType.name,
                    durationMs = item.durationMs,
                    width = item.width,
                    height = item.height
                )
            )
        }
    }

    suspend fun restoreFromVault(mediaId: Long) = withContext(Dispatchers.IO) {
        val entity = vaultDao.getVaultItem(mediaId) ?: return@withContext
        val vaultFile = File(entity.vaultFilePath)
        if (vaultFile.exists()) {
            val restoredDir = File(context.filesDir, "gallery_edits")
            if (!restoredDir.exists()) restoredDir.mkdirs()
            val restoredFile = File(restoredDir, entity.name)
            vaultFile.copyTo(restoredFile, overwrite = true)
            vaultFile.delete()
        }
        vaultDao.removeFromVault(mediaId)
    }

    suspend fun deleteFromVaultPermanently(mediaId: Long) = withContext(Dispatchers.IO) {
        val entity = vaultDao.getVaultItem(mediaId) ?: return@withContext
        val vaultFile = File(entity.vaultFilePath)
        if (vaultFile.exists()) {
            vaultFile.delete()
        }
        vaultDao.removeFromVault(mediaId)
    }

    fun getVaultPin(): String {
        val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        return prefs.getString("vault_pin", "0000") ?: "0000"
    }

    fun setVaultPin(pin: String) {
        val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("vault_pin", pin).apply()
    }

    private fun formatBytes(bytes: Long): String {
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

    /**
     * Generate diverse, beautiful sample demo media files covering all categories and duplicates.
     */
    suspend fun generateDemoMediaIfEmpty(): Boolean = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "gallery_edits")
        if (!dir.exists()) dir.mkdirs()

        val sampleConfigs = listOf(
            SampleImageConfig("غروب_الشمس.jpg", "غروب الشمس", Color.rgb(255, 94, 98), Color.rgb(255, 153, 102), "Camera", 0),
            SampleImageConfig("غروب_الشمس_نسخة.jpg", "غروب الشمس (نسخة مكررة)", Color.rgb(255, 94, 98), Color.rgb(255, 153, 102), "Camera", -1000L),
            SampleImageConfig("وثيقة_عقد_رسمي.jpg", "وثيقة رسمية ومستند", Color.rgb(240, 243, 246), Color.rgb(220, 226, 235), "Documents", -86400000L),
            SampleImageConfig("بورتريه_شخصي.jpg", "بورتريه شخصي وسيلفي", Color.rgb(238, 205, 163), Color.rgb(205, 153, 114), "Camera", -86400000L * 2),
            SampleImageConfig("قطة_أليفة.jpg", "قطة أليفة جميلة", Color.rgb(255, 175, 189), Color.rgb(255, 195, 160), "Camera", -86400000L * 3),
            SampleImageConfig("سيارة_حديثة.jpg", "سيارة حديثة ومقتنيات", Color.rgb(43, 88, 118), Color.rgb(78, 67, 118), "Camera", -86400000L * 4),
            SampleImageConfig("جبال_الألب.jpg", "جبال الألب الطبيعية", Color.rgb(36, 198, 220), Color.rgb(81, 74, 157), "Camera", -86400000L * 5),
            SampleImageConfig("شاطئ_البحر.jpg", "شاطئ وأمواج البحر", Color.rgb(15, 32, 67), Color.rgb(32, 58, 67), "Camera", -86400000L * 6)
        )

        for (cfg in sampleConfigs) {
            val file = File(dir, cfg.fileName)
            if (!file.exists()) {
                val bitmap = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Draw rich gradient
                val shader = LinearGradient(0f, 0f, 1200f, 1200f, cfg.colorStart, cfg.colorEnd, Shader.TileMode.CLAMP)
                val paint = Paint().apply { this.shader = shader }
                canvas.drawRect(0f, 0f, 1200f, 1200f, paint)

                // Draw geometric artistic aesthetic elements
                val circlePaint = Paint().apply {
                    color = Color.argb(45, 255, 255, 255)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(600f, 500f, 320f, circlePaint)
                canvas.drawCircle(600f, 500f, 200f, circlePaint)

                // Draw decorative lines
                val mountainPaint = Paint().apply {
                    color = Color.argb(80, 0, 0, 0)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val path = android.graphics.Path().apply {
                    moveTo(0f, 1200f)
                    lineTo(300f, 800f)
                    lineTo(600f, 950f)
                    lineTo(900f, 750f)
                    lineTo(1200f, 1200f)
                    close()
                }
                canvas.drawPath(path, mountainPaint)

                // Text label
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 58f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }
                canvas.drawText(cfg.title, 600f, 1100f, textPaint)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                file.setLastModified(System.currentTimeMillis() + cfg.offsetMs)
            }
        }
        true
    }

    private data class SampleImageConfig(
        val fileName: String,
        val title: String,
        val colorStart: Int,
        val colorEnd: Int,
        val bucket: String,
        val offsetMs: Long
    )
}

