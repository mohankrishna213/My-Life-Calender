package com.mohanbuilds.focus.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.mohanbuilds.focus.domain.WallpaperSnapshot
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max

/**
 * Renders the lock-screen dashboard bitmap from a [WallpaperSnapshot],
 * following lockscreen_wallpaper_design.html:
 * dark #0D1117 canvas, title + countdown, circular dot grid with today ring,
 * stats/date, and task count.
 *
 * Layout: the whole content block is measured first and then centered
 * horizontally and vertically inside the safe area below the system
 * status bar + clock, so the system date/time never overlaps the design.
 */
object LockScreenWallpaperRenderer {

    // Canvas color tokens from the design reference.
    private const val COLOR_BG = 0xFF0D1117.toInt()
    private const val COLOR_TITLE = Color.WHITE
    private const val COLOR_BRAND = 0xFF39D353.toInt()
    private const val COLOR_MUTED = 0xFF8B949E.toInt()
    private const val COLOR_STATS = 0xFFC9D1D9.toInt()
    private const val COLOR_TODAY_RING = 0xFF58A6FF.toInt()
    private const val COLOR_DIVIDER = 0xFF21262D.toInt()

    /** Dot fill colors indexed by intensity level 0-4. */
    private val DOT_COLORS = intArrayOf(
        0xFF21262D.toInt(),
        0xFF0E4429.toInt(),
        0xFF006D32.toInt(),
        0xFF26A641.toInt(),
        0xFF39D353.toInt(),
    )

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    /** Fraction of screen height reserved for status bar + system clock. */
    private const val TOP_SAFE_FRACTION = 0.20f

    /** Fraction of screen height reserved at the bottom (gesture/shortcuts). */
    private const val BOTTOM_SAFE_FRACTION = 0.06f

    fun render(snapshot: WallpaperSnapshot, widthPx: Int, heightPx: Int): Bitmap {
        val width = max(widthPx, 1)
        val height = max(heightPx, 1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.LEFT
        }

        drawBackground(canvas, paint, width, height)

        val layout = measureContent(snapshot, width, height)
        val safeTop = height * TOP_SAFE_FRACTION
        val safeBottom = height * (1f - BOTTOM_SAFE_FRACTION)
        val available = safeBottom - safeTop
        // Center the measured content block vertically in the safe area.
        val contentTop = safeTop + max((available - layout.totalHeight) / 2f, 0f)

        drawContent(canvas, paint, snapshot, width, contentTop, layout)
        return bitmap
    }

    private class ContentLayout(
        val brandTextSize: Float,
        val titleTextSize: Float,
        val countdownTextSize: Float,
        val statsTextSize: Float,
        val dateTextSize: Float,
        val taskTextSize: Float,
        val titleBlockHeight: Float,
        val gridHeight: Float,
        val gridTopGap: Float,
        val statsBlockHeight: Float,
        val statsTopGap: Float,
        val dotDiameter: Float,
        val dotGap: Float,
        val columns: Int,
        val rows: Int,
        val gridWidth: Float,
    ) {
        val totalHeight: Float
            get() = titleBlockHeight + gridTopGap + gridHeight + statsTopGap + statsBlockHeight
    }

    private fun measureContent(snapshot: WallpaperSnapshot, width: Int, height: Int): ContentLayout {
        val brandTextSize = height * 0.018f
        val titleTextSize = height * 0.042f
        val countdownTextSize = height * 0.024f
        val statsTextSize = height * 0.023f
        val dateTextSize = height * 0.020f
        val taskTextSize = height * 0.020f

        // Title block: brand line + title + countdown, with line gaps.
        val titleBlockHeight = brandTextSize * 1.4f + titleTextSize * 1.5f + countdownTextSize * 1.8f

        val totalDays = max(snapshot.totalDays, 1)
        val columns = if (totalDays <= 60) {
            ceil(totalDays / 5.0).toInt().coerceAtLeast(1)
        } else {
            15
        }
        val rows = ceil(totalDays.toDouble() / columns).toInt().coerceAtLeast(1)

        val gapRatio = 0.5f // gap is half the dot diameter, matching the mockup.
        val availableWidth = width * 0.84f
        val availableGridHeight = height * 0.46f
        val dotByWidth = availableWidth / (columns + (columns - 1) * gapRatio)
        val dotByHeight = availableGridHeight / (rows + (rows - 1) * gapRatio)
        val dotDiameter = max(minOf(dotByWidth, dotByHeight), 2f)
        val dotGap = dotDiameter * gapRatio
        val gridWidth = columns * dotDiameter + (columns - 1) * dotGap
        val gridHeight = rows * dotDiameter + (rows - 1) * dotGap

        // Stats block: divider + day/percent + date + task count.
        val statsBlockHeight = statsTextSize * 1.2f + dateTextSize * 1.6f + taskTextSize * 1.8f

        return ContentLayout(
            brandTextSize = brandTextSize,
            titleTextSize = titleTextSize,
            countdownTextSize = countdownTextSize,
            statsTextSize = statsTextSize,
            dateTextSize = dateTextSize,
            taskTextSize = taskTextSize,
            titleBlockHeight = titleBlockHeight,
            gridHeight = gridHeight,
            gridTopGap = height * 0.025f,
            statsBlockHeight = statsBlockHeight,
            statsTopGap = height * 0.030f,
            dotDiameter = dotDiameter,
            dotGap = dotGap,
            columns = columns,
            rows = rows,
            gridWidth = gridWidth,
        )
    }

