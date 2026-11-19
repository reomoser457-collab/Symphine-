package com.symphine.gamespace.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.symphine.gamespace.backup.BackupValidationResult
import com.symphine.gamespace.backup.SymphineBackupSchema
import com.symphine.gamespace.core.model.OperationPreview
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.RiskLevel
import com.symphine.gamespace.ui.theme.JetBrainsMonoFamily
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import com.symphine.gamespace.ui.theme.SymphineDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSymphineColors.current
    val radius = cornerRadius ?: colors.cornerRadius
    val shape = RoundedCornerShape(radius)
    val effectiveAccent = accentColor ?: colors.purpleAccent

    val glassBrush = Brush.linearGradient(
        colors = listOf(
            colors.surfaceElevated.copy(alpha = (colors.glassOpacity + 0.08f).coerceAtMost(0.96f)),
            colors.surface.copy(alpha = colors.glassOpacity),
            effectiveAccent.copy(alpha = if (colors.glassGlow) 0.08f else 0.02f)
        )
    )
    val borderColor = if (colors.glassGlow) {
        effectiveAccent.copy(alpha = 0.28f)
    } else {
        colors.border
    }

    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassBrush, shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .then(clickableModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = LocalSymphineColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.purpleAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.purpleAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.softSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun StatusBadge(
    status: OperationStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (status) {
        OperationStatus.VERIFIED, OperationStatus.RESTORED, OperationStatus.APPLIED -> Triple(
            SymphineDefaults.StatusEmerald.copy(alpha = 0.15f),
            SymphineDefaults.StatusEmerald,
            Icons.Rounded.CheckCircle
        )
        OperationStatus.AVAILABLE -> Triple(
            SymphineDefaults.StatusSky.copy(alpha = 0.15f),
            SymphineDefaults.StatusSky,
            Icons.Rounded.Shield
        )
        OperationStatus.REQUIRES_SHIZUKU, OperationStatus.REQUIRES_PERMISSION -> Triple(
            SymphineDefaults.StatusAmber.copy(alpha = 0.15f),
            SymphineDefaults.StatusAmber,
            Icons.Rounded.Lock
        )
        OperationStatus.PENDING, OperationStatus.APPLYING, OperationStatus.RESTORING -> Triple(
            SymphineDefaults.PurpleAccent.copy(alpha = 0.16f),
            SymphineDefaults.SecondaryAccent,
            Icons.Rounded.HourglassTop
        )
        OperationStatus.FAILED, OperationStatus.ROLLBACK_FAILED -> Triple(
            SymphineDefaults.StatusRose.copy(alpha = 0.16f),
            SymphineDefaults.StatusRose,
            Icons.Rounded.ErrorOutline
        )
        OperationStatus.UNSUPPORTED, OperationStatus.UNKNOWN -> Triple(
            SymphineDefaults.MutedText.copy(alpha = 0.16f),
            SymphineDefaults.MutedText,
            Icons.Rounded.WarningAmber
        )
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    val colors = LocalSymphineColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedText,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor ?: colors.softSilver,
            modifier = Modifier.weight(0.55f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TelemetryMeterBar(
    label: String,
    valueText: String,
    progress: Float,
    accent: Color? = null
) {
    val colors = LocalSymphineColors.current
    val effectiveAccent = accent ?: colors.purpleAccent
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = colors.softSilver)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape),
            color = effectiveAccent,
            trackColor = colors.border
        )
    }
}

@Composable
fun LiveTelemetrySparkline(
    values: List<Float>,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalSymphineColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.background.copy(alpha = 0.65f))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        if (values.size < 2) {
            Text(
                text = "Start monitoring to capture live telemetry points...",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            ) {
                val range = (maxValue - minValue).coerceAtLeast(1f)
                val stepX = size.width / (values.size - 1).coerceAtLeast(1)
                val path = Path()
                val fillPath = Path()

                values.forEachIndexed { idx, v ->
                    val norm = ((v - minValue) / range).coerceIn(0f, 1f)
                    val x = idx * stepX
                    val y = size.height - (norm * size.height * 0.88f) - size.height * 0.06f
                    if (idx == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    if (idx == values.lastIndex) {
                        fillPath.lineTo(x, size.height)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val lastNorm = ((values.last() - minValue) / range).coerceIn(0f, 1f)
                val lastY = size.height - (lastNorm * size.height * 0.88f) - size.height * 0.06f
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width, lastY)
                )
            }
        }
    }
}

