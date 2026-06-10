package org.sosnetwork.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        IdentityEntity::class,
        VerificationRequestEntity::class,
        ReceivedAlertEntity::class,
        SeenMessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SosDatabase : RoomDatabase() {
    abstract fun identityDao(): IdentityDao
    abstract fun verificationDao(): VerificationDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var instance: SosDatabase? = null

        fun get(context: Context): SosDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SosDatabase::class.java,
                    "sos_network.db"
                ).build().also { instance = it }
            }
    }
}
