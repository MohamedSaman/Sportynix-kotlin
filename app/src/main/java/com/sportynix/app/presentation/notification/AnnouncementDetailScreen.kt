package com.sportynix.app.presentation.notification

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.sportynix.app.domain.model.AnnouncementDetailPayload
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val DetailGreen = Color(0xFF16A05D)

@Composable
fun AnnouncementDetailScreen(content: AnnouncementDetailPayload, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val fallback = parseColor(content.fallbackBgColor) ?: DetailGreen
    var imageFailed by remember(content.imageUrl) { mutableStateOf(content.imageUrl == null) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(Modifier.fillMaxWidth().height(350.dp)) {
                if (!imageFailed) AsyncImage(
                    model = content.imageUrl, contentDescription = content.title,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    onState = { if (it is AsyncImagePainter.State.Error) imageFailed = true }
                )
                if (imageFailed) Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(fallback, fallback.copy(alpha = .7f)))))
                Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .62f), Color.Transparent))))
            }
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                content.label?.let { Text(it.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = DetailGreen, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(DetailGreen.copy(alpha = .1f)).padding(horizontal = 10.dp, vertical = 5.dp)) }
                Text(content.title, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                content.subtitle?.let { Text(it, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .12f))
                content.shortDescription?.let { Text(it, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold) }
                content.fullDescription?.let { Text(it, fontSize = 15.sp, lineHeight = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                content.publishedAt?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccessTime, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(7.dp)); Text("Published: ${formatPublishedDate(it)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                validHttpUrl(content.readMoreUrl)?.let { uri ->
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = DetailGreen)) {
                        Text(content.readMoreLabel ?: "Read Full Story", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.OpenInNew, null, Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        FilledIconButton(onClick = onNavigateBack, modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 14.dp).size(44.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .42f))) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
    }
}

private fun validHttpUrl(raw: String?): Uri? = runCatching { Uri.parse(raw).takeIf { it.scheme == "http" || it.scheme == "https" } }.getOrNull()
private fun parseColor(raw: String?): Color? = runCatching { raw?.let { Color(android.graphics.Color.parseColor(it)) } }.getOrNull()

internal fun formatPublishedDate(raw: String): String {
    val instant = runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
        ?: return raw
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date.from(instant))
}
