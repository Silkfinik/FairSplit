package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes
import com.silkfinik.fairsplit.core.ui.theme.customToComposeShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FairSplitPasswordField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String = "Пароль",
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val shapeSize = 14.dp
    val shapeSpacing = 4.dp
    val itemWidthPx = with(density) { (shapeSize + shapeSpacing).toPx() }

    val passwordShapes = remember {
        listOf(
            MaterialShapes.Cookie4Sided.customToComposeShape(),
            MaterialShapes.Burst.customToComposeShape(),
            MaterialShapes.Diamond.customToComposeShape(),
            MaterialShapes.Triangle.customToComposeShape(),
            MaterialShapes.Cookie9Sided.customToComposeShape(),
            MaterialShapes.Flower.customToComposeShape()
        )
    }

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            for (i in 0..10) {
                val intensity = if (i % 2 == 0) 10f else -10f
                val decay = (11 - i) / 10f
                shakeOffset.animateTo(
                    targetValue = intensity * decay,
                    animationSpec = tween(durationMillis = 50)
                )
            }
            shakeOffset.animateTo(0f)
        }
    }

    LaunchedEffect(state.text.length) {
        if (!isPasswordVisible && state.text.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val transparentSelectionColors = TextSelectionColors(
        handleColor = Color.Transparent,
        backgroundColor = Color.Transparent
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    CompositionLocalProvider(
        LocalTextSelectionColors provides if (!isPasswordVisible) transparentSelectionColors else LocalTextSelectionColors.current
    ) {
        BasicSecureTextField(
            state = state,
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) },
            enabled = enabled,
            textObfuscationMode = if (isPasswordVisible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = if (!isPasswordVisible) SolidColor(Color.Transparent) else SolidColor(MaterialTheme.colorScheme.primary),
            decorator = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = state.text.toString(),
                    innerTextField = {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .alpha(if (isPasswordVisible) 1f else 0f)
                                    .fillMaxWidth()
                            ) {
                                innerTextField()
                            }

                            if (!isPasswordVisible) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(scrollState),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.CenterStart,
                                        modifier = Modifier.pointerInput(Unit) {
                                            detectTapGestures { tapOffset ->
                                                focusRequester.requestFocus()
                                                val textLength = state.text.length
                                                if (textLength > 0) {
                                                    val absoluteX = tapOffset.x + scrollState.value
                                                    val rawIndex = (absoluteX / itemWidthPx).roundToInt()
                                                    val safeIndex = rawIndex.coerceIn(0, textLength)
                                                    state.edit { selection = TextRange(safeIndex) }
                                                }
                                            }
                                        }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            state.text.forEachIndexed { index, _ ->
                                                val shape = passwordShapes[index % passwordShapes.size]
                                                Box(
                                                    modifier = Modifier
                                                        .padding(end = shapeSpacing)
                                                        .size(shapeSize)
                                                        .clip(shape)
                                                        .background(MaterialTheme.colorScheme.onSurface)
                                                )
                                            }
                                        }

                                        if (isFocused) {
                                            val cursorIndex = state.selection.start
                                            val cursorOffset = (shapeSize + shapeSpacing) * cursorIndex

                                            BlinkingCursor(
                                                offset = cursorOffset,
                                                height = 18.dp,
                                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    label = { Text(label) },
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    colors = textFieldColors,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = textFieldColors,
                            shape = FairSplitShapes.medium
                        )
                    },
                    supportingText = if (supportingText != null) {
                        { Text(supportingText) }
                    } else null,
                    trailingIcon = {
                        FilledIconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isPasswordVisible,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null)
                            }
                            AnimatedVisibility(
                                visible = !isPasswordVisible,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun BlinkingCursor(
    offset: Dp,
    height: Dp,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Box(
        modifier = Modifier
            .offset(x = offset)
            .width(2.dp)
            .height(height)
            .background(color.copy(alpha = alpha))
    )
}