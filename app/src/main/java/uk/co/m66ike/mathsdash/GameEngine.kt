package uk.co.m66ike.mathsdash

import kotlin.random.Random

enum class Operation(val displayName: String, val symbol: String) {
    ADD("Addition", "+"),
    SUBTRACT("Subtraction", "−"),
    MULTIPLY("Multiplication", "×"),
    DIVIDE("Division", "÷")
}

data class GameSettings(
    val operation: Operation = Operation.ADD,
    val tileCount: Int = 6,
    val maxNumber: Int = 30,
    val selectedTables: Set<Int> = setOf(2, 3, 4, 5),
    val multiplierLimit: Int = 12,
    val timeSeconds: Int = 60
) {
    val isTablesMode: Boolean get() = operation == Operation.MULTIPLY || operation == Operation.DIVIDE

    fun isValid(): Boolean = tileCount in 4..12 &&
        (timeSeconds == 0 || timeSeconds in 10..600) &&
        (if (isTablesMode) {
            selectedTables.isNotEmpty() && selectedTables.all { it in 2..12 } && multiplierLimit in 2..12
        } else {
            maxNumber in (tileCount * 2)..999
        })

    fun key(): String = if (isTablesMode) {
        "${operation.name}|$tileCount|${selectedTables.sorted().joinToString(",")}|$multiplierLimit|$timeSeconds"
    } else {
        "${operation.name}|$tileCount|$maxNumber|$timeSeconds"
    }

    fun summary(): String {
        val seconds = if (timeSeconds == 0) "Practice" else "${timeSeconds}s"
        return if (isTablesMode) {
            "${operation.displayName} • ${selectedTables.sorted().joinToString(",")}× to ×$multiplierLimit • $tileCount tiles • $seconds"
        } else {
            "${operation.displayName} • 1–$maxNumber • $tileCount tiles • $seconds"
        }
    }
}

data class Puzzle(
    val numbers: List<Int>,
    val correctIndex: Int,
    val instruction: String,
    val explanation: String,
    val operandA: Int,
    val operandB: Int,
    val usesEquation: Boolean
)

class GameEngine(private val random: Random = Random.Default) {
    fun next(settings: GameSettings): Puzzle {
        require(settings.isValid()) { "Invalid game settings" }
        return when (settings.operation) {
            Operation.ADD -> classic(settings, isProduct = false)
            Operation.MULTIPLY -> multiplicationTiles(settings)
            Operation.SUBTRACT -> subtraction(settings)
            Operation.DIVIDE -> division(settings)
        }
    }

    private fun classic(settings: GameSettings, isProduct: Boolean): Puzzle {
        val maximum = if (isProduct) {
            maxOf(24, (settings.selectedTables.maxOrNull() ?: 1) * settings.multiplierLimit, settings.tileCount + 5)
        } else {
            settings.maxNumber
        }
        val eligibleTables = settings.selectedTables.filter { it >= 2 }
        repeat(3000) {
            val a: Int
            val b: Int
            if (isProduct) {
                if (eligibleTables.isEmpty()) return multiplicationTiles(settings)
                a = eligibleTables.random(random)
                b = random.nextInt(2, settings.multiplierLimit + 1)
            } else {
                a = random.nextInt(1, settings.maxNumber)
                b = random.nextInt(1, settings.maxNumber - a + 1)
            }

            val result = if (isProduct) a * b else a + b
            if (result > maximum || a == b || a == result || b == result) return@repeat
            val seed = listOf(a, b, result)
            val extras = (1..maximum)
                .filter { it !in seed }
                .shuffled(random)
                .take(settings.tileCount - 3)
            if (extras.size != settings.tileCount - 3) return@repeat
            val shuffled = (seed + extras).shuffled(random)
            val validIndices = validAnswerIndices(shuffled, isProduct)
            if (validIndices.size == 1 && shuffled[validIndices.single()] == result) {
                return Puzzle(
                    numbers = shuffled,
                    correctIndex = validIndices.single(),
                    instruction = if (isProduct) "Find the product of two numbers" else "Find the sum of two numbers",
                    explanation = "$a ${if (isProduct) "×" else "+"} $b = $result",
                    operandA = a,
                    operandB = b,
                    usesEquation = false
                )
            }
        }
        // Some dense combinations cannot form a unique Kakooma puzzle.
        // Preserve the exact arithmetic facts with an unambiguous equation instead.
        return if (isProduct) multiplicationTiles(settings) else additionEquation(settings)
    }

