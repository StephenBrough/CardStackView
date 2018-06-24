package com.yuyakaido.android.cardstackview.sample

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.LeftSwipe
import com.yuyakaido.android.cardstackview.RightSwipe
import com.yuyakaido.android.cardstackview.SwipeDirection
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: TouristSpotCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setup()
        reload()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_activity_main_reload -> reload()
            R.id.menu_activity_main_add_first -> addFirst()
            R.id.menu_activity_main_add_last -> addLast()
            R.id.menu_activity_main_remove_first -> removeFirst()
            R.id.menu_activity_main_remove_last -> removeLast()
            R.id.menu_activity_main_swipe_left -> swipeLeft()
            R.id.menu_activity_main_swipe_right -> swipeRight()
            R.id.menu_activity_main_reverse -> reverse()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createTouristSpot(): TouristSpot =
            TouristSpot("Yasaka Shrine", "Kyoto", "https://source.unsplash.com/Xq1ntWruZQI/600x800")


    private fun createTouristSpots(): List<TouristSpot> {
        val spots = ArrayList<TouristSpot>()
        with(spots) {
            add(TouristSpot("Yasaka Shrine", "Kyoto", "https://source.unsplash.com/Xq1ntWruZQI/600x800"))
            add(TouristSpot("Fushimi Inari Shrine", "Kyoto", "https://source.unsplash.com/NYyCqdBOKwc/600x800"))
            add(TouristSpot("Bamboo Forest", "Kyoto", "https://source.unsplash.com/buF62ewDLcQ/600x800"))
            add(TouristSpot("Brooklyn Bridge", "New York", "https://source.unsplash.com/THozNzxEP3g/600x800"))
            add(TouristSpot("Empire State Building", "New York", "https://source.unsplash.com/USrZRcRS2Lw/600x800"))
            add(TouristSpot("The statue of Liberty", "New York", "https://source.unsplash.com/PeFk7fzxTdk/600x800"))
            add(TouristSpot("Louvre Museum", "Paris", "https://source.unsplash.com/LrMWHKqilUw/600x800"))
            add(TouristSpot("Eiffel Tower", "Paris", "https://source.unsplash.com/HN-5Z6AmxrM/600x800"))
            add(TouristSpot("Big Ben", "London", "https://source.unsplash.com/CdVAUADdqEc/600x800"))
            add(TouristSpot("Great Wall of China", "China", "https://source.unsplash.com/AWh9C-QjhE4/600x800"))
        }
        return spots
    }

    private fun createTouristSpotCardAdapter(): TouristSpotCardAdapter =
            TouristSpotCardAdapter(applicationContext).apply { addAll(createTouristSpots()) }

    private fun setup() {
        activityMainCardStackView!!.setCardEventListener(object : CardStackView.CardEventListener {
            override fun onCardDragging(percentX: Float, percentY: Float) {
                Log.d("CardStackView", "onCardDragging")
            }

            override fun onCardSwiped(direction: SwipeDirection?) {
                Log.d("CardStackView", "onCardSwiped: " + direction?.toString())
                Log.d("CardStackView", "topIndex: " + activityMainCardStackView!!.topIndex)
                if (activityMainCardStackView.topIndex == adapter.count - 5) {
                    Log.d("CardStackView", "Paginate: " + activityMainCardStackView!!.topIndex)
                    paginate()
                }
            }

            override fun onCardReversed() {
                Log.d("CardStackView", "onCardReversed")
            }

            override fun onCardMovedToOrigin() {
                Log.d("CardStackView", "onCardMovedToOrigin")
            }

            override fun onCardClicked(index: Int) {
                Log.d("CardStackView", "onCardClicked: $index")
            }
        })
    }

    private fun reload() {
        activityMainCardStackView.visibility = View.GONE
        activityMainProgressBar.visibility = View.VISIBLE
        Handler().postDelayed({
            adapter = createTouristSpotCardAdapter()
            activityMainCardStackView.setAdapter(adapter)
            activityMainCardStackView.visibility = View.VISIBLE
            activityMainProgressBar.visibility = View.GONE
        }, 1000)
    }

    private fun extractRemainingTouristSpots(): LinkedList<TouristSpot> {
        val spots = LinkedList<TouristSpot>()
        for (i in activityMainCardStackView.topIndex until adapter.count) {
            spots.add(adapter.getItem(i))
        }
        return spots
    }

    private fun addFirst() {
        val spots = extractRemainingTouristSpots()
        spots.addFirst(createTouristSpot())
        adapter.clear()
        adapter.addAll(spots)
        adapter.notifyDataSetChanged()
    }

    private fun addLast() {
        val spots = extractRemainingTouristSpots()
        spots.addLast(createTouristSpot())
        adapter.clear()
        adapter.addAll(spots)
        adapter.notifyDataSetChanged()
    }

    private fun removeFirst() {
        val spots = extractRemainingTouristSpots()
        if (spots.isEmpty()) {
            return
        }

        spots.removeFirst()
        adapter.clear()
        adapter.addAll(spots)
        adapter.notifyDataSetChanged()
    }

    private fun removeLast() {
        val spots = extractRemainingTouristSpots()
        if (spots.isEmpty()) {
            return
        }

        spots.removeLast()
        adapter.clear()
        adapter.addAll(spots)
        adapter.notifyDataSetChanged()
    }

    private fun paginate() {
        activityMainCardStackView.setPaginationReserved()
        adapter.addAll(createTouristSpots())
        adapter.notifyDataSetChanged()
    }

    private fun swipeLeft() {
        val spots = extractRemainingTouristSpots()
        if (spots.isEmpty()) {
            return
        }

        val target = activityMainCardStackView.topView
        val targetOverlay = activityMainCardStackView.topView.overlayContainer

        val rotation = ObjectAnimator.ofPropertyValuesHolder(
                target, PropertyValuesHolder.ofFloat("rotation", -10f))
        rotation.duration = 200
        val translateX = ObjectAnimator.ofPropertyValuesHolder(
                target, PropertyValuesHolder.ofFloat("translationX", 0f, -2000f))
        val translateY = ObjectAnimator.ofPropertyValuesHolder(
                target, PropertyValuesHolder.ofFloat("translationY", 0f, 500f))
        translateX.startDelay = 100
        translateY.startDelay = 100
        translateX.duration = 500
        translateY.duration = 500
        val cardAnimationSet = AnimatorSet()
        cardAnimationSet.playTogether(rotation, translateX, translateY)

        val overlayAnimator = ObjectAnimator.ofFloat(targetOverlay, "alpha", 0f, 1f)
        overlayAnimator.duration = 200
        val overlayAnimationSet = AnimatorSet()
        overlayAnimationSet.playTogether(overlayAnimator)

        activityMainCardStackView.swipe(LeftSwipe, cardAnimationSet, overlayAnimationSet)
    }

    private fun swipeRight() {
        val spots = extractRemainingTouristSpots()
        if (spots.isEmpty()) {
            return
        }

        val target = activityMainCardStackView.topView
        val targetOverlay = activityMainCardStackView.topView.overlayContainer

        val rotation = ObjectAnimator.ofPropertyValuesHolder(
                target, PropertyValuesHolder.ofFloat("rotation", 10f))
        rotation.duration = 200
        val translateX = ObjectAnimator.ofPropertyValuesHolder(
                target, PropertyValuesHolder.ofFloat("translationX", 0f, 2000f))
        val translateY = ObjectAnimator.ofPropertyValuesHolder(
                target, PropertyValuesHolder.ofFloat("translationY", 0f, 500f))
        translateX.startDelay = 100
        translateY.startDelay = 100
        translateX.duration = 500
        translateY.duration = 500
        val cardAnimationSet = AnimatorSet()
        cardAnimationSet.playTogether(rotation, translateX, translateY)

        val overlayAnimator = ObjectAnimator.ofFloat(targetOverlay, "alpha", 0f, 1f)
        overlayAnimator.duration = 200
        val overlayAnimationSet = AnimatorSet()
        overlayAnimationSet.playTogether(overlayAnimator)

        activityMainCardStackView.swipe(RightSwipe, cardAnimationSet, overlayAnimationSet)
    }

    private fun reverse() {
        activityMainCardStackView.reverse()
    }

}
