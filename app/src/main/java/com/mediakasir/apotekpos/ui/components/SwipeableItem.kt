package com.mediakasir.apotekpos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SwipeRedBright = Color(0xFFD32F2F)
private val SwipeRedLight = Color(0xFFEF5350)
private val SwipeAmber = Color(0xFFF57C00)
private val SwipeAmberLight = Color(0xFFFFB74D)

/**
 * Wraps [content] in a [SwipeToDismissBox] that reveals a coloured background
 * with an icon + label when the user swipes.
 *
 * @param state          External [SwipeToDismissBoxState] so the parent can react to dismissal.
 * @param enableStartToEnd  If true the START→END (right) swipe direction is active.
 * @param enableEndToStart  If true the END→START (left) swipe direction is active.
 * @param startToEndIcon    Icon shown when swiping right (default: Delete).
 * @param startToEndLabel   Label shown when swiping right.
 * @param startToEndColor   Background colour for the right-swipe reveal.
 * @param endToStartIcon    Icon shown when swiping left.
 * @param endToStartLabel   Label shown when swiping left.
 * @param endToStartColor   Background colour for the left-swipe reveal.
 * @param content           The foreground composable (list item UI).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItem(
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    enableStartToEnd: Boolean = false,
    enableEndToStart: Boolean = true,
    startToEndIcon: ImageVector = Icons.Outlined.Delete,
    startToEndLabel: String = "Hapus",
    startToEndColor: Color = SwipeRedLight,
    endToStartIcon: ImageVector = Icons.Outlined.Delete,
    endToStartLabel: String = "Hapus",
    endToStartColor: Color = SwipeRedBright,
    content: @Composable RowScope.() -> Unit,
) {
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = enableStartToEnd,
        enableDismissFromEndToStart = enableEndToStart,
        backgroundContent = {
            val direction = state.dismissDirection
            val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
            val isEndToStart = direction == SwipeToDismissBoxValue.EndToStart

            val targetColor = when {
                isStartToEnd -> startToEndColor
                isEndToStart -> endToStartColor
                else -> Color.Transparent
            }
            val bgColor by animateColorAsState(targetColor, label = "swipe_bg")

            val icon = when {
                isStartToEnd -> startToEndIcon
                else -> endToStartIcon
            }
            val label = when {
                isStartToEnd -> startToEndLabel
                else -> endToStartLabel
            }
            val alignment = when {
                isStartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                if (isStartToEnd || isEndToStart) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        content = content,
    )
}
