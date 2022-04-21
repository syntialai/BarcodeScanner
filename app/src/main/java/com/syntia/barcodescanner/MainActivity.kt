package com.syntia.barcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.syntia.barcodescanner.databinding.ActivityMainBinding
import com.syntia.barcodescanner.util.BarcodeScanner
import com.syntia.barcodescanner.util.BarcodeScannerListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), BarcodeScannerListener {

  companion object {
    private const val CAMERA_REQUEST_CODE = 101
    private const val TAG = "MAIN_ACTIVITY"
  }

  private lateinit var viewBinding: ActivityMainBinding

  private val executorService: ExecutorService by lazy {
    Executors.newSingleThreadExecutor()
  }

  private val barcodeScanner by lazy {
    BarcodeScanner(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

    setupCamera()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
      grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == CAMERA_REQUEST_CODE && isCameraPermissionGranted()) {
      startCamera()
    }
  }

  private fun setupCamera() {
    if (isCameraPermissionGranted()) {
      startCamera()
    } else {
      requestPermission()
    }
  }

  private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
      val cameraProvider = cameraProviderFuture.get()
      val preview = Preview.Builder().build().apply {
        setSurfaceProvider(viewBinding.pvCameraPreview.surfaceProvider)
      }
      val imageAnalyzer = ImageAnalysis.Builder().build().apply {
        setAnalyzer(executorService, getImageAnalyzerListener())
      }

      try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            this,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalyzer
        )
      } catch (throwable: Throwable) {
        Log.e(TAG, "Use case binding failed", throwable)
      }
    }, ContextCompat.getMainExecutor(this))
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun getImageAnalyzerListener(): Analyzer {
    return Analyzer { imageProxy ->
      val image = imageProxy.image ?: return@Analyzer
      val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
      barcodeScanner.scanImage(inputImage) {
        imageProxy.close()
      }
    }
  }

  override fun onSuccessScan(result: List<Barcode>) {
    result.forEachIndexed { index, barcode ->
      Toast.makeText(this, "Barcode value: ${barcode.rawValue}", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onScanFailed() {
    Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
  }

  private fun isCameraPermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(this,
        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
  }

  private fun requestPermission() {
    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),
        CAMERA_REQUEST_CODE)
  }

  override fun onDestroy() {
    super.onDestroy()
    barcodeScanner.closeScanner()
    executorService.shutdown()
  }
}