package com.unaj.senderos.di

import com.unaj.senderos.BuildConfig
import com.unaj.senderos.core.network.di.NombreNubeCloudinary
import com.unaj.senderos.core.network.di.PresetSubidaCloudinary
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @NombreNubeCloudinary
    fun provideNombreNubeCloudinary(): String = BuildConfig.CLOUDINARY_NOMBRE_NUBE

    @Provides
    @PresetSubidaCloudinary
    fun providePresetSubidaCloudinary(): String = BuildConfig.CLOUDINARY_PRESET_SUBIDA
}
