package uk.co.m66ike.mathsdash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ColourChoice(val name: String, val color: Color)

val backgroundChoices = listOf(
    ColourChoice("Midnight", Color(0xFF0B1220)),
    ColourChoice("Charcoal", Color(0xFF242832)),
    ColourChoice("Forest", Color(0xFF14362E)),
    ColourChoice("Plum", Color(0xFF382544)),
    ColourChoice("Ocean", Color(0xFF234565)),
    ColourChoice("Cream", Color(0xFFFFF1D8)),
    ColourChoice("Ice", Color(0xFFE4F5FF)),
    ColourChoice("Peach", Color(0xFFFFE5D7))
)

val buttonChoices = listOf(
    ColourChoice("Sky", Color(0xFF65C7FF)),
    ColourChoice("Blue", Color(0xFF548CFF)),
    ColourChoice("Mint", Color(0xFF81E6C4)),
    ColourChoice("Lime", Color(0xFFBDEA70)),
    ColourChoice("Orange", Color(0xFFFFAD59)),
    ColourChoice("Pink", Color(0xFFFF95C3)),
    ColourChoice("Purple", Color(0xFFB9A0FF)),
    ColourChoice("Red", Color(0xFFFA777C))
)

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
    val bg = backgroundChoices[appearance.backgroundIndex.coerceIn(0, backgroundChoices.lastIndex)].color
    val button = buttonChoices[appearance.buttonIndex.coerceIn(0, buttonChoices.lastIndex)].color
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
    val preview = remember(draft) { paletteFor(draft) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your colours", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text("Background", fontWeight = FontWeight.SemiBold)
                ColourRows(
                    choices = backgroundChoices,
                    selectedIndex = draft.backgroundIndex,
                    onSelect = { draft = draft.copy(backgroundIndex = it) }
                )
                Text("Number buttons & highlights", fontWeight = FontWeight.SemiBold)
                ColourRows(
                    choices = buttonChoices,
                    selectedIndex = draft.buttonIndex,
                    onSelect = { draft = draft.copy(buttonIndex = it) }
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
                Text("Text changes automatically for readability.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft) }) { Text("Apply theme") }
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
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        choices.indices.toList().chunked(4).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                group.forEach { index ->
                    val choice = choices[index]
                    val selected = index == selectedIndex
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onSelect(index) },
                        color = choice.color,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (selected) 3.dp else 1.dp,
                            if (selected) readableText(choice.color) else readableText(choice.color).copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.height(56.dp).padding(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                if (selected) "✓" else "●",
                                color = readableText(choice.color),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                choice.name,
                                color = readableText(choice.color),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
