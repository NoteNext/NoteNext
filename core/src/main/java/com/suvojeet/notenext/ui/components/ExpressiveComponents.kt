@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.core.R

/**
 * A top-app-bar icon button with a short cross-fade when the icon changes.
 *
 * The [delay] parameter is kept for source compatibility with existing call
 * sites but no longer staggers the buttons in — a toolbar should appear as one
 * unit rather than popping in one icon at a time.
 */
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    @Suppress("UNUSED_PARAMETER") delay: Int = 0,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 120))
                    .togetherWith(fadeOut(animationSpec = tween(durationMillis = 120)))
            },
            label = "IconChange"
        ) { targetIcon ->
            Icon(
                imageVector = targetIcon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}

@Composable
fun AnimatedDropdownItem(
    text: String,
    onClick: () -> Unit,
    textColor: Color = Color.Unspecified
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        },
        onClick = onClick
    )
}

@Composable
fun ExpressiveSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

/**
 * Subtle press feedback for tappable surfaces. The scale change is small and
 * non-bouncy so it reads as tactile rather than toy-like.
 */
@Composable
fun Modifier.springPress(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessHigh
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "PressScale"
    )
    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

@Composable
fun ExpressiveLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Static brand mark used on the welcome/setup screens. The previous version
 * morphed between shapes forever, which never settles and pulls attention away
 * from the page content.
 */
@Composable
fun MorphingLogo(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.72f)
            )
        }
    }
}

@Composable
fun AnimatedSetupStep(
    visible: Boolean,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 220, delayMillis = delay)) +
                slideInVertically(tween(durationMillis = 220, delayMillis = delay)) { it / 6 },
        exit = fadeOut()
    ) {
        content()
    }
}

@Composable
fun SetupLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingLogo(size = 120.dp)
            Spacer(modifier = Modifier.height(32.dp))
            LoadingIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Setting things up…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
