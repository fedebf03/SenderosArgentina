package com.unaj.senderos.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SenderoEntidad::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)]
)
@TypeConverters(ConvertidoresSendero::class)
abstract class BaseDatosSenderos : RoomDatabase() {

    abstract fun senderoDao(): SenderoDao
}
