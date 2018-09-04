package de.flynamic.mobileabis.inference

import android.os.SystemClock
import android.graphics.Bitmap
import android.util.Log
import android.app.Activity
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ImageClassifier(activity: Activity) {
    private val TAG = "INFO"
    private lateinit var tflite: Interpreter
    protected var imgData: ByteBuffer? = null
    private val DIM_BATCH_SIZE = 1
    private val DIM_PIXEL_SIZE = 1
    private var intValues = IntArray(getImageSizeX() * getImageSizeY())
    private var labelProbArray: Array<FloatArray>? = null

    val IMAGE_MEAN = 138f
    val IMAGE_STD = 53f

    init {
        tflite = Interpreter(loadModelFile(activity))
        imgData = ByteBuffer.allocateDirect(
                DIM_BATCH_SIZE
                        * getImageSizeX()
                        * getImageSizeY()
                        * DIM_PIXEL_SIZE
                        * getNumBytesPerChannel())
        imgData?.order(ByteOrder.nativeOrder())
        labelProbArray = Array(1) { FloatArray(124) }
        Log.d(TAG, "Created a Tensorflow Lite Image Classifier.")
    }

    /** Classifies a frame from the preview stream.  */
    fun classifyFrame(bitmap: Bitmap): List<Pair<Int, Float>> {
        convertBitmapToByteBuffer(bitmap)
        // Here's where the magic happens!!!
        val startTime = SystemClock.uptimeMillis()
        runInference()
        val endTime = SystemClock.uptimeMillis()
        Log.d(TAG, "Timecost to run model inference: " + java.lang.Long.toString(endTime - startTime))
        val result = labelProbArray!![0]
        val indices = result.indices.sortedWith(Comparator<Int> { i1: Int, i2: Int ->
            when {
                result[i1] > result[i2] -> 1
                result[i1] == result[i2] -> 0
                else -> -1
            }
        }).reversed()
        val labelProbPairs = mutableListOf<Pair<Int, Float>>()
        for (index in indices) {
            labelProbPairs.add(Pair(index, result[index]))
        }
        return labelProbPairs
    }

    protected fun runInference() {
        tflite.run(imgData!!, labelProbArray!!)
    }

    /** Writes Image data into a `ByteBuffer`.  */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) {
            return
        }
        imgData?.rewind()
        val scaled = Bitmap.createScaledBitmap(bitmap, getImageSizeX(), getImageSizeY(), true)
        scaled.getPixels(intValues, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        // Convert the image to floating point.
        var pixel = 0
        val startTime = SystemClock.uptimeMillis()
        for (i in 0 until getImageSizeX()) {
            for (j in 0 until getImageSizeY()) {
                val value = intValues[pixel++]
                addPixelValue(value)
            }
        }
        val endTime = SystemClock.uptimeMillis()
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + java.lang.Long.toString(endTime - startTime))
    }

    private fun addPixelValue(pixelValue: Int) {
        val r = (pixelValue shr 16 and 0xFF)
        val g = (pixelValue shr 8 and 0xFF)
        val b = (pixelValue and 0xFF)
        val intensity = (r + g + b) / 3
        imgData?.putFloat((intensity - IMAGE_MEAN) / IMAGE_STD)
    }

    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd("beenet_17.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getImageSizeX(): Int {
        return 256
    }

    fun getImageSizeY(): Int {
        return 256
    }

    fun getNumBytesPerChannel(): Int {
        return 4
    }
}