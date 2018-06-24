package com.yuyakaido.android.cardstackview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.content.Context
import android.content.res.TypedArray
import android.database.DataSetObserver
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout

import com.yuyakaido.android.cardstackview.internal.CardContainerView
import com.yuyakaido.android.cardstackview.internal.CardStackOption
import com.yuyakaido.android.cardstackview.internal.CardStackState
import com.yuyakaido.android.cardstackview.internal.Util

import java.util.LinkedList

import androidx.core.view.ViewCompat

class CardStackView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attrs, defStyle) {

    private val option = CardStackOption()
    private val state = CardStackState()

    private var adapter: BaseAdapter? = null
    private val containers = LinkedList<CardContainerView>()
    private var cardEventListener: CardEventListener? = null
    private val dataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            var shouldReset = false
            if (state.isPaginationReserved) {
                state.isPaginationReserved = false
            } else {
                val isSameCount = state.lastCount == adapter!!.count
                shouldReset = !isSameCount
            }
            initialize(shouldReset)
            state.lastCount = adapter!!.count
        }
    }
    private val containerEventListener = object : CardContainerView.ContainerEventListener {
        override fun onContainerDragging(percentX: Float, percentY: Float) = update(percentX, percentY)
        override fun onContainerSwiped(point: Point, direction: SwipeDirection?) = swipe(point, direction)

        override fun onContainerMovedToOrigin() {
            initializeCardStackPosition()
            cardEventListener?.onCardMovedToOrigin()
        }

        override fun onContainerClicked() {
            cardEventListener?.onCardClicked(state.topIndex)
        }
    }

    val topView: CardContainerView
        get() = containers.first

    private val bottomView: CardContainerView
        get() = containers.last

    val topIndex: Int
        get() = state.topIndex

    interface CardEventListener {
        fun onCardDragging(percentX: Float, percentY: Float)
        fun onCardSwiped(direction: SwipeDirection?)
        fun onCardReversed()
        fun onCardMovedToOrigin()
        fun onCardClicked(index: Int)
    }

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.CardStackView)
        setVisibleCount(array.getInt(R.styleable.CardStackView_visibleCount, option.visibleCount))
        setSwipeThreshold(array.getFloat(R.styleable.CardStackView_swipeThreshold, option.swipeThreshold))
        setTranslationDiff(array.getFloat(R.styleable.CardStackView_translationDiff, option.translationDiff))
        setScaleDiff(array.getFloat(R.styleable.CardStackView_scaleDiff, option.scaleDiff))
        setStackFrom(StackFrom.values()[array.getInt(R.styleable.CardStackView_stackFrom, option.stackFrom.ordinal)])
        setElevationEnabled(array.getBoolean(R.styleable.CardStackView_elevationEnabled, option.isElevationEnabled))
        setSwipeEnabled(array.getBoolean(R.styleable.CardStackView_swipeEnabled, option.isSwipeEnabled))
        setSwipeDirection(from(array.getInt(R.styleable.CardStackView_swipeDirection, 0)))
        setLeftOverlay(array.getResourceId(R.styleable.CardStackView_leftOverlay, 0))
        setRightOverlay(array.getResourceId(R.styleable.CardStackView_rightOverlay, 0))
        setBottomOverlay(array.getResourceId(R.styleable.CardStackView_bottomOverlay, 0))
        setTopOverlay(array.getResourceId(R.styleable.CardStackView_topOverlay, 0))
        array.recycle()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (state.isInitialized && visibility == View.VISIBLE) {
            initializeCardStackPosition()
        }
    }

    private fun initialize(shouldReset: Boolean) {
        resetIfNeeded(shouldReset)
        initializeViews()
        initializeCardStackPosition()
        initializeViewContents()
    }

    private fun resetIfNeeded(shouldReset: Boolean) {
        if (shouldReset) {
            state.reset()
        }
    }

    private fun initializeViews() {
        removeAllViews()
        containers.clear()

        for (i in 0 until option.visibleCount) {
            val view = LayoutInflater.from(context).inflate(R.layout.card_container, this, false) as CardContainerView
            view.setDraggable(false)
            view.setCardStackOption(option)
            view.setOverlay(option.leftOverlay, option.rightOverlay, option.bottomOverlay, option.topOverlay)
            containers.add(0, view)
            addView(view)
        }

        containers.first.setContainerEventListener(containerEventListener)

        state.isInitialized = true
    }

    private fun initializeCardStackPosition() {
        clear()
        update(0f, 0f)
    }

    private fun initializeViewContents() {
        for (i in 0 until option.visibleCount) {
            val container = containers[i]
            val adapterIndex = state.topIndex + i

            if (adapterIndex < adapter!!.count) {
                val parent = container.contentContainer
                val child = adapter!!.getView(adapterIndex, parent?.getChildAt(0), parent)
                if (parent?.childCount == 0) {
                    parent.addView(child)
                }
                container.visibility = View.VISIBLE
            } else {
                container.visibility = View.GONE
            }
        }
        if (!adapter!!.isEmpty) {
            topView.setDraggable(true)
        }
    }

    private fun loadNextView() {
        val lastIndex = state.topIndex + option.visibleCount - 1
        val hasNextCard = lastIndex < adapter!!.count
        if (hasNextCard) {
            val container = bottomView
            container.setDraggable(false)
            val parent = container.contentContainer
            val child = adapter!!.getView(lastIndex, parent?.getChildAt(0), parent)
            if (parent?.childCount == 0) {
                parent.addView(child)
            }
        } else {
            val container = bottomView
            container.setDraggable(false)
            container.visibility = View.GONE
        }

        val hasCard = state.topIndex < adapter!!.count
        if (hasCard) {
            topView.setDraggable(true)
        }
    }

    private fun clear() {
        for (i in 0 until option.visibleCount) {
            val view = containers[i]
            view.reset()
            view.translationX = 0f
            view.translationY = 0f
            view.scaleX = 1f
            view.scaleY = 1f
            view.rotation = 0f
        }
    }

    private fun update(percentX: Float, percentY: Float) {
        if (cardEventListener != null) {
            cardEventListener!!.onCardDragging(percentX, percentY)
        }

        if (!option.isElevationEnabled) {
            return
        }

        for (i in 1 until option.visibleCount) {
            val view = containers[i]

            val currentScale = 1f - i * option.scaleDiff
            val nextScale = 1f - (i - 1) * option.scaleDiff
            val percent = currentScale + (nextScale - currentScale) * Math.abs(percentX)
            view.scaleX = percent
            view.scaleY = percent

            var currentTranslationY = i * Util.toPx(context, option.translationDiff)
            if (option.stackFrom === StackFrom.Top) {
                currentTranslationY *= -1f
            }

            var nextTranslationY = (i - 1) * Util.toPx(context, option.translationDiff)
            if (option.stackFrom === StackFrom.Top) {
                nextTranslationY *= -1f
            }

            val translationY = currentTranslationY - Math.abs(percentX) * (currentTranslationY - nextTranslationY)
            view.translationY = translationY
        }
    }

    fun performReverse(point: Point?, prevView: View, listener: Animator.AnimatorListener) {
        reorderForReverse(prevView)
        val topView = topView
        topView.translationX = point!!.x.toFloat()
        topView.translationY = (-point.y).toFloat()
        topView.animate()
                .translationX(topView.viewOriginX)
                .translationY(topView.viewOriginY)
                .setListener(listener)
                .setDuration(400L)
                .start()
    }

    fun performSwipe(point: Point, listener: Animator.AnimatorListener) {
        topView.animate()
                .translationX(point.x.toFloat())
                .translationY((-point.y).toFloat())
                .setDuration(400L)
                .setListener(listener)
                .start()
    }

    fun performSwipe(direction: SwipeDirection?, set: AnimatorSet, overlayAnimatorSet: AnimatorSet?, listener: Animator.AnimatorListener) {
        var showOverlay = true

        when (direction) {
            LeftSwipe -> topView.showLeftOverlay()
            RightSwipe -> topView.showRightOverlay()
            TopSwipe -> topView.showTopOverlay()
            BottomSwipe -> topView.showBottomOverlay()
            else -> showOverlay = false
        }

        if (showOverlay) {
            if (overlayAnimatorSet != null) {
                topView.setOverlayAlpha(overlayAnimatorSet)
            } else {
                topView.setOverlayAlpha(1f)
            }
        }

        with(set) {
            addListener(listener)
            interpolator = TimeInterpolator { input ->
                val view = topView
                update(view.percentX, view.percentY)
                input
            }
            start()
        }
    }

    private fun moveToBottom(container: CardContainerView) {
        val parent = container.parent as CardStackView
        parent.removeView(container)
        parent.addView(container, 0)
    }

    private fun moveToTop(container: CardContainerView, child: View) {
        val parent = container.parent as CardStackView
        parent.removeView(container)
        parent.addView(container)

        container.contentContainer?.removeAllViews()
        container.contentContainer?.addView(child)
        container.visibility = View.VISIBLE
    }

    private fun reorderForSwipe() {
        moveToBottom(topView)
        containers.addLast(containers.removeFirst())
    }

    private fun reorderForReverse(prevView: View) {
        val bottomView = bottomView
        moveToTop(bottomView, prevView)
        containers.addFirst(containers.removeLast())
    }

    private fun executePreSwipeTask() {
        containers.first.setContainerEventListener(null)
        containers.first.setDraggable(false)
        if (containers.size > 1) {
            containers[1].setContainerEventListener(containerEventListener)
            containers[1].setDraggable(true)
        }
    }

    private fun executePostSwipeTask(point: Point, direction: SwipeDirection?) {
        reorderForSwipe()

        state.lastPoint = point

        initializeCardStackPosition()

        state.topIndex = state.topIndex + 1

        if (cardEventListener != null) {
            cardEventListener!!.onCardSwiped(direction)
        }

        loadNextView()

        containers.last.setContainerEventListener(null)
        containers.first.setContainerEventListener(containerEventListener)
    }

    private fun executePostReverseTask() {
        state.lastPoint = null

        initializeCardStackPosition()

        state.topIndex = state.topIndex - 1

        if (cardEventListener != null) {
            cardEventListener!!.onCardReversed()
        }

        containers.last.setContainerEventListener(null)
        containers.first.setContainerEventListener(containerEventListener)

        topView.setDraggable(true)
    }

    fun setCardEventListener(listener: CardEventListener) {
        this.cardEventListener = listener
    }

    fun setAdapter(adapter: BaseAdapter) {
        this.adapter?.unregisterDataSetObserver(dataSetObserver)
        this.adapter = adapter
        this.adapter?.registerDataSetObserver(dataSetObserver)
        this.state.lastCount = adapter.count
        initialize(true)
    }

    private fun setVisibleCount(visibleCount: Int) {
        option.visibleCount = visibleCount
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setSwipeThreshold(swipeThreshold: Float) {
        option.swipeThreshold = swipeThreshold
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setTranslationDiff(translationDiff: Float) {
        option.translationDiff = translationDiff
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setScaleDiff(scaleDiff: Float) {
        option.scaleDiff = scaleDiff
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setStackFrom(stackFrom: StackFrom) {
        option.stackFrom = stackFrom
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setElevationEnabled(isElevationEnabled: Boolean) {
        option.isElevationEnabled = isElevationEnabled
        if (adapter != null) {
            initialize(false)
        }
    }

    fun setSwipeEnabled(isSwipeEnabled: Boolean) {
        option.isSwipeEnabled = isSwipeEnabled
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setSwipeDirection(swipeDirection: List<SwipeDirection>) {
        option.swipeDirection = swipeDirection
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setLeftOverlay(leftOverlay: Int) {
        option.leftOverlay = leftOverlay
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setRightOverlay(rightOverlay: Int) {
        option.rightOverlay = rightOverlay
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setBottomOverlay(bottomOverlay: Int) {
        option.bottomOverlay = bottomOverlay
        if (adapter != null) {
            initialize(false)
        }
    }

    private fun setTopOverlay(topOverlay: Int) {
        option.topOverlay = topOverlay
        if (adapter != null) {
            initialize(false)
        }
    }

    fun setPaginationReserved() {
        state.isPaginationReserved = true
    }

    fun swipe(point: Point, direction: SwipeDirection?) {
        executePreSwipeTask()
        performSwipe(point, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                executePostSwipeTask(point, direction)
            }
        })
    }

    fun swipe(direction: SwipeDirection, set: AnimatorSet) {
        swipe(direction, set, null)
    }

    fun swipe(direction: SwipeDirection, cardAnimatorSet: AnimatorSet, overlayAnimatorSet: AnimatorSet?) {
        executePreSwipeTask()
        performSwipe(direction, cardAnimatorSet, overlayAnimatorSet, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                executePostSwipeTask(Point(0, -2000), direction)
            }
        })
    }

    fun reverse() {
        if (state.lastPoint != null) {
            val parent = containers.last
            val prevView = adapter!!.getView(state.topIndex - 1, null, parent)
            performReverse(state.lastPoint, prevView, object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    executePostReverseTask()
                }
            })
        }
    }

}
