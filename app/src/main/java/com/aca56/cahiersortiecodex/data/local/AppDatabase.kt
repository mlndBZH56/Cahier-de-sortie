package com.aca56.cahiersortiecodex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aca56.cahiersortiecodex.data.local.converter.SessionStatusConverter
import com.aca56.cahiersortiecodex.data.local.dao.BoatDao
import com.aca56.cahiersortiecodex.data.local.dao.DestinationDao
import com.aca56.cahiersortiecodex.data.local.dao.RemarkDao
import com.aca56.cahiersortiecodex.data.local.dao.RowerDao
import com.aca56.cahiersortiecodex.data.local.dao.SessionDao
import com.aca56.cahiersortiecodex.data.local.dao.SessionRowerDao
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionRowerEntity

@Database(
    entities = [
        RowerEntity::class,
        BoatEntity::class,
        DestinationEntity::class,
        SessionEntity::class,
        SessionRowerEntity::class,
        RemarkEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(SessionStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rowerDao(): RowerDao
    abstract fun boatDao(): BoatDao
    abstract fun destinationDao(): DestinationDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionRowerDao(): SessionRowerDao
    abstract fun remarkDao(): RemarkDao

    fun resetAllData() {
        val database = openHelper.writableDatabase
        database.execSQL("PRAGMA foreign_keys = OFF")
        database.beginTransaction()
        try {
            database.execSQL("DELETE FROM remarks")
            database.execSQL("DELETE FROM session_rowers")
            database.execSQL("DELETE FROM sessions")
            database.execSQL("DELETE FROM destinations")
            database.execSQL("DELETE FROM boats")
            database.execSQL("DELETE FROM rowers")
            runCatching {
                database.execSQL("DELETE FROM sqlite_sequence")
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
            database.execSQL("PRAGMA foreign_keys = ON")
            runCatching {
                database.query("PRAGMA wal_checkpoint(FULL)").close()
            }
        }
    }

    companion object {
        const val DATABASE_NAME = "cahier_sortie.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS destinations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_destinations_name ON destinations(name)",
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO destinations(name)
                    SELECT DISTINCT TRIM(destination)
                    FROM sessions
                    WHERE TRIM(destination) <> ''
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sessions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        boatId INTEGER NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT,
                        destinationId INTEGER,
                        km REAL NOT NULL,
                        remarks TEXT,
                        status TEXT NOT NULL,
                        FOREIGN KEY(boatId) REFERENCES boats(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(destinationId) REFERENCES destinations(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_new_boatId ON sessions_new(boatId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_new_destinationId ON sessions_new(destinationId)")
                database.execSQL(
                    """
                    INSERT INTO sessions_new(id, date, boatId, startTime, endTime, destinationId, km, remarks, status)
                    SELECT
                        s.id,
                        s.date,
                        s.boatId,
                        s.startTime,
                        s.endTime,
                        CASE
                            WHEN TRIM(s.destination) = '' THEN NULL
                            ELSE (SELECT d.id FROM destinations d WHERE d.name = TRIM(s.destination) LIMIT 1)
                        END,
                        s.km,
                        s.notes,
                        s.status
                    FROM sessions s
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE sessions")
                database.execSQL("ALTER TABLE sessions_new RENAME TO sessions")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_boatId ON sessions(boatId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_destinationId ON sessions(destinationId)")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS session_rowers_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        rowerId INTEGER,
                        guestName TEXT,
                        FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(rowerId) REFERENCES rowers(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_session_rowers_new_sessionId ON session_rowers_new(sessionId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_session_rowers_new_rowerId ON session_rowers_new(rowerId)")
                database.execSQL(
                    """
                    INSERT INTO session_rowers_new(sessionId, rowerId, guestName)
                    SELECT sessionId, rowerId, NULL
                    FROM session_rowers
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE session_rowers")
                database.execSQL("ALTER TABLE session_rowers_new RENAME TO session_rowers")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_session_rowers_sessionId ON session_rowers(sessionId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_session_rowers_rowerId ON session_rowers(rowerId)")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS remarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        boatId INTEGER,
                        content TEXT NOT NULL,
                        date TEXT NOT NULL,
                        FOREIGN KEY(boatId) REFERENCES boats(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_remarks_boatId ON remarks(boatId)")
            }
        }
    }
}
