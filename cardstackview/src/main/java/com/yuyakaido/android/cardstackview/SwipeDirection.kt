package com.yuyakaido.android.cardstackview

sealed class SwipeDirection
object LeftSwipe : SwipeDirection()
object RightSwipe : SwipeDirection()
object TopSwipe : SwipeDirection()
object BottomSwipe : SwipeDirection()

val FREEDOM = listOf(LeftSwipe, RightSwipe, TopSwipe, BottomSwipe)
val FREEDOM_NO_BOTTOM = listOf(TopSwipe, LeftSwipe, RightSwipe)
val HORIZONTAL = listOf(LeftSwipe, RightSwipe)
val VERTICAL = listOf(TopSwipe, BottomSwipe)

fun from(value: Int): List<SwipeDirection> {
    return when (value) {
        0 -> FREEDOM
        1 -> FREEDOM_NO_BOTTOM
        2 -> HORIZONTAL
        3 -> VERTICAL
        else -> FREEDOM
    }
}
