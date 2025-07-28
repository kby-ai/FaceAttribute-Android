package com.kbyai.faceattribute

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kbyai.facesdk.FaceDetectionParam
import com.kbyai.facesdk.FaceSDK
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CameraActivityKt : AppCompatActivity() {

    val TAG = CameraActivityKt::class.java.simpleName
    val PREVIEW_WIDTH = 720
    val PREVIEW_HEIGHT = 1280

    private lateinit var cameraView: CameraView
    private lateinit var faceView: FaceView
    private lateinit var fotoapparat: Fotoapparat
    private lateinit var context: Context

    private var recognized = false
    private var hasSavedBitmap = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_kt)

        context = this
        cameraView = findViewById(R.id.preview)
        faceView = findViewById(R.id.faceView)

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
        recognized = false
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fotoapparat.start()
        }
    }

    override fun onPause() {
        super.onPause()
        fotoapparat.stop()
        faceView.setFaceBoxes(null)
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

    private fun saveBitmapToFile(bitmap: Bitmap, context: Context) {
        val filename = "face_frame_${System.currentTimeMillis()}.png"
        val file = File(context.getExternalFilesDir(null), filename)

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Log.d("FaceFrameProcessor", "Saved bitmap to: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("FaceFrameProcessor", "Failed to save bitmap: ${e.message}", e)
        }
    }

    inner class FaceFrameProcessor : FrameProcessor {

        override fun process(frame: Frame) {

            if(recognized == true) {
                return
            }

            var cameraMode = 7  // 7: portrait mode, 2: landscape mode
            if (SettingsActivity.getCameraLens(context) == CameraSelector.LENS_FACING_BACK) {
                cameraMode = 6
            }

            val bitmap = FaceSDK.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, cameraMode)
// For testing, the below line saves one frame to mobile device.
//            if (!hasSavedBitmap) {
//                saveBitmapToFile(bitmap, context)
//                hasSavedBitmap = true
//            }


            val faceDetectionParam = FaceDetectionParam()
            faceDetectionParam.check_liveness = true
            faceDetectionParam.check_liveness_level = SettingsActivity.getLivenessLevel(context)
            val faceBoxes = FaceSDK.faceDetection(bitmap, faceDetectionParam)

            runOnUiThread {
//                faceView.setFrameSize(Size(bitmap.height, bitmap.width)) // landscape mode
                faceView.setFrameSize(Size(bitmap.width, bitmap.height)) // portrait mode
                faceView.setFaceBoxes(faceBoxes)
            }

            if(faceBoxes.size > 0) {
                val faceBox = faceBoxes[0]
                if (faceBox.liveness > SettingsActivity.getLivenessThreshold(context)) {
                    val templates = FaceSDK.templateExtraction(bitmap, faceBox)

                    var maxSimiarlity = 0f
                    var maximiarlityPerson: Person? = null
                    for (person in DBManager.personList) {
                        val similarity = FaceSDK.similarityCalculation(templates, person.templates)
                        if (similarity > maxSimiarlity) {
                            maxSimiarlity = similarity
                            maximiarlityPerson = person
                        }
                    }
                    if (maxSimiarlity > SettingsActivity.getIdentifyThreshold(context)) {
                        recognized = true
                        val identifiedPerson = maximiarlityPerson
                        val identifiedSimilarity = maxSimiarlity

                        runOnUiThread {
                            val faceImage = Utils.cropFace(bitmap, faceBox)
                            val intent = Intent(context, ResultActivity::class.java)
                            intent.putExtra("identified_face", faceImage)
                            intent.putExtra("enrolled_face", identifiedPerson!!.face)
                            intent.putExtra("identified_name", identifiedPerson!!.name)
                            intent.putExtra("similarity", identifiedSimilarity)
                            intent.putExtra("liveness", faceBox.liveness)
                            intent.putExtra("yaw", faceBox.yaw)
                            intent.putExtra("roll", faceBox.roll)
                            intent.putExtra("pitch", faceBox.pitch)
                            intent.putExtra("face_quality", faceBox.face_quality)
                            intent.putExtra("face_luminance", faceBox.face_luminance)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}