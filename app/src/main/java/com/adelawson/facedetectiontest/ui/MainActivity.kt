package com.adelawson.facedetectiontest.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.adelawson.facedetectiontest.DataBindingTriggerClass
import com.adelawson.facedetectiontest.R
import com.adelawson.facedetectiontest.ui.fragments.cameraCapture.CameraCaptureFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostContainer) as NavHostFragment
        val navController = navHostFragment.navController
        navController.graph = navController.navInflater.inflate(
            R.navigation.main_nav_graph
        )

//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.navHostContainer, CameraCaptureFragment.newInstance())
//                .commitNow()
//        }
    }
}