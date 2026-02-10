package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes
import kotlin.math.roundToInt

@Composable
fun FairSplitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null
) {
    val expressiveSpatialSpec = spring<IntSize>(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

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

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (label != null) { { Text(label) } } else null,
        modifier = modifier
            .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
            .animateContentSize(expressiveSpatialSpec),
        textStyle = textStyle,
        shape = FairSplitShapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            errorBorderColor = MaterialTheme.colorScheme.error,
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        readOnly = readOnly,
        isError = isError,
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        suffix = suffix,
        placeholder = placeholder,
        singleLine = singleLine,
        enabled = enabled
    )
}