    private fun multiplicationTiles(settings: GameSettings): Puzzle {
        val a = settings.selectedTables.random(random)
        val b = random.nextInt(2, settings.multiplierLimit + 1)
        val result = a * b
        // Two equal operands are allowed for square facts such as 2 × 2.
        // There are still exactly two source tiles and one result tile.
        val seed = listOf(a, b, result)
        val maximum = maxOf(36, settings.selectedTables.max() * settings.multiplierLimit, settings.tileCount * 4)
        val extras = mutableListOf<Int>()

        fun tryAdd(candidate: Int) {
            if (candidate in seed || candidate in extras || candidate == 1) return
            val trial = seed + extras + candidate
            if (validAnswerIndices(trial, product = true) == listOf(2)) {
                extras += candidate
            }
        }

        // Prefer distractors near the actual multiplication facts.
        (2..maximum).shuffled(random).forEach { candidate ->
            if (extras.size < settings.tileCount - 3) tryAdd(candidate)
        }
        // If a dense board needs more, larger primes are safe distractors.
        if (extras.size < settings.tileCount - 3) {
            (13..199).filter { n ->
                (2..kotlin.math.sqrt(n.toDouble()).toInt()).none { n % it == 0 }
            }.shuffled(random).forEach { candidate ->
                if (extras.size < settings.tileCount - 3) tryAdd(candidate)
            }
        }
        check(extras.size == settings.tileCount - 3) {
            "Could not generate a unique multiplication board"
        }
        val shuffled = (seed + extras).shuffled(random)
        val answerIndex = shuffled.indexOf(result)
        check(validAnswerIndices(shuffled, product = true) == listOf(answerIndex))

        return Puzzle(
            numbers = shuffled,
            correctIndex = answerIndex,
            instruction = "Find the product of two numbers",
            explanation = "$a × $b = $result",
            operandA = a,
            operandB = b,
            usesEquation = false
        )
    }

    private fun validAnswerIndices(numbers: List<Int>, product: Boolean): List<Int> =
        numbers.indices.filter { target ->
            numbers.indices.any { i ->
                i != target && (i + 1 until numbers.size).any { j ->
                    j != target &&
                        (if (product) numbers[i] * numbers[j] else numbers[i] + numbers[j]) == numbers[target]
                }
            }
        }

    private fun equationPuzzle(
        settings: GameSettings,
        left: Int,
        right: Int,
        answer: Int,
        symbol: String,
        maximum: Int
    ): Puzzle {
        val options = ((1..maximum).filter { it != answer }.shuffled(random)
            .take(settings.tileCount - 1) + answer).shuffled(random)
        require(options.size == settings.tileCount) { "Not enough unique choices" }
        return Puzzle(
            numbers = options,
            correctIndex = options.indexOf(answer),
            instruction = "$left $symbol $right = ?",
            explanation = "$left $symbol $right = $answer",
            operandA = left,
            operandB = right,
            usesEquation = true
        )
    }

    private fun additionEquation(settings: GameSettings): Puzzle {
        val a = random.nextInt(1, settings.maxNumber)
        val b = random.nextInt(1, settings.maxNumber - a + 1)
        return equationPuzzle(settings, a, b, a + b, "+", settings.maxNumber)
    }

    private fun subtraction(settings: GameSettings): Puzzle {
        val a = random.nextInt(2, settings.maxNumber + 1)
        val b = random.nextInt(1, a)
        return equationPuzzle(settings, a, b, a - b, "−", settings.maxNumber)
    }

    private fun division(settings: GameSettings): Puzzle {
        val table = settings.selectedTables.random(random)
        val multiplier = random.nextInt(2, settings.multiplierLimit + 1)
        val dividend = table * multiplier
        val divisor = if (random.nextBoolean()) table else multiplier
        val answer = dividend / divisor
        val maximum = maxOf(24, (settings.selectedTables.maxOrNull() ?: 1) * settings.multiplierLimit, settings.tileCount + 5)
        return equationPuzzle(settings, dividend, divisor, answer, "÷", maximum)
    }
}
