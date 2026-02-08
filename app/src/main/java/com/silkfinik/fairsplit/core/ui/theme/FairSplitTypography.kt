package com.silkfinik.fairsplit.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fairsplit.design.theme.createExpressiveFont
val FairSplitTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = createExpressiveFont(weight = 550, width = 125f, opticalSize = 57f),
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.W400
    ),
    displayMedium = TextStyle(
        fontFamily = createExpressiveFont(weight = 550, width = 120f, opticalSize = 45f),
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.W400
    ),
    displaySmall = TextStyle(
        fontFamily = createExpressiveFont(weight = 550, width = 120f, opticalSize = 36f),
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.W400
    ),

    headlineLarge = TextStyle(
        fontFamily = createExpressiveFont(weight = 400, width = 110f, opticalSize = 32f),
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.W400
    ),
    headlineMedium = TextStyle(
        fontFamily = createExpressiveFont(weight = 400, width = 110f, opticalSize = 28f),
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.W400
    ),
    headlineSmall = TextStyle(
        fontFamily = createExpressiveFont(weight = 500, width = 105f, opticalSize = 24f),
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.W500
    ),

    titleLarge = TextStyle(
        fontFamily = createExpressiveFont(weight = 500, width = 105f, opticalSize = 22f),
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.W500
    ),
    titleMedium = TextStyle(
        fontFamily = createExpressiveFont(weight = 500, width = 100f, opticalSize = 16f),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = createExpressiveFont(weight = 600, width = 100f, opticalSize = 14f), // Акцент на подзаголовках
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.1.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = createExpressiveFont(weight = 400, width = 100f, opticalSize = 16f),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = createExpressiveFont(weight = 400, width = 100f, opticalSize = 14f),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = createExpressiveFont(weight = 400, width = 100f, opticalSize = 12f),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.4.sp
    ),

    labelLarge = TextStyle(
        fontFamily = createExpressiveFont(weight = 500, width = 110f, opticalSize = 14f),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = createExpressiveFont(weight = 500, width = 105f, opticalSize = 12f),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = createExpressiveFont(weight = 500, width = 105f, opticalSize = 11f),
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.5.sp
    )
)