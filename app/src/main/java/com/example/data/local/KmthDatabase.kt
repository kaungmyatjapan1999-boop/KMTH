package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VpnServerEntity::class, ConnectionLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KmthDatabase : RoomDatabase() {

    abstract fun vpnDao(): VpnDao

    companion object {
        @Volatile
        private var INSTANCE: KmthDatabase? = null

        fun getDatabase(context: Context): KmthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KmthDatabase::class.java,
                    "kmth_vpn_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
