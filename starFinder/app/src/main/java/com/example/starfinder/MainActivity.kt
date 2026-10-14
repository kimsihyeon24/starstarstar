package com.example.starfinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var previewView: PreviewView
    private lateinit var overlay: StarOverlayView
    private lateinit var flash: View
    private lateinit var sensorManager: SensorManager
    private var locationManager: LocationManager? = null
    private val rot = FloatArray(9)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startEverything() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        previewView = findViewById(R.id.previewView)
        overlay = findViewById(R.id.overlay)
        flash = findViewById(R.id.flash)
        // 화면을 사진으로 뽑기 쉬운 방식으로 카메라 미리보기 설정
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        findViewById<View>(R.id.shutter).setOnClickListener { takePhoto() }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        overlay.verticalFovDeg = readCameraVerticalFov()

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun has(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun startEverything() {
        if (has(Manifest.permission.CAMERA)) startCamera()
        if (has(Manifest.permission.ACCESS_FINE_LOCATION) ||
            has(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) startLocation()
    }

    // ---------- 카메라 ----------
    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun readCameraVerticalFov(): Float {
        return try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.first {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK
            }
            val ch = cm.getCameraCharacteristics(id)
            val focal = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!![0]
            val size = ch.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!
            val longSide = maxOf(size.width, size.height)
            Math.toDegrees(2 * atan(longSide / (2.0 * focal))).toFloat()
        } catch (e: Exception) {
            70f
        }
    }

    // ---------- 사진 촬영 ----------
    // 카메라 화면 + 별·별자리 그림을 한 장으로 합쳐서 갤러리에 저장
    @SuppressLint("ThreadConstraint")
    private fun takePhoto() {
        val cam = previewView.bitmap
        if (cam == null || overlay.width == 0) {
            Toast.makeText(this, "카메라 화면을 아직 못 가져왔어요", Toast.LENGTH_SHORT).show()
            return
        }
        val out = Bitmap.createBitmap(overlay.width, overlay.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(cam, null, Rect(0, 0, out.width, out.height), null)
        overlay.draw(canvas)

        // 찰칵 효과
        flash.alpha = 0.8f
        flash.animate().alpha(0f).setDuration(250).start()

        val ok = saveToGallery(out)
        Toast.makeText(
            this,
            if (ok) "갤러리 > Pictures/StarFinder에 저장했어요" else "저장에 실패했어요",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveToGallery(bmp: Bitmap): Boolean {
        val name = "StarFinder_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StarFinder")
            }
        }
        return try {
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            contentResolver.openOutputStream(uri)?.use {
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, it)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    // ---------- 위치 ----------
    @SuppressLint("MissingPermission")
    private fun startLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = lm
        val last = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        last?.let { onLocationChanged(it) }

        for (p in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
            if (lm.isProviderEnabled(p)) {
                lm.requestLocationUpdates(p, 30_000L, 50f, this)
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        overlay.setLocation(location.latitude, location.longitude, location.altitude)
    }

    // ---------- 센서 ----------
    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rot, event.values.copyOf(4))
        overlay.updateRotation(rot)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            overlay.needsCalibration = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
        }
    }
}