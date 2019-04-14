package de.flynamic.mobileabis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import de.flynamic.mobileabis.inference.ImageClassifier

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.FileInputStream

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class InferencePerformanceTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("de.flynamic.mobileabis", appContext.packageName)
    }

    @Test
    fun inferenceSpeed() {
        val context = InstrumentationRegistry.getTargetContext()
        val classifier = ImageClassifier(context.assets)
        val genera = context.assets.list("test")
        var inferenceTime: Long = 0
        var inferences = 0
        for (genus in genera!!) {
            val speciesList = context.assets.list("test/$genus")
            for (species in speciesList!!) {
                val files = context.assets.list("test/$genus/$species")
                for (file in files!!) {
                    val inputStream = context.assets.open("test/$genus/$species/$file")
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val startTime = SystemClock.uptimeMillis()
                    classifier.classifyFrame(bitmap)
                    val endTime = SystemClock.uptimeMillis()
                    inferenceTime += endTime - startTime
                    inferences++
                    Log.d("TEST", "Processed inference #$inferences")
                }
            }
        }
        val speed = inferenceTime / inferences
        Log.d("TEST", "Average inference speed: $speed ms")
    }
}
