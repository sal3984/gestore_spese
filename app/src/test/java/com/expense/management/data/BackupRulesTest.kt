package com.expense.management.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Le regole del backup automatico di Android nominano i file a mano: se il database cambia nome o
 * i due file divergono, il backup smette di salvare i dati senza alcun errore.
 */
@DisplayName("Regole del backup automatico")
class BackupRulesTest {

    private val expected = listOf(
        "database" to DATABASE_NAME,
        "database" to "$DATABASE_NAME-wal",
        "database" to "$DATABASE_NAME-shm",
        "sharedpref" to "prefs.xml",
        "file" to "category_images/",
    )

    @Test
    fun `android 11 and earlier back up database, preferences and category images only`() {
        assertEquals(expected, includes(rules("backup_rules.xml").documentElement))
    }

    @Test
    fun `android 12 and later use the same rules for cloud backup and device transfer`() {
        val root = rules("data_extraction_rules.xml").documentElement
        listOf("cloud-backup", "device-transfer").forEach { section ->
            val element = root.getElementsByTagName(section).item(0) as Element
            assertEquals(expected, includes(element), section)
        }
    }

    @Test
    fun `nothing is excluded explicitly, so pre-migration copies stay out by not being included`() {
        listOf("backup_rules.xml", "data_extraction_rules.xml").forEach { name ->
            assertEquals(0, rules(name).getElementsByTagName("exclude").length, name)
        }
    }

    private fun rules(name: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(File("src/main/res/xml/$name"))

    private fun includes(parent: Element): List<Pair<String, String>> {
        val nodes = parent.getElementsByTagName("include")
        return (0 until nodes.length).map { i ->
            val include = nodes.item(i) as Element
            include.getAttribute("domain") to include.getAttribute("path")
        }
    }
}
