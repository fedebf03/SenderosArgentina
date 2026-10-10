package com.unaj.senderos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Base de datos Room con los senderos de todos los usuarios del dispositivo
@Database(entities = [SenderoEntidad::class], version = 1, exportSchema = true)
@TypeConverters(ConvertidoresSendero::class)
abstract class BaseDatosSenderos : RoomDatabase() {

    // Obtener el DAO de senderos
    abstract fun senderoDao(): SenderoDao
}
