package com.example.editor.model

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri

enum class EditorToolTab {
    CROP,
    ADJUST,
    BEAUTY,
    FILTERS,
    DRAW,
    TEXT,
    REDACT,
    OVERLAY
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    ORIGINAL("الأصل", null),
    FREE("حر", null),
    RATIO_1_1("1:1", 1.0f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f)
}

enum class FilterType(val displayName: String, val isDarkFilter: Boolean = false) {
    ORIGINAL("طبيعي (Natural)"),
    VIVID("حيوي (Vivid)"),
    WARM("دافئ (Warm)"),
    COOL("بارد (Cool)"),
    CLASSIC("كلاسيك (Classic)"),
    FILM("سينمائي (Film)"),
    BW("أبيض وأسود (B&W)"),
    SOFT("ناعم (Soft)"),
    DRAMATIC("درامي (Dramatic)"),
    VINTAGE("عتيق (Vintage)"),
    SEPIA("سيبيا (Sepia)")
}

enum class RedactMode {
    BLUR,
    PIXELATE
}

data class AdjustmentParams(
    val brightness: Float = 0f,    // -100 to 100
    val contrast: Float = 0f,      // -100 to 100
    val saturation: Float = 0f,    // -100 to 100
    val temperature: Float = 0f,   // -100 to 100 (warmth)
    val tint: Float = 0f,          // -100 to 100 (green/magenta)
    val highlights: Float = 0f,    // -100 to 100
    val shadows: Float = 0f,       // -100 to 100
    val whites: Float = 0f,        // -100 to 100
    val blacks: Float = 0f,        // -100 to 100
    val sharpness: Float = 0f,     // 0 to 100
    val vignette: Float = 0f,      // 0 to 100
    val blur: Float = 0f           // 0 to 100
)

data class DrawStroke(
    val points: List<PointF>,
    val color: Int,
    val strokeWidth: Float,
    val alpha: Float = 1.0f,
    val isEraser: Boolean = false,
    val isRedactBlur: Boolean = false,
    val isRedactPixelate: Boolean = false
)

data class TextLayer(
    val id: Long = System.currentTimeMillis(),
    var text: String = "نص جديد",
    var xRatio: Float = 0.5f,
    var yRatio: Float = 0.5f,
    var fontSize: Float = 36f,
    var color: Int = Color.WHITE,
    var hasBackground: Boolean = true,
    var backgroundColor: Int = Color.argb(180, 0, 0, 0),
    var rotation: Float = 0f,
    var alpha: Float = 1.0f
)

data class ImageOverlayLayer(
    val id: Long = System.currentTimeMillis(),
    val bitmap: Bitmap,
    var xRatio: Float = 0.5f,
    var yRatio: Float = 0.5f,
    var scale: Float = 0.4f,
    var rotation: Float = 0f,
    var alpha: Float = 1.0f
)

data class TransformParams(
    val rotationAngle: Float = 0f, // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRectNormalized: CropRectNormalized? = null // relative bounds [0..1]
)

data class CropRectNormalized(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

data class BeautyParams(
    val skinSmooth: Float = 0f,   // 0 to 100: Softens skin blemishes and pores while keeping eyes sharp
    val faceBrighten: Float = 0f, // 0 to 100: Portrait face brightening and luminance lift
    val warmGlow: Float = 0f,     // 0 to 100: Rosy warmth and radiant peach skin tone
    val eyeClarity: Float = 0f    // 0 to 100: Detail clarity & micro-contrast for face features
)

data class EditorState(
    val baseBitmap: Bitmap,
    val transform: TransformParams = TransformParams(),
    val adjustments: AdjustmentParams = AdjustmentParams(),
    val beauty: BeautyParams = BeautyParams(),
    val filter: FilterType = FilterType.ORIGINAL,
    val strokes: List<DrawStroke> = emptyList(),
    val textLayers: List<TextLayer> = emptyList(),
    val overlayLayers: List<ImageOverlayLayer> = emptyList()
)
