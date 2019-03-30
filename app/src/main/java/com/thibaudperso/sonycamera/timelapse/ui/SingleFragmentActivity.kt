package com.thibaudperso.sonycamera.timelapse.ui

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.sdk.CameraAPI
import com.thibaudperso.sonycamera.timelapse.control.connection.ConnectionState
import com.thibaudperso.sonycamera.timelapse.control.connection.ConnectionStateMachine
import com.thibaudperso.sonycamera.timelapse.control.connection.NFCHandler
import com.thibaudperso.sonycamera.timelapse.control.connection.WifiHandler
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import mu.KLogging
import javax.inject.Inject

/**
 * An activity with a single fragment
 * https://github.com/tkunstek/android-big-nerd-ranch/blob/master/20_CameraImage_CriminalIntent/src/com/bignerdranch/android/criminalintent/SingleFragmentActivity.java
 */
abstract class SingleFragmentActivity<T : Fragment> : DaggerAppCompatActivity() {

    companion object : KLogging();

    @Inject lateinit var stateMachineConnection: ConnectionStateMachine
    @Inject lateinit var cameraAPI: CameraAPI
    @Inject lateinit var wifiHandler: WifiHandler

    protected var enableStateMachineConnection = true
    protected var notConnectedMessage = true
    private var snackBarConnectionLost: Snackbar? = null
    private var statesSubscription: Disposable? = null

    protected lateinit var fragment: T

    @Suppress("UNCHECKED_CAST")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)
        val manager = supportFragmentManager
        fragment = manager.findFragmentById(R.id.fragment_container) as T? ?: createFragment().apply {
            manager.beginTransaction()
                    .add(R.id.fragment_container, this)
                    .commit()
        }

        title = "${getString(R.string.app_name)} - $guideTitle"
    }

    override fun onStart() {
        super.onStart()
        val frameLayout = findViewById<FrameLayout>(R.id.fragment_container)
        if (frameLayout != null && frameLayout.childCount > 0) {
            snackBarConnectionLost = Snackbar.make(frameLayout.getChildAt(0),
                    getString(R.string.connection_with_camera_lost), Snackbar.LENGTH_INDEFINITE)
        } else {
            notConnectedMessage = false
        }

    }

    override fun onResume() {
        super.onResume()

        if (notConnectedMessage) {
            statesSubscription = stateMachineConnection.states
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { connectionState ->
                        if (connectionState is ConnectionState.GoodApiAccess) {
                            snackBarConnectionLost?.dismiss()
                        } else {
                            snackBarConnectionLost?.show()
                        }
                    }.subscribe()
        }

        if (enableStateMachineConnection) {
            stateMachineConnection.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (enableStateMachineConnection) {
            stateMachineConnection.stop()
        }

        if (notConnectedMessage) {
            statesSubscription?.dispose()
            statesSubscription = null
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            logger.warn { "onNewIntent: $intent" }
            val loginPwd = NFCHandler.parseIntent(intent) ?: return
            wifiHandler.createIfNeededThenConnectToWifi(loginPwd.first, loginPwd.second)
        } catch (e: Exception) {
            logger.warn(e) { "Error: ${e.message}" }
        }

    }

    protected abstract val guideTitle: String

    protected abstract fun createFragment(): T

}