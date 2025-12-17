package com.example.jniproject

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.jniproject.jnidemo.JNIUtils

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var jniUtils = JNIUtils()
        Log.d(TAG, "onCreate: ${jniUtils.nativeString}")


    }
}