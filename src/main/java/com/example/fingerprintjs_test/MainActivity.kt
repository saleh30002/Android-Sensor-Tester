package com.example.fingerprintjs_test

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.CaseMap.Title
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.internal.ManufacturerUtils
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.StringJoiner
import java.util.TimeZone
import kotlin.concurrent.thread
import kotlin.math.log


class MainActivity : AppCompatActivity(), SensorEventListener {

    // UI Components
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var infoPage: LinearLayout
    private lateinit var vibratePage: LinearLayout
    private lateinit var shakePage: LinearLayout

    // Info Page Component
    private lateinit var nameEditText: EditText
    private lateinit var sabanciIdEditText: EditText
    private lateinit var saveButton: Button

    // Page 1 Components (Vibration Test)
    private lateinit var situationSpinner1: Spinner
    private lateinit var vibrateButton: Button
    private lateinit var sensorDataText1: TextView
    private lateinit var clearDataButton1: Button

    // Page 2 Components (Infinity Test)
    private lateinit var situationSpinner2: Spinner
    private lateinit var infinityButton: Button
    private lateinit var countdownText: TextView
    private lateinit var instructionText: TextView
    private lateinit var sensorDataText2: TextView
    private lateinit var clearDataButton2: Button

    // Sensors and Vibration
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var vibrator: Vibrator

    // Data storage
    private val vibrationTestData = mutableListOf<String>()
    private val infinityTestData = mutableListOf<String>()
    private var isVibrationRecording = false
    private var isInfinityRecording = false
    private var countdownTimer: CountDownTimer? = null

    // Predefined lists
    private val situations = arrayOf(
        "Sitting, phone in hand",
        "Phone on a flat surface (ex: table)",
        "Phone on an inclined/declined surface",
        "Standing, phone in hand",
        "Walking, phone in hand",
        "Running, phone in hand",
        "In a vehicle, phone in hand",
        "In a vehicle, phone on flat surface"
    )

    private val manufacturers = arrayOf(
        "Samsung",
        "Apple",
        "Google",
        "OnePlus",
        "Xiaomi",
        "Huawei",
        "LG",
        "Sony",
        "Motorola",
        "Nokia",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupUI()
        setupSensors()
        setupVibrator()
        setupSpinners()
        setupBottomNavigation()

        // Request vibration permission for newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.VIBRATE), 1)
            }
        }
    }

    private fun saveUserInfo(
        name: String,
        sabanciId: String
    ) {
        val prefs = getSharedPreferences("user_info", MODE_PRIVATE)
        prefs.edit()
            .putString("name", name)
            .putString("sabanci_id", sabanciId)
            .apply()
    }

    private fun loadUserInfo(): Map<String, String>{
        val prefs = getSharedPreferences("user_info", MODE_PRIVATE)
        return mapOf(
            "name" to (prefs.getString("name", "") ?: ""),
            "sabanci_id" to (prefs.getString("sabanci_id", "") ?: "")
        )
    }

    private fun setupUI() {
        // Create info page layout
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            //setPadding(32, 32, 32, 32)
        }

        infoPage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        val page0Content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply{
            text="User Information"
            textSize=20f
        }

        nameEditText = EditText(this).apply {
            hint="Name"
            inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        sabanciIdEditText = EditText(this).apply {
            hint="12345"
            inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }


        saveButton = Button(this).apply {
            text = "Save Info"
            setOnClickListener{
                saveUserInfo(
                    nameEditText.text.toString(),
                    sabanciIdEditText.text.toString(),
                )
                Toast.makeText(this@MainActivity, "User info saved!", Toast.LENGTH_SHORT).show()
            }
        }

        page0Content.addView(title)
        page0Content.addView(nameEditText)
        page0Content.addView(sabanciIdEditText)
        page0Content.addView(saveButton)

        scrollView.addView(page0Content)
        infoPage.addView(scrollView)

        // Create vibration page layout
        val vibrateLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Create Page 1 (Vibration Test)
        vibratePage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val scrollView1 = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val page1Content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Page 1 Components
        val title1 = TextView(this).apply {
            text = "VIBRATION TEST"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        }

        /*val nameLabel1 = TextView(this).apply {
            text = "Enter your name:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }*/

        /*nameEditText1 = EditText(this).apply {
            hint = "Your name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }*/

        val situationLabel1 = TextView(this).apply {
            text = "Current situation:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }

        situationSpinner1 = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
        }

       /* val manufacturerLabel1 = TextView(this).apply {
            text = "Phone manufacturer:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }*/

       /* manufacturerSpinner1 = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
        }*/

       /* val phoneModelLabel1 = TextView(this).apply {
            text = "Phone model:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }*/

        /*phoneModelEditText1 = EditText(this).apply {
            hint = "e.g., Galaxy S21, iPhone 14"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
*/
        vibrateButton = Button(this).apply {
            text = "START VIBRATION TEST"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 24, 0, 0)
            setOnClickListener { handleVibrateButton() }
        }

        val sensorTitle1 = TextView(this).apply {
            text = "Sensor Data:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }

        sensorDataText1 = TextView(this).apply {
            text = "No data recorded yet."
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200
            )
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        sensorDataText1.movementMethod = android.text.method.ScrollingMovementMethod()
        sensorDataText1.isVerticalScrollBarEnabled = true

        clearDataButton1 = Button(this).apply {
            text = "CLEAR VIBRATION DATA"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 0)
            setOnClickListener { clearVibrationData() }
        }

        // Add components to Page 1
        page1Content.addView(title1)
