package com.expense.management.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Lint non vede i testi scritti nelle composable: questo test legge i sorgenti della UI e fallisce
 * se un testo visibile non passa da strings.xml. Le parti `${...}` e `$nome` dei template non
 * contano: "$currencySymbol *****" va bene, "$count rate" no.
 */
@DisplayName("Nessun testo della UI scritto nel codice")
class HardcodedUiTextTest {

    private val sourceRoot = File("src/main/java/com/expense/management")

    @Test
    fun `ui sources show only text from string resources`() {
        val files = (sourceRoot.resolve("ui").walkTopDown().filter { it.extension == "kt" } + sourceRoot.resolve("MainActivity.kt")).toList()
        assertTrue(files.size > 30, "sorgenti della UI non trovati in ${sourceRoot.absolutePath}")
        val offenders = files.flatMap { file -> findHardcodedText(file.readLines()).map { "${file.relativeTo(sourceRoot)}:$it" } }.toList()

        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `the check recognizes hardcoded text and ignores templates of values`() {
        val lines = listOf(
            """Text("Salva piano")""",
            """label = { Text("Importo rata") },""",
            """contentDescription = "Pagina ${'$'}{i + 1} di ${'$'}n"""",
            """Icon(Icons.Default.DateRange, "Seleziona data")""",
            """Text(""",
            """    "Rate future",""",
            """text = "${'$'}{proj.methodName}: ${'$'}{proj.installments.size} rate",""",
            """text = if (hidden) "${'$'}currencySymbol *****" else "${'$'}currencySymbol ${'$'}{String.format(locale, "%.2f", amount)}",""",
            """Text(stringResource(R.string.save))""",
            """CategoryEntity(id = "test", label = "Cibo", icon = "x", type = TransactionType.EXPENSE)""",
        )

        assertEquals(listOf(1, 2, 3, 4, 6, 7), findHardcodedText(lines))
        assertTrue(findHardcodedText(listOf("""Text("${'$'}a/${'$'}b")""")).isEmpty())
    }

    /** Numeri di riga (da 1) con un testo visibile scritto nel codice. */
    private fun findHardcodedText(lines: List<String>): List<Int> = lines.indices.filter { i ->
        val line = lines[i]
        if (line.contains("Entity(")) return@filter false
        val literal = UI_ARGUMENT.find(line)?.let { literalAt(line, it.range.last) }
            ?: if (i > 0 && lines[i - 1].trimEnd().endsWith("Text(")) {
                line.indexOf('"').takeIf { it >= 0 && line.substring(0, it).isBlank() }?.let { literalAt(line, it) }
            } else {
                null
            }
        literal != null && hasWords(literal)
    }.map { it + 1 }

    /** Il letterale che inizia al doppio apice in [quote], senza le espressioni `${...}`. */
    private fun literalAt(line: String, quote: Int): String? {
        if (line.getOrNull(quote) != '"') return null
        val text = StringBuilder()
        var i = quote + 1
        while (i < line.length) {
            when {
                line[i] == '\\' -> i += 2
                line.startsWith("\${", i) -> {
                    var depth = 1
                    i += 2
                    while (i < line.length && depth > 0) {
                        if (line[i] == '{') depth++
                        if (line[i] == '}') depth--
                        i++
                    }
                }
                line[i] == '$' -> {
                    i++
                    while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                }
                line[i] == '"' -> return text.toString()
                else -> text.append(line[i++])
            }
        }
        return null
    }

    private fun hasWords(literal: String) = Regex("""\p{L}{2,}""").containsMatchIn(literal)

    private companion object {
        /** Posizione del doppio apice dove la UI riceve un testo: Text("..."), label = "...", Icon(icona, "..."). */
        val UI_ARGUMENT = Regex("""(?:\bText\(\s*|\b(?:text|contentDescription|label|title|placeholder)\s*=\s*(?:if \([^)]*\)\s*)?|\bIcon\([^,()]+,\s*)"""")
    }
}
