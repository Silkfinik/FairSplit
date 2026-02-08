package com.fairsplit.design.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.silkfinik.fairsplit.R

object FontAxes {
    const val Grade = "GRAD"
    const val Width = "wdth"
}

@OptIn(ExperimentalTextApi::class)
fun createExpressiveFont(
    weight: Int = 400,
    width: Float = 100f,
    opticalSize: Float = 14f,
    grade: Int = 0
): FontFamily {
    return FontFamily(
        Font(
            resId = R.font.roboto_flex,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width),
                FontVariation.grade(grade),
                FontVariation.opticalSizing(opticalSize.sp)
            )
        )
    )
}