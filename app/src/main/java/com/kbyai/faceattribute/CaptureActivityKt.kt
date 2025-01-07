package com.kbyai.faceattribute

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kbyai.faceattribute.SettingsActivity.Companion.getEyecloseThreshold
import com.kbyai.faceattribute.SettingsActivity.Companion.getLivenessLevel
import com.kbyai.faceattribute.SettingsActivity.Companion.getLivenessThreshold
import com.kbyai.faceattribute.SettingsActivity.Companion.getMouthopenThreshold
import com.kbyai.faceattribute.SettingsActivity.Companion.getOcclusionThreshold
import com.kbyai.faceattribute.SettingsActivity.Companion.getPitchThreshold
import com.kbyai.faceattribute.SettingsActivity.Companion.getRollThreshold
import com.kbyai.faceattribute.SettingsActivity.Companion.getYawThreshold
import com.kbyai.facesdk.FaceBox
import com.kbyai.facesdk.FaceDetectionParam
import com.kbyai.facesdk.FaceSDK
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CaptureActivityKt : AppCompatActivity(), CaptureView.ViewModeChanged {

    val TAG = CaptureActivityKt::class.java.simpleName
    val PREVIEW_WIDTH = 720
    val PREVIEW_HEIGHT = 1280

    private lateinit var fotoapparat: Fotoapparat
    private lateinit var context: Context

    private lateinit var cameraView: CameraView

    private lateinit var captureView: CaptureView

    private lateinit var warningTxt: TextView

    private lateinit var livenessTxt: TextView

    private lateinit var qualityTxt: TextView

    private lateinit var luminaceTxt: TextView

    private lateinit var lytCaptureResult: ConstraintLayout

    private var capturedBitmap: Bitmap? = null

    private var capturedFace: FaceBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_kt)

        context = this
        cameraView = findViewById(R.id.preview)
        captureView = findViewById(R.id.captureView)
        warningTxt = findViewById(R.id.txtWarning)
        livenessTxt = findViewById(R.id.txtLiveness)
        qualityTxt = findViewById(R.id.txtQuality)
        luminaceTxt = findViewById(R.id.txtLuminance)
        lytCaptureResult = findViewById(R.id.lytCaptureResult)

        captureView.setViewModeInterface(this)
        captureView.setViewMode(CaptureView.VIEW_MODE.NO_FACE_PREPARE)

        findViewById<View>(R.id.buttonEnroll).setOnClickListener {
            val faceImage = Utils.cropFace(capturedBitmap, capturedFace)
            val templates = FaceSDK.templateExtraction(capturedBitmap, capturedFace)

            val dbManager = DBManager(context)
            val min = 10000
            val max = 20000
            val random = Random().nextInt((max - min) + 1) + min

            dbManager.insertPerson("Person$random", faceImage, templates)
            Toast.makeText(context, getString(R.string.person_enrolled), Toast.LENGTH_SHORT).show()
            finish()
        }

        fotoapparat = Fotoapparat.with(this)
            .into(cameraView)
            .lensPosition(front())
            .frameProcessor(FaceFrameProcessor())
            .previewResolution { Resolution(PREVIEW_HEIGHT,PREVIEW_WIDTH) }
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            fotoapparat.start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fotoapparat.start()
        }
    }

    override fun onPause() {
        super.onPause()
        fotoapparat.stop()
        captureView.setFaceBoxes(null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                fotoapparat.start()
            }
        }
    }

    override fun view5_finished() {
        val param = FaceDetectionParam()
        param.check_liveness = true
        param.check_liveness_level = getLivenessLevel(this)

        val faceBoxes = FaceSDK.faceDetection(capturedBitmap, param)
        if (faceBoxes != null && faceBoxes.size > 0) {
            if (faceBoxes[0].liveness > getLivenessThreshold(context)) {
                val msg = String.format("Liveness: Real, score = %.03f", faceBoxes[0].liveness)
                livenessTxt.text = msg
            } else {
                val msg = String.format("Liveness: Spoof, score =  %.03f", faceBoxes[0].liveness)
                livenessTxt.text = msg
            }
        }

        if (capturedFace!!.face_quality < 0.5f) {
            val msg = String.format("Quality: Low, score = %.03f", capturedFace!!.face_quality)
            qualityTxt.text = msg
        } else if (capturedFace!!.face_quality < 0.75f) {
            val msg = String.format("Quality: Medium, score = %.03f", capturedFace!!.face_quality)
            qualityTxt.text = msg
        } else {
            val msg = String.format("Quality: High, score = %.03f", capturedFace!!.face_quality)
            qualityTxt.text = msg
        }

        val msg = String.format("Luminance: %.03f", capturedFace!!.face_luminance)
        luminaceTxt.text = msg

        lytCaptureResult.visibility = View.VISIBLE
    }

    fun checkFace(faceBoxes: List<FaceBox>?, context: Context?): FACE_CAPTURE_STATE {
        if (faceBoxes == null || faceBoxes.size == 0) return FACE_CAPTURE_STATE.NO_FACE

        if (faceBoxes.size > 1) {
            return FACE_CAPTURE_STATE.MULTIPLE_FACES
        }

        val faceBox = faceBoxes[0]
        var faceLeft = Float.MAX_VALUE
        var faceRight = 0f
        var faceBottom = 0f
        for (i in 0..67) {
            faceLeft = min(faceLeft.toDouble(), faceBox.landmarks_68[i * 2].toDouble()).toFloat()
            faceRight =
                max(faceRight.toDouble(), faceBox.landmarks_68[i * 2].toDouble()).toFloat()
            faceBottom =
                max(faceBottom.toDouble(), faceBox.landmarks_68[i * 2 + 1].toDouble()).toFloat()
        }

        val sizeRate = 0.30f
        val interRate = 0.03f
        val frameSize = Size(CaptureActivity.PREVIEW_WIDTH, CaptureActivity.PREVIEW_HEIGHT)
        val roiRect = CaptureView.getROIRect(frameSize)
        val centerY = ((faceBox.y2 + faceBox.y1) / 2).toFloat()
        val topY = centerY - (faceBox.y2 - faceBox.y1) * 2 / 3
        val interX =
            (max(0.0, (roiRect.left - faceLeft).toDouble()) + max(
                0.0,
                (faceRight - roiRect.right).toDouble()
            )).toFloat()
        val interY =
            (max(0.0, (roiRect.top - topY).toDouble()) + max(
                0.0,
                (faceBottom - roiRect.bottom).toDouble()
            )).toFloat()
        if (interX / roiRect.width() > interRate || interY / roiRect.height() > interRate) {
            return FACE_CAPTURE_STATE.FIT_IN_CIRCLE
        }

        if (interX / roiRect.width() > interRate || interY / roiRect.height() > interRate) {
            return FACE_CAPTURE_STATE.FIT_IN_CIRCLE
        }

        if ((faceBox.y2 - faceBox.y1) * (faceBox.x2 - faceBox.x1) < roiRect.width() * roiRect.height() * sizeRate) {
            return FACE_CAPTURE_STATE.MOVE_CLOSER
        }

        if (abs(faceBox.yaw.toDouble()) > getYawThreshold(context!!) || abs(
                faceBox.roll.toDouble()
            ) > getRollThreshold(context!!) || abs(faceBox.pitch.toDouble()) > getPitchThreshold(
                context!!
            )
        ) {
            return FACE_CAPTURE_STATE.NO_FRONT
        }

        if (faceBox.face_occlusion > getOcclusionThreshold(context!!)) {
            return FACE_CAPTURE_STATE.FACE_OCCLUDED
        }

        if (faceBox.left_eye_closed > getEyecloseThreshold(context!!) ||
            faceBox.right_eye_closed > getEyecloseThreshold(context!!)
        ) {
            return FACE_CAPTURE_STATE.EYE_CLOSED
        }

        if (faceBox.mouth_opened > getMouthopenThreshold(context!!)) {
            return FACE_CAPTURE_STATE.MOUTH_OPENED
        }

        return FACE_CAPTURE_STATE.CAPTURE_OK
    }

    inner class FaceFrameProcessor : FrameProcessor {

        override fun process(frame: Frame) {

            if(captureView.viewMode == CaptureView.VIEW_MODE.NO_FACE_PREPARE) {
                return
            }

            var cameraMode = 7
            if (SettingsActivity.getCameraLens(context) == CameraSelector.LENS_FACING_BACK) {
                cameraMode = 6
            }

            val bitmap = FaceSDK.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, cameraMode)

            val faceDetectionParam = FaceDetectionParam()
            faceDetectionParam.check_face_occlusion = true
            faceDetectionParam.check_eye_closeness = true
            faceDetectionParam.check_mouth_opened = true
            val faceBoxes = FaceSDK.faceDetection(bitmap, faceDetectionParam)

            val faceCaptureState = CaptureActivity.checkFace(faceBoxes, context)

            if (captureView.viewMode == CaptureView.VIEW_MODE.REPEAT_NO_FACE_PREPARE) {
                if (faceCaptureState.compareTo(FACE_CAPTURE_STATE.NO_FACE) > 0) {
                    runOnUiThread { captureView.setViewMode(CaptureView.VIEW_MODE.TO_FACE_CIRCLE) }
                }
            } else if (captureView.viewMode == CaptureView.VIEW_MODE.FACE_CIRCLE) {
                runOnUiThread {
                    captureView.setFrameSize(Size(bitmap.width, bitmap.height))
                    captureView.setFaceBoxes(faceBoxes)
                    if (faceCaptureState == FACE_CAPTURE_STATE.NO_FACE) {
                        warningTxt.text = ""

                        captureView.setViewMode(CaptureView.VIEW_MODE.FACE_CIRCLE_TO_NO_FACE)
                    } else if (faceCaptureState == FACE_CAPTURE_STATE.MULTIPLE_FACES) warningTxt.text =
                        "Multiple face detected!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.FIT_IN_CIRCLE) warningTxt.text =
                        "Fit in circle!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.MOVE_CLOSER) warningTxt.text =
                        "Move closer!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.NO_FRONT) warningTxt.text =
                        "Not fronted face!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.FACE_OCCLUDED) warningTxt.text =
                        "Face occluded!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.EYE_CLOSED) warningTxt.text =
                        "Eye closed!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.MOUTH_OPENED) warningTxt.text =
                        "Mouth opened!"
                    else if (faceCaptureState == FACE_CAPTURE_STATE.SPOOFED_FACE) warningTxt.text =
                        "Spoof face"
                    else {
                        warningTxt.text = ""
                        captureView.setViewMode(CaptureView.VIEW_MODE.FACE_CAPTURE_PREPARE)

                        capturedBitmap = bitmap
                        capturedFace = faceBoxes[0]
                        captureView.setCapturedBitmap(capturedBitmap)
                    }
                }
            } else if (captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_PREPARE) {
                if (faceCaptureState == FACE_CAPTURE_STATE.CAPTURE_OK) {
                    if (faceBoxes[0].face_quality > capturedFace!!.face_quality) {
                        capturedBitmap = bitmap
                        capturedFace = faceBoxes[0]
                        captureView.setCapturedBitmap(capturedBitmap)
                    }
                }
            } else if (captureView.viewMode == CaptureView.VIEW_MODE.FACE_CAPTURE_DONE) {
                runOnUiThread { fotoapparat.stop() }
            }
        }
    }
}