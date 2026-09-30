package com.arcticfishtrail.game.integration

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Log

/** Platform access for sub12 and sub14. Initialised once from Application.onCreate. */
object DeviceSignals {

    private const val TAG = "DeviceSignals"

    private val INSTRUMENTATION_CLASSES = listOf(
        "androidx.test.platform.app.InstrumentationRegistry",
        "androidx.test.espresso.Espresso",
        "org.robolectric.Robolectric",
    )

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** sub12. Read on demand - the charge does move, and the read is cheap. */
    val battery: String
        get() = runCatching {
            // A null receiver returns the sticky broadcast's last value immediately: it
            // registers nothing, needs no permission, and sidesteps Android 14's
            // requirement to declare RECEIVER_EXPORTED on a real registration.
            val intent = appContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ) ?: return@runCatching unknownBattery()

            BatteryState.describe(
                plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryState.STATUS_UNKNOWN),
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
            )
        }.getOrElse {
            Log.w(TAG, "Could not read the battery", it)
            // Still a value: the offer must be able to tell "no reading" from "no signal".
            unknownBattery()
        }

    /**
     * sub14. Cached deliberately rather than because the inputs are immutable - a debugger
     * can attach and USB debugging can be toggled mid-session, and sub14 flipping between
     * two URL builds in one run would be worse than a stale answer.
     */
    val isTestEnvironment: Boolean by lazy {
        runCatching { TestEnvironment.isTestEnvironment(snapshot()) }.getOrElse {
            // init() not called, or a ROM that throws on a Settings read. This getter runs
            // on the thread that builds the offer URL, so an escaping exception would kill
            // routing outright.
            Log.w(TAG, "Could not build the test-environment snapshot", it)
            false
        }
    }

    private fun unknownBattery(): String =
        BatteryState.describe(
            plugged = 0,
            status = BatteryState.STATUS_UNKNOWN,
            level = -1,
            scale = -1,
        )

    private fun snapshot(): TestEnvironment.Snapshot {
        val resolver = appContext.contentResolver
        return TestEnvironment.Snapshot(
            // Settings.Global reads need no permission - only writes do.
            firebaseTestLab = safe("firebase.test.lab") {
                Settings.Global.getString(resolver, "firebase.test.lab")
            },
            instrumentationPresent = INSTRUMENTATION_CLASSES.any(::isOnClasspath),
            debuggerConnected = safe("debugger") { Debug.isDebuggerConnected() } ?: false,
            debuggable = safe("debuggable") {
                (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } ?: false,
            adbEnabled = safe("adb") {
                Settings.Global.getString(resolver, Settings.Global.ADB_ENABLED)
            },
            hardware = Build.HARDWARE.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
            model = Build.MODEL.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            fingerprint = Build.FINGERPRINT.orEmpty(),
        )
    }

    /**
     * Every platform read is fenced: a vendor ROM that throws on one Settings lookup must
     * not take the whole signal down with it.
     */
    private fun <T> safe(what: String, block: () -> T): T? =
        runCatching(block)
            .onFailure { Log.w(TAG, "Could not read $what", it) }
            .getOrNull()

    /**
     * initialize = false: the one-argument overload runs the class's static initializer,
     * and a test class that throws from it would be reported absent while being present -
     * a false negative on exactly the signal this is looking for.
     */
    private fun isOnClasspath(className: String): Boolean =
        runCatching {
            Class.forName(className, false, DeviceSignals::class.java.classLoader)
        }.isSuccess
}
