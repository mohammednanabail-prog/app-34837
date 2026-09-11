package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.editor.model.AdjustmentParams
import com.example.editor.model.CropAspectRatio
import com.example.editor.model.CropRectNormalized
import com.example.editor.model.DrawStroke
import com.example.editor.model.EditorState
import com.example.editor.model.EditorToolTab
import com.example.editor.model.FilterType
import com.example.editor.model.ImageOverlayLayer
import com.example.editor.model.RedactMode
import com.example.editor.model.TextLayer
import com.example.editor.model.TransformParams
import com.example.editor.processing.BitmapUtils
import com.example.editor.processing.ImageProcessor
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccessUri: Uri? = null,
    val activeTab: EditorToolTab = EditorToolTab.CROP,
    val currentState: EditorState? = null,
    val previewBitmap: Bitmap? = null,
    val selectedCropRatio: CropAspectRatio = CropAspectRatio.ORIGINAL,
    val selectedFilter: FilterType = FilterType.ORIGINAL,
    // Draw tool properties
    val isEraserActive: Boolean = false,
    val redactMode: RedactMode = RedactMode.BLUR,
    val brushColor: Int = Color.RED,
    val brushSize: Float = 16f,
    val brushOpacity: Float = 1.0f,
    // Active Text editing
    val activeTextLayerId: Long? = null,
    // History
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val toastMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<EditorState>()
    private val redoStack = mutableListOf<EditorState>()

    private var originalFileName: String = "photo.jpg"

    fun initEditor(uri: Uri, fileName: String = "photo.jpg") {
        originalFileName = fileName
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val bitmap = BitmapUtils.decodeSampledBitmapFromUri(getApplication(), uri, 2048, 2048)
            if (bitmap != null) {
                val initialState = EditorState(baseBitmap = bitmap)
                undoStack.clear()
                redoStack.clear()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentState = initialState,
                        canUndo = false,
                        canRedo = false
                    )
                }
                updatePreview(initialState)
            } else {
                _uiState.update { it.copy(isLoading = false, toastMessage = "فشل تحميل الصورة للتعديل") }
            }
        }
    }

    private fun pushHistory(newState: EditorState) {
        val curr = _uiState.value.currentState ?: return
        undoStack.add(curr)
        redoStack.clear()
        _uiState.update {
            it.copy(
                currentState = newState,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
        updatePreview(newState)
    }

    private fun updatePreview(state: EditorState) {
        viewModelScope.launch {
            val preview = ImageProcessor.renderFinalBitmap(state)
            _uiState.update { it.copy(previewBitmap = preview) }
        }
    }

    fun setTab(tab: EditorToolTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // Transformations
    fun rotateLeft() {
        val curr = _uiState.value.currentState ?: return
        val currentAngle = curr.transform.rotationAngle
        val newAngle = (currentAngle - 90f + 360f) % 360f
        val newTransform = curr.transform.copy(rotationAngle = newAngle)
        pushHistory(curr.copy(transform = newTransform))
    }

    fun rotateRight() {
        val curr = _uiState.value.currentState ?: return
        val currentAngle = curr.transform.rotationAngle
        val newAngle = (currentAngle + 90f) % 360f
        val newTransform = curr.transform.copy(rotationAngle = newAngle)
        pushHistory(curr.copy(transform = newTransform))
    }

    fun flipHorizontal() {
        val curr = _uiState.value.currentState ?: return
        val newTransform = curr.transform.copy(flipHorizontal = !curr.transform.flipHorizontal)
        pushHistory(curr.copy(transform = newTransform))
    }

    fun flipVertical() {
        val curr = _uiState.value.currentState ?: return
        val newTransform = curr.transform.copy(flipVertical = !curr.transform.flipVertical)
        pushHistory(curr.copy(transform = newTransform))
    }

    fun setCropRatio(ratio: CropAspectRatio) {
        _uiState.update { it.copy(selectedCropRatio = ratio) }
        val curr = _uiState.value.currentState ?: return
        if (ratio.ratio != null) {
            val bmpW = curr.baseBitmap.width.toFloat()
            val bmpH = curr.baseBitmap.height.toFloat()
            val targetRatio = ratio.ratio
            val currentRatio = bmpW / bmpH

            val newCrop = if (currentRatio > targetRatio) {
                // Width is larger, trim sides
                val targetW = bmpH * targetRatio
                val left = ((bmpW - targetW) / 2f) / bmpW
                val right = 1f - left
                CropRectNormalized(left = left, top = 0f, right = right, bottom = 1f)
            } else {
                // Height is larger, trim top/bottom
                val targetH = bmpW / targetRatio
                val top = ((bmpH - targetH) / 2f) / bmpH
                val bottom = 1f - top
                CropRectNormalized(left = 0f, top = top, right = 1f, bottom = bottom)
            }
            pushHistory(curr.copy(transform = curr.transform.copy(cropRectNormalized = newCrop)))
        } else if (ratio == CropAspectRatio.ORIGINAL) {
            pushHistory(curr.copy(transform = curr.transform.copy(cropRectNormalized = null)))
        }
    }

    // Adjustments
    fun updateAdjustments(newAdjustments: AdjustmentParams) {
        val curr = _uiState.value.currentState ?: return
        val updated = curr.copy(adjustments = newAdjustments)
        // Update state and preview
        _uiState.update { it.copy(currentState = updated) }
        updatePreview(updated)
    }

    fun commitAdjustmentChange() {
        val curr = _uiState.value.currentState ?: return
        pushHistory(curr)
    }

    // Beauty & Face Retouching
    fun updateBeauty(newBeauty: com.example.editor.model.BeautyParams) {
        val curr = _uiState.value.currentState ?: return
        val updated = curr.copy(beauty = newBeauty)
        _uiState.update { it.copy(currentState = updated) }
        updatePreview(updated)
    }

    fun commitBeautyChange() {
        val curr = _uiState.value.currentState ?: return
        pushHistory(curr)
    }

    // Filter
    fun selectFilter(filter: FilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
        val curr = _uiState.value.currentState ?: return
        val updated = curr.copy(filter = filter)
        pushHistory(updated)
    }

    // Drawing & Redacting
    fun addStroke(stroke: DrawStroke) {
        val curr = _uiState.value.currentState ?: return
        val updatedStrokes = curr.strokes + stroke
        pushHistory(curr.copy(strokes = updatedStrokes))
    }

    fun setBrushColor(color: Int) {
        _uiState.update { it.copy(brushColor = color, isEraserActive = false) }
    }

    fun setBrushSize(size: Float) {
        _uiState.update { it.copy(brushSize = size) }
    }

    fun setBrushOpacity(opacity: Float) {
        _uiState.update { it.copy(brushOpacity = opacity) }
    }

    fun toggleEraser(active: Boolean) {
        _uiState.update { it.copy(isEraserActive = active) }
    }

    fun setRedactMode(mode: RedactMode) {
        _uiState.update { it.copy(redactMode = mode) }
    }

    // Text Layers
    fun addTextLayer(text: String = "نص جديد", color: Int = Color.WHITE) {
        val curr = _uiState.value.currentState ?: return
        val newLayer = TextLayer(
            text = text,
            color = color,
            xRatio = 0.5f,
            yRatio = 0.5f
        )
        val updated = curr.copy(textLayers = curr.textLayers + newLayer)
        _uiState.update { it.copy(activeTextLayerId = newLayer.id) }
        pushHistory(updated)
    }

    fun updateTextLayer(layerId: Long, text: String, color: Int, size: Float, hasBg: Boolean) {
        val curr = _uiState.value.currentState ?: return
        val updatedLayers = curr.textLayers.map { layer ->
            if (layer.id == layerId) {
                layer.copy(
                    text = text,
                    color = color,
                    fontSize = size,
                    hasBackground = hasBg
                )
            } else layer
        }
        pushHistory(curr.copy(textLayers = updatedLayers))
    }

    fun removeTextLayer(layerId: Long) {
        val curr = _uiState.value.currentState ?: return
        val updatedLayers = curr.textLayers.filterNot { it.id == layerId }
        pushHistory(curr.copy(textLayers = updatedLayers))
    }

    // Overlay Picture-in-Picture Image
    fun addImageOverlay(bitmap: Bitmap) {
        val curr = _uiState.value.currentState ?: return
        val newLayer = ImageOverlayLayer(
            bitmap = bitmap,
            xRatio = 0.5f,
            yRatio = 0.5f,
            scale = 0.35f
        )
        val updated = curr.copy(overlayLayers = curr.overlayLayers + newLayer)
        pushHistory(updated)
    }

    // Local Algorithmic Auto Enhance
    fun applyAutoEnhance() {
        val curr = _uiState.value.currentState ?: return
        val autoAdj = ImageProcessor.computeAutoEnhancement()
        val updated = curr.copy(adjustments = autoAdj)
        pushHistory(updated)
        _uiState.update { it.copy(toastMessage = "تم تطبيق التحسين التلقائي بنجاح") }
    }

    // Undo & Redo
    fun undo() {
        if (undoStack.isEmpty()) return
        val curr = _uiState.value.currentState ?: return
        redoStack.add(curr)
        val prev = undoStack.removeAt(undoStack.lastIndex)
        _uiState.update {
            it.copy(
                currentState = prev,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true,
                selectedFilter = prev.filter
            )
        }
        updatePreview(prev)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val curr = _uiState.value.currentState ?: return
        undoStack.add(curr)
        val next = redoStack.removeAt(redoStack.lastIndex)
        _uiState.update {
            it.copy(
                currentState = next,
                canUndo = true,
                canRedo = redoStack.isNotEmpty(),
                selectedFilter = next.filter
            )
        }
        updatePreview(next)
    }

    // Save
    fun saveImage(saveAsCopy: Boolean = true) {
        val state = _uiState.value.currentState ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val finalBitmap = ImageProcessor.renderFinalBitmap(state)
            val savedUri = repository.saveEditedBitmap(finalBitmap, originalFileName, saveAsCopy)
            if (savedUri != null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccessUri = savedUri,
                        toastMessage = "تم حفظ الصورة بنجاح في المعرض"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        toastMessage = "فشل حفظ الصورة"
                    )
                }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
