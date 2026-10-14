package com.ares.ewe_man.presentation.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.ares.ewe_man.R

@Composable
fun CourierAvatarImage(
    photoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val placeholder = painterResource(R.drawable.img_courier_avatar_placeholder)
    AsyncImage(
        model = resolveProfileImageUrl(photoUrl),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
    )
}