//        page1Content.addView(nameLabel1)
//        page1Content.addView(nameEditText1)
        page1Content.addView(situationLabel1)
        page1Content.addView(situationSpinner1)
//        page1Content.addView(manufacturerLabel1)
//        page1Content.addView(manufacturerSpinner1)
//        page1Content.addView(phoneModelLabel1)
//        page1Content.addView(phoneModelEditText1)
        page1Content.addView(vibrateButton)
        page1Content.addView(sensorTitle1)
        page1Content.addView(sensorDataText1)
        page1Content.addView(clearDataButton1)

        scrollView1.addView(page1Content)
        vibratePage.addView(scrollView1)

        // Create Page 2 (Infinity Test)
        shakePage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            visibility = View.GONE
        }

        val scrollView2 = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val page2Content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Page 2 Components
        val title2 = TextView(this).apply {
            text = "INFINITY MOVEMENT TEST"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        }

       /* val nameLabel2 = TextView(this).apply {
            text = "Enter your name:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }*/

       /* nameEditText2 = EditText(this).apply {
            hint = "Your name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }*/

        val situationLabel2 = TextView(this).apply {
            text = "Current situation:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }

        situationSpinner2 = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
        }

        /*val manufacturerLabel2 = TextView(this).apply {
            text = "Phone manufacturer:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }*/

       /* manufacturerSpinner2 = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
        }*/

        /*val phoneModelLabel2 = TextView(this).apply {
            text = "Phone model:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }*/

       /* phoneModelEditText2 = EditText(this).apply {
            hint = "e.g., Galaxy S21, iPhone 14"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }*/

        instructionText = TextView(this).apply {
            text = "Instructions: Hold your phone and move it in the shape of an infinity " +
                    "symbol (∞) continuously for 10 seconds."
            textSize = 14f
            setPadding(16, 24, 16, 16)
            setBackgroundColor(0xFFE3F2FD.toInt())
        }

        infinityButton = Button(this).apply {
            text = "START INFINITY TEST"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 0)
            setOnClickListener { handleInfinityButton() }
        }

        countdownText = TextView(this).apply {
            text = ""
            textSize = 24f
            setPadding(0, 16, 0, 16)
            visibility = View.GONE
        }

        val sensorTitle2 = TextView(this).apply {
            text = "Sensor Data:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }

        sensorDataText2 = TextView(this).apply {
            text = "No data recorded yet."
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200
            )
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        sensorDataText2.movementMethod = android.text.method.ScrollingMovementMethod()
        sensorDataText2.isVerticalScrollBarEnabled = true

        clearDataButton2 = Button(this).apply {
            text = "CLEAR INFINITY DATA"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 0)
            setOnClickListener { clearInfinityData() }
        }

        // Add components to Page 2
        page2Content.addView(title2)
//        page2Content.addView(nameLabel2)
//        page2Content.addView(nameEditText2)
        page2Content.addView(situationLabel2)
        page2Content.addView(situationSpinner2)