    private fun drawContent(
        canvas: Canvas,
        paint: Paint,
        snapshot: WallpaperSnapshot,
        width: Int,
        contentTop: Float,
        layout: ContentLayout,
    ) {
        val leftMargin = width * 0.08f
        var y = contentTop

        // --- Title block (left-aligned) ---
        paint.color = COLOR_BRAND
        paint.textSize = layout.brandTextSize
        paint.isFakeBoldText = true
        paint.letterSpacing = 0.25f
        y += layout.brandTextSize * 1.2f
        canvas.drawText("FOCUS", leftMargin, y, paint)
        paint.letterSpacing = 0f
        paint.isFakeBoldText = false

        y += layout.titleTextSize * 1.3f
        paint.color = COLOR_TITLE
        paint.textSize = layout.titleTextSize
        paint.isFakeBoldText = true
        canvas.drawText(ellipsize(paint, snapshot.goalTitle, width * 0.9f), leftMargin, y, paint)
        paint.isFakeBoldText = false

        y += layout.countdownTextSize * 1.6f
        paint.color = COLOR_MUTED
        paint.textSize = layout.countdownTextSize
        canvas.drawText("${snapshot.daysRemaining} days left", leftMargin, y, paint)

        // --- Dot grid (centered) ---
        val gridStartY = contentTop + layout.titleBlockHeight + layout.gridTopGap
        val gridStartX = (width - layout.gridWidth) / 2f

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = COLOR_TODAY_RING
            strokeWidth = max(layout.dotDiameter * 0.14f, 1.4f)
        }

        val radius = layout.dotDiameter / 2f
        for (index in 0 until max(snapshot.totalDays, 1)) {
            val col = index % layout.columns
            val row = index / layout.columns
            val cx = gridStartX + col * (layout.dotDiameter + layout.dotGap) + radius
            val cy = gridStartY + row * (layout.dotDiameter + layout.dotGap) + radius
            val dayNumber = index + 1

            val level = when {
                snapshot.todayIndex != null && dayNumber > snapshot.todayIndex -> 0
                else -> snapshot.intensities.getOrElse(index) { 0 }
            }
            dotPaint.color = DOT_COLORS[level.coerceIn(0, DOT_COLORS.lastIndex)]
            canvas.drawCircle(cx, cy, radius, dotPaint)

            if (dayNumber == snapshot.todayIndex) {
                canvas.drawCircle(cx, cy, radius + layout.dotDiameter * 0.22f, ringPaint)
            }
        }

        // --- Stats block (left-aligned) ---
        var sy = gridStartY + layout.gridHeight + layout.statsTopGap

        paint.color = COLOR_DIVIDER
        paint.strokeWidth = max(canvas.height * 0.002f, 0.8f)
        val dividerHalf = width * 0.42f
        canvas.drawLine(leftMargin, sy, leftMargin + dividerHalf * 2, sy, paint)

        sy += layout.statsTextSize * 1.1f
        paint.color = COLOR_STATS
        paint.textSize = layout.statsTextSize
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText(
            "Day ${snapshot.daysElapsed} of ${snapshot.totalDays} · ${"%.1f".format(snapshot.progressPercent)}%",
            leftMargin,
            sy,
            paint,
        )

        sy += layout.dateTextSize * 1.5f
        paint.color = COLOR_MUTED
        paint.textSize = layout.dateTextSize
        canvas.drawText(snapshot.today.format(dateFormatter), leftMargin, sy, paint)

        sy += layout.taskTextSize * 1.6f
        paint.textSize = layout.taskTextSize
        canvas.drawText(
            "${snapshot.tasksTodayCompleted} of ${snapshot.tasksTodayTotal} tasks done today",
            leftMargin,
            sy,
            paint,
        )
        paint.typeface = Typeface.DEFAULT
    }

    private fun drawBackground(canvas: Canvas, paint: Paint, width: Int, height: Int) {
        paint.color = COLOR_BG
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun ellipsize(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var result = text
        while (result.length > 1 && paint.measureText("$result…") > maxWidth) {
            result = result.dropLast(1)
        }
        return "$result…"
    }
}
