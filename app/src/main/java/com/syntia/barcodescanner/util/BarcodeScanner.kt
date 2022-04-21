package com.syntia.barcodescanner.util

import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScanner(private val barcodeScannerListener: BarcodeScannerListener) {

  private val barcodeScanner: BarcodeScanner by lazy {
    constructBarcodeScanner()
  }

  private val executorService: ExecutorService by lazy {
    Executors.newSingleThreadExecutor()
  }

  fun scanImage(inputImage: InputImage, onScanComplete: (() -> Unit)? = null) {
    barcodeScanner.process(inputImage)
        .addOnCompleteListener {
          onScanComplete?.invoke()
        }
        .addOnSuccessListener {
          barcodeScannerListener.onSuccessScan(it)
        }
        .addOnFailureListener {
          Log.e("Scanner fail", "caused:", it)
          barcodeScannerListener.onScanFailed()
        }
  }

  fun closeScanner() {
    barcodeScanner.close()
    executorService.shutdown()
  }

  private fun constructBarcodeScanner(): BarcodeScanner {
    val barcodeScannerOptions = BarcodeScannerOptions.Builder()
        .setExecutor(executorService)
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_PDF417
        )
        .build()
    return BarcodeScanning.getClient(barcodeScannerOptions)
  }
}