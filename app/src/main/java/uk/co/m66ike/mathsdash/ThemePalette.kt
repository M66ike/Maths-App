package uk.co.m66ike.mathsdash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class ColourChoice(val name: String, val color: Color)

// The first eight are the original themes, so saved selections keep working.
private val originalBackgrounds = listOf(
    ColourChoice("Midnight", Color(0xFF0B1220)),
    ColourChoice("Charcoal", Color(0xFF242832)),
    ColourChoice("Forest", Color(0xFF14362E)),
    ColourChoice("Plum", Color(0xFF382544)),
    ColourChoice("Ocean", Color(0xFF234565)),
    ColourChoice("Cream", Color(0xFFFFF1D8)),
    ColourChoice("Ice", Color(0xFFE4F5FF)),
    ColourChoice("Peach", Color(0xFFFFE5D7))
)

private val originalButtons = listOf(
    ColourChoice("Sky", Color(0xFF65C7FF)),
    ColourChoice("Soft blue", Color(0xFF548CFF)),
    ColourChoice("Mint", Color(0xFF81E6C4)),
    ColourChoice("Lime", Color(0xFFBDEA70)),
    ColourChoice("Orange", Color(0xFFFFAD59)),
    ColourChoice("Pink", Color(0xFFFF95C3)),
    ColourChoice("Purple", Color(0xFFB9A0FF)),
    ColourChoice("Soft red", Color(0xFFFA777C))
)

// Familiar bright and standard-office colours, available for either surface.
private val standardColours = listOf(
    ColourChoice("White", Color(0xFFFFFFFF)),
    ColourChoice("Black", Color(0xFF000000)),
    ColourChoice("Silver", Color(0xFFC0C0C0)),
    ColourChoice("Grey", Color(0xFF808080)),
    ColourChoice("Dark red", Color(0xFFC00000)),
    ColourChoice("Red", Color(0xFFFF0000)),
    ColourChoice("Coral", Color(0xFFFF6B55)),
    ColourChoice("Gold", Color(0xFFFFC000)),
    ColourChoice("Yellow", Color(0xFFFFFF00)),
    ColourChoice("Lt green", Color(0xFF92D050)),
    ColourChoice("Green", Color(0xFF00B050)),
    ColourChoice("Dk green", Color(0xFF00652E)),
    ColourChoice("Teal", Color(0xFF008080)),
    ColourChoice("Cyan", Color(0xFF00FFFF)),
    ColourChoice("Lt blue", Color(0xFF00B0F0)),
    ColourChoice("Blue", Color(0xFF0070C0)),
    ColourChoice("Navy", Color(0xFF002060)),
    ColourChoice("Violet", Color(0xFF7030A0)),
    ColourChoice("Magenta", Color(0xFFFF00FF)),
    ColourChoice("Brown", Color(0xFF804000))
)

val backgroundChoices = originalBackgrounds + standardColours
val buttonChoices = originalButtons + standardColours

fun readableText(colour: Color): Color =
    if (colour.luminance() > 0.179f) Color.Black else Color.White

data class GamePalette(
    val background: Color,
    val panel: Color,
    val tile: Color,
    val button: Color,
    val selected: Color,
    val border: Color,
    val textOnBackground: Color,
    val textOnPanel: Color,
    val textOnButton: Color,
    val muted: Color,
    val positive: Color,
    val negative: Color,
    val light: Boolean
)

fun paletteFor(appearance: Appearance): GamePalette {
    val bg = appearance.customBackgroundArgb?.let { Color(it.toInt()) }
        ?: backgroundChoices[appearance.backgroundIndex.coerceIn(0, backgroundChoices.lastIndex)].color
    val button = appearance.customButtonArgb?.let { Color(it.toInt()) }
        ?: buttonChoices[appearance.buttonIndex.coerceIn(0, buttonChoices.lastIndex)].color
    val text = readableText(bg)
    val light = text == Color.Black
    val surface = lerp(bg, text, if (light) 0.045f else 0.085f)
    val tile = lerp(bg, text, if (light) 0.105f else 0.155f)
    val selected = lerp(button, bg, if (light) 0.65f else 0.52f)
    return GamePalette(
        background = bg,
        panel = surface,
        tile = tile,
        button = button,
        selected = selected,
        border = lerp(tile, readableText(tile), 0.27f),
        textOnBackground = text,
        textOnPanel = readableText(surface),
        textOnButton = readableText(button),
        muted = if (light) Color(0xFF526174) else Color(0xFFB4C3D9),
        positive = if (light) Color(0xFF13673B) else Color(0xFF92DF91),
        negative = if (light) Color(0xFFB42336) else Color(0xFFFF787F),
        light = light
    )
}

val LocalMathPalette = staticCompositionLocalOf { paletteFor(Appearance()) }

