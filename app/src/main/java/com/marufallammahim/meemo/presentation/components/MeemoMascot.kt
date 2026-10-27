package com.marufallammahim.meemo.presentation.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marufallammahim.meemo.assistant.engine.AssistantExecutionState
import com.marufallammahim.meemo.presentation.theme.MeemoPalette

enum class MascotVisualState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    SUCCESS,
    ERROR;

    companion object {
        fun fromExecutionState(
            execState: AssistantExecutionState,
            isSpeaking: Boolean
        ): MascotVisualState {
            if (isSpeaking) return SPEAKING
            return when (execState) {
                AssistantExecutionState.IDLE, AssistantExecutionState.CANCELLED -> IDLE
                AssistantExecutionState.LISTENING -> LISTENING
                AssistantExecutionState.TRANSCRIBING,
                AssistantExecutionState.PROCESSING,
                AssistantExecutionState.CHECKING_CAPABILITIES,
                AssistantExecutionState.AWAITING_CONFIRMATION,
                AssistantExecutionState.EXECUTING,
                AssistantExecutionState.VERIFYING -> THINKING
                AssistantExecutionState.SUCCESS, AssistantExecutionState.PARTIAL_SUCCESS -> SUCCESS
                AssistantExecutionState.FAILED, AssistantExecutionState.UNAVAILABLE -> ERROR
            }
        }
    }
}

/**
 * Original Compose Canvas vector artwork for the Meemo Cat Mascot.
 * Supports Idle, Listening, Thinking, Speaking, Success, and Error states,
 * respects system reduced-motion settings, and exposes clear accessibility semantics.
 */
