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
        "None",
        "Sitting, phone in hand",
        "Phone on a flat surface (ex: table)",
        "Phone on an inclined surface",
        "Phone on a declined surface",
        "Standing, phone in hand",
        "Walking, phone in hand",
        "Running, phone in hand",
        "In a vehicle, phone in hand",
        "In a vehicle, phone on flat surface"
    )

    private val manufacturers = arrayOf(
        "Samsung",
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

        val noneIndex = situations.indexOf("None")
        situationSpinner1.setSelection(noneIndex)

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

        situationSpinner2.setSelection(noneIndex)

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
                                "Please enter your name and ID in the info page to proceed.",
                        Toast.LENGTH_LONG)
                    .show()
                return
            }

            if (situationSpinner1.selectedItem.toString() == "None") {
                Toast
                    .makeText(
                        this,
                        "Please pick your current situation to proceed with the test.",
                        Toast.LENGTH_LONG
                    )
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

            if (situationSpinner2.selectedItem.toString() == "None") {
                Toast
                    .makeText(
                        this,
                        "Please pick your current situation to proceed with the test.",
                        Toast.LENGTH_LONG
                    )
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
        }.start() //test comment
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