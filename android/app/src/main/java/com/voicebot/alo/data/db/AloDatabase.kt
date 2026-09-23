package com.voicebot.alo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, DroppedEntity::class, ChatCursorEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AloDatabase : RoomDatabase() {

    abstract fun dao(): AloDao

    companion object {
        @Volatile
        private var instance: AloDatabase? = null

        fun get(context: Context): AloDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AloDatabase::class.java,
                    "alo.db",
                )
                    // Fase 1: .openHelperFactory(SupportFactory(passphrase))  // SQLCipher
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
