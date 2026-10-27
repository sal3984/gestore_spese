package com.expense.management.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseBackupTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        dbFile = context.getDatabasePath(TEST_DB)
        dbFile.parentFile?.listFiles()?.filter { it.name.startsWith(TEST_DB) }?.forEach { it.delete() }
    }

    @Test
    fun copiesAnOlderDatabaseWithItsDataBeforeMigrating() {
        createDatabase(version = 23, rows = 3)

        AppDatabase.backupBeforeMigration(context, TEST_DB, targetVersion = 24)

        val backup = backupFile(24)
        assertTrue(backup.exists())
        SQLiteDatabase.openDatabase(backup.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(23, db.version)
            db.rawQuery("SELECT COUNT(*) FROM sample", null).use {
                it.moveToFirst()
                assertEquals(3, it.getInt(0))
            }
        }
    }

    @Test
    fun skipsADatabaseAlreadyAtTheTargetVersion() {
        createDatabase(version = 24, rows = 1)

        AppDatabase.backupBeforeMigration(context, TEST_DB, targetVersion = 24)

        assertFalse(backupFile(24).exists())
    }

    @Test
    fun skipsAFreshInstallWithoutCreatingTheDatabase() {
        AppDatabase.backupBeforeMigration(context, TEST_DB, targetVersion = 24)

        assertFalse(dbFile.exists())
        assertFalse(backupFile(24).exists())
    }

    @Test
    fun keepsOnlyTheLatestBackupAndNeverOverwritesIt() {
        createDatabase(version = 23, rows = 1)
        backupFile(23).writeText("vecchia copia")
        backupFile(24).writeText("copia gia' fatta")

        AppDatabase.backupBeforeMigration(context, TEST_DB, targetVersion = 24)

        assertEquals("copia gia' fatta", backupFile(24).readText())

        backupFile(24).delete()
        AppDatabase.backupBeforeMigration(context, TEST_DB, targetVersion = 24)

        assertTrue(backupFile(24).exists())
        assertFalse(backupFile(23).exists())
    }

    private fun createDatabase(version: Int, rows: Int) {
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL("CREATE TABLE sample (id INTEGER PRIMARY KEY)")
            repeat(rows) { db.execSQL("INSERT INTO sample DEFAULT VALUES") }
            db.version = version
        }
    }

    private fun backupFile(targetVersion: Int) = File(dbFile.parentFile, "$TEST_DB.pre-v$targetVersion.bak")

    private companion object {
        const val TEST_DB = "backup-test"
    }
}
