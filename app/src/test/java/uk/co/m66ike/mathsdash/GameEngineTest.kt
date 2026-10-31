package uk.co.m66ike.mathsdash

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {
    private val engine = GameEngine(Random(2026))

    @Test fun additionHasOneValidAnswerAndStaysWithinRange() {
        val settings = GameSettings(operation = Operation.ADD, tileCount = 6, maxNumber = 30)
        repeat(250) {
            val p = engine.next(settings)
            assertEquals(6, p.numbers.size)
            assertEquals(6, p.numbers.toSet().size)
            assertTrue(p.numbers.all { it in 1..30 })
            assertTrue(p.numbers[p.correctIndex] == p.operandA + p.operandB)
            if (!p.usesEquation) assertEquals(listOf(p.correctIndex), answers(p.numbers, false))
        }
    }

    @Test fun multiplicationRespectsSelectedTablesAndHasOneAnswer() {
        val settings = GameSettings(
            operation = Operation.MULTIPLY,
            tileCount = 6,
            selectedTables = setOf(3, 6),
            multiplierLimit = 6
        )
        repeat(250) {
            val p = engine.next(settings)
            assertEquals(p.operandA * p.operandB, p.numbers[p.correctIndex])
            assertTrue(p.operandA in settings.selectedTables)
            assertTrue(p.operandB in 1..6)
            if (!p.usesEquation) assertEquals(listOf(p.correctIndex), answers(p.numbers, true))
        }
    }

    @Test fun subtractionAlwaysPositiveAndCorrect() {
        val settings = GameSettings(operation = Operation.SUBTRACT, maxNumber = 20)
        repeat(250) {
            val p = engine.next(settings)
            assertTrue(p.usesEquation)
            assertTrue(p.operandA > p.operandB)
            assertEquals(p.operandA - p.operandB, p.numbers[p.correctIndex])
        }
    }

    @Test fun divisionIsExactAndFromSelectedTables() {
        val settings = GameSettings(
            operation = Operation.DIVIDE,
            selectedTables = setOf(3, 6),
            multiplierLimit = 12
        )
        repeat(250) {
            val p = engine.next(settings)
            assertEquals(0, p.operandA % p.operandB)
            assertEquals(p.operandA / p.operandB, p.numbers[p.correctIndex])
            assertTrue((p.operandA / 3 in 1..12 && p.operandA % 3 == 0) ||
                (p.operandA / 6 in 1..12 && p.operandA % 6 == 0))
        }
    }

    @Test fun multiplicationByOneWorksAsAnEquation() {
        val p = engine.next(GameSettings(
            operation = Operation.MULTIPLY,
            selectedTables = setOf(3),
            multiplierLimit = 1
        ))
        assertTrue(p.usesEquation)
        assertEquals(p.operandA * p.operandB, p.numbers[p.correctIndex])
    }

    @Test fun leaderboardKeysIncludeRelevantSettings() {
        val a = GameSettings()
        assertNotEquals(a.key(), a.copy(maxNumber = 50).key())
        assertEquals(a.key(), a.copy(selectedTables = setOf(12)).key())
        val mult = a.copy(operation = Operation.MULTIPLY)
        assertNotEquals(mult.key(), mult.copy(selectedTables = setOf(3)).key())
        assertNotEquals(mult.key(), mult.copy(timeSeconds = 30).key())
    }

    private fun answers(values: List<Int>, product: Boolean): List<Int> =
        values.indices.filter { target ->
            values.indices.any { i ->
                i != target && (i + 1 until values.size).any { j ->
                    j != target && (if (product) values[i] * values[j] else values[i] + values[j]) == values[target]
                }
            }
        }
}
