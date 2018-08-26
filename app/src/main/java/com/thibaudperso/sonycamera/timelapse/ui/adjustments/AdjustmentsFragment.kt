package com.thibaudperso.sonycamera.timelapse.ui.adjustments

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.content.edit
import androidx.core.text.parseAsHtml
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.sdk.CameraAPI
import com.thibaudperso.sonycamera.sdk.SonyCameraApi.ZoomAction
import com.thibaudperso.sonycamera.sdk.SonyCameraApi.ZoomDirection
import com.thibaudperso.sonycamera.timelapse.BaseFragment
import com.thibaudperso.sonycamera.timelapse.Constants.PREF_AUTOMATIC_CONTINUE
import com.thibaudperso.sonycamera.timelapse.control.connection.ConnectionState
import com.thibaudperso.sonycamera.timelapse.control.connection.ConnectionStateMachine
import com.thibaudperso.sonycamera.timelapse.control.connection.WifiHandler
import com.thibaudperso.sonycamera.timelapse.ui.settings.SettingsActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_adjustments.*
import mu.KLogging
import okhttp3.*
import java.io.File
import java.io.IOException
import javax.inject.Inject

class AdjustmentsFragment : BaseFragment() {

    companion object : KLogging() {
        private const val PREVIEW_PICTURE_ACTIVITY_RESULT = 0x1
        private const val PREVIEW_PICTURE_NAME = "preview_picture.jpg"
    }

    @Inject lateinit var stateMachineConnection: ConnectionStateMachine
    @Inject lateinit var cameraAPI: CameraAPI
    @Inject lateinit var wifiHandler: WifiHandler
    @Inject lateinit var liveViewStreamService: LiveViewStreamService
    @Inject lateinit var okHttpClient: OkHttpClient

    private val temporaryPreviewPicture: File by lazy {
        val imgPath = File(requireContext().cacheDir, "images").apply {
            if (!exists() && !mkdirs()) {
                throw RuntimeException("Impossible to create " + toString())
            }
        }
        File(imgPath, PREVIEW_PICTURE_NAME)
    }
    private var stateSubscription: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_adjustments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adjustments_message.text = getString(R.string.adjustments_message).parseAsHtml()

        adjustments_zoom_in.setOnClickListener { cameraAPI.actZoom(ZoomDirection.IN).subscribeOn(Schedulers.io()).subscribe() }
        adjustments_zoom_out.setOnClickListener { cameraAPI.actZoom(ZoomDirection.OUT).subscribeOn(Schedulers.io()).subscribe() }
        adjustments_zoom_in.setOnLongClickListener {
            cameraAPI.actZoom(ZoomDirection.IN, ZoomAction.START).subscribeOn(Schedulers.io()).subscribe()
            return@setOnLongClickListener true
        }
        adjustments_zoom_out.setOnLongClickListener {
            cameraAPI.actZoom(ZoomDirection.OUT, ZoomAction.START).subscribeOn(Schedulers.io()).subscribe()
            return@setOnLongClickListener true
        }

        adjustments_zoom_in.setOnTouchListener(object : View.OnTouchListener {

            var downTime: Long = -1

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                v.performClick()

                if (event.action == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500L) {
                        cameraAPI.actZoom(ZoomDirection.IN, ZoomAction.STOP).subscribeOn(Schedulers.io()).subscribe()
                    }
                }
                if (event.action == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis()
                }
                return false
            }
        })

        adjustments_zoom_out.setOnTouchListener(object : View.OnTouchListener {

            var downTime: Long = -1

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                v.performClick()

                if (event.action == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500L) {
                        cameraAPI.actZoom(ZoomDirection.OUT, ZoomAction.STOP)
                                .subscribeOn(Schedulers.io())
                                .subscribe()
                    }
                }
                if (event.action == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis()
                }
                return false
            }
        })

        val useFlashButton = view.findViewById<CompoundButton>(R.id.adjustments_use_flash)
        useFlashButton.setOnCheckedChangeListener { _, isChecked -> cameraAPI.setFlash(isChecked).subscribeOn(Schedulers.io()).subscribe() }

        adjustments_take_picture_button.setOnClickListener {
            adjustments_take_picture_button.isEnabled = false

            // To take a picture, we need to first half press the shutter and then take the picture
            cameraAPI.halfPressShutter()
                    .andThen(cameraAPI.takePicture())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { imageUrl ->
                        logger.warn("ImageUrl: {}", imageUrl)
                        onResultPicture(imageUrl)
                        adjustments_take_picture_button.isEnabled = true
                    }
                    .doOnError {
                        adjustments_take_picture_button.isEnabled = true
                    }
                    .subscribe()
        }

        adjustments_previous.setOnClickListener { askToDisconnectCamera() }

        adjustments_next.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.enter, R.anim.exit)
        }
    }

    override fun onResume() {
        super.onResume()
        stateSubscription = stateMachineConnection.states
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { connectionState ->
                    logger.warn("state -> {}", connectionState)
                    if (connectionState is ConnectionState.GoodApiAccess) {
                        startLiveView()
                    } else {
                        stopLiveView()
                    }
                }
                .doOnDispose(this::stopLiveView)
                .subscribe()
    }

    override fun onPause() {
        super.onPause()
        stateSubscription?.dispose()
        stateSubscription = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PREVIEW_PICTURE_ACTIVITY_RESULT && temporaryPreviewPicture.exists()) {
            temporaryPreviewPicture.delete()
        }
    }

    private fun askToDisconnectCamera() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.alert_disconnect_camera_title)
                .setMessage(R.string.alert_disconnect_camera_message)
                .setPositiveButton(R.string.alert_disconnect_camera_yes) { _, _ ->
                    PreferenceManager.getDefaultSharedPreferences(context).edit {
                        putBoolean(PREF_AUTOMATIC_CONTINUE, false)
                    }
                    cameraAPI.closeConnection().subscribeOn(Schedulers.io()).subscribe()
                    wifiHandler.disconnect()
                    requireActivity().finish()
                }
                .setNegativeButton(R.string.alert_disconnect_camera_no, null)
                .show()
    }

    private fun onResultPicture(url: String) {
        // It's not possible to directly use ACTION_VIEW because the external app will use mobile
        // data network to try to download this image (by default) and it will fail. So we need to
        // use the current process to download and store it in a public directory.

        val request = Request.Builder()
                .get()
                .url(url)
                .build()
        val call = okHttpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val body = resp.body()
                    if (body != null) {
                        if (context == null) {
                            return
                        }
                        val uri = getUriForFile(requireContext(),
                                "com.thibaudperso.sonycamera.fileprovider",
                                temporaryPreviewPicture)
                        val intent = Intent()
                        intent.action = Intent.ACTION_VIEW
                        intent.setDataAndType(uri, "image/jpeg")
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        startActivityForResult(intent, PREVIEW_PICTURE_ACTIVITY_RESULT)
                        requireActivity().overridePendingTransition(0, 0)
                    }
                }
            }
        })
    }

    private fun startLiveView() {
        if (adjustments_liveview.isStarted) {
            logger.warn("Ignore start live stream")
            return
        }

        cameraAPI.startLiveView()
                .subscribeOn(Schedulers.io())
                .doOnSuccess { url -> adjustments_liveview.start(liveViewStreamService, url) }
                .subscribe()
    }

    private fun stopLiveView() {
        if (!adjustments_liveview.isStarted) {
            return
        }
        adjustments_liveview.stop()
        cameraAPI.stopLiveView().subscribeOn(Schedulers.io()).subscribe()
    }

}