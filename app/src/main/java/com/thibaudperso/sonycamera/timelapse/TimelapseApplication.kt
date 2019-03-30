package com.thibaudperso.sonycamera.timelapse

import android.os.Looper
import com.thibaudperso.sonycamera.BuildConfig.VERSION_CODE
import com.thibaudperso.sonycamera.BuildConfig.VERSION_NAME
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import mu.KLogging
import kotlin.system.measureTimeMillis

class TimelapseApplication: DaggerApplication() {

    companion object: KLogging() {
        init {
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { AndroidSchedulers.from(Looper.getMainLooper(), true) }
            RxJavaPlugins.setErrorHandler { throwable ->
                logger.warn("Uncaught error: {}", throwable.message, throwable)
            }
        }
    }

    private val appComponent: TimelapseApplicationComponent by lazy {
        DaggerTimelapseApplicationComponent.builder().apply {
            seedInstance(this@TimelapseApplication)
        }.build()
    }

    override fun onCreate() {
        super.onCreate()
        measureTimeMillis {
            appComponent.inject(this)
        }.let { millis ->
            logger.warn("{} version {} ({}) took {}ms to init", this::class.java.simpleName, VERSION_NAME, VERSION_CODE, millis)
        }
    }

    // ==========================================================================================================================
    // DaggerApplication
    // ==========================================================================================================================

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> = appComponent

}
