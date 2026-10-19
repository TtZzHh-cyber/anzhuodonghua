package com.example.douyintransitiondemo

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Standalone, dependency-free demo that imitates a vertical short-video feed.
 *
 * This is not an injection module and does not modify the official Douyin app.
 * Gesture release chooses the adjacent page using drag distance and velocity,
 * then springs the page stack to the selected position.
 */
class MainActivity : Activity() {
    private lateinit var feed: SpringFeedView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        feed = SpringFeedView(this)
        setContentView(feed)
    }
}

private class SpringFeedView(activity: Activity) : FrameLayout(activity) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val pages = mutableListOf<FrameLayout>()
    private var currentIndex = 0
    private var downY = 0f
    private var downX = 0f
    private var lastY = 0f
    private var lastTime = 0L
    private var velocityY = 0f
    private var dragging = false
    private var pageHeight = 1
    private var offsetY = 0f
    private var animating = false

    private val titles = listOf(
        "过渡动画演示", "下一条视频", "第三条视频", "第四条视频", "第五条视频"
    )
    private val subtitles = listOf(
        "上下滑动，观察弹簧过渡", "松手后根据距离和速度切页",
        "模拟短视频竖向信息流", "快速上滑或下滑试试", "已到示例列表末尾"
    )
    private val colors = intArrayOf(
        Color.rgb(25, 32, 48),
        Color.rgb(44, 25, 52),
        Color.rgb(20, 48, 47),
        Color.rgb(56, 36, 25),
        Color.rgb(29, 39, 61)
    )

    init {
        setBackgroundColor(Color.BLACK)
        isClickable = true
        for (i in titles.indices) {
            pages += makePage(i)
            addView(pages.last(), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        post { layoutPages() }
    }

    private fun makePage(index: Int): FrameLayout {
        val page = FrameLayout(context).apply {
            setBackgroundColor(colors[index % colors.size])
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }
        val title = TextView(context).apply {
            text = titles[index]
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(context).apply {
            text = subtitles[index]
            textSize = 16f
            setTextColor(Color.rgb(220, 220, 220))
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }
        val counter = TextView(context).apply {
            text = "${index + 1} / ${titles.size}"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, 0)
        }
        content.addView(title, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        content.addView(subtitle, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        content.addView(counter, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        page.addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val hint = TextView(context).apply {
            text = "↑ 上滑 / 下滑 ↓"
            textSize = 14f
            setTextColor(Color.WHITE)
            alpha = 0.78f
            gravity = Gravity.CENTER
        }
        val hintParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(52), Gravity.BOTTOM)
        page.addView(hint, hintParams)
        return page
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pageHeight = max(1, h)
        layoutPages()
    }

    private fun layoutPages() {
        if (height <= 0) return
        pageHeight = height
        pages.forEachIndexed { index, page ->
            page.layoutParams = (page.layoutParams as LayoutParams).apply {
                width = widthOfParent()
                height = pageHeight
            }
            page.translationY = (index - currentIndex) * pageHeight.toFloat() + offsetY
        }
        invalidate()
    }

    private fun widthOfParent(): Int = if (width > 0) width else LayoutParams.MATCH_PARENT

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                downX = event.x
                lastY = event.y
                lastTime = event.eventTime
                velocityY = 0f
                dragging = false
                // Let an active transition be interrupted naturally.
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - downY
                val dx = event.x - downX
                if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                downX = event.x
                lastY = event.y
                lastTime = event.eventTime
                velocityY = 0f
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val now = event.eventTime
                val dt = max(1L, now - lastTime)
                velocityY = (event.y - lastY) / dt.toFloat() // px/ms
                lastY = event.y
                lastTime = now
                val raw = event.y - downY
                if (abs(raw) > touchSlop) dragging = true
                if (dragging) {
                    val bounded = when {
                        currentIndex == 0 && raw > 0 -> raw * 0.32f
                        currentIndex == pages.lastIndex && raw < 0 -> raw * 0.32f
                        else -> raw
                    }
                    offsetY = bounded
                    placePages()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) settleAfterRelease()
                else performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun settleAfterRelease() {
        val threshold = pageHeight * 0.22f
        val fastFling = abs(velocityY) > 0.65f
        var target = currentIndex
        if (offsetY < -threshold || (fastFling && velocityY < -0.65f)) {
            target = min(pages.lastIndex, currentIndex + 1)
        } else if (offsetY > threshold || (fastFling && velocityY > 0.65f)) {
            target = max(0, currentIndex - 1)
        }
        animateTo(target)
    }

    private fun animateTo(targetIndex: Int) {
        if (animating) {
            // Animator is deliberately simple and dependency-free; a new gesture can
            // still start from the current visual offset.
        }
        animating = true
        val startOffset = offsetY
        val endOffset = (currentIndex - targetIndex) * pageHeight.toFloat()
        val delta = endOffset - startOffset
        val distancePages = abs(targetIndex - currentIndex)
        val duration = (260L + min(100L, (abs(velocityY) * 80).toLong()) +
            if (distancePages == 0) 40L else 0L).coerceIn(240L, 380L)

        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator(1.7f)
        animator.addUpdateListener { a ->
            val t = a.animatedValue as Float
            // Damped spring-like overshoot, with a little initial-velocity influence.
            val damping = 5.8f
            val frequency = 10.0f
            val spring = 1f - (kotlin.math.exp((-damping * t).toDouble()).toFloat() *
                kotlin.math.cos((frequency * t).toDouble()).toFloat())
            val velocityInfluence = min(0.10f, abs(velocityY) * 0.05f)
            val eased = (spring * (1f - velocityInfluence) + t * velocityInfluence)
                .coerceIn(-0.06f, 1.08f)
            offsetY = startOffset + delta * eased
            placePages()
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                currentIndex = targetIndex
                offsetY = 0f
                animating = false
                placePages()
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {
                animating = false
            }
        })
        animator.start()
    }

    private fun placePages() {
        pages.forEachIndexed { index, page ->
            page.translationY = (index - currentIndex) * pageHeight.toFloat() + offsetY
        }
        invalidate()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
