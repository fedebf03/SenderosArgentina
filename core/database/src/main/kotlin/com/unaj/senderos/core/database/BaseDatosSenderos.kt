package com.unaj.senderos.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SenderoEntidad::class, RecorridoEntidad::class, PuntoCapturadoEntidad::class],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8)
    ]
)
@TypeConverters(ConvertidoresSendero::class)
abstract class BaseDatosSenderos : RoomDatabase() {

    abstract fun senderoDao(): SenderoDao

    abstract fun recorridoDao(): RecorridoDao

    abstract fun puntoCapturadoDao(): PuntoCapturadoDao
}
