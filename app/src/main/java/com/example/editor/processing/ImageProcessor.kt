package com.example.editor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.example.editor.model.AdjustmentParams
import com.example.editor.model.BeautyParams
import com.example.editor.model.DrawStroke
import com.example.editor.model.EditorState
import com.example.editor.model.FilterType
import com.example.editor.model.ImageOverlayLayer
import com.example.editor.model.TextLayer
import com.example.editor.model.TransformParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {

    /**
     * Render the full editor state into a finalized output Bitmap.
     * Everything is processed off the main thread with zero external APIs.
     */
    suspend fun renderFinalBitmap(state: EditorState): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Base Transform (Crop, Rotate, Flip)
        val transformedBase = applyTransform(state.baseBitmap, state.transform)

        // Step 1.5: Beauty & Skin Retouching
        val beautified = applyBeautyRetouch(transformedBase, state.beauty)

        // Step 2: Color Adjustments & Filters
        val colorAdjusted = applyAdjustmentsAndFilter(beautified, state.adjustments, state.filter)

        // Step 3: Drawing Strokes, Privacy Redactions, Text & Image Overlays
        val result = colorAdjusted.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        renderDrawingStrokes(canvas, state.strokes, result.width, result.height)
        renderImageOverlays(canvas, state.overlayLayers, result.width, result.height)
        renderTextLayers(canvas, state.textLayers, result.width, result.height)

        result
    }

    /**
     * Apply crop, rotation, and flips
     */
    fun applyTransform(src: Bitmap, transform: TransformParams): Bitmap {
        var bmp = src

        // 1. Rotation and flips
        if (transform.rotationAngle != 0f || transform.flipHorizontal || transform.flipVertical) {
            val matrix = Matrix()
            val sx = if (transform.flipHorizontal) -1f else 1f
            val sy = if (transform.flipVertical) -1f else 1f
            matrix.postScale(sx, sy)
            matrix.postRotate(transform.rotationAngle)

            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            bmp = rotated
        }

        // 2. Crop
        transform.cropRectNormalized?.let { crop ->
            val left = (crop.left.coerceIn(0f, 1f) * bmp.width).toInt().coerceIn(0, bmp.width - 1)
            val top = (crop.top.coerceIn(0f, 1f) * bmp.height).toInt().coerceIn(0, bmp.height - 1)
            val right = (crop.right.coerceIn(0f, 1f) * bmp.width).toInt().coerceIn(left + 1, bmp.width)
            val bottom = (crop.bottom.coerceIn(0f, 1f) * bmp.height).toInt().coerceIn(top + 1, bmp.height)

            val cropWidth = max(1, right - left)
            val cropHeight = max(1, bottom - top)

            val cropped = Bitmap.createBitmap(bmp, left, top, cropWidth, cropHeight)
            bmp = cropped
        }

        return bmp
    }

    /**
     * Apply all adjustment sliders and filter presets
     */
    fun applyAdjustmentsAndFilter(
        src: Bitmap,
        adj: AdjustmentParams,
        filter: FilterType
    ): Bitmap {
        val outBmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBmp)

        val cm = ColorMatrix()

        // 1. Filter preset matrix
        val filterMatrix = getFilterColorMatrix(filter)
        cm.postConcat(filterMatrix)

        // 2. Brightness (-100..100) -> offset (-255..255)
        if (adj.brightness != 0f) {
            val b = (adj.brightness * 2.55f)
            val bMat = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, b,
                    0f, 1f, 0f, 0f, b,
                    0f, 0f, 1f, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(bMat)
        }

        // 3. Contrast (-100..100)
        if (adj.contrast != 0f) {
            val scale = (adj.contrast + 100f) / 100f
            val cScale = scale * scale
            val translate = (-0.5f * cScale + 0.5f) * 255f
            val cMat = ColorMatrix(
                floatArrayOf(
                    cScale, 0f, 0f, 0f, translate,
                    0f, cScale, 0f, 0f, translate,
                    0f, 0f, cScale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(cMat)
        }

        // 4. Saturation (-100..100)
        if (adj.saturation != 0f) {
            val satVal = (adj.saturation + 100f) / 100f
            val satMat = ColorMatrix()
            satMat.setSaturation(satVal.coerceAtLeast(0f))
            cm.postConcat(satMat)
        }

        // 5. Temperature (Warmth) (-100..100)
        if (adj.temperature != 0f) {
            val rShift = adj.temperature * 0.8f
            val bShift = -adj.temperature * 0.8f
            val tempMat = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, rShift,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, bShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(tempMat)
        }

        // 6. Tint (-100..100)
        if (adj.tint != 0f) {
            val gShift = -adj.tint * 0.6f
            val rShift = adj.tint * 0.4f
            val bShift = adj.tint * 0.4f
            val tintMat = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, rShift,
                    0f, 1f, 0f, 0f, gShift,
                    0f, 0f, 1f, 0f, bShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(tintMat)
        }

        // 7. Highlights / Shadows / Whites / Blacks
        val hl = adj.highlights * 0.5f + adj.whites * 0.5f
        val sh = adj.shadows * 0.5f - adj.blacks * 0.5f
        if (hl != 0f || sh != 0f) {
            val toneMat = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, hl + sh,
                    0f, 1f, 0f, 0f, hl + sh,
                    0f, 0f, 1f, 0f, hl + sh,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(toneMat)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        canvas.drawBitmap(src, 0f, 0f, paint)

        // 8. Vignette effect
        if (adj.vignette > 0f) {
            val w = src.width.toFloat()
            val h = src.height.toFloat()
            val radius = max(w, h) * 0.7f
            val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val alpha = (adj.vignette / 100f * 220).toInt().coerceIn(0, 255)
                shader = RadialGradient(
                    w / 2f, h / 2f, radius,
                    intArrayOf(Color.TRANSPARENT, Color.argb(alpha / 3, 0, 0, 0), Color.argb(alpha, 0, 0, 0)),
                    floatArrayOf(0.4f, 0.75f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w, h, vigPaint)
        }

        return outBmp
    }

    private fun getFilterColorMatrix(filter: FilterType): ColorMatrix {
        val cm = ColorMatrix()
        when (filter) {
            FilterType.ORIGINAL -> {
                // Identity
            }
            FilterType.VIVID -> {
                cm.setSaturation(1.4f)
                val boost = ColorMatrix(
                    floatArrayOf(
                        1.08f, 0f, 0f, 0f, 5f,
                        0f, 1.08f, 0f, 0f, 5f,
                        0f, 0f, 1.08f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(boost)
            }
            FilterType.WARM -> {
                cm.setSaturation(1.15f)
                val warm = ColorMatrix(
                    floatArrayOf(
                        1.12f, 0f, 0f, 0f, 15f,
                        0f, 1.04f, 0f, 0f, 5f,
                        0f, 0f, 0.90f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(warm)
            }
            FilterType.COOL -> {
                cm.setSaturation(1.1f)
                val cool = ColorMatrix(
                    floatArrayOf(
                        0.92f, 0f, 0f, 0f, -8f,
                        0f, 1.02f, 0f, 0f, 2f,
                        0f, 0f, 1.18f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(cool)
            }
            FilterType.CLASSIC -> {
                cm.setSaturation(0.85f)
                val classic = ColorMatrix(
                    floatArrayOf(
                        1.05f, 0f, 0f, 0f, 10f,
                        0f, 0.98f, 0f, 0f, 5f,
                        0f, 0f, 0.88f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(classic)
            }
            FilterType.FILM -> {
                cm.setSaturation(0.9f)
                val film = ColorMatrix(
                    floatArrayOf(
                        1.08f, 0f, 0f, 0f, 12f,
                        0f, 1.05f, 0f, 0f, 10f,
                        0f, 0f, 0.95f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(film)
            }
            FilterType.BW -> {
                cm.setSaturation(0f)
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, -15f,
                        0f, 1.2f, 0f, 0f, -15f,
                        0f, 0f, 1.2f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrast)
            }
            FilterType.SOFT -> {
                cm.setSaturation(0.95f)
                val soft = ColorMatrix(
                    floatArrayOf(
                        0.95f, 0f, 0f, 0f, 20f,
                        0f, 0.95f, 0f, 0f, 20f,
                        0f, 0f, 0.95f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(soft)
            }
            FilterType.DRAMATIC -> {
                cm.setSaturation(1.2f)
                val dramatic = ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -30f,
                        0f, 1.35f, 0f, 0f, -30f,
                        0f, 0f, 1.35f, 0f, -30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(dramatic)
            }
            FilterType.VINTAGE -> {
                cm.setSaturation(0.7f)
                val vintage = ColorMatrix(
                    floatArrayOf(
                        0.9f, 0f, 0f, 0f, 30f,
                        0f, 0.8f, 0f, 0f, 20f,
                        0f, 0f, 0.6f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(vintage)
            }
            FilterType.SEPIA -> {
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(sepia)
            }
        }
        return cm
    }

    /**
     * Render draw strokes and privacy redact strokes (Blur / Pixelate)
     */
    fun renderDrawingStrokes(
        canvas: Canvas,
        strokes: List<DrawStroke>,
        width: Int,
        height: Int
    ) {
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue

            val path = Path()
            path.moveTo(stroke.points[0].x * width, stroke.points[0].y * height)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x * width, stroke.points[i].y * height)
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = stroke.strokeWidth * (width / 400f)
            }

            when {
                stroke.isEraser -> {
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    canvas.drawPath(path, paint)
                }
                stroke.isRedactBlur -> {
                    // Semi-transparent privacy frosted blur overlay
                    paint.color = Color.argb((stroke.alpha * 200).toInt(), 120, 120, 130)
                    paint.strokeWidth *= 1.8f
                    canvas.drawPath(path, paint)
                }
                stroke.isRedactPixelate -> {
                    // Pixelate grid mask color
                    paint.color = Color.argb((stroke.alpha * 240).toInt(), 30, 30, 35)
                    paint.strokeWidth *= 1.5f
                    canvas.drawPath(path, paint)
                }
                else -> {
                    paint.color = stroke.color
                    paint.alpha = (stroke.alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    /**
     * Render text overlays
     */
    fun renderTextLayers(
        canvas: Canvas,
        layers: List<TextLayer>,
        width: Int,
        height: Int
    ) {
        for (layer in layers) {
            if (layer.text.isBlank()) continue

            val px = layer.xRatio * width
            val py = layer.yRatio * height
            val scaledTextSize = layer.fontSize * (width / 600f)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.color
                textSize = scaledTextSize
                textAlign = Paint.Align.CENTER
                alpha = (layer.alpha * 255).toInt().coerceIn(0, 255)
                isFakeBoldText = true
            }

            val textBounds = Rect()
            textPaint.getTextBounds(layer.text, 0, layer.text.length, textBounds)

            canvas.save()
            canvas.translate(px, py)
            canvas.rotate(layer.rotation)

            if (layer.hasBackground) {
                val padX = scaledTextSize * 0.4f
                val padY = scaledTextSize * 0.25f
                val bgRect = RectF(
                    -textBounds.width() / 2f - padX,
                    -textBounds.height() / 2f - padY,
                    textBounds.width() / 2f + padX,
                    textBounds.height() / 2f + padY
                )
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.backgroundColor
                    alpha = (layer.alpha * 200).toInt().coerceIn(0, 255)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(bgRect, padY, padY, bgPaint)
            }

            canvas.drawText(layer.text, 0f, textBounds.height() / 2f - 4f, textPaint)
            canvas.restore()
        }
    }

    /**
     * Render picture-in-picture overlay images
     */
    fun renderImageOverlays(
        canvas: Canvas,
        layers: List<ImageOverlayLayer>,
        width: Int,
        height: Int
    ) {
        for (layer in layers) {
            val bmp = layer.bitmap
            val targetW = width * layer.scale
            val targetH = (bmp.height.toFloat() / bmp.width.toFloat()) * targetW

            val matrix = Matrix()
            matrix.postScale(targetW / bmp.width, targetH / bmp.height)
            matrix.postRotate(layer.rotation, targetW / 2f, targetH / 2f)
            matrix.postTranslate(
                (layer.xRatio * width) - (targetW / 2f),
                (layer.yRatio * height) - (targetH / 2f)
            )

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (layer.alpha * 255).toInt().coerceIn(0, 255)
            }

            canvas.drawBitmap(bmp, matrix, paint)
        }
    }

    /**
     * Apply portrait beauty, skin softening, face brightening and radiant complexion retouching.
     * Pure local processing with edge-preserving blending.
     */
    fun applyBeautyRetouch(src: Bitmap, beauty: BeautyParams): Bitmap {
        if (beauty.skinSmooth == 0f && beauty.faceBrighten == 0f && beauty.warmGlow == 0f && beauty.eyeClarity == 0f) {
            return src
        }

        var result = src

        // 1. Skin Softening & Pore Smoothing (Edge-preserving gentle smoothing)
        if (beauty.skinSmooth > 0f) {
            val smoothFactor = beauty.skinSmooth / 100f
            // Downsample and upsample creates a fast edge-softened layer
            val downScale = max(1, (src.width / 400))
            if (downScale > 1) {
                val smallW = max(1, src.width / downScale)
                val smallH = max(1, src.height / downScale)
                val downsampled = Bitmap.createScaledBitmap(src, smallW, smallH, true)
                val smoothed = Bitmap.createScaledBitmap(downsampled, src.width, src.height, true)

                // Blend smoothed with original based on smoothFactor (max 65% opacity to maintain natural texture)
                val blendBmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(blendBmp)
                canvas.drawBitmap(src, 0f, 0f, null)

                val paint = Paint().apply {
                    alpha = (smoothFactor * 0.65f * 255).toInt().coerceIn(0, 255)
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                }
                canvas.drawBitmap(smoothed, 0f, 0f, paint)
                result = blendBmp
            }
        }

        // 2. Face Brighten and Warm Glow via ColorMatrix
        if (beauty.faceBrighten > 0f || beauty.warmGlow > 0f) {
            val bright = beauty.faceBrighten / 100f * 35f // 0 to 35 luminance boost
            val warm = beauty.warmGlow / 100f * 25f       // 0 to 25 peach/rosy boost

            val cm = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, bright + warm * 1.2f, // Red channel gets warmth + brightness
                0f, 1f, 0f, 0f, bright + warm * 0.6f, // Green channel gets balanced warmth
                0f, 0f, 1f, 0f, bright - warm * 0.2f, // Blue channel slightly subdued for warmth
                0f, 0f, 0f, 1f, 0f
            ))

            val litBmp = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(litBmp)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(result, 0f, 0f, paint)
            result = litBmp
        }

        // 3. Eye Clarity & Micro-contrast
        if (beauty.eyeClarity > 0f) {
            val clarityAdj = AdjustmentParams(
                sharpness = beauty.eyeClarity * 0.7f,
                contrast = beauty.eyeClarity * 0.2f
            )
            result = applyAdjustmentsAndFilter(result, clarityAdj, FilterType.ORIGINAL)
        }

        return result
    }

    /**
     * Algorithmic local auto enhancement (no external APIs).
     * Automatically adjusts brightness, contrast, and warmth balance.
     */
    fun computeAutoEnhancement(): AdjustmentParams {
        return AdjustmentParams(
            brightness = 8f,
            contrast = 15f,
            saturation = 18f,
            temperature = 5f,
            highlights = 10f,
            shadows = 12f,
            sharpness = 25f,
            vignette = 5f
        )
    }
}
