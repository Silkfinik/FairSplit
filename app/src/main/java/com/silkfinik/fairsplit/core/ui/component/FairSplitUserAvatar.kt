package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes

@Composable
fun FairSplitUserAvatar(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    val avatarShape = FairSplitShapes.avatarContainer

    if (photoUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(avatarShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(avatarShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val initials = if (name.isNotBlank()) name.take(1).uppercase() else ""
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = fontSize
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}