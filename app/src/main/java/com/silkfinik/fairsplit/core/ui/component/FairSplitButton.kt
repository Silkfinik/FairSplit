package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes

enum class FairSplitButtonStyle {
    Primary,
    Secondary,
    Text
}

@Composable
fun FairSplitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FairSplitButtonStyle = FairSplitButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "ButtonScale"
    )

    val buttonHeight = 50.dp

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val targetWidth = if (isLoading) buttonHeight else maxWidth

        val animatedWidth by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            label = "ButtonWidth"
        )

        val animatedCornerRadius by animateDpAsState(
            targetValue = if (isLoading) 25.dp else 12.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            label = "ButtonShape"
        )

        val animatedShape = RoundedCornerShape(animatedCornerRadius)

        val content: @Composable (Color) -> Unit = { contentColor ->
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "ButtonContent"
            ) { loading ->
                if (loading) {
                    Box(contentAlignment = Alignment.Center) {
                        FairSplitLoader(
                            modifier = Modifier.size(24.dp),
                            color = contentColor
                        )
                    }
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        when (style) {
            FairSplitButtonStyle.Primary -> {
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(buttonHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    enabled = enabled,
                    shape = animatedShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(0.dp),
                    interactionSource = interactionSource
                ) {
                    content(MaterialTheme.colorScheme.onPrimary)
                }
            }
            FairSplitButtonStyle.Secondary -> {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(buttonHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    enabled = enabled,
                    shape = animatedShape,
                    contentPadding = PaddingValues(0.dp),
                    interactionSource = interactionSource
                ) {
                    content(MaterialTheme.colorScheme.primary)
                }
            }
            FairSplitButtonStyle.Text -> {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    enabled = enabled && !isLoading,
                    shape = FairSplitShapes.medium,
                    interactionSource = interactionSource
                ) {
                    if (isLoading) {
                        FairSplitLoader(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}