/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.ui.animation

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition



/**
 * Displays a Lottie animation or a static drawable depending on the user's
 * reduced-motion accessibility setting.
 *
 * @param animationResource Raw resource ID for the Lottie JSON animation.
 * @param staticDrawableResource Drawable resource ID shown when animations are disabled.
 * @param modifier Modifier applied to the composable.
 * @param contentDescription Accessibility content description.
 * @param iterations Number of times to play the animation. Defaults to infinite.
 * @param contentScale How the content should be scaled.
 */
@Composable
fun Animation(
    @RawRes animationResource: Int,
    @DrawableRes staticDrawableResource: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    iterations: Int = LottieConstants.IterateForever,
    contentScale: ContentScale = ContentScale.Fit,
) {
    if (isReducedMotionEnabled()) {
        Image(
            painter = painterResource(id = staticDrawableResource),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(animationResource),
        )
        LottieAnimation(
            composition = composition,
            iterations = iterations,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val animationScale = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
        )
    } catch (e: Settings.SettingNotFoundException) {
        1.0f
    }
    return animationScale == 0.0f
}
