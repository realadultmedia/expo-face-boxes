package expo.modules.faceboxes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

/*
  Finds faces in a photo, on the phone. Nothing leaves the device.

  detectFaces(uri) answers with one box per face as fractions (0-1) of the
  photo the right way up, top-left origin, ready to draw over the photo as
  it is shown.
*/
class ExpoFaceBoxesModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoFaceBoxes")

    AsyncFunction("detectFaces") { uri: String ->
      val context = appContext.reactContext ?: throw FaceBoxesException("No app context.")
      val parsed = Uri.parse(uri)
      val resolver = context.contentResolver

      // Brought down to about 2048 px on the long side; faces stay findable.
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
      var sample = 1
      while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2048) sample *= 2
      val decoded = resolver.openInputStream(parsed)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
      } ?: throw FaceBoxesException("That photo could not be read.")

      // The pixels as stored, turned upright by the EXIF orientation, so the
      // boxes are measured on the photo as it is seen.
      val orientation = resolver.openInputStream(parsed)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      } ?: ExifInterface.ORIENTATION_NORMAL
      val upright = upright(decoded, orientation)

      val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
          .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
          .setMinFaceSize(0.05f)
          .build()
      )

      try {
        // Already on a background thread: wait for ML Kit's answer here.
        val faces = Tasks.await(detector.process(InputImage.fromBitmap(upright, 0)))
        val width = upright.width.toDouble()
        val height = upright.height.toDouble()

        faces.map { face ->
          val box = face.boundingBox
          val left = box.left.coerceAtLeast(0).toDouble()
          val top = box.top.coerceAtLeast(0).toDouble()
          mapOf(
            "x" to left / width,
            "y" to top / height,
            "width" to (box.right.toDouble().coerceAtMost(width) - left) / width,
            "height" to (box.bottom.toDouble().coerceAtMost(height) - top) / height,
          )
        }
      } finally {
        detector.close()
      }
    }
  }

  private fun upright(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
      ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
      else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
}

internal class FaceBoxesException(message: String) : CodedException(message)