@Composable
fun InstalledAppIcon(
    packageName: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val context = LocalContext.current
    val colors = LocalSymphineColors.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawableToBitmap(drawable).asImageBitmap()
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.purpleAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = contentDescription,
                tint = colors.purpleAccent,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val w = drawable.intrinsicWidth.coerceIn(64, 192)
    val h = drawable.intrinsicHeight.coerceIn(64, 192)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
}

@Composable
fun OperationVerificationDialog(
    preview: OperationPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalSymphineColors.current
    val canExecute = preview.compatibilityStatus == OperationStatus.AVAILABLE ||
        preview.compatibilityStatus == OperationStatus.VERIFIED

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        titleContentColor = colors.softSilver,
        textContentColor = colors.softSilver,
        title = {
            Column {
                Text(preview.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(preview.compatibilityStatus)
                    Surface(
                        shape = CircleShape,
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = preview.riskLevel.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.lavenderGray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(preview.description, style = MaterialTheme.typography.bodyMedium, color = colors.lavenderGray)

                if (!preview.warning.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SymphineDefaults.StatusAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SymphineDefaults.StatusAmber.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = preview.warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = SymphineDefaults.StatusAmber,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Text(
                    "Changes & Snapshot Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.softSilver
                )
                preview.changes.forEach { ch ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.background.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            Text(
                                "${ch.namespace}/${ch.key}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.purpleAccent,
                                fontFamily = JetBrainsMonoFamily
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Current: ${ch.currentValue ?: "null"}  →  Target: ${ch.targetValue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.softSilver
                            )
                        }
                    }
                }

                Text(
                    "Rollback: ${preview.restoreInstructions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canExecute,
                modifier = Modifier.testTag("confirm_operation_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.purpleAccent,
                    contentColor = Color(0xFF08090D)
                )
            ) {
                Text(if (canExecute) "Apply & Verify" else preview.compatibilityStatus.label, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_operation_button")
            ) {
                Text("Cancel", color = colors.lavenderGray)
            }
        }
    )
}

@Composable
fun BackupValidationDialog(
    validation: BackupValidationResult,
    onConfirmRestore: (SymphineBackupSchema, Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalSymphineColors.current
    var restoreUi by remember { mutableStateOf(true) }
    var restoreLibrary by remember { mutableStateOf(true) }
    var restoreProfiles by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        title = {
            Text(
                if (validation.isValid) "Validate & Preview Backup" else "Invalid Backup File",
                style = MaterialTheme.typography.titleLarge,
                color = colors.softSilver
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!validation.isValid || validation.schema == null) {
                    Text(
                        validation.errorMessage ?: "Unknown validation error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SymphineDefaults.StatusRose
                    )
                } else {
                    val s = validation.schema
                    KeyValueRow("App Version", s.appVersion)
                    KeyValueRow("Source Device", "${s.deviceManufacturer} ${s.deviceModel}")
                    KeyValueRow("Games Customized", "${s.gameCustomizations.size}")
                    KeyValueRow("Custom Profiles", "${s.customProfiles.size}")

                    validation.compatibilityNotes.forEach { note ->
                        Text(note, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                    }
                    if (validation.conflicts.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SymphineDefaults.StatusAmber.copy(alpha = 0.12f)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Conflicts (${validation.conflicts.size})", style = MaterialTheme.typography.labelMedium, color = SymphineDefaults.StatusAmber)
                                validation.conflicts.take(4).forEach {
                                    Text("• $it", style = MaterialTheme.typography.bodySmall, color = colors.softSilver)
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreLibrary, onCheckedChange = { restoreLibrary = it })
                        Text("Restore Game Library & Customizations", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreProfiles, onCheckedChange = { restoreProfiles = it })
                        Text("Restore Profiles & Presets", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreUi, onCheckedChange = { restoreUi = it })
                        Text("Restore UI Appearance & Theme", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (validation.isValid && validation.schema != null) {
                Button(
                    onClick = { onConfirmRestore(validation.schema, restoreUi, restoreLibrary, restoreProfiles) },
                    modifier = Modifier.testTag("confirm_backup_restore_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                ) {
                    Text("Restore Selected")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