@Composable
fun ThemePicker(
    current: Appearance,
    onDismiss: () -> Unit,
    onApply: (Appearance) -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }
    var editButtons by remember { mutableStateOf(false) }
    val preview = remember(draft) { paletteFor(draft) }
    val choices = if (editButtons) buttonChoices else backgroundChoices
    val chosenIndex = if (editButtons) draft.buttonIndex else draft.backgroundIndex
    val customArgb = if (editButtons) draft.customButtonArgb else draft.customBackgroundArgb
    val selectedColour = if (editButtons) preview.button else preview.background

    fun selectColour(colour: Color) {
        draft = if (editButtons) {
            draft.copy(customButtonArgb = colour.toArgb().toLong())
        } else {
            draft.copy(customBackgroundArgb = colour.toArgb().toLong())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your colours", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    FilterChip(
                        selected = !editButtons,
                        onClick = { editButtons = false },
                        label = { Text("Background") }
                    )
                    FilterChip(
                        selected = editButtons,
                        onClick = { editButtons = true },
                        label = { Text("Buttons") }
                    )
                }
                Text("Choose a preset", fontWeight = FontWeight.SemiBold)
                ColourRows(
                    choices = choices,
                    selectedIndex = if (customArgb == null) chosenIndex else -1,
                    onSelect = { index ->
                        draft = if (editButtons) {
                            draft.copy(buttonIndex = index, customButtonArgb = null)
                        } else {
                            draft.copy(backgroundIndex = index, customBackgroundArgb = null)
                        }
                    }
                )
                HorizontalDivider()
                Text("Or choose any colour", fontWeight = FontWeight.SemiBold)
                Text("Tap or drag on the wheel; adjust brightness below.", style = MaterialTheme.typography.bodySmall)
                ColourWheel(
                    colour = selectedColour,
                    onColourChange = ::selectColour
                )
                Surface(color = preview.background, shape = RoundedCornerShape(14.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Preview", color = preview.textOnBackground, fontWeight = FontWeight.Bold)
                        Surface(
                            color = preview.button,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.width(130.dp).height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("12", color = preview.textOnButton, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text("Text switches between white and black automatically.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ColourRows(
    choices: List<ColourChoice>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.indices.toList().chunked(5).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                group.forEach { index ->
                    val choice = choices[index]
                    val selected = index == selectedIndex
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onSelect(index) },
                        color = choice.color,
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(
                            if (selected) 3.dp else 1.dp,
                            if (selected) readableText(choice.color) else readableText(choice.color).copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.height(46.dp).padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(if (selected) "✓" else "●", color = readableText(choice.color), fontSize = 14.sp)
                            Text(
                                choice.name,
                                color = readableText(choice.color),
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
                repeat(5 - group.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ColourWheel(
    colour: Color,
    onColourChange: (Color) -> Unit
) {
    val hsv = remember(colour) { FloatArray(3).also { android.graphics.Color.colorToHSV(colour.toArgb(), it) } }
    val latestColour by rememberUpdatedState(colour)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier.size(188.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val value = FloatArray(3).also {
                            android.graphics.Color.colorToHSV(latestColour.toArgb(), it)
                        }[2]
                        onColourChange(wheelColour(offset, size, value))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val value = FloatArray(3).also {
                                android.graphics.Color.colorToHSV(latestColour.toArgb(), it)
                            }[2]
                            onColourChange(wheelColour(offset, size, value))
                        },
                        onDrag = { change, _ ->
                            val value = FloatArray(3).also {
                                android.graphics.Color.colorToHSV(latestColour.toArgb(), it)
                            }[2]
                            onColourChange(wheelColour(change.position, size, value))
                            change.consume()
                        }
                    )
                }
        ) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                ),
                radius = radius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    radius = radius
                ),
                radius = radius
            )
            val x = center.x + radius * hsv[1] * cos(Math.toRadians(hsv[0].toDouble())).toFloat()
            val y = center.y + radius * hsv[1] * sin(Math.toRadians(hsv[0].toDouble())).toFloat()
            drawCircle(color = Color.Black, radius = 11f, center = Offset(x, y))
            drawCircle(color = Color.White, radius = 7.5f, center = Offset(x, y))
        }
        Spacer(Modifier.height(5.dp))
        Text("Brightness", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = hsv[2],
            onValueChange = { brightness ->
                onColourChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], brightness))))
            },
            valueRange = 0.05f..1f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            String.format("#%06X", colour.toArgb() and 0xFFFFFF),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun wheelColour(position: Offset, size: IntSize, brightness: Float): Color {
    val dx = position.x - size.width / 2f
    val dy = position.y - size.height / 2f
    val radius = minOf(size.width, size.height) / 2f
    val hue = ((Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f)
    val saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
}
