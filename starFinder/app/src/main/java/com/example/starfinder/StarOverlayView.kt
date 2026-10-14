package com.example.starfinder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.hardware.GeomagneticField
import android.util.AttributeSet
import android.view.View
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

/** 카메라 화면 위에 별, 별자리 선, 이름, 방향을 그리는 투명 View */
class StarOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var verticalFovDeg = 70f
    var needsCalibration = false

    // 위치를 못 받았을 때 임시로 서울 사용
    private var lat = 37.5665
    private var lon = 126.9780
    private var hasFix = false
    private var declination = -9f

    private val rot = FloatArray(9)

    private var hasRot = false

    private class Projected(val star: Star, val x: Float, val y: Float, val dist: Float, val alt: Double)

    private val dp = resources.displayMetrics.density

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1.5f * dp
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 214, 90); style = Paint.Style.STROKE; strokeWidth = 3f * dp
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 14f * dp; setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 220, 220, 220); textSize = 11f * dp; setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }
    private val bigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 214, 90); textSize = 26f * dp; isFakeBoldText = true
        textAlign = Paint.Align.CENTER; setShadowLayer(6f, 0f, 0f, Color.BLACK)
    }
    private val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 14f * dp; textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 0, 0, 0) }
    private val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 120, 120); textSize = 13f * dp; textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }


    // 별자리 선: 파란 실선 / 대삼각형: 주황 점선 / 별자리 이름: 하늘색
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 120, 180, 255); style = Paint.Style.STROKE; strokeWidth = 1.5f * dp
    }
    private val asterismPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 255, 200, 120); style = Paint.Style.STROKE; strokeWidth = 1.2f * dp
        pathEffect = DashPathEffect(floatArrayOf(10f * dp, 8f * dp), 0f)
    }
    private val constNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 150, 200, 255); textSize = 13f * dp; textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    // 하단 방향 표시
    private val dirBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 0, 0, 0) }
    private val dirPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 16f * dp; isFakeBoldText = true; textAlign = Paint.Align.CENTER
    }

    private fun dirName(az: Double): String {
        val names = arrayOf("북", "북동", "동", "남동", "남", "남서", "서", "북서")
        return names[((az + 22.5) / 45.0).toInt() % 8]
    }

    fun setLocation(lat: Double, lon: Double, alt: Double) {
        this.lat = lat
        this.lon = lon
        hasFix = true
        declination = GeomagneticField(
            lat.toFloat(), lon.toFloat(), alt.toFloat(), System.currentTimeMillis()
        ).declination

        invalidate()
    }

    fun updateRotation(r: FloatArray) {
        if (!hasRot) {
            r.copyInto(rot); hasRot = true
        } else {
            for (i in 0 until 9) rot[i] += 0.25f * (r[i] - rot[i])
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        if (!hasRot) {
            canvas.drawText("센서 준비 중…", cx, cy, infoPaint)
            return
        }

        val f = (h / 2f) / tan(Math.toRadians(verticalFovDeg / 2.0)).toFloat()
        val now = System.currentTimeMillis()

        val visible = ArrayList<Projected>()
        val pos = HashMap<String, PointF>()
        var best: Projected? = null

        for (s in STARS) {

            val (alt, az) = Astro.altAz(s.ra, s.dec, lat, lon, now)
            if (alt < -1.0) continue

            val azR = Math.toRadians(az - declination)
            val altR = Math.toRadians(alt)
            val e = (sin(azR) * cos(altR)).toFloat()
            val n = (cos(azR) * cos(altR)).toFloat()
            val u = sin(altR).toFloat()

            val dx = rot[0] * e + rot[3] * n + rot[6] * u
            val dy = rot[1] * e + rot[4] * n + rot[7] * u
            val dz = rot[2] * e + rot[5] * n + rot[8] * u

            val forward = -dz
            if (forward <= 0.05f) continue

            val px = cx + dx / forward * f
            val py = cy - dy / forward * f
            pos[s.en] = PointF(px, py)
            if (px < -60 || px > w + 60 || py < -60 || py > h + 60) continue

            val p = Projected(s, px, py, hypot(px - cx, py - cy), alt)
            visible.add(p)
            if (best == null || p.dist < best.dist) best = p
        }

        // 별자리 선
        for (c in CONSTELLATIONS) {
            val paint = if (c.asterism) asterismPaint else linePaint
            var sumX = 0f
            var sumY = 0f
            var count = 0

            for ((a, b2) in c.lines) {
                val p1 = pos[a] ?: continue
                val p2 = pos[b2] ?: continue
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                for (p in listOf(p1, p2)) {
                    if (p.x in 0f..w && p.y in 0f..h) {
                        sumX += p.x; sumY += p.y; count++
                    }
                }
            }
            if (count >= 2) {
                canvas.drawText(c.ko, sumX / count, sumY / count + 28f * dp, constNamePaint)
            }
        }

        // 별
        for (p in visible) {
            val r = max(2.5f, 9f - 2.2f * p.star.mag.toFloat()) * dp / 2f + 2f
            canvas.drawCircle(p.x, p.y, r, starPaint)
            canvas.drawCircle(p.x, p.y, r + 6f * dp, ringPaint)
            canvas.drawText(p.star.ko, p.x + r + 8f * dp, p.y - 2f * dp, namePaint)
            canvas.drawText(p.star.en, p.x + r + 8f * dp, p.y + 12f * dp, subPaint)
        }

        // 가운데 조준원
        canvas.drawCircle(cx, cy, 28f * dp, ringPaint)
        canvas.drawLine(cx - 8f * dp, cy, cx + 8f * dp, cy, ringPaint)
        canvas.drawLine(cx, cy - 8f * dp, cx, cy + 8f * dp, ringPaint)

        // 가운데에 가장 가까운 별 강조
        val b = best
        if (b != null && b.dist < w * 0.25f) {

            canvas.drawCircle(b.x, b.y, 20f * dp, highlightPaint)
            // 아래쪽은 방향 표시와 촬영 버튼 자리라서 조금 위에 표시
            val boxTop = h - 290f * dp
            canvas.drawRoundRect(24f * dp, boxTop, w - 24f * dp, boxTop + 96f * dp, 16f * dp, 16f * dp, boxPaint)
            canvas.drawText(b.star.ko, cx, boxTop + 40f * dp, bigPaint)
            canvas.drawText(
                "${b.star.en} · ${"%.1f".format(b.star.mag)}등급 · 고도 ${b.alt.toInt()}°",
                cx, boxTop + 72f * dp, infoPaint
            )
        }

        // 하단: 지금 카메라가 향한 방위각·고도
        val camAzMag = Math.toDegrees(atan2(-rot[2].toDouble(), -rot[5].toDouble()))
        val camAz = ((camAzMag + declination) % 360.0 + 360.0) % 360.0
        val camAlt = Math.toDegrees(asin((-rot[8]).coerceIn(-1f, 1f).toDouble()))
        val dirText = "방위 %s %.0f°   ·   고도 %+.0f°".format(dirName(camAz), camAz, camAlt)
        val dirY = h - 150f * dp
        val halfW = dirPaint.measureText(dirText) / 2f + 18f * dp
        canvas.drawRoundRect(cx - halfW, dirY - 26f * dp, cx + halfW, dirY + 12f * dp, 19f * dp, 19f * dp, dirBoxPaint)
        canvas.drawText(dirText, cx, dirY, dirPaint)

        // 상태 표시
        var y = 72f * dp
        if (!hasFix) {
            canvas.drawText("위치 확인 중… (임시로 서울 기준)", cx, y, warnPaint); y += 20f * dp
        }
        if (needsCalibration) {
            canvas.drawText("나침반 정확도 낮음: 폰을 8자로 흔들어 주세요", cx, y, warnPaint)
        }
    }
}