package com.thibaudperso.sonycamera.timelapse

import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    // Dagger modules
    AndroidInjectionModule::class,
    AndroidSupportInjectionModule::class,
    //
    BindingsModule::class,
    NetworkModule::class
])

interface TimelapseApplicationComponent : AndroidInjector<TimelapseApplication> {

    // ==========================================================================================================================
    // Builder
    // ==========================================================================================================================

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<TimelapseApplication>() {
        abstract override fun build(): TimelapseApplicationComponent
    }

}