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
            assertTrue(p.operandB in 2..6)
            assertFalse(p.usesEquation)
            assertFalse(p.numbers.contains(1))
            assertEquals(listOf(p.correctIndex), answers(p.numbers, true))
        }
    }

    @Test fun multiplicationDistractorsPreferOnlySelectedTableAnswers() {
        val cases = listOf(
            GameSettings(
                operation = Operation.MULTIPLY,
                selectedTables = setOf(5),
                multiplierLimit = 6,
                tileCount = 6
            ),
            GameSettings(
                operation = Operation.MULTIPLY,
                selectedTables = setOf(3, 5),
                multiplierLimit = 8,
                tileCount = 6
            )
        )
        for (settings in cases) {
            val selectedAnswers = settings.selectedTables.flatMap { table ->
                (2..settings.multiplierLimit).map { table * it }
            }.toSet()
            repeat(80) {
                val p = engine.next(settings)
                val remaining = p.numbers.toMutableList()
                assertTrue(remaining.remove(p.operandA))
                assertTrue(remaining.remove(p.operandB))
                assertTrue(remaining.remove(p.numbers[p.correctIndex]))
                assertEquals(settings.tileCount - 3, remaining.size)
                assertTrue("Non-table distractors were used despite enough choices",
                    remaining.all { it in selectedAnswers })
                assertEquals(listOf(p.correctIndex), answers(p.numbers, true))
            }
        }
    }

    @Test fun shortTablesCanFillTheRemainingTilesWithOtherNumbers() {
        val settings = GameSettings(
            operation = Operation.MULTIPLY,
            selectedTables = setOf(2),
            multiplierLimit = 2,
            tileCount = 12
        )
        repeat(60) {
            val p = engine.next(settings)
            assertEquals(12, p.numbers.size)
            assertFalse(p.usesEquation)
            assertEquals(listOf(p.correctIndex), answers(p.numbers, true))
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
            assertTrue(p.numbers.all { it in 1..12 })
            assertTrue((p.operandA / 3 in 1..12 && p.operandA % 3 == 0) ||
                (p.operandA / 6 in 1..12 && p.operandA % 6 == 0))
        }
    }

    @Test fun multiplicationAtSmallestRangeStaysTileBased() {
        for (tiles in listOf(4, 6, 9, 12)) {
            val settings = GameSettings(
                operation = Operation.MULTIPLY,
                tileCount = tiles,
                selectedTables = setOf(2),
                multiplierLimit = 2
            )
            repeat(100) {
                val p = engine.next(settings)
                assertFalse(p.usesEquation)
                assertEquals(tiles, p.numbers.size)
                assertEquals(4, p.numbers[p.correctIndex])
                assertEquals(2, p.operandA)
                assertEquals(2, p.operandB)
                assertFalse(p.numbers.contains(1))
                assertEquals(listOf(p.correctIndex), answers(p.numbers, true))
            }
        }
        assertFalse(GameSettings(
            operation = Operation.MULTIPLY,
            multiplierLimit = 1
        ).isValid())
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
