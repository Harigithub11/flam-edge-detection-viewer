package com.flam.edgeviewer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.util.Log
import com.flam.edgeviewer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"

        // Load native library
        init {
            System.loadLibrary("edgeviewer")
        }
    }

    // Native function declarations
    external fun stringFromJNI(): String
    external fun getOpenCVVersion(): String
    external fun testNativeProcessing(value: Int): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Test native functions
        testNativeFunctions()
    }

    private fun testNativeFunctions() {
        try {
            // Test basic JNI
            val jniMessage = stringFromJNI()
            Log.d(TAG, "JNI Message: $jniMessage")

            // Test OpenCV version
            val opencvVersion = getOpenCVVersion()
            Log.d(TAG, "OpenCV Version: $opencvVersion")

            // Test processing
            val testInput = 42
            val testOutput = testNativeProcessing(testInput)
            Log.d(TAG, "Test: $testInput * 2 = $testOutput")

            // Update UI
            binding.sampleText.text = """
                ✅ Native Library: OK
                ✅ OpenCV: $opencvVersion
                ✅ Test Processing: $testInput → $testOutput
            """.trimIndent()

        } catch (e: Exception) {
            Log.e(TAG, "Native function error", e)
            binding.sampleText.text = "❌ Error: ${e.message}"
        }
    }
}
