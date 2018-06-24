package com.yuyakaido.android.cardstackview.internal

import com.yuyakaido.android.cardstackview.FREEDOM
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeDirection

class CardStackOption {
    var visibleCount = 3
    var swipeThreshold = 0.75f // Percentage
    var translationDiff = 12f // DP
    var scaleDiff = 0.02f // Percentage
    var stackFrom = StackFrom.DEFAULT
    var isElevationEnabled = true
    var isSwipeEnabled = true
    var leftOverlay = 0 // Layout Resource ID
    var rightOverlay = 0 // Layout Resource ID
    var bottomOverlay = 0 // Layout Resource ID
    var topOverlay = 0 // Layout Resource ID
    var swipeDirection = FREEDOM
}
