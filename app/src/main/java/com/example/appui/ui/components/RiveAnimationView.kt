package com.example.appui.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop
import app.rive.runtime.kotlin.RiveAnimationView
import com.example.appui.R

/**
 * Rive Animation Composable for Jetpack Compose
 *
 * @param modifier Modifier
 * @param resId Raw resource ID of .riv file
 * @param autoplay Auto play animation
 * @param artboardName Artboard name (optional)
 * @param animationName Animation name (optional)
 * @param stateMachineName State machine name (optional)
 * @param fit How to fit animation in view
 * @param alignment Alignment in view
 * @param loop Loop mode
 * @param contentDescription Accessibility description
 * @param updateFunction Callback to update RiveAnimationView
 */
@Composable
fun RiveAnimation(
    modifier: Modifier = Modifier,
    @RawRes resId: Int,
    autoplay: Boolean = true,
    artboardName: String? = null,
    animationName: String? = null,
    stateMachineName: String? = null,
    fit: Fit = Fit.CONTAIN,
    alignment: Alignment = Alignment.CENTER,
    loop: Loop = Loop.AUTO,
    contentDescription: String? = null,
    updateFunction: (RiveAnimationView) -> Unit = { }
) {
    // Preview mode - show placeholder
    if (LocalInspectionMode.current) {
        Image(
            modifier = modifier.size(100.dp),
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = contentDescription
        )
    } else {
        val semantics = if (contentDescription != null) {
            Modifier.semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
        } else {
            Modifier
        }

        AndroidView(
            modifier = modifier.then(semantics),
            factory = { context ->
                RiveAnimationView(context).apply {
                    setRiveResource(
                        resId,
                        artboardName,
                        animationName,
                        stateMachineName,
                        autoplay,
                        fit,
                        alignment,
                        loop
                    )
                }
            },
            update = { view ->
                updateFunction.invoke(view)
            }
        )
    }
}
