package uk.co.m66ike.mathsdash

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val background = Color(0xFF0B1220)
private val panel = Color(0xFF17243B)
private val tileColor = Color(0xFF23344F)
private val accent = Color(0xFF65C7FF)
private val positive = Color(0xFF92DF91)
private val negative = Color(0xFFFF787F)
private val muted = Color(0xFFAFC0D6)

private enum class Screen { HOME, GAME, RESULT, LEADERBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = accent,
                    onPrimary = background,
                    background = background,
                    onBackground = Color.White,
                    surface = panel,
                    onSurface = Color.White,
                    surfaceVariant = tileColor,
                    onSurfaceVariant = muted,
                    error = negative
                )
            ) {
                MathsDashApp()
            }
        }
    }
}

@Composable
private fun MathsDashApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { ScoreStore(context) }
    val engine = remember { GameEngine() }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var settings by remember { mutableStateOf(store.loadSettings()) }
    var runSettings by remember { mutableStateOf(settings) }
    var leaderboardSettings by remember { mutableStateOf(settings) }
    var scores by remember { mutableStateOf(store.load()) }
    var onlyMatching by remember { mutableStateOf(false) }
    var puzzle by remember { mutableStateOf<Puzzle?>(null) }
    var points by remember { mutableIntStateOf(0) }
    var attempted by remember { mutableIntStateOf(0) }
    var revealedIndex by remember { mutableIntStateOf(-1) }
    var endTime by remember { mutableLongStateOf(0L) }
    var remainingMillis by remember { mutableLongStateOf(0L) }
    var playerName by remember { mutableStateOf(store.lastName()) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(settings) { store.saveSettings(settings) }

    fun startGame(config: GameSettings = settings) {
        if (!config.isValid()) return
        runSettings = config
        points = 0
        attempted = 0
        revealedIndex = -1
        saved = false
        puzzle = engine.next(config)
        remainingMillis = config.timeSeconds.toLong() * 1000
        endTime = SystemClock.elapsedRealtime() + remainingMillis
        screen = Screen.GAME
    }

    LaunchedEffect(screen, endTime) {
        if (screen == Screen.GAME && runSettings.timeSeconds > 0) {
            while (screen == Screen.GAME) {
                remainingMillis = (endTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                if (remainingMillis == 0L) {
                    screen = Screen.RESULT
                    break
                }
                delay(75)
            }
        }
    }

    LaunchedEffect(screen, revealedIndex) {
        if (screen == Screen.GAME && revealedIndex >= 0) {
            delay(850)
            if (screen == Screen.GAME &&
                (runSettings.timeSeconds == 0 || SystemClock.elapsedRealtime() < endTime)
            ) {
                puzzle = engine.next(runSettings)
                revealedIndex = -1
            }
        }
    }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = when (screen) {
            Screen.GAME -> Screen.RESULT
            Screen.RESULT, Screen.LEADERBOARD -> Screen.HOME
            Screen.HOME -> Screen.HOME
        }
    }

    Surface(color = background, modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                settings = settings,
                onSettingsChange = { settings = it },
                onStart = { startGame() },
                onLeaderboard = {
                    leaderboardSettings = settings
                    onlyMatching = false
                    scores = store.load()
                    screen = Screen.LEADERBOARD
                }
            )
            Screen.GAME -> puzzle?.let { activePuzzle ->
                GameScreen(
                    settings = runSettings,
                    puzzle = activePuzzle,
                    points = points,
                    attempted = attempted,
                    remainingMillis = remainingMillis,
                    revealedIndex = revealedIndex,
                    onNumberTap = { index ->
                        if (revealedIndex == -1) {
                            revealedIndex = index
                            attempted++
                            if (index == activePuzzle.correctIndex) points++
                        }
                    },
                    onEnd = { screen = Screen.RESULT }
                )
            }
            Screen.RESULT -> ResultScreen(
                settings = runSettings,
                points = points,
                attempted = attempted,
                playerName = playerName,
                onNameChange = { playerName = it.take(20) },
                saved = saved,
                onSave = {
                    val name = playerName.trim()
                    if (name.isNotEmpty() && !saved && runSettings.timeSeconds > 0) {
                        scores = store.add(
                            ScoreEntry(
                                name = name,
                                points = points,
                                settingsKey = runSettings.key(),
                                settingsDescription = runSettings.summary(),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        store.saveLastName(name)
                        playerName = name
                        saved = true
                    }
                },
                onLeaderboard = {
                    scores = store.load()
                    leaderboardSettings = runSettings
                    onlyMatching = false
                    screen = Screen.LEADERBOARD
                },
                onPlayAgain = { startGame(runSettings) },
                onHome = { screen = Screen.HOME }
            )
            Screen.LEADERBOARD -> LeaderboardScreen(
                entries = scores,
                matchingSettings = leaderboardSettings,
                onlyMatching = onlyMatching,
                onToggleMatching = { onlyMatching = it },
                onBack = { screen = Screen.HOME }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    onStart: () -> Unit,
    onLeaderboard: () -> Unit
) {
    var customMax by remember { mutableStateOf(false) }
    var customMaxText by remember { mutableStateOf(settings.maxNumber.toString()) }
    var customTime by remember { mutableStateOf(settings.timeSeconds !in setOf(0, 30, 60, 90, 120)) }
    var customTimeText by remember { mutableStateOf("180") }

    Column(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MATHS DASH", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("Find it. Tap it. Beat your best!", color = muted)
            }
            OutlinedButton(onClick = onLeaderboard) { Text("🏆 Scores") }
        }

        SectionCard("1  Pick your maths") {
            Operation.values().toList().chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    row.forEach { operation ->
                        val selected = settings.operation == operation
                        Surface(
                            modifier = Modifier.weight(1f).height(90.dp)
                                .clickable { onSettingsChange(settings.copy(operation = operation)) },
                            color = if (selected) Color(0xFF174968) else tileColor,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else Color(0xFF40516A))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(operation.symbol, color = if (selected) accent else Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                Text(operation.displayName, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }

        SectionCard("2  Numbers on screen") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Number tiles", modifier = Modifier.weight(1f), color = muted)
                Text("${settings.tileCount}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accent)
            }
            Slider(
                value = settings.tileCount.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(tileCount = it.roundToInt())) },
                valueRange = 4f..12f,
                steps = 7
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("4  Easy", color = muted, fontSize = 12.sp)
                Text("12  Tricky", color = muted, fontSize = 12.sp)
            }
        }

        if (!settings.isTablesMode) {
            SectionCard("3  Highest number") {
                Text("All displayed numbers stay within this range.", color = muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 20, 30).forEach { n ->
                        FilterChip(
                            selected = !customMax && settings.maxNumber == n,
                            onClick = {
                                customMax = false
                                onSettingsChange(settings.copy(maxNumber = n))
                            },
                            label = { Text(n.toString()) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50, 100).forEach { n ->
                        FilterChip(
                            selected = !customMax && settings.maxNumber == n,
                            onClick = {
                                customMax = false
                                onSettingsChange(settings.copy(maxNumber = n))
                            },
                            label = { Text(n.toString()) }
                        )
                    }
                    FilterChip(
                        selected = customMax,
                        onClick = {
                            customMax = true
                            customMaxText = settings.maxNumber.toString()
                        },
                        label = { Text("Custom") }
                    )
                }
                if (customMax) {
                    OutlinedTextField(
                        value = customMaxText,
                        onValueChange = {
                            customMaxText = it.filter(Char::isDigit).take(3)
                            onSettingsChange(settings.copy(maxNumber = customMaxText.toIntOrNull() ?: 0))
                        },
                        label = { Text("Highest number (up to 999)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (settings.maxNumber < settings.tileCount * 2) {
                    Text(
                        "For ${settings.tileCount} tiles, choose a maximum of at least ${settings.tileCount * 2}.",
                        color = negative, fontSize = 13.sp
                    )
                }
            }
        } else {
            SectionCard("3  Times tables") {
                Text("Choose one or more tables to practise.", color = muted, fontSize = 13.sp)
                (1..12).toList().chunked(4).forEach { tableRow ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tableRow.forEach { table ->
                            val selected = table in settings.selectedTables
                            Surface(
                                modifier = Modifier.weight(1f).height(49.dp).clickable {
                                    val next = if (selected) settings.selectedTables - table else settings.selectedTables + table
                                    onSettingsChange(settings.copy(selectedTables = next))
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) Color(0xFF174968) else tileColor,
                                border = BorderStroke(1.dp, if (selected) accent else Color(0xFF40516A))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${table}×", fontWeight = FontWeight.Bold, color = if (selected) accent else Color.White)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Practise up to", color = muted, modifier = Modifier.weight(1f))
                    Text("×${settings.multiplierLimit}", color = accent, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.multiplierLimit.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(multiplierLimit = it.roundToInt())) },
                    valueRange = 1f..12f,
                    steps = 10
                )
                if (settings.selectedTables.isEmpty()) {
                    Text("Select at least one times table.", color = negative, fontSize = 13.sp)
                } else {
                    Text(
                        "${settings.selectedTables.sorted().joinToString(", ")} times tables, up to ×${settings.multiplierLimit}",
                        color = muted, fontSize = 13.sp
                    )
                }
            }
        }

        SectionCard("4  Time limit") {
            Text("Practice mode has no timer.", color = muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 30, 60).forEach { s ->
                    FilterChip(
                        selected = !customTime && settings.timeSeconds == s,
                        onClick = {
                            customTime = false
                            onSettingsChange(settings.copy(timeSeconds = s))
                        },
                        label = { Text(if (s == 0) "Practice" else "${s}s") }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(90, 120).forEach { s ->
                    FilterChip(
                        selected = !customTime && settings.timeSeconds == s,
                        onClick = {
                            customTime = false
                            onSettingsChange(settings.copy(timeSeconds = s))
                        },
                        label = { Text(if (s == 120) "2 mins" else "90s") }
                    )
                }
                FilterChip(
                    selected = customTime,
                    onClick = {
                        customTime = true
                        onSettingsChange(settings.copy(timeSeconds = customTimeText.toIntOrNull() ?: 180))
                    },
                    label = { Text("Custom") }
                )
            }
            if (customTime) {
                OutlinedTextField(
                    value = customTimeText,
                    onValueChange = {
                        customTimeText = it.filter(Char::isDigit).take(3)
                        onSettingsChange(settings.copy(timeSeconds = customTimeText.toIntOrNull() ?: -1))
                    },
                    label = { Text("Seconds (10–600)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (settings.timeSeconds !in 10..600) {
                    Text("Enter between 10 and 600 seconds.", color = negative, fontSize = 13.sp)
                }
            }
        }

        Button(
            onClick = onStart,
            enabled = settings.isValid(),
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(if (settings.timeSeconds == 0) "START PRACTICE" else "LET'S PLAY!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = panel)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            content()
        }
    }
}

@Composable
private fun GameScreen(
    settings: GameSettings,
    puzzle: Puzzle,
    points: Int,
    attempted: Int,
    remainingMillis: Long,
    revealedIndex: Int,
    onNumberTap: (Int) -> Unit,
    onEnd: () -> Unit
) {
    val reveal = revealedIndex >= 0
    val correct = reveal && revealedIndex == puzzle.correctIndex

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(settings.operation.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Score  $points", color = positive, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(15.dp))
        if (settings.timeSeconds > 0) {
            val seconds = (remainingMillis + 999) / 1000
            Text(
                "⏱ ${seconds}s",
                color = if (seconds <= 10) negative else accent,
                fontSize = 35.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (remainingMillis.toFloat() / (settings.timeSeconds * 1000)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = accent,
                trackColor = tileColor
            )
        } else {
            Text("PRACTICE MODE", color = accent, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(26.dp))
        Text(
            puzzle.instruction,
            fontSize = if (puzzle.usesEquation) 33.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("Tap the correct number", color = muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        val columns = if (puzzle.numbers.size <= 4) 2 else 3
        puzzle.numbers.indices.toList().chunked(columns).forEach { rowIndices ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowIndices.forEach { index ->
                    val isCorrect = reveal && index == puzzle.correctIndex
                    val isWrong = reveal && index == revealedIndex && index != puzzle.correctIndex
                    val color = when {
                        isCorrect -> Color(0xFF236C4F)
                        isWrong -> Color(0xFF8D303B)
                        else -> tileColor
                    }
                    val borderColor = when {
                        isCorrect -> positive
                        isWrong -> negative
                        else -> Color(0xFF3B526D)
                    }
                    Surface(
                        modifier = Modifier.weight(1f).height(if (columns == 2) 114.dp else 82.dp)
                            .clickable(enabled = !reveal) { onNumberTap(index) },
                        color = color,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(2.dp, borderColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                puzzle.numbers[index].toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = if (puzzle.numbers[index] > 99) 28.sp else 34.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                repeat(columns - rowIndices.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(14.dp))
        if (reveal) {
            Text(
                if (correct) "✓ Brilliant!" else "Not quite — the green tile is correct",
                color = if (correct) positive else negative,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(puzzle.explanation, color = muted, fontSize = 19.sp)
        } else {
            Text("$attempted questions answered", color = muted, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onEnd) {
            Text(if (settings.timeSeconds == 0) "Finish practice" else "End game", color = muted)
        }
    }
}

@Composable
private fun ResultScreen(
    settings: GameSettings,
    points: Int,
    attempted: Int,
    playerName: String,
    onNameChange: (String) -> Unit,
    saved: Boolean,
    onSave: () -> Unit,
    onLeaderboard: () -> Unit,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(25.dp))
        Text(if (settings.timeSeconds == 0) "Practice finished!" else "Time's up!", fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("⭐", fontSize = 55.sp)
        Text("$points", color = accent, fontSize = 80.sp, fontWeight = FontWeight.Black)
        Text("correct answers out of $attempted", color = muted)
        SectionCard("Your game") {
            Text(settings.summary(), color = muted)
        }

        if (settings.timeSeconds > 0) {
            SectionCard("Save your score") {
                if (saved) {
                    Text("✓ Saved to the leaderboard!", color = positive, fontWeight = FontWeight.Bold)
                    Text("Player: ${playerName.trim()}", color = muted)
                } else {
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = onNameChange,
                        label = { Text("Your name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = onSave,
                        enabled = playerName.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("SAVE SCORE") }
                }
            }
        } else {
            Text("Practice runs aren't added to the leaderboard.", color = muted, textAlign = TextAlign.Center)
        }
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("PLAY AGAIN") }
        OutlinedButton(onClick = onLeaderboard, modifier = Modifier.fillMaxWidth()) { Text("VIEW LEADERBOARD") }
        TextButton(onClick = onHome) { Text("Back to settings") }
    }
}

@Composable
private fun LeaderboardScreen(
    entries: List<ScoreEntry>,
    matchingSettings: GameSettings,
    onlyMatching: Boolean,
    onToggleMatching: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val displayed = entries
        .filter { !onlyMatching || it.settingsKey == matchingSettings.key() }
        .sortedWith(compareByDescending<ScoreEntry> { it.points }.thenBy { it.timestamp })

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏆 Leaderboard", fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Done") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !onlyMatching, onClick = { onToggleMatching(false) }, label = { Text("All scores") })
            FilterChip(selected = onlyMatching, onClick = { onToggleMatching(true) }, label = { Text("Same settings") })
        }
        if (onlyMatching) {
            Text(matchingSettings.summary(), color = muted, fontSize = 13.sp)
        } else {
            Text("All players and game types — highest scores first.", color = muted, fontSize = 13.sp)
        }

        if (displayed.isEmpty()) {
            SectionCard("No scores yet") {
                Text("Play a timed game and save your name to get started!", color = muted)
            }
        } else {
            displayed.take(250).forEachIndexed { index, entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = panel),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${index + 1}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent, modifier = Modifier.width(47.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(entry.settingsDescription, fontSize = 12.sp, color = muted)
                            Text(
                                SimpleDateFormat("dd MMM yyyy", Locale.UK).format(Date(entry.timestamp)),
                                color = muted, fontSize = 11.sp
                            )
                        }
                        Text(entry.points.toString(), color = positive, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
