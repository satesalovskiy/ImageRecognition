
package com.tsa.imagerecognition

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
//import android.support.v7.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar.title = "Pipsa"
         setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.findFragmentById(R.id.ux_fragment)
        //auth = FirebaseAuth.getInstance()
        //Toast.makeText(this, auth.uid, Toast.LENGTH_LONG).show()

        kek.setOnClickListener{
            val fragment: ARFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ARFragment
            fragment.addNewImage()
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        if(ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf("android.permission.WRITE_EXTERNAL_STORAGE"), 1)
            return
        }
    }


}
