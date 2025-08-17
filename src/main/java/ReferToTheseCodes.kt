/*package com.example.fingerprintjs_test

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fingerprintjs_test.ui.theme.FingerprintjstestTheme
import com.fingerprintjs.android.fingerprint.Fingerprinter
import com.fingerprintjs.android.fingerprint.FingerprinterFactory
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import androidx.annotation.WorkerThread
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fingerprintjs.android.fingerprint.info_providers.CpuInfo
import com.fingerprintjs.android.fingerprint.tools.hashers.Hasher
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.fingerprintjs.android.fingerprint.info_providers.SensorData
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import java.util.UUID
import com.google.firebase.installations.FirebaseInstallations
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.widget.TextView

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private lateinit var gyroscopeTextView: TextView

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                Log.d("GYRO", "X: $x, Y: $y, Z: $z")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getOrCreateInstallationId(this)
        firebaseInstanceGetorCreate(this)
        enableEdgeToEdge()

        setContent {
            var deviceId by remember { mutableStateOf("Loading...") }
            var fingerprint by remember { mutableStateOf("Loading...") }
            var gsfId by remember { mutableStateOf("Loading...") }
            var androidId by remember { mutableStateOf("Loading...") }
            var mediaDrmId by remember { mutableStateOf("Loading...") }
            var signals by remember { mutableStateOf<List<String>>(listOf("Loading...")) }
            val gyroX = remember { mutableFloatStateOf(0f) }
            val gyroY = remember { mutableFloatStateOf(0f) }
            val gyroZ = remember { mutableFloatStateOf(0f) }


            // Initialize the Fingerprinter
            val fingerprinter = FingerprinterFactory.create(this)

            // Gyroscope Handling
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            val sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        gyroX.value = it.values[0]
                        gyroY.value = it.values[1]
                        gyroZ.value = it.values[2]
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Handle accuracy changes if necessary
                }
            }

            // Start listening to sensor changes when the screen is active
            DisposableEffect(Unit) {
                sensor?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
                onDispose { sensorManager.unregisterListener(sensorEventListener) }
            }

            LaunchedEffect(Unit) {
                val (customFingerprint, extractedSignals) = customFp(fingerprinter)
                signals = extractedSignals.ifEmpty { listOf("No signals found") }
                fingerprint = customFingerprint ?: "Failed to generate fingerprint"
            }

            Log.d("TESTING", "RESTARTED APP!")

            // Get Device ID
            fingerprinter.getDeviceId(version = Fingerprinter.Version.V_5) { result ->
                deviceId = result.deviceId
                gsfId = result.gsfId
                androidId = result.androidId
                mediaDrmId = result.mediaDrmId
            }

            // Get Fingerprint (Unhashed)
            val hasher = object : Hasher {
                override fun hash(data: String): String {
                    return data // Return data as-is
                }
            }
            fingerprinter.getFingerprint(version = Fingerprinter.Version.V_5, hasher = hasher) { fp ->
                fingerprint = fp
            }

            // Set Content
            FingerprintjstestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        deviceId = deviceId,
                        fingerprint = fingerprint,
                        g = gsfId,
                        a = androidId,
                        m = mediaDrmId,
                        signals = signals,
                        gyroX = gyroX.value,
                        gyroY = gyroY.value,
                        gyroZ = gyroZ.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume(){
        super.onResume()
        gyroscope?.let {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorEventListener)
    }

    fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val x = event.values[0]  // Rotation around X-axis
            val y = event.values[1]  // Rotation around Y-axis
            val z = event.values[2]  // Rotation around Z-axis

            gyroscopeTextView.text = "Gyroscope Data:\nX: $x\nY: $y\nZ: $z"
        }
    }


}

fun getOrCreateInstallationId(context: Context) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val savedId = sharedPreferences.getString("device_uuid", null)

    if (savedId == null) { // First-time install: Generate new UUID
        val newUuid = UUID.randomUUID().toString()
        sharedPreferences.edit().putString("device_uuid", newUuid).apply()
        Log.d("DeviceUUID", "New Installation ID: $newUuid")
    } else {
        Log.d("DeviceUUID", "Using saved Installation ID: $savedId")
    }
}

fun firebaseInstanceGetorCreate (context: Context) {
    val sharedPreferences = context.getSharedPreferences("app_prefs",Context.MODE_PRIVATE)
    val savedId = sharedPreferences.getString("firebase_installation_id", null)

    if (savedId == null) {
        FirebaseInstallations.getInstance().id
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val installationId = task.result
                    sharedPreferences.edit().putString("firebase_installation_id", installationId).apply()
                    Log.d("FirebaseID", "New Installation ID: $installationId")
                } else {
                    Log.e("FirebaseID", "Failed to get installation ID", task.exception)
                }
            }
    } else {
        Log.d("FirebaseID", "Using saved installation ID: $savedId")
    }
}

@Composable
fun Greeting(
    deviceId: String, fingerprint: String, g: String, a: String, m: String, signals: List<String>, gyroX: Float,
    gyroY: Float,
    gyroZ: Float,  modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        item {
            Text(
                text = "Device Information",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item { StyledText("Device ID", deviceId) }
        item { StyledText("GSF ID", g) }
        item { StyledText("Android ID", a) }
        item { StyledText("Media DRM ID", m) }
        item { StyledText("gyroX", gyroX.toString()) }
        item { StyledText("gyroY", gyroY.toString()) }
        item { StyledText("gyroZ", gyroZ.toString()) }

        item {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Collected Signals",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(signals) { signal ->
            StyledText("Signal", signal)
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Generated Fingerprint",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item { StyledText("Fingerprint", fingerprint) }
    }
}


@Composable
fun StyledText(label: String, value: String) {
    Column(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .background(Color.LightGray, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(text = "$label:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = value, fontSize = 14.sp)
    }
}

@WorkerThread
fun buildCustomFingerprint(fingerprinter: Fingerprinter, infoList: List<String>): List<String> {
    val signalsProvider = fingerprinter.getFingerprintingSignalsProvider()
    signalsProvider?.let {
        listOf(
            it.developmentSettingsEnabledSignal,
            signalsProvider.procCpuInfoV2Signal,
            signalsProvider.dateFormatSignal,
            signalsProvider.procCpuInfoSignal
        )
    }
    // Add extra signals to infoList
    val customSignals = infoList.toMutableList()
    signalsProvider?.let {
        customSignals.add(it.regionCountrySignal.toString())
        customSignals.add(it.procCpuInfoSignal.toString())
        customSignals.add(it.applicationsListSignal.toString())
        //customSignals.add(it.)
    }

    Log.d("INFO:",customSignals.toString())

    return customSignals
}
/*
@WorkerThread
fun customFp(fingerprinter: Fingerprinter): String? {
    val signalsProvider = fingerprinter.getFingerprintingSignalsProvider()
    val neededSignals = signalsProvider?.let {
        listOf(
        it.developmentSettingsEnabledSignal,
        it.procCpuInfoSignal,
        it.dateFormatSignal
    )
    }

    return neededSignals?.let { fingerprinter.getFingerprint(fingerprintingSignals = it) }
}*/
@WorkerThread
fun customFp(fingerprinter: Fingerprinter): Pair<String?, List<String>> {
    Log.d("CustomFP", "customFp function was called!")

    val signalsProvider = fingerprinter.getFingerprintingSignalsProvider()
    if (signalsProvider == null) {
        Log.e("CustomFP", "Signals provider is null!")
        return Pair(null, listOf("Signals provider is unavailable"))
    }



    // Collect the actual FingerprintingSignal objects
    val developmentSettingsSignal = signalsProvider.developmentSettingsEnabledSignal
    val procCpuInfoSignal = signalsProvider.procCpuInfoSignal
    val dateFormatSignal = signalsProvider.dateFormatSignal
    val applicationsListSignal = signalsProvider.applicationsListSignal
    val abiTypeSignal = signalsProvider.abiTypeSignal
    val regionCountrySignal = signalsProvider.regionCountrySignal
    val accessibilityEnabledSignal = signalsProvider.accessibilityEnabledSignal
    val time12Or24Signal = signalsProvider.time12Or24Signal
    val androidVersionSignal = signalsProvider.androidVersionSignal
    val procCpuInfoV2Signal = signalsProvider.procCpuInfoV2Signal
    val abdEnabledSignal = signalsProvider.adbEnabledSignal
    val alarmAlertPathSignal = signalsProvider.alarmAlertPathSignal
    val availableLocalesSignal = signalsProvider.availableLocalesSignal
    val batteryFullCapacitySignal = signalsProvider.batteryFullCapacitySignal

// New signals added
    val batteryHealthSignal = signalsProvider.batteryHealthSignal
    val codecListSignal = signalsProvider.codecListSignal
    val coresCountSignal = signalsProvider.coresCountSignal
    val dataRomaingEnabledSignal = signalsProvider.dataRoamingEnabledSignal
    val defaultInputMethodSignal = signalsProvider.defaultInputMethodSignal
    val defaultLanguageSignal = signalsProvider.defaultLanguageSignal
    val developmentSettingsEnabledSignal = signalsProvider.developmentSettingsEnabledSignal
    val encryptionStatusSignal = signalsProvider.encryptionStatusSignal
    val endButtonBehaviourSignal = signalsProvider.endButtonBehaviourSignal
    val fingerprintSensorStatusSignal = signalsProvider.fingerprintSensorStatusSignal
    val fingerprintSignal = signalsProvider.fingerprintSignal
    val fontScaleSignal = signalsProvider.fontScaleSignal
    val glesVersionSignal = signalsProvider.glesVersionSignal
    val httpProxySignal = signalsProvider.httpProxySignal
    val inputDevicesSignal = signalsProvider.inputDevicesSignal
    val inputDevicesV2Signal = signalsProvider.inputDevicesV2Signal
    val isPinSecureEnabledSignal = signalsProvider.isPinSecurityEnabledSignal
    val kernelVersionSignal = signalsProvider.kernelVersionSignal
    val manufacturerNameSignal = signalsProvider.manufacturerNameSignal
    val modelNameSignal = signalsProvider.modelNameSignal
    val ringtoneSourceSignal = signalsProvider.ringtoneSourceSignal
    val rttCallingModeSignal = signalsProvider.rttCallingModeSignal
    val screenOffTimeoutSignal = signalsProvider.screenOffTimeoutSignal
    val sdkVersionSignal = signalsProvider.sdkVersionSignal
    val securityProviderSignal = signalsProvider.securityProvidersSignal
    val sensorsSignal = signalsProvider.sensorsSignal
    val systemApplicationListSignal = signalsProvider.systemApplicationsListSignal
    val textAutoPunctuateSignal = signalsProvider.textAutoPunctuateSignal
    val textAutoReplaceEnabledSignal = signalsProvider.textAutoReplaceEnabledSignal
    val timezoneSignal = signalsProvider.timezoneSignal
    val totalInternalStorageSpaceSignal = signalsProvider.totalInternalStorageSpaceSignal
    val totalRamSignal = signalsProvider.totalRamSignal
    val touchExplorationEnabledSignal = signalsProvider.touchExplorationEnabledSignal
    val transitionAnimationScaleSignal = signalsProvider.transitionAnimationScaleSignal
    val windowAnimationScaleSignal = signalsProvider.windowAnimationScaleSignal
    val sensorDetails = signalsProvider.sensorsSignal.value[0]

    Log.d("INPUT DEVICE INFO",inputDevicesSignal.value[0].name.toString())

    Log.d("HERERERERERERE", applicationsListSignal.value.toString())
    Log.d("HERERERERERERE", systemApplicationListSignal.value.toString())
    Log.d("HERERERERERERE", sensorDetails.sensorName.toString())

// Extract the values of each signal (this depends on how each signal works)
    val extractedSignals = listOf(
        "One Sensor DATA:${sensorDetails}",
        "Development Settings: ${developmentSettingsSignal.value}",
        "CPU Info: ${procCpuInfoSignal.value}",
        "Date Format: ${dateFormatSignal.value}",
        //"Applications List: ${applicationsListSignal.value}",
        "ABI Type: ${abiTypeSignal.value}",
        "Region Country: ${regionCountrySignal.value}",
        "Accessibility Enabled: ${accessibilityEnabledSignal.value}",
        "Time 12 or 24: ${time12Or24Signal.value}",
        "Android Version: ${androidVersionSignal.value}",
        //"CPU Info V2: ${procCpuInfoV2Signal.value}",
        "ABD Enabled: ${abdEnabledSignal.value}",
        "Alarm Alert Path: ${alarmAlertPathSignal.value}",
        "Available Locales: ${availableLocalesSignal.value}",
        "Battery Full Capacity: ${batteryFullCapacitySignal.value}",
        // New signals
        "Battery Health: ${batteryHealthSignal.value}",
        //"Codec List: ${codecListSignal.value}",
        "Cores Count: ${coresCountSignal.value}",
        "Data Roaming Enabled: ${dataRomaingEnabledSignal.value}",
        "Default Input Method: ${defaultInputMethodSignal.value}",
        "Default Language: ${defaultLanguageSignal.value}",
        "Development Settings Enabled: ${developmentSettingsEnabledSignal.value}",
        "Encryption Status: ${encryptionStatusSignal.value}",
        "End Button Behaviour: ${endButtonBehaviourSignal.value}",
        "Fingerprint Sensor Status: ${fingerprintSensorStatusSignal.value}",
        "Fingerprint: ${fingerprintSignal.value}",
        "Font Scale: ${fontScaleSignal.value}",
        "GLES Version: ${glesVersionSignal.value}",
        "HTTP Proxy: ${httpProxySignal.value}",
        "Input Devices: ${inputDevicesSignal.value}",
        "Input Devices V2: ${inputDevicesV2Signal.value}",
        "Is Pin Secure Enabled: ${isPinSecureEnabledSignal.value}",
        "Kernel Version: ${kernelVersionSignal.value}",
        "Manufacturer Name: ${manufacturerNameSignal.value}",
        "Model Name: ${modelNameSignal.value}",
        "Ringtone Source: ${ringtoneSourceSignal.value}",
        "RTT Calling Mode: ${rttCallingModeSignal.value}",
        "Screen Off Timeout: ${screenOffTimeoutSignal.value}",
        "SDK Version: ${sdkVersionSignal.value}",
        "Security Provider: ${securityProviderSignal.value}",
        "Sensors: ${sensorsSignal.value}",
        "System Application List: ${systemApplicationListSignal.value}",
        "Text Auto Punctuate: ${textAutoPunctuateSignal.value}",
        "Text Auto Replace Enabled: ${textAutoReplaceEnabledSignal.value}",
        "Time 12 or 24: ${time12Or24Signal.value}",
        "Timezone: ${timezoneSignal.value}",
        "Total Internal Storage Space: ${totalInternalStorageSpaceSignal.value}",
        "Total RAM: ${totalRamSignal.value}",
        "Touch Exploration Enabled: ${touchExplorationEnabledSignal.value}",
        "Transition Animation Scale: ${transitionAnimationScaleSignal.value}",
        "Window Animation Scale: ${windowAnimationScaleSignal.value}"
    )

    Log.d("CustomFP", "Extracted signals: $extractedSignals")

// Use the actual FingerprintingSignal objects for fingerprinting
    val actualSignals = listOf(
        developmentSettingsSignal,
        procCpuInfoSignal,
        dateFormatSignal,
        //applicationsListSignal,
        abiTypeSignal,
        regionCountrySignal,
        accessibilityEnabledSignal,
        time12Or24Signal,
        androidVersionSignal,
        procCpuInfoV2Signal,
        abdEnabledSignal,
        alarmAlertPathSignal,
        availableLocalesSignal,
        batteryFullCapacitySignal,
        // New signals
        batteryHealthSignal,
        codecListSignal,
        coresCountSignal,
        dataRomaingEnabledSignal,
        defaultInputMethodSignal,
        defaultLanguageSignal,
        developmentSettingsEnabledSignal,
        encryptionStatusSignal,
        endButtonBehaviourSignal,
        fingerprintSensorStatusSignal,
        fingerprintSignal,
        fontScaleSignal,
        glesVersionSignal,
        httpProxySignal,
        inputDevicesSignal,
        inputDevicesV2Signal,
        isPinSecureEnabledSignal,
        kernelVersionSignal,
        manufacturerNameSignal,
        modelNameSignal,
        ringtoneSourceSignal,
        rttCallingModeSignal,
        screenOffTimeoutSignal,
        sdkVersionSignal,
        securityProviderSignal,
        sensorsSignal,
        systemApplicationListSignal,
        textAutoPunctuateSignal,
        textAutoReplaceEnabledSignal,
        time12Or24Signal,
        timezoneSignal,
        totalInternalStorageSpaceSignal,
        totalRamSignal,
        touchExplorationEnabledSignal,
        transitionAnimationScaleSignal,
        windowAnimationScaleSignal
    )

    // Get the fingerprint using the actual signals
    val fingerprint = fingerprinter.getFingerprint(fingerprintingSignals = actualSignals).also {
        Log.d("CustomFP", "Generated fingerprint using extracted signals: $it")
    }

    return Pair(fingerprint, extractedSignals)
}




fun someFunc(fingerprinter: Fingerprinter){
    val hasher = object : Hasher {
        override fun hash(data: String): String {
            return data
        }
    }

    fingerprinter.getFingerprint(
        version = Fingerprinter.Version.V_5,
        hasher = hasher,
    ) {fingerprint -> println(fingerprint) }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FingerprintjstestTheme {
        Greeting(deviceId = "SampleDeviceID", g = "Sample g", a = "Sample a", m = "Sample m", fingerprint = "SampleFingerprint", signals = listOf("Sample Signal 1", "Sample Signal 2"), gyroX = 1f, gyroY = 1f, gyroZ = 1f)
    }
}*/