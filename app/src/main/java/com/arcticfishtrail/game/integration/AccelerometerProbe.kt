package com.arcticfishtrail.game.integration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object AccelerometerProbe {

    private const val TAG = "AccelerometerProbe"
    private const val SAMPLE_WINDOW_MS = 1_000L
    private const val AWAIT_TIMEOUT_SECONDS = 3L

    private val started = AtomicBoolean(false)
    private val ready = CountDownLatch(1)

    @Volatile private var first: FloatArray? = null
    @Volatile private var last: FloatArray? = null
    @Volatile private var sensorManager: SensorManager? = null

    /**
     * Sensor events and the cut-off both run here rather than on the main looper.
     *
     * The three-argument registerListener dispatches on the main thread, and stop() would
     * be posted there too - so anything that blocked the main thread while waiting for
     * sub13 would also be blocking the very events it is waiting for, deadlocking until
     * the timeout and returning "na_na". Its own looper makes the window independent of
     * whatever the caller's thread is doing.
     */
    private val samplingHandler: Handler by lazy {
        Handler(HandlerThread("accel-probe").apply { start() }.looper)
    }

    /**
     * Started from Application.onCreate rather than from the URL builder: the window is a
     * whole second, and by the time routing asks for sub13 the reading has to be there.
     */
    fun start(context: Context) {
        // compareAndSet, not a plain flag: a second start() would register a second
        // listener and leave one of them running for the life of the process.
        if (!started.compareAndSet(false, true)) return
        runCatching { beginSampling(context) }.onFailure {
            Log.w(TAG, "Could not start the accelerometer", it)
            // This runs inside Application.onCreate, so an unfenced throw from a vendor ROM
            // would crash the app at launch - and would leave the latch closed, so every
            // later await() would pay the full timeout.
            ready.countDown()
        }
    }

    private fun beginSampling(context: Context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) {
            Log.w(TAG, "No accelerometer available")
            // Open the latch at once - nothing is coming, and await() must not block for
            // three seconds to learn that.
            ready.countDown()
            return
        }

        val registered =
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, samplingHandler)
        if (!registered) {
            Log.w(TAG, "registerListener refused the accelerometer")
            ready.countDown()
            return
        }

        // Assigned only after a successful registration, so a refused probe does not hold a
        // SensorManager for the life of the process with nothing attached.
        sensorManager = manager
        samplingHandler.postDelayed(::stop, SAMPLE_WINDOW_MS)
    }

    /**
     * Blocks until the sampling window closes, capped at three seconds in case the URL is
     * assembled before it does. Background threads only - on the main thread this is a
     * three-second freeze, not a one-second one, because the wait outlives the window.
     */
    fun await(): String {
        if (!started.get()) {
            // Missing the Application.onCreate wiring is the likeliest single mistake in
            // this change: everything still compiles and runs, sub13 is just always na_na
            // and every URL build pays the timeout. Say so instead of stalling silently.
            Log.w(TAG, "await() before start() - AccelerometerProbe.start is missing from Application.onCreate")
            return MotionSample.describe(null, null)
        }
        runCatching { ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
            .onFailure {
                Log.w(TAG, "Interrupted while waiting for the accelerometer", it)
                Thread.currentThread().interrupt()
            }
        return MotionSample.describe(first, last)
    }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // SensorEvent.values is a buffer the framework reuses; without the copy both
            // fields would end up pointing at the same mutating array.
            val values = event?.values?.copyOf() ?: return
            // First event is the "at launch" reading; every later one replaces the "one
            // second in" reading, so what survives the cut-off is the latest sample.
            if (first == null) first = values else last = values
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun stop() {
        runCatching { sensorManager?.unregisterListener(listener) }
            .onFailure { Log.w(TAG, "unregisterListener failed", it) }
        // The sensor must not stay live for the life of the app - it costs battery and the
        // reading is only needed once.
        sensorManager = null
        ready.countDown()
    }
}
