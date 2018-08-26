package com.thibaudperso.sonycamera.timelapse

import android.app.Application
import com.thibaudperso.sonycamera.sdk.RealSonyCameraApi
import com.thibaudperso.sonycamera.sdk.SonyCameraApi
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService
import com.thibaudperso.sonycamera.timelapse.ui.adjustments.AdjustmentsActivity
import com.thibaudperso.sonycamera.timelapse.ui.adjustments.AdjustmentsFragment
import com.thibaudperso.sonycamera.timelapse.ui.connection.ConnectionActivity
import com.thibaudperso.sonycamera.timelapse.ui.connection.ConnectionFragment
import com.thibaudperso.sonycamera.timelapse.ui.processing.ProcessingActivity
import com.thibaudperso.sonycamera.timelapse.ui.processing.ProcessingFragment
import com.thibaudperso.sonycamera.timelapse.ui.settings.SettingsActivity
import com.thibaudperso.sonycamera.timelapse.ui.settings.SettingsFragment
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class BindingsModule {

    @Binds
    abstract fun bindApplication(timelapseApplication: TimelapseApplication): Application

    @Binds
    abstract fun bindSonyCameraApi(realSonyCameraApi: RealSonyCameraApi): SonyCameraApi

    // ==========================================================================================================================
    // Activities
    // ==========================================================================================================================

    @ContributesAndroidInjector
    abstract fun adjustmentsActivityInjector(): AdjustmentsActivity

    @ContributesAndroidInjector
    abstract fun connectionActivityInjector(): ConnectionActivity

    @ContributesAndroidInjector
    abstract fun processingActivityInjector(): ProcessingActivity

    @ContributesAndroidInjector
    abstract fun settingsActivityInjector(): SettingsActivity

    // ==========================================================================================================================
    // Fragments
    // ==========================================================================================================================

    @ContributesAndroidInjector
    abstract fun contributeAdjustmentsFragmentInjector(): AdjustmentsFragment

    @ContributesAndroidInjector
    abstract fun contributeConnectionFragmentInjector(): ConnectionFragment

    @ContributesAndroidInjector
    abstract fun contributeProcessingFragmentInjector(): ProcessingFragment

    @ContributesAndroidInjector
    abstract fun contributeSettingsFragmentInjector(): SettingsFragment

    // ==========================================================================================================================
    // Services
    // ==========================================================================================================================

    @ContributesAndroidInjector
    abstract fun contributeIntervalometerServiceInjector(): IntervalometerService

}