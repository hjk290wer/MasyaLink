package com.shiroyama.messenger.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object ShapeTokens {
    val Card = RoundedCornerShape(28.dp)
    val CardSmall = RoundedCornerShape(20.dp)
    val BubbleMine = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 7.dp)
    val BubbleOthers = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 7.dp, bottomEnd = 24.dp)
    val Input = RoundedCornerShape(28.dp)
    val Button = RoundedCornerShape(18.dp)
    val Media = RoundedCornerShape(20.dp)
    val Sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}