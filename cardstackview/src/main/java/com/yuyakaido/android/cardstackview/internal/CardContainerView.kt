package com.yuyakaido.android.cardstackview.internal

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import com.yuyakaido.android.cardstackview.*

class CardContainerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private var option: CardStackOption? = null
    var viewOriginX = 0f
    var viewOriginY = 0f
    private var motionOriginX = 0f
    private var motionOriginY = 0f
    private var isDragging = false
    private var isDraggable = true

    var contentContainer: ViewGroup? = null
    var overlayContainer: ViewGroup? = null
    private var leftOverlayView: View? = null
    private var rightOverlayView: View? = null
    private var bottomOverlayView: View? = null
    private var topOverlayView: View? = null

    private var containerEventListener: ContainerEventListener? = null
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            containerEventListener?.onContainerClicked()
            return true
        }
    }
    private val gestureDetector = GestureDetector(getContext(), gestureListener)

    val percentX: Float
        get() {
            var percent = 2f * (translationX - viewOriginX) / width
            if (percent > 1) {
                percent = 1f
            }
            if (percent < -1) {
                percent = -1f
            }
            return percent
        }

    val percentY: Float
        get() {
            var percent = 2f * (translationY - viewOriginY) / height
            if (percent > 1) {
                percent = 1f
            }
            if (percent < -1) {
                percent = -1f
            }
            return percent
        }

    interface ContainerEventListener {
        fun onContainerDragging(percentX: Float, percentY: Float)
        fun onContainerSwiped(point: Point, direction: SwipeDirection?)
        fun onContainerMovedToOrigin()
        fun onContainerClicked()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        View.inflate(context, R.layout.card_frame, this)
        contentContainer = findViewById(R.id.card_frame_content_container)
        overlayContainer = findViewById(R.id.card_frame_overlay_container)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        if (option?.isSwipeEnabled == false || !isDraggable) {
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event)
                parent.parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> {
                handleActionUp(event)
                parent.parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> parent.parent.requestDisallowInterceptTouchEvent(false)
            MotionEvent.ACTION_MOVE -> handleActionMove(event)
        }

        return true
    }

    private fun handleActionDown(event: MotionEvent) {
        motionOriginX = event.rawX
        motionOriginY = event.rawY
    }

    private fun handleActionUp(event: MotionEvent) {
        if (isDragging) {
            isDragging = false

            val motionCurrentX = event.rawX
            val motionCurrentY = event.rawY

            val point = Util.getTargetPoint(motionOriginX, motionOriginY, motionCurrentX, motionCurrentY)
            val quadrant = Util.getQuadrant(motionOriginX, motionOriginY, motionCurrentX, motionCurrentY)
            var radian = Util.getRadian(motionOriginX, motionOriginY, motionCurrentX, motionCurrentY)
            var degree: Double
            var direction: SwipeDirection? = null
            when (quadrant) {
                Quadrant.TopLeft -> {
                    degree = Math.toDegrees(radian)
                    degree = 180 - degree
                    radian = Math.toRadians(degree)
                    if (Math.cos(radian) < -0.5) {
                        direction = LeftSwipe
                    } else {
                        direction = TopSwipe
                    }
                }
                Quadrant.TopRight -> {
                    degree = Math.toDegrees(radian)
                    radian = Math.toRadians(degree)
                    if (Math.cos(radian) < 0.5) {
                        direction = TopSwipe
                    } else {
                        direction = RightSwipe
                    }
                }
                Quadrant.BottomLeft -> {
                    degree = Math.toDegrees(radian)
                    degree = 180 + degree
                    radian = Math.toRadians(degree)
                    if (Math.cos(radian) < -0.5) {
                        direction = LeftSwipe
                    } else {
                        direction = BottomSwipe
                    }
                }
                Quadrant.BottomRight -> {
                    degree = Math.toDegrees(radian)
                    degree = 360 - degree
                    radian = Math.toRadians(degree)
                    if (Math.cos(radian) < 0.5) {
                        direction = BottomSwipe
                    } else {
                        direction = RightSwipe
                    }
                }
            }

            val percent: Float =
                    if (direction === LeftSwipe || direction === RightSwipe) {
                        percentX
                    } else {
                        percentY
                    }

            if (Math.abs(percent) > option!!.swipeThreshold) {
                if (option!!.swipeDirection.contains(direction)) {
                    if (containerEventListener != null) {
                        containerEventListener!!.onContainerSwiped(point, direction)
                    }
                } else {
                    moveToOrigin()
                    if (containerEventListener != null) {
                        containerEventListener!!.onContainerMovedToOrigin()
                    }
                }
            } else {
                moveToOrigin()
                if (containerEventListener != null) {
                    containerEventListener!!.onContainerMovedToOrigin()
                }
            }
        }

        motionOriginX = event.rawX
        motionOriginY = event.rawY
    }

    private fun handleActionMove(event: MotionEvent) {
        isDragging = true

        updateTranslation(event)
        updateRotation()
        updateAlpha()

        if (containerEventListener != null) {
            containerEventListener!!.onContainerDragging(percentX, percentY)
        }
    }

    private fun updateTranslation(event: MotionEvent) {
        translationX = viewOriginX + event.rawX - motionOriginX
        translationY = viewOriginY + event.rawY - motionOriginY
    }

    private fun updateRotation() {
        rotation = percentX * 20
    }

    private fun updateAlpha() {
        val percentX = percentX
        val percentY = percentY

        if (option!!.swipeDirection === HORIZONTAL) {
            showHorizontalOverlay(percentX)
        } else if (option!!.swipeDirection === VERTICAL) {
            showVerticalOverlay(percentY)
        } else if (option!!.swipeDirection === FREEDOM_NO_BOTTOM) {
            if (Math.abs(percentX) < Math.abs(percentY) && percentY < 0) {
                showTopOverlay()
                setOverlayAlpha(Math.abs(percentY))
            } else {
                showHorizontalOverlay(percentX)
            }
        } else if (option!!.swipeDirection === FREEDOM) {
            if (Math.abs(percentX) > Math.abs(percentY)) {
                showHorizontalOverlay(percentX)
            } else {
                showVerticalOverlay(percentY)
            }
        } else {
            if (Math.abs(percentX) > Math.abs(percentY)) {
                if (percentX < 0) {
                    showLeftOverlay()
                } else {
                    showRightOverlay()
                }
                setOverlayAlpha(Math.abs(percentX))
            } else {
                if (percentY < 0) {
                    showTopOverlay()
                } else {
                    showBottomOverlay()
                }
                setOverlayAlpha(Math.abs(percentY))
            }
        }
    }

    private fun showHorizontalOverlay(percentX: Float) {
        if (percentX < 0) {
            showLeftOverlay()
        } else {
            showRightOverlay()
        }
        setOverlayAlpha(Math.abs(percentX))
    }

    private fun showVerticalOverlay(percentY: Float) {
        if (percentY < 0) {
            showTopOverlay()
        } else {
            showBottomOverlay()
        }
        setOverlayAlpha(Math.abs(percentY))
    }

    private fun moveToOrigin() {
        animate().translationX(viewOriginX)
                .translationY(viewOriginY)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setListener(null)
                .start()
    }

    fun setContainerEventListener(listener: ContainerEventListener?) {
        this.containerEventListener = listener
        viewOriginX = translationX
        viewOriginY = translationY
    }

    fun setCardStackOption(option: CardStackOption) {
        this.option = option
    }

    fun setDraggable(isDraggable: Boolean) {
        this.isDraggable = isDraggable
    }

    fun reset() {
        contentContainer!!.alpha = 1f
        overlayContainer!!.alpha = 0f
    }

    fun setOverlay(left: Int, right: Int, bottom: Int, top: Int) {
        if (leftOverlayView != null) {
            overlayContainer!!.removeView(leftOverlayView)
        }
        if (left != 0) {
            leftOverlayView = LayoutInflater.from(context).inflate(left, overlayContainer, false)
            overlayContainer!!.addView(leftOverlayView)
            leftOverlayView!!.alpha = 0f
        }

        if (rightOverlayView != null) {
            overlayContainer!!.removeView(rightOverlayView)
        }
        if (right != 0) {
            rightOverlayView = LayoutInflater.from(context).inflate(right, overlayContainer, false)
            overlayContainer!!.addView(rightOverlayView)
            rightOverlayView!!.alpha = 0f
        }

        if (bottomOverlayView != null) {
            overlayContainer!!.removeView(bottomOverlayView)
        }
        if (bottom != 0) {
            bottomOverlayView = LayoutInflater.from(context).inflate(bottom, overlayContainer, false)
            overlayContainer!!.addView(bottomOverlayView)
            bottomOverlayView!!.alpha = 0f
        }

        if (topOverlayView != null) {
            overlayContainer!!.removeView(topOverlayView)
        }
        if (top != 0) {
            topOverlayView = LayoutInflater.from(context).inflate(top, overlayContainer, false)
            overlayContainer!!.addView(topOverlayView)
            topOverlayView!!.alpha = 0f
        }
    }

    fun setOverlayAlpha(overlayAnimatorSet: AnimatorSet?) {
        overlayAnimatorSet?.start()
    }

    fun setOverlayAlpha(alpha: Float) {
        overlayContainer!!.alpha = alpha
    }

    fun showLeftOverlay() {
        if (leftOverlayView != null) {
            leftOverlayView!!.alpha = 1f
        }
        if (rightOverlayView != null) {
            rightOverlayView!!.alpha = 0f
        }
        if (bottomOverlayView != null) {
            bottomOverlayView!!.alpha = 0f
        }
        if (topOverlayView != null) {
            topOverlayView!!.alpha = 0f
        }
    }

    fun showRightOverlay() {
        if (leftOverlayView != null) {
            leftOverlayView!!.alpha = 0f
        }

        if (bottomOverlayView != null) {
            bottomOverlayView!!.alpha = 0f
        }

        if (topOverlayView != null) {
            topOverlayView!!.alpha = 0f
        }

        if (rightOverlayView != null) {
            rightOverlayView!!.alpha = 1f
        }
    }

    fun showBottomOverlay() {
        if (leftOverlayView != null) {
            leftOverlayView!!.alpha = 0f
        }

        if (bottomOverlayView != null) {
            bottomOverlayView!!.alpha = 1f
        }

        if (topOverlayView != null) {
            topOverlayView!!.alpha = 0f
        }

        if (rightOverlayView != null) {
            rightOverlayView!!.alpha = 0f
        }
    }

    fun showTopOverlay() {
        if (leftOverlayView != null) {
            leftOverlayView!!.alpha = 0f
        }

        if (bottomOverlayView != null) {
            bottomOverlayView!!.alpha = 0f
        }

        if (topOverlayView != null) {
            topOverlayView!!.alpha = 1f
        }

        if (rightOverlayView != null) {
            rightOverlayView!!.alpha = 0f
        }
    }
}
