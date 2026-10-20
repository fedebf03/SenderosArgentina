package com.unaj.senderos.core.database.di

import android.content.Context
import androidx.room.Room
import com.unaj.senderos.core.database.BaseDatosSenderos
import com.unaj.senderos.core.database.EjecutorTransacciones
import com.unaj.senderos.core.database.EjecutorTransaccionesRoom
import com.unaj.senderos.core.database.puntocapturado.PuntoCapturadoDao
import com.unaj.senderos.core.database.recorrido.RecorridoDao
import com.unaj.senderos.core.database.sendero.SenderoDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    abstract fun bindEjecutorTransacciones(impl: EjecutorTransaccionesRoom): EjecutorTransacciones

    companion object {

        @Provides
        @Singleton
        fun provideBaseDatos(@ApplicationContext contexto: Context): BaseDatosSenderos =
            Room.databaseBuilder(contexto, BaseDatosSenderos::class.java, "senderos.db").build()

        @Provides
        fun provideSenderoDao(baseDatos: BaseDatosSenderos): SenderoDao = baseDatos.senderoDao()

        @Provides
        fun provideRecorridoDao(baseDatos: BaseDatosSenderos): RecorridoDao = baseDatos.recorridoDao()

        @Provides
        fun providePuntoCapturadoDao(baseDatos: BaseDatosSenderos): PuntoCapturadoDao = baseDatos.puntoCapturadoDao()
    }
}