//        page2Content.addView(manufacturerLabel2)
//        page2Content.addView(manufacturerSpinner2)
//        page2Content.addView(phoneModelLabel2)
//        page2Content.addView(phoneModelEditText2)
        page2Content.addView(instructionText)
        page2Content.addView(infinityButton)
        page2Content.addView(countdownText)
        page2Content.addView(sensorTitle2)
        page2Content.addView(sensorDataText2)
        page2Content.addView(clearDataButton2)

        scrollView2.addView(page2Content)
        shakePage.addView(scrollView2)

        // Create Bottom Navigation
        bottomNavigation = BottomNavigationView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Add all to main layout
        infoLayout.addView(infoPage)
        infoLayout.addView(vibratePage)
        infoLayout.addView(shakePage)
        infoLayout.addView(bottomNavigation)

        setContentView(infoLayout)
    }

    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (accelerometer == null && gyroscope == null) {
            Toast.makeText(this, "No accelerometer or gyroscope found on this device", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun setupSpinners() {
        // Setup situation spinners
        val situationAdapter1 = ArrayAdapter(this, android.R.layout.simple_spinner_item, situations)
        situationAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        situationSpinner1.adapter = situationAdapter1

        val situationAdapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, situations)
        situationAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        situationSpinner2.adapter = situationAdapter2

        // Setup manufacturer spinners
        /*val manufacturerAdapter1 = ArrayAdapter(this, android.R.layout.simple_spinner_item, manufacturers)
        manufacturerAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        manufacturerSpinner1.adapter = manufacturerAdapter1

        val manufacturerAdapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, manufacturers)
        manufacturerAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        manufacturerSpinner2.adapter = manufacturerAdapter2*/
    }



    private fun setupBottomNavigation() {
        val menu = bottomNavigation.menu
        menu.add(0, R.id.nav_info, 0, "Info").setIcon(R.drawable.baseline_backup_24)
        menu.add(0, R.id.nav_vibration, 0, "Vibration Test").setIcon(R.drawable.baseline_vibration_24)
        menu.add(0, R.id.nav_infinity, 0, "Infinity Test").setIcon(R.drawable.baseline_10k_24)

        //bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED)
        //bottomNavigation.setBackgroundColor(0xFF6200EE.toInt())
        bottomNavigation.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        bottomNavigation.itemTextColor = ColorStateList.valueOf(Color.WHITE)
        bottomNavigation.itemIconTintList = ColorStateList.valueOf(Color.WHITE)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_info -> {
                    infoPage.visibility = View.VISIBLE
                    vibratePage.visibility = View.GONE
                    shakePage.visibility = View.GONE
                    updateInfoDisplay()
                    true
                }
                R.id.nav_vibration -> {
                    infoPage.visibility = View.GONE
                    vibratePage.visibility = View.VISIBLE
                    shakePage.visibility = View.GONE
                    updateVibrationDisplay()
                    true
                }
                R.id.nav_infinity -> {
                    infoPage.visibility = View.GONE
                    vibratePage.visibility = View.GONE
                    shakePage.visibility = View.VISIBLE
                    updateInfinityDisplay()
                    true
                }
                else -> false
            }
        }

        // Select first tab by default
        bottomNavigation.selectedItemId = R.id.nav_info
    }

  /*  private fun handleVibrateButton() {
        if (!isVibrationRecording) {
            // Validate input
            if (nameEditText1.text.isBlank()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return
            }

            if (phoneModelEditText1.text.isBlank()) {
                Toast.makeText(this, "Please enter your phone model", Toast.LENGTH_SHORT).show()
                return
            }

            startVibrationTest()
        } else {
            stopVibrationTest()
        }
    }*/

    private fun handleVibrateButton() {
        if (!isVibrationRecording) {

            val prefs = getSharedPreferences("user_info", MODE_PRIVATE)
            val savedName = prefs.getString("name", "") ?: ""
            val savedId = prefs.getString("sabanci_id", "") ?: ""

            if (savedName.isBlank() || savedId.isBlank()){
                Toast
                    .makeText(this,
                        "There is no name or ID data stored for this device. " +
                                "Please enter your name and ID in the info page to proceed.",
                        Toast.LENGTH_LONG)
                    .show()
                return
            }

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setTitle("Use saved information?")
                .setMessage("The following data is stored on the phone:\n\nName: $savedName\nID: $savedId" +
                        "\n\nDo you want to use this information for this test?")
                .setPositiveButton("Yes"){_, _ ->
                    startVibrationTest()
                }
                .setNegativeButton("No"){_, _->
                    Toast
                        .makeText(this,
                            "Please update your information from the user info page to proceed.",
                            Toast.LENGTH_SHORT)
                        .show()
                }


            val dialog : AlertDialog = builder.create()
            dialog.show()

        } else {
            stopVibrationTest()
        }
    }

   /* private fun handleInfinityButton() {
        if (!isInfinityRecording) {
            // Validate input
            if (nameEditText2.text.isBlank()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return
            }

            if (phoneModelEditText2.text.isBlank()) {
                Toast.makeText(this, "Please enter your phone model", Toast.LENGTH_SHORT).show()
                return
            }

            startInfinityTest()
        }
    }*/

    private fun handleInfinityButton() {
        if (!isInfinityRecording) {
            val prefs = getSharedPreferences("user_info", MODE_PRIVATE)
            val savedName = prefs.getString("name", "") ?: ""
            val savedId = prefs.getString("sabanci_id", "") ?: ""

            if (savedName.isBlank() || savedId.isBlank()){
                Toast
                    .makeText(this,
                        "There is no name or ID data stored for this device. " +
                                "Please enter your name and ID in the info page to proceed.",
                        Toast.LENGTH_LONG)
                    .show()
                return
            }

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setTitle("Use saved information?")
                .setMessage("The following data is stored on the phone:\n\nName: $savedName\nID: $savedId" +
                        "\n\nDo you want to use this information for this test?")
                .setPositiveButton("Yes"){_, _ ->
                    startInfinityTest()
                }
                .setNegativeButton("No"){_, _->
                    Toast
                        .makeText(this,
                            "Please update your information from the user info page to proceed.",
                            Toast.LENGTH_SHORT)
                        .show()
                }


            val dialog : AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun startVibrationTest() {
        isVibrationRecording = true
        vibrateButton.isEnabled = false
        //vibrateButton.text = "STOP VIBRATION TEST"

        // Add session info
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
       /* val sessionInfo = """
            --- VIBRATION TEST SESSION: $timestamp ---
            Name: ${nameEditText1.text}
            Situation: ${situationSpinner1.selectedItem}
            Manufacturer: ${manufacturerSpinner1.selectedItem}
            Phone Model: ${phoneModelEditText1.text}
            Available Sensors: ${if (accelerometer != null) "Accelerometer " else ""}${if (gyroscope != null) "Gyroscope" else ""}
            --- SENSOR DATA ---
        """.trimIndent()

        vibrationTestData.add(sessionInfo)*/

        // Start sensor recording
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Start vibration with custom pattern
        startCustomVibration()

        Toast.makeText(this, "Vibration test started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopVibrationTest() {
        /*isVibrationRecording = false
        vibrateButton.text = "START VIBRATION TEST"

        // Stop sensors
        sensorManager.unregisterListener(this)

        // Stop vibration
        vibrator.cancel()

        vibrationTestData.add("--- VIBRATION TEST ENDED ---\n")
        updateVibrationDisplay()*/

        if(!isVibrationRecording) return

        isVibrationRecording = false
        vibrateButton.isEnabled = true
        vibrator.cancel()

        updateVibrationDisplay()

        Toast.makeText(this, "Vibration test completed!", Toast.LENGTH_SHORT).show()

        sendDataToServer(vibrationTestData, "vibrate")

    }

    private fun startInfinityTest() {
        isInfinityRecording = true
        infinityButton.text = "TEST IN PROGRESS..."
        infinityButton.isEnabled = false
        countdownText.visibility = View.VISIBLE

        // Add session info
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
       /* val sessionInfo = """
            --- INFINITY TEST SESSION: $timestamp ---
            Name: ${nameEditText2.text}
            Situation: ${situationSpinner2.selectedItem}
            Manufacturer: ${manufacturerSpinner2.selectedItem}
            Phone Model: ${phoneModelEditText2.text}
            Available Sensors: ${if (accelerometer != null) "Accelerometer " else ""}${if (gyroscope != null) "Gyroscope" else ""}
            --- SENSOR DATA ---
        """.trimIndent()

        infinityTestData.add(sessionInfo)*/

        // Start sensor recording
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Start 10-second countdown
        countdownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                countdownText.text = "Time remaining: $secondsLeft seconds"
            }

            override fun onFinish() {
                stopInfinityTest()
            }
        }.start()

        Toast.makeText(this, "Start moving your phone in infinity shape!", Toast.LENGTH_LONG).show()
    }

    private fun stopInfinityTest() {
        isInfinityRecording = false
        infinityButton.text = "START INFINITY TEST"
        infinityButton.isEnabled = true
        countdownText.visibility = View.GONE

        // Stop sensors FIRST
        sensorManager.unregisterListener(this)

        // Then vibrate to indicate end of test
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
        updateInfinityDisplay()

        countdownTimer?.cancel()
        Toast.makeText(this, "Infinity test completed!", Toast.LENGTH_SHORT).show()

        sendDataToServer(infinityTestData, "infinity-shake")
    }

    private fun startCustomVibration() {
        var totalDuration: Long =  0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create custom vibration pattern: 500ms vibrate, 200ms pause, 300ms vibrate, 500ms pause, repeat
            val pattern = longArrayOf(0, 500, 200, 300, 500, 800, 100, 200, 300)
            val amplitudes = intArrayOf(0, 255, 0, 180, 0, 255, 0, 100, 150)
            totalDuration = pattern.sum()
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
            vibrator.vibrate(effect)
        } else {
            // Fallback for older versions
            val pattern = longArrayOf(0, 500, 200, 300, 500, 800, 100, 200, 300)
            totalDuration = pattern.sum()
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }

        object : CountDownTimer(totalDuration, totalDuration) {
            override fun onTick(millisUntilFinished: Long){}
            override fun onFinish(){
                stopVibrationTest()
            }
        }.start() //random comment to force a commit
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isVibrationRecording && !isInfinityRecording) return

        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val sensorType = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> "ACC"
            Sensor.TYPE_GYROSCOPE -> "GYR"
            else -> "UNK"
        }

        val data = "$timestamp [$sensorType] X: ${String.format("%.3f", event.values[0])}, " +
                "Y: ${String.format("%.3f", event.values[1])}, " +
                "Z: ${String.format("%.3f", event.values[2])}"

        if (isVibrationRecording) {
            vibrationTestData.add(data)
            // Limit data to prevent memory issues
            if (vibrationTestData.size > 1000) {
                vibrationTestData.removeAt(1) // Keep the header
            }
        } else if (isInfinityRecording) {
            infinityTestData.add(data)
            // Limit data to prevent memory issues
            if (infinityTestData.size > 1000) {
                infinityTestData.removeAt(1) // Keep the header
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun updateInfoDisplay(){
        val prefs = getSharedPreferences("user_info", MODE_PRIVATE)
        val savedName = prefs.getString("name", "") ?: ""
        val savedId = prefs.getString("sabanci_id", "") ?: ""

        nameEditText.setText(savedName)
        sabanciIdEditText.setText(savedId)
    }

    private fun updateVibrationDisplay() {
        if (vibrationTestData.isEmpty()) {
            sensorDataText1.text = "No vibration test data recorded yet."
        } else {
            sensorDataText1.text = vibrationTestData.takeLast(20).joinToString("\n")
        }
    }

    private fun updateInfinityDisplay() {
        if (infinityTestData.isEmpty()) {
            sensorDataText2.text = "No infinity test data recorded yet."
        } else {
            sensorDataText2.text = infinityTestData.takeLast(20).joinToString("\n")
        }
    }

    private fun clearVibrationData() {
        vibrationTestData.clear()
        updateVibrationDisplay()
        Toast.makeText(this, "Vibration test data cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearInfinityData() {
        infinityTestData.clear()
        updateInfinityDisplay()
        Toast.makeText(this, "Infinity test data cleared", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun sendDataToServer(sensorReadings: MutableList<String>, testType: String){
        val prefs = getSharedPreferences("user_info", MODE_PRIVATE)
        val name = prefs.getString("name", "") ?: ""
        val sabanciId = prefs.getString("sabanci_id", "") ?: ""

        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        val sensorDataJson = sensorReadings.joinToString(
            prefix = "[", postfix = "]", separator = ","
        ) { "\"$it\"" }

        var currentSituation = "N/A"
        if (testType == "vibrate") {
            currentSituation = situationSpinner1.selectedItem.toString()
        } else if (testType == "infinity-shake"){
            currentSituation = situationSpinner2.selectedItem.toString()
        }

        val jsonObject = JSONObject().apply {
            put("name", name)
            put("sabanci_id", sabanciId)
            put("situation", currentSituation)
            put("manufacturer", manufacturer)
            put("model", model)
            put("timestamp", getCurrentTimestamp())
            put("sensorReadings", JSONArray(sensorReadings))
            put("testType", testType)
        }

        val jsonData = jsonObject.toString() // Final JSON string

        Thread {
            try {
                //val url = URL("http://10.51.52.197:8000/upload-sensor-data")
                val url = URL("https://b0cfcdfba96a.ngrok-free.app/upload-sensor-data")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Tupe", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(jsonData.toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                val responseMessage = conn.inputStream.bufferedReader().use{ it.readText() }

                runOnUiThread{
                    Toast
                        .makeText(
                            this,
                            "Server response $responseMessage",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            } catch (e: Exception){
                Log.e("SendDataError", "Error sending data to the server: ${e.message}")
                e.printStackTrace()
                runOnUiThread{
                    Toast
                        .makeText(
                            this,
                            "Error sending data to the server ${e.message}",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }
        }.start()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (isVibrationRecording || isInfinityRecording) {
            sensorManager.unregisterListener(this)
            vibrator.cancel()
        }
        countdownTimer?.cancel()
    }
}








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