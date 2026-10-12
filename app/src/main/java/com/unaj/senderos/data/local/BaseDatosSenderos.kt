package com.unaj.senderos.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SenderoEntidad::class, RecorridoEntidad::class, PuntoCapturadoEntidad::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ]
)
@TypeConverters(ConvertidoresSendero::class)
abstract class BaseDatosSenderos : RoomDatabase() {

    // Obtener el DAO de senderos
    abstract fun senderoDao(): SenderoDao

    // Obtener el DAO de recorridos realizados
    abstract fun recorridoDao(): RecorridoDao

    // Obtener el DAO de puntos de interés capturados
    abstract fun puntoCapturadoDao(): PuntoCapturadoDao
}
