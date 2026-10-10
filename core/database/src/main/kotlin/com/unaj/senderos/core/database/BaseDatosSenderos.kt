package com.unaj.senderos.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Base de datos Room con los senderos de todos los usuarios del dispositivo
@Database(
    entities = [SenderoEntidad::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(ConvertidoresSendero::class)
abstract class BaseDatosSenderos : RoomDatabase() {

    // Obtener el DAO de senderos
    abstract fun senderoDao(): SenderoDao
}
