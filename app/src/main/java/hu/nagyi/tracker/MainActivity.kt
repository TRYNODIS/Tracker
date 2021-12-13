package hu.nagyi.tracker

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import hu.nagyi.tracker.databinding.ActivityMainBinding
import hu.nagyi.tracker.services.ForegroundService

class MainActivity : AppCompatActivity() {

    //region VARIABLES

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    lateinit var binding: ActivityMainBinding

    //endregion

    //region METHODS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.binding = ActivityMainBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)

        val intentMyTimeService = Intent(this, ForegroundService::class.java)
        this.binding.btnStart.setOnClickListener {
            this.startService(intentMyTimeService)
        }
        this.binding.btnStop.setOnClickListener {
            this.stopService(intentMyTimeService)
        }


        if (this.allPermissionsGranted()) {
            this.setViews()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this.baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (this.allPermissionsGranted()) {
                this.setViews()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setViews() {
        this.binding.btnStart.isEnabled = true
        this.binding.btnStop.isEnabled = true
    }

    //endregion

}