package hu.nagyi.tracker.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import hu.nagyi.tracker.R
import hu.nagyi.tracker.location.MainLocationManager
import java.util.*

class ForegroundService : Service(), MainLocationManager.OnNewLocationAvailable {

    //region VARIABLES

    private val NOTIFICATION_CHANNEL_ID = "time_service_notifications"
    private val NOTIFICATION_CHANNEL_NAME = "Time Service notifications"
    private val NOTIF_FOREGROUND_ID = 101

    private lateinit var mainLocationManager: MainLocationManager

    private var enabled = false
    private var previousLocation: Location? = null
    private val seconds: Long = 30

    //endregion

    //region INNER CLASS TIME THREAD

    inner class TimeThread : Thread() {
        override fun run() {

            while (this@ForegroundService.enabled) {

                if (this@ForegroundService.previousLocation != null) {
                    this@ForegroundService.updateNotification(
                        this@ForegroundService.previousLocation!!.latitude,
                        this@ForegroundService.previousLocation!!.longitude
                    )
                }

                sleep(this@ForegroundService.seconds * 1000)
            }
        }
    }

    //endregion

    //region METHODS

    override fun onCreate() {
        super.onCreate()

        this.mainLocationManager = MainLocationManager(this, this)
        this.showLastKnownLocation()
        this.mainLocationManager.startLocationMonitoring()
    }

    private fun showLastKnownLocation() {
        this.mainLocationManager.getLastLocation { location ->
            this.previousLocation = location
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.startForeground(this.NOTIF_FOREGROUND_ID, this.getMyNotification(0.0, 0.0))

        if (!this.enabled) {
            this.enabled = true
            this.TimeThread().start()
        }

        return START_STICKY_COMPATIBILITY
    }

    private fun updateNotification(lat: Double, lon: Double) {
        val notification = this.getMyNotification(lat, lon)
        val notifMan = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        notifMan?.notify(this.NOTIF_FOREGROUND_ID, notification)
    }

    private fun getMyNotification(lat: Double, lon: Double): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.createNotificationChannel()
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon"))
        mapIntent.setPackage("com.google.android.apps.maps")
        mapIntent.resolveActivity(packageManager)?.let {
            startActivity(mapIntent)
        }

        if (this.previousLocation == null) {
            return NotificationCompat.Builder(this, this.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("There is no coordinates.. the app retries after ${this.seconds} seconds ")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVibrate(longArrayOf(1000, 2000, 1000))
                .build()
        } else {
            val contentIntent = PendingIntent.getActivity(
                this,
                this.NOTIF_FOREGROUND_ID,
                mapIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            return NotificationCompat.Builder(this, this.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("This is the coordinates")
                .setContentText("$lat,$lon")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVibrate(longArrayOf(1000, 2000, 1000))
                .setContentIntent(contentIntent).build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            this.NOTIFICATION_CHANNEL_ID,
            this.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        this.mainLocationManager.stopLocationMonitoring()
        this.stopForeground(false)
        this.enabled = false
        super.onDestroy()
    }

    override fun onNewLocation(location: Location) {
        this.previousLocation = location
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    //endregion

}