package com.unaj.senderos.core.network.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unaj.senderos.core.network.FirestoreRecorridoFuenteRemota
import com.unaj.senderos.core.network.FirestoreSenderoFuenteRemota
import com.unaj.senderos.core.network.FirestoreSenderoPublicoFuenteRemota
import com.unaj.senderos.core.network.RecorridoFuenteRemota
import com.unaj.senderos.core.network.SenderoFuenteRemota
import com.unaj.senderos.core.network.SenderoPublicoFuenteRemota
import com.unaj.senderos.core.network.SubidorFotos
import com.unaj.senderos.core.network.SubidorFotosCloudinary
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.unaj.senderos.core.network.FirebaseFuenteAutenticacion
import com.unaj.senderos.core.network.FuenteAutenticacion
import com.unaj.senderos.core.network.ApiElevacionOpenMeteo
import com.unaj.senderos.core.network.ApiPronosticoOpenMeteo
import com.unaj.senderos.core.network.crearApiElevacionOpenMeteo
import com.unaj.senderos.core.network.crearApiPronosticoOpenMeteo

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindSenderoFuenteRemota(impl: FirestoreSenderoFuenteRemota): SenderoFuenteRemota

    @Binds
    abstract fun bindRecorridoFuenteRemota(impl: FirestoreRecorridoFuenteRemota): RecorridoFuenteRemota

    @Binds
    abstract fun bindSenderoPublicoFuenteRemota(impl: FirestoreSenderoPublicoFuenteRemota): SenderoPublicoFuenteRemota

    @Binds
    abstract fun bindFuenteAutenticacion(impl: FirebaseFuenteAutenticacion): FuenteAutenticacion

    @Binds
    abstract fun bindSubidorFotos(impl: SubidorFotosCloudinary): SubidorFotos

    companion object {

        @Provides
        @Singleton
        fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideApiElevacion(): ApiElevacionOpenMeteo = crearApiElevacionOpenMeteo()

        @Provides
        @Singleton
        fun provideApiPronostico(): ApiPronosticoOpenMeteo = crearApiPronosticoOpenMeteo()

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
