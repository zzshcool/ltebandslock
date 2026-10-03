package com.ltebandslock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltebandslock.ui.theme.JoyConBlue
import com.ltebandslock.ui.theme.NintendoRed
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalFair
import com.ltebandslock.ui.theme.Slate400
import com.ltebandslock.ui.theme.Slate700

/**
 * 5-bar stepped cellular signal strength indicator
 * e.g.  ▂▃▅█
 */
@Composable
fun SignalBarsIndicator(
    bars: Int,
    maxBars: Int = 5,
    showLabel: Boolean = false,
    labelSuffix: String = "",
    modifier: Modifier = Modifier
) {
    val clampedBars = bars.coerceIn(0, maxBars)

    // Dynamic bar color based on strength
    val activeColor = when (clampedBars) {
        5 -> SignalExcellent        // Mario 1UP Green (#00C853)
        4 -> JoyConBlue             // Joy-Con Blue (#00B5E2)
        3 -> SignalFair             // Star Amber (#FFB300)
        in 1..2 -> NintendoRed      // Nintendo Red (#E60012)
        else -> Slate400
    }

    val inactiveColor = Slate700.copy(alpha = 0.45f)
    val heights = listOf(4.dp, 7.dp, 10.dp, 13.dp, 16.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..maxBars) {
            val isActive = i <= clampedBars
            val h = heights.getOrElse(i - 1) { (i * 3 + 1).dp }

            Box(
                modifier = Modifier
                    .width(3.2.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) activeColor else inactiveColor)
            )
        }

        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (labelSuffix.isNotEmpty()) "$clampedBars/$maxBars $labelSuffix" else "$clampedBars/$maxBars",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = activeColor
            )
        }
    }
}
