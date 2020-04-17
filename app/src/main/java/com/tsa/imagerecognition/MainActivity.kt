
package com.tsa.imagerecognition

import android.os.Bundle
import android.widget.Toast
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.findFragmentById(R.id.ux_fragment)
        auth = FirebaseAuth.getInstance()
        Toast.makeText(this, auth.uid, Toast.LENGTH_LONG).show()
    }
}
