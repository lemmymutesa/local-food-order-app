package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderStatus
import com.example.ui.theme.OrangePrimary

@Composable
fun DeliveryMapVisual(
    status: OrderStatus,
    restaurantName: String,
    deliveryStreet: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE5ECE9))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val gridColor = Color(0xFFD4DDD7)
            val strokeWidth = 8f

            drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = strokeWidth)
            drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = strokeWidth * 1.5f)
            drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = strokeWidth)

            drawLine(gridColor, Offset(w * 0.2f, 0f), Offset(w * 0.2f, h), strokeWidth = strokeWidth)
            drawLine(gridColor, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = strokeWidth * 1.5f)
            drawLine(gridColor, Offset(w * 0.8f, 0f), Offset(w * 0.8f, h), strokeWidth = strokeWidth)

            val startX = w * 0.18f
            val startY = h * 0.28f

            val endX = w * 0.82f
            val endY = h * 0.72f

            val p1X = w * 0.48f
            val p1Y = h * 0.28f

            val p2X = w * 0.48f
            val p2Y = h * 0.72f

            val routePath = Path().apply {
                moveTo(startX, startY)
                lineTo(p1X, p1Y)
                lineTo(p2X, p2Y)
                lineTo(endX, endY)
            }

            drawPath(
                path = routePath,
                color = OrangePrimary.copy(alpha = 0.3f),
                style = Stroke(width = 10f)
            )
            drawPath(
                path = routePath,
                color = OrangePrimary,
                style = Stroke(
                    width = 5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f), 0f)
                )
            )

            drawCircle(
                color = Color.White,
                radius = 16f,
                center = Offset(startX, startY)
            )
            drawCircle(
                color = Color(0xFFD84315),
                radius = 12f,
                center = Offset(startX, startY)
            )

            drawCircle(
                color = Color(0xFF2E7D32).copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = Offset(endX, endY)
            )
            drawCircle(
                color = Color.White,
                radius = 18f,
                center = Offset(endX, endY)
            )
            drawCircle(
                color = Color(0xFF2E7D32),
                radius = 13f,
                center = Offset(endX, endY)
            )

            val t = status.progress
            val driverPos = when {
                t <= 0.33f -> {
                    val subT = t / 0.33f
                    Offset(startX + (p1X - startX) * subT, startY)
                }
                t <= 0.66f -> {
                    val subT = (t - 0.33f) / 0.33f
                    Offset(p1X, p1Y + (p2Y - p1Y) * subT)
                }
                else -> {
                    val subT = (t - 0.66f) / 0.34f
                    Offset(p2X + (endX - p2X) * subT, p2Y)
                }
            }

            drawCircle(
                color = Color(0xFFFF9800).copy(alpha = 0.4f),
                radius = 22f,
                center = driverPos
            )
            drawCircle(
                color = Color.Black,
                radius = 16f,
                center = driverPos
            )
            drawCircle(
                color = Color(0xFFFFB300),
                radius = 10f,
                center = driverPos
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "📍 $restaurantName",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Text(
                text = "🏠 $deliveryStreet",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
