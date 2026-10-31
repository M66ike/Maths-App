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
        val selectedProducts = settings.selectedTables
            .flatMap { table -> (2..settings.multiplierLimit).map { multiplier -> table * multiplier } }
            .distinct()
        val pairs = settings.selectedTables
            .flatMap { table -> (2..settings.multiplierLimit).map { table to it } }
            .shuffled(random)
        val requiredExtras = settings.tileCount - 3

        fun canAdd(seed: List<Int>, extras: List<Int>, number: Int): Boolean {
            if (number == 1 || number in seed || number in extras) return false
            return validAnswerIndices(seed + extras + number, product = true) == listOf(2)
        }

        fun puzzleFrom(seed: List<Int>, extras: List<Int>): Puzzle {
            val shuffled = (seed + extras).shuffled(random)
            val correctIndex = shuffled.indexOf(seed[2])
            check(validAnswerIndices(shuffled, product = true) == listOf(correctIndex))
            return Puzzle(
                numbers = shuffled,
                correctIndex = correctIndex,
                instruction = "Find the product of two numbers",
                explanation = "${seed[0]} × ${seed[1]} = ${seed[2]}",
                operandA = seed[0],
                operandB = seed[1],
                usesEquation = false
            )
        }

        // First try to fill every other tile with an actual answer from
        // the chosen tables and chosen multiplier limit.
        var bestSeed: List<Int>? = null
        var bestExtras: List<Int> = emptyList()
        for ((a, b) in pairs) {
            val seed = listOf(a, b, a * b)
            repeat(4) {
                val extras = mutableListOf<Int>()
                for (number in selectedProducts.shuffled(random)) {
                    if (extras.size == requiredExtras) break
                    if (canAdd(seed, extras, number)) extras += number
                }
                if (extras.size == requiredExtras) return puzzleFrom(seed, extras)
                if (bestSeed == null || extras.size > bestExtras.size) {
                    bestSeed = seed
                    bestExtras = extras.toList()
                }
            }
        }

        // Very short table selections may not have enough distinct valid
        // answers to fill a larger board. Use other numbers only then.
        val seed = checkNotNull(bestSeed)
        val extras = bestExtras.toMutableList()
        val maximum = maxOf(36, selectedProducts.maxOrNull() ?: 36, settings.tileCount * 4)
        for (number in (2..maximum).shuffled(random)) {
            if (extras.size == requiredExtras) break
            if (canAdd(seed, extras, number)) extras += number
        }
        if (extras.size < requiredExtras) {
            for (number in (13..199).shuffled(random)) {
                if (extras.size == requiredExtras) break
                if (canAdd(seed, extras, number)) extras += number
            }
        }
        check(extras.size == requiredExtras) {
            "Could not generate a unique multiplication board"
        }
        return puzzleFrom(seed, extras)
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
        // Quotients are always 1..12, unlike the larger dividend.
        return equationPuzzle(settings, dividend, divisor, answer, "÷", 12)
    }
}
