package com.example.editor.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.PointF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Crop32
import androidx.compose.material.icons.filled.Crop54
import androidx.compose.material.icons.filled.Crop75
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.editor.model.AdjustmentParams
import com.example.editor.model.CropAspectRatio
import com.example.editor.model.DrawStroke
import com.example.editor.model.EditorToolTab
import com.example.editor.model.FilterType
import com.example.editor.model.RedactMode
import com.example.editor.model.TextLayer
import com.example.editor.processing.BitmapUtils
import com.example.ui.viewmodel.EditorViewModel
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    viewModel: EditorViewModel,
    onClose: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showSaveSheet by remember { mutableStateOf(false) }
    var textInputDialogLayer by remember { mutableStateOf<TextLayer?>(null) }

    // Overlay photo picker launcher
    val overlayPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream: InputStream? = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bmp != null) {
                    viewModel.addImageOverlay(bmp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(uiState.saveSuccessUri) {
        uiState.saveSuccessUri?.let { uri ->
            Toast.makeText(context, "تم حفظ الصورة بنجاح", Toast.LENGTH_SHORT).show()
            onSaved(uri)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101216))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Editor Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("editor_close")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء", tint = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.undo),
                            tint = if (uiState.canUndo) Color.White else Color.Gray.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = stringResource(R.string.redo),
                            tint = if (uiState.canRedo) Color.White else Color.Gray.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.applyAutoEnhance() },
                        modifier = Modifier.testTag("auto_enhance_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.auto_enhance),
                            tint = Color(0xFFFFD700)
                        )
                    }
                }

                Button(
                    onClick = { showSaveSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("save_button")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }

            // 2. Center Preview & Interaction Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else if (uiState.previewBitmap != null) {
                    EditorCanvas(
                        preview = uiState.previewBitmap!!,
                        activeTab = uiState.activeTab,
                        brushColor = uiState.brushColor,
                        brushSize = uiState.brushSize,
                        brushOpacity = uiState.brushOpacity,
                        isEraser = uiState.isEraserActive,
                        redactMode = uiState.redactMode,
                        onAddStroke = { stroke -> viewModel.addStroke(stroke) }
                    )
                }
            }

            // 3. Tool Specific Adjustment Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF181A20),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .navigationBarsPadding()
                ) {
                    // Tool panel content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (uiState.activeTab) {
                            EditorToolTab.CROP -> CropRotateToolPanel(viewModel, uiState)
                            EditorToolTab.ADJUST -> AdjustmentToolPanel(viewModel, uiState)
                            EditorToolTab.BEAUTY -> BeautyToolPanel(viewModel, uiState)
                            EditorToolTab.FILTERS -> FiltersToolPanel(viewModel, uiState)
                            EditorToolTab.DRAW -> DrawToolPanel(viewModel, uiState)
                            EditorToolTab.TEXT -> TextToolPanel(
                                viewModel = viewModel,
                                uiState = uiState,
                                onOpenTextInput = { layer -> textInputDialogLayer = layer }
                            )
                            EditorToolTab.REDACT -> RedactToolPanel(viewModel, uiState)
                            EditorToolTab.OVERLAY -> OverlayToolPanel(
                                onPickOverlay = { overlayPicker.launch("image/*") }
                            )
                        }
                    }

                    // Bottom category icons row
                    EditorBottomToolTabs(
                        activeTab = uiState.activeTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                }
            }
        }

        // Saving Progress Overlay
        if (uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.saving),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Save Choice Bottom Sheet
    if (showSaveSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSaveSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "خيارات الحفظ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        showSaveSheet = false
                        viewModel.saveImage(saveAsCopy = true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save_as_new))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        showSaveSheet = false
                        viewModel.saveImage(saveAsCopy = false)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.replace_original))
                }
            }
        }
    }

    // Text Layer Edit Dialog
    if (textInputDialogLayer != null) {
        var textValue by remember { mutableStateOf(textInputDialogLayer!!.text) }
        var hasBg by remember { mutableStateOf(textInputDialogLayer!!.hasBackground) }

        AlertDialog(
            onDismissRequest = { textInputDialogLayer = null },
            title = { Text("تعديل النص") },
            text = {
                Column {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text("النص") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("خلفية النص (Pill)")
                        androidx.compose.material3.Switch(
                            checked = hasBg,
                            onCheckedChange = { hasBg = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val layer = textInputDialogLayer!!
                    viewModel.updateTextLayer(
                        layerId = layer.id,
                        text = textValue,
                        color = layer.color,
                        size = layer.fontSize,
                        hasBg = hasBg
                    )
                    textInputDialogLayer = null
                }) {
                    Text("تطبيق")
                }
            },
            dismissButton = {
                TextButton(onClick = { textInputDialogLayer = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun EditorCanvas(
    preview: Bitmap,
    activeTab: EditorToolTab,
    brushColor: Int,
    brushSize: Float,
    brushOpacity: Float,
    isEraser: Boolean,
    redactMode: RedactMode,
    onAddStroke: (DrawStroke) -> Unit
) {
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }
    val currentPoints = remember { mutableListOf<PointF>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                canvasWidth = it.width.toFloat().coerceAtLeast(1f)
                canvasHeight = it.height.toFloat().coerceAtLeast(1f)
            }
            .then(
                if (activeTab == EditorToolTab.DRAW || activeTab == EditorToolTab.REDACT) {
                    Modifier.pointerInput(activeTab, brushColor, brushSize, brushOpacity, isEraser, redactMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints.clear()
                                currentPoints.add(PointF(offset.x / canvasWidth, offset.y / canvasHeight))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints.add(PointF(change.position.x / canvasWidth, change.position.y / canvasHeight))
                            },
                            onDragEnd = {
                                if (currentPoints.size >= 2) {
                                    val isRedact = activeTab == EditorToolTab.REDACT
                                    val stroke = DrawStroke(
                                        points = currentPoints.toList(),
                                        color = brushColor,
                                        strokeWidth = brushSize,
                                        alpha = brushOpacity,
                                        isEraser = isEraser && !isRedact,
                                        isRedactBlur = isRedact && redactMode == RedactMode.BLUR,
                                        isRedactPixelate = isRedact && redactMode == RedactMode.PIXELATE
                                    )
                                    onAddStroke(stroke)
                                }
                                currentPoints.clear()
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = preview.asImageBitmap(),
            contentDescription = "معاينة التعديل",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EditorBottomToolTabs(
    activeTab: EditorToolTab,
    onTabSelected: (EditorToolTab) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolTabItem(
            icon = Icons.Default.Crop,
            label = stringResource(R.string.crop_rotate),
            isSelected = activeTab == EditorToolTab.CROP,
            onClick = { onTabSelected(EditorToolTab.CROP) },
            testTag = "tab_crop"
        )
        ToolTabItem(
            icon = Icons.Default.Tune,
            label = stringResource(R.string.adjust),
            isSelected = activeTab == EditorToolTab.ADJUST,
            onClick = { onTabSelected(EditorToolTab.ADJUST) },
            testTag = "tab_adjust"
        )
        ToolTabItem(
            icon = Icons.Default.Face,
            label = stringResource(R.string.beauty_retouch),
            isSelected = activeTab == EditorToolTab.BEAUTY,
            onClick = { onTabSelected(EditorToolTab.BEAUTY) },
            testTag = "tab_beauty"
        )
        ToolTabItem(
            icon = Icons.Default.Filter,
            label = stringResource(R.string.filters),
            isSelected = activeTab == EditorToolTab.FILTERS,
            onClick = { onTabSelected(EditorToolTab.FILTERS) },
            testTag = "tab_filters"
        )
        ToolTabItem(
            icon = Icons.Default.Brush,
            label = stringResource(R.string.draw),
            isSelected = activeTab == EditorToolTab.DRAW,
            onClick = { onTabSelected(EditorToolTab.DRAW) },
            testTag = "tab_draw"
        )
        ToolTabItem(
            icon = Icons.Default.TextFields,
            label = stringResource(R.string.text_tool),
            isSelected = activeTab == EditorToolTab.TEXT,
            onClick = { onTabSelected(EditorToolTab.TEXT) },
            testTag = "tab_text"
        )
        ToolTabItem(
            icon = Icons.Default.PrivacyTip,
            label = stringResource(R.string.privacy_redact),
            isSelected = activeTab == EditorToolTab.REDACT,
            onClick = { onTabSelected(EditorToolTab.REDACT) },
            testTag = "tab_redact"
        )
        ToolTabItem(
            icon = Icons.Default.AddPhotoAlternate,
            label = stringResource(R.string.overlay_image),
            isSelected = activeTab == EditorToolTab.OVERLAY,
            onClick = { onTabSelected(EditorToolTab.OVERLAY) },
            testTag = "tab_overlay"
        )
    }
}

@Composable
private fun ToolTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.LightGray
        )
    }
}

// 1. Crop & Rotate Panel
@Composable
private fun CropRotateToolPanel(
    viewModel: EditorViewModel,
    uiState: com.example.ui.viewmodel.EditorUiState
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Quick Action Buttons (Rotate Left, Rotate Right, Flip H, Flip V)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { viewModel.rotateLeft() }) {
                Icon(Icons.Default.Rotate90DegreesCcw, "تدوير يسار", tint = Color.White)
            }
            IconButton(onClick = { viewModel.rotateRight() }) {
                Icon(Icons.Default.Rotate90DegreesCw, "تدوير يمين", tint = Color.White)
            }
            IconButton(onClick = { viewModel.flipHorizontal() }) {
                Icon(Icons.Default.Flip, "قلب أفقي", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Crop Ratio Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropAspectRatio.values().forEach { ratio ->
                FilterChip(
                    selected = uiState.selectedCropRatio == ratio,
                    onClick = { viewModel.setCropRatio(ratio) },
                    label = { Text(ratio.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

// 2. Adjustments Panel
@Composable
private fun AdjustmentToolPanel(
    viewModel: EditorViewModel,
    uiState: com.example.ui.viewmodel.EditorUiState
) {
    val adjustments = uiState.currentState?.adjustments ?: AdjustmentParams()
    val scrollState = rememberScrollState()

    var activeParam by remember { mutableStateOf("brightness") }

    val currentVal = when (activeParam) {
        "brightness" -> adjustments.brightness
        "contrast" -> adjustments.contrast
        "saturation" -> adjustments.saturation
        "temperature" -> adjustments.temperature
        "tint" -> adjustments.tint
        "highlights" -> adjustments.highlights
        "shadows" -> adjustments.shadows
        "whites" -> adjustments.whites
        "blacks" -> adjustments.blacks
        "vignette" -> adjustments.vignette
        else -> adjustments.brightness
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Slider + Value Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = currentVal,
                onValueChange = { newVal ->
                    val updated = when (activeParam) {
                        "brightness" -> adjustments.copy(brightness = newVal)
                        "contrast" -> adjustments.copy(contrast = newVal)
                        "saturation" -> adjustments.copy(saturation = newVal)
                        "temperature" -> adjustments.copy(temperature = newVal)
                        "tint" -> adjustments.copy(tint = newVal)
                        "highlights" -> adjustments.copy(highlights = newVal)
                        "shadows" -> adjustments.copy(shadows = newVal)
                        "whites" -> adjustments.copy(whites = newVal)
                        "blacks" -> adjustments.copy(blacks = newVal)
                        "vignette" -> adjustments.copy(vignette = newVal)
                        else -> adjustments
                    }
                    viewModel.updateAdjustments(updated)
                },
                onValueChangeFinished = {
                    viewModel.commitAdjustmentChange()
                },
                valueRange = if (activeParam == "vignette") 0f..100f else -100f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = "${currentVal.toInt()}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(40.dp)
                    .padding(start = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        // Adjust Parameter Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdjustChip(name = stringResource(R.string.brightness), isSelected = activeParam == "brightness") { activeParam = "brightness" }
            AdjustChip(name = stringResource(R.string.contrast), isSelected = activeParam == "contrast") { activeParam = "contrast" }
            AdjustChip(name = stringResource(R.string.saturation), isSelected = activeParam == "saturation") { activeParam = "saturation" }
            AdjustChip(name = stringResource(R.string.temperature), isSelected = activeParam == "temperature") { activeParam = "temperature" }
            AdjustChip(name = stringResource(R.string.tint), isSelected = activeParam == "tint") { activeParam = "tint" }
            AdjustChip(name = stringResource(R.string.highlights), isSelected = activeParam == "highlights") { activeParam = "highlights" }
            AdjustChip(name = stringResource(R.string.shadows), isSelected = activeParam == "shadows") { activeParam = "shadows" }
            AdjustChip(name = stringResource(R.string.whites), isSelected = activeParam == "whites") { activeParam = "whites" }
            AdjustChip(name = stringResource(R.string.blacks), isSelected = activeParam == "blacks") { activeParam = "blacks" }
            AdjustChip(name = stringResource(R.string.vignette), isSelected = activeParam == "vignette") { activeParam = "vignette" }
        }
    }
}

@Composable
private fun AdjustChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(name, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

// 2.5 Beauty & Face Retouching Tool Panel
@Composable
private fun BeautyToolPanel(
    viewModel: EditorViewModel,
    uiState: com.example.ui.viewmodel.EditorUiState
) {
    val beauty = uiState.currentState?.beauty ?: com.example.editor.model.BeautyParams()
    var activeParam by remember { mutableStateOf("skinSmooth") }
    val scrollState = rememberScrollState()

    val currentVal = when (activeParam) {
        "skinSmooth" -> beauty.skinSmooth
        "faceBrighten" -> beauty.faceBrighten
        "warmGlow" -> beauty.warmGlow
        "eyeClarity" -> beauty.eyeClarity
        else -> beauty.skinSmooth
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Slider + Value Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = currentVal,
                onValueChange = { newVal ->
                    val updated = when (activeParam) {
                        "skinSmooth" -> beauty.copy(skinSmooth = newVal)
                        "faceBrighten" -> beauty.copy(faceBrighten = newVal)
                        "warmGlow" -> beauty.copy(warmGlow = newVal)
                        "eyeClarity" -> beauty.copy(eyeClarity = newVal)
                        else -> beauty
                    }
                    viewModel.updateBeauty(updated)
                },
                onValueChangeFinished = {
                    viewModel.commitBeautyChange()
                },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = "${currentVal.toInt()}%",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(44.dp)
                    .padding(start = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        // Beauty Options Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdjustChip(name = stringResource(R.string.skin_smooth), isSelected = activeParam == "skinSmooth") { activeParam = "skinSmooth" }
            AdjustChip(name = stringResource(R.string.face_brighten), isSelected = activeParam == "faceBrighten") { activeParam = "faceBrighten" }
            AdjustChip(name = stringResource(R.string.warm_glow), isSelected = activeParam == "warmGlow") { activeParam = "warmGlow" }
            AdjustChip(name = stringResource(R.string.eye_clarity), isSelected = activeParam == "eyeClarity") { activeParam = "eyeClarity" }
        }
    }
}

// 3. Filters Panel
@Composable
private fun FiltersToolPanel(
    viewModel: EditorViewModel,
    uiState: com.example.ui.viewmodel.EditorUiState
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterType.values().forEach { filter ->
            val isSelected = uiState.selectedFilter == filter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFF262A34))
                    .clickable { viewModel.selectFilter(filter) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when (filter) {
                                FilterType.ORIGINAL -> Color(0xFF78909C)
                                FilterType.VIVID -> Color(0xFFFF7043)
                                FilterType.WARM -> Color(0xFFFFA726)
                                FilterType.COOL -> Color(0xFF29B6F6)
                                FilterType.CLASSIC -> Color(0xFF8D6E63)
                                FilterType.FILM -> Color(0xFF66BB6A)
                                FilterType.BW -> Color(0xFF424242)
                                FilterType.SOFT -> Color(0xFFEC407A)
                                FilterType.DRAMATIC -> Color(0xFF212121)
                                FilterType.VINTAGE -> Color(0xFFBCAAA4)
                                FilterType.SEPIA -> Color(0xFFD7CCC8)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = filter.displayName.substringBefore(" "),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                )
            }
        }
    }
}

// 4. Draw Tool Panel
@Composable
private fun DrawToolPanel(
    viewModel: EditorViewModel,
    uiState: com.example.ui.viewmodel.EditorUiState
) {
    val colors = listOf(
        AndroidColor.WHITE,
        AndroidColor.BLACK,
        AndroidColor.RED,
        AndroidColor.BLUE,
        AndroidColor.GREEN,
        AndroidColor.YELLOW,
        AndroidColor.MAGENTA,
        AndroidColor.CYAN
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Pen / Eraser & Brush Size Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !uiState.isEraserActive,
                onClick = { viewModel.toggleEraser(false) },
                label = { Text("قلم", fontSize = 11.sp) }
            )
            FilterChip(
                selected = uiState.isEraserActive,
                onClick = { viewModel.toggleEraser(true) },
                label = { Text("ممحاة", fontSize = 11.sp) }
            )

            Slider(
                value = uiState.brushSize,
                onValueChange = { viewModel.setBrushSize(it) },
                valueRange = 4f..48f,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Color Palette Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { c ->
                val isSelected = uiState.brushColor == c && !uiState.isEraserActive
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { viewModel.setBrushColor(c) }
                )
            }
        }
    }
}

// 5. Text Tool Panel
@Composable
private fun TextToolPanel(
    viewModel: EditorViewModel,
    uiState: com.example.ui.viewmodel.EditorUiState,
    onOpenTextInput: (TextLayer) -> Unit
) {
    val textLayers = uiState.currentState?.textLayers ?: emptyList()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    viewModel.addTextLayer()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة نص جديد +")
            }

            Text(
                text = "${textLayers.size} نصوص مضافة",
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active text layers chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            textLayers.forEach { layer ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E323D)),
                    modifier = Modifier.clickable { onOpenTextInput(layer) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(layer.text, color = Color.White, fontSize = 12.sp, maxLines = 1)
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.removeTextLayer(layer.id) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// 6. Redaction Tool Panel (Blur & Pixelate)
@Composable
private fun RedactToolPanel(
    viewModel: EditorViewModel,
    uiState: com.example.ui.viewmodel.EditorUiState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.redactMode == RedactMode.BLUR,
                onClick = { viewModel.setRedactMode(RedactMode.BLUR) },
                label = { Text("ضبابي (Blur)", fontSize = 12.sp) }
            )

            FilterChip(
                selected = uiState.redactMode == RedactMode.PIXELATE,
                onClick = { viewModel.setRedactMode(RedactMode.PIXELATE) },
                label = { Text("بكسلة (Pixelate)", fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("حجم الفرشاة:", color = Color.LightGray, fontSize = 12.sp)
            Slider(
                value = uiState.brushSize,
                onValueChange = { viewModel.setBrushSize(it) },
                valueRange = 10f..60f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 7. Overlay Image Panel
@Composable
private fun OverlayToolPanel(
    onPickOverlay: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onPickOverlay,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("اختيار صورة لإضافتها كطبقة")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "يمكنك وضع صورة أخرى فوق الصورة وتعديل مكانها وحجمها",
            color = Color.LightGray,
            fontSize = 11.sp
        )
    }
}