@Composable
fun MeemoMascot(
    visualState: MascotVisualState,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0.0f
        }.getOrDefault(false)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "MeemoMascotTransition")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MascotPulse"
    )

    val effectivePulse = if (reducedMotion) 0.5f else pulsePhase

    val accessibilityLabel = when (visualState) {
        MascotVisualState.IDLE -> "Meemo mascot is idle and ready"
        MascotVisualState.LISTENING -> "Meemo mascot is actively listening to microphone"
        MascotVisualState.THINKING -> "Meemo mascot is thinking and verifying capabilities"
        MascotVisualState.SPEAKING -> "Meemo mascot is speaking response aloud"
        MascotVisualState.SUCCESS -> "Meemo mascot indicates command succeeded"
        MascotVisualState.ERROR -> "Meemo mascot indicates command needs attention"
    }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = accessibilityLabel }
    ) {
        val w = this.size.width
        val h = this.size.height

        // 1. State Aura Ring for LISTENING / SPEAKING / SUCCESS / ERROR
        when (visualState) {
            MascotVisualState.LISTENING -> {
                drawCircle(
                    color = MeemoPalette.BrandAccent.copy(alpha = 0.28f * (1.2f - effectivePulse)),
                    radius = (w * 0.46f) * (0.9f + 0.15f * effectivePulse),
                    center = Offset(w * 0.5f, h * 0.54f)
                )
            }
            MascotVisualState.SUCCESS -> {
                drawCircle(
                    color = MeemoPalette.StatusSuccess.copy(alpha = 0.22f),
                    radius = w * 0.46f,
                    center = Offset(w * 0.5f, h * 0.54f)
                )
            }
            MascotVisualState.ERROR -> {
                drawCircle(
                    color = MeemoPalette.StatusError.copy(alpha = 0.22f),
                    radius = w * 0.46f,
                    center = Offset(w * 0.5f, h * 0.54f)
                )
            }
            else -> Unit
        }

        // 2. Left & Right Cat Ears
        val leftEar = Path().apply {
            moveTo(w * 0.22f, h * 0.42f)
            lineTo(w * 0.15f, h * 0.14f)
            lineTo(w * 0.42f, h * 0.28f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(w * 0.78f, h * 0.42f)
            lineTo(w * 0.85f, h * 0.14f)
            lineTo(w * 0.58f, h * 0.28f)
            close()
        }
        drawPath(leftEar, color = MeemoPalette.BrandPrimary)
        drawPath(rightEar, color = MeemoPalette.BrandPrimary)

        // 3. Rounded Cat Head
        drawRoundRect(
            color = MeemoPalette.BrandPrimaryVariant,
            topLeft = Offset(w * 0.18f, h * 0.26f),
            size = Size(w * 0.64f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.26f, w * 0.26f)
        )
        drawRoundRect(
            color = MeemoPalette.BrandAccent,
            topLeft = Offset(w * 0.18f, h * 0.26f),
            size = Size(w * 0.64f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.26f, w * 0.26f),
            style = Stroke(width = w * 0.025f)
        )

        // 4. Eyes based on MascotVisualState
        val leftEyeCenter = Offset(w * 0.37f, h * 0.50f)
        val rightEyeCenter = Offset(w * 0.63f, h * 0.50f)

        when (visualState) {
            MascotVisualState.SUCCESS -> {
                // Happy chevron eyes
                drawLine(
                    color = MeemoPalette.DarkTextPrimary,
                    start = Offset(w * 0.31f, h * 0.52f),
                    end = Offset(w * 0.37f, h * 0.46f),
                    strokeWidth = w * 0.035f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = MeemoPalette.DarkTextPrimary,
                    start = Offset(w * 0.37f, h * 0.46f),
                    end = Offset(w * 0.43f, h * 0.52f),
                    strokeWidth = w * 0.035f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = MeemoPalette.DarkTextPrimary,
                    start = Offset(w * 0.57f, h * 0.52f),
                    end = Offset(w * 0.63f, h * 0.46f),
                    strokeWidth = w * 0.035f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = MeemoPalette.DarkTextPrimary,
                    start = Offset(w * 0.63f, h * 0.46f),
                    end = Offset(w * 0.69f, h * 0.52f),
                    strokeWidth = w * 0.035f,
                    cap = StrokeCap.Round
                )
            }
            MascotVisualState.ERROR -> {
                drawCircle(color = MeemoPalette.StatusWarning, radius = w * 0.055f, center = leftEyeCenter)
                drawCircle(color = MeemoPalette.StatusWarning, radius = w * 0.055f, center = rightEyeCenter)
            }
            else -> {
                val eyeRadius = if (visualState == MascotVisualState.LISTENING) w * 0.065f else w * 0.052f
                drawCircle(color = MeemoPalette.DarkTextPrimary, radius = eyeRadius, center = leftEyeCenter)
                drawCircle(color = MeemoPalette.DarkTextPrimary, radius = eyeRadius, center = rightEyeCenter)
            }
        }

        // 5. Nose & Mouth / Speaking Waveform
        val nosePath = Path().apply {
            moveTo(w * 0.46f, h * 0.58f)
            lineTo(w * 0.54f, h * 0.58f)
            lineTo(w * 0.50f, h * 0.63f)
            close()
        }
        drawPath(nosePath, color = MeemoPalette.BrandAccent)

        if (visualState == MascotVisualState.SPEAKING) {
            val mouthHeight = (h * 0.04f) + (h * 0.04f * effectivePulse)
            drawRoundRect(
                color = MeemoPalette.DarkTextPrimary,
                topLeft = Offset(w * 0.43f, h * 0.66f),
                size = Size(w * 0.14f, mouthHeight),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
        } else {
            drawLine(
                color = MeemoPalette.DarkTextPrimary,
                start = Offset(w * 0.42f, h * 0.68f),
                end = Offset(w * 0.58f, h * 0.68f),
                strokeWidth = w * 0.03f,
                cap = StrokeCap.Round
            )
        }

        // 6. Thinking Dots when in THINKING state
        if (visualState == MascotVisualState.THINKING) {
            drawCircle(
                color = MeemoPalette.BrandAccent,
                radius = w * 0.03f,
                center = Offset(w * 0.40f, h * 0.10f)
            )
            drawCircle(
                color = MeemoPalette.BrandAccent,
                radius = w * 0.038f,
                center = Offset(w * 0.50f, h * 0.07f)
            )
            drawCircle(
                color = MeemoPalette.BrandAccent,
                radius = w * 0.03f,
                center = Offset(w * 0.60f, h * 0.10f)
            )
        }
    }
}
