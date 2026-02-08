package com.silkfinik.fairsplit.core.ui.theme

import android.graphics.Matrix
import android.graphics.Path
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object FairSplitShapes {
    val small: Shape = RoundedCornerShape(4.dp)
    val medium: Shape = RoundedCornerShape(12.dp)
    val large: Shape = RoundedCornerShape(24.dp)

    val fabShape: Shape = MaterialShapes.Cookie4Sided.customToComposeShape()

    val avatarContainer: Shape = MaterialShapes.Cookie9Sided.customToComposeShape()

    val badge: Shape = MaterialShapes.Burst.customToComposeShape()

    val searchField: Shape = MaterialShapes.Pill.customToComposeShape()

    val iconButtonContainer: Shape = MaterialShapes.PixelCircle.customToComposeShape()
}

fun RoundedPolygon.customToComposeShape(): Shape {
    return object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = Path()

            this@customToComposeShape.toPath(path)

            val matrix = Matrix()
            matrix.setScale(size.width, size.height)
            path.transform(matrix)

            return Outline.Generic(path.asComposePath())
        }
    }
}