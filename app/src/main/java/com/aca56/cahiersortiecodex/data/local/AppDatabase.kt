package com.aca56.cahiersortiecodex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aca56.cahiersortiecodex.data.local.converter.RemarkStatusConverter
import com.aca56.cahiersortiecodex.data.local.converter.SessionStatusConverter
import com.aca56.cahiersortiecodex.data.local.dao.BoatDao
import com.aca56.cahiersortiecodex.data.local.dao.BoatPhotoDao
import com.aca56.cahiersortiecodex.data.local.dao.BoatRepairDao
import com.aca56.cahiersortiecodex.data.local.dao.DestinationDao
import com.aca56.cahiersortiecodex.data.local.dao.RemarkDao
import com.aca56.cahiersortiecodex.data.local.dao.RepairUpdateDao
import com.aca56.cahiersortiecodex.data.local.dao.RowerDao
import com.aca56.cahiersortiecodex.data.local.dao.SessionDao
import com.aca56.cahiersortiecodex.data.local.dao.SessionRowerDao
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.BoatPhotoEntity
import com.aca56.cahiersortiecodex.data.local.entity.BoatRepairEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.RepairUpdateEntity
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionRowerEntity

@Database(
    entities = [
        RowerEntity::class,
        BoatEntity::class,
        BoatRepairEntity::class,
        BoatPhotoEntity::class,
        DestinationEntity::class,
        SessionEntity::class,
        SessionRowerEntity::class,
        RemarkEntity::class,
        RepairUpdateEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
@TypeConverters(SessionStatusConverter::class, RemarkStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rowerDao(): RowerDao
    abstract fun boatDao(): BoatDao
    abstract fun boatRepairDao(): BoatRepairDao
    abstract fun boatPhotoDao(): BoatPhotoDao
    abstract fun destinationDao(): DestinationDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionRowerDao(): SessionRowerDao
    abstract fun remarkDao(): RemarkDao
    abstract fun repairUpdateDao(): RepairUpdateDao

    fun resetAllData() {
        val database = openHelper.writableDatabase
        database.execSQL("PRAGMA foreign_keys = OFF")
        database.beginTransaction()
        try {
            database.execSQL("DELETE FROM remarks")
            database.execSQL("DELETE FROM repair_updates")
            database.execSQL("DELETE FROM boat_photos")
            database.execSQL("DELETE FROM boat_repairs")
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
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                )
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE boats ADD COLUMN type TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE boats ADD COLUMN weightRange TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE boats ADD COLUMN riggingType TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE boats ADD COLUMN year INTEGER")
                database.execSQL("ALTER TABLE boats ADD COLUMN notes TEXT NOT NULL DEFAULT ''")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS boat_repairs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        boatId INTEGER NOT NULL,
                        issue TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        repairedAt TEXT,
                        repairNote TEXT,
                        FOREIGN KEY(boatId) REFERENCES boats(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_boat_repairs_boatId ON boat_repairs(boatId)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS boat_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        boatId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        FOREIGN KEY(boatId) REFERENCES boats(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_boat_photos_boatId ON boat_photos(boatId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE remarks ADD COLUMN status TEXT NOT NULL DEFAULT 'NORMAL'")
                database.execSQL("ALTER TABLE remarks ADD COLUMN photoPath TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS repair_updates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        remarkId INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        photoPath TEXT,
                        createdAt TEXT NOT NULL,
                        FOREIGN KEY(remarkId) REFERENCES remarks(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_repair_updates_remarkId ON repair_updates(remarkId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS remarks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        boatId INTEGER,
                        sessionId INTEGER,
                        content TEXT NOT NULL,
                        date TEXT NOT NULL,
                        status TEXT NOT NULL,
                        photoPath TEXT,
                        FOREIGN KEY(boatId) REFERENCES boats(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_remarks_new_boatId ON remarks_new(boatId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_remarks_new_sessionId ON remarks_new(sessionId)")
                database.execSQL(
                    """
                    INSERT INTO remarks_new(id, boatId, sessionId, content, date, status, photoPath)
                    SELECT id, boatId, NULL, content, date, status, photoPath
                    FROM remarks
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE remarks")
                database.execSQL("ALTER TABLE remarks_new RENAME TO remarks")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_remarks_boatId ON remarks(boatId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_remarks_sessionId ON remarks(sessionId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE boats ADD COLUMN weight REAL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE rowers ADD COLUMN level TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE boats ADD COLUMN requiredLevel TEXT NOT NULL DEFAULT 'DEBUTANT'")
                database.execSQL("ALTER TABLE boats ADD COLUMN authorizedRowerIds TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
