package com.mizan.civilleitner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mizan.civilleitner.security.SecureDatabaseKeyStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ClientCaseEntity::class,
        CaseTimelineEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SecureClientDatabase : RoomDatabase() {
    abstract fun clientPortalDao(): ClientPortalDao

    companion object {
        @Volatile private var INSTANCE: SecureClientDatabase? = null

        fun get(context: Context): SecureClientDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    System.loadLibrary("sqlcipher")
                    val passphrase = SecureDatabaseKeyStore.getOrCreatePassphrase(context)
                    val factory = SupportOpenHelperFactory(passphrase)
                    Room.databaseBuilder(
                        context.applicationContext,
                        SecureClientDatabase::class.java,
                        "client-portal-secure.db",
                    )
                        .openHelperFactory(factory)
                        .build()
                        .also { INSTANCE = it }
                }
            }
    }
}
