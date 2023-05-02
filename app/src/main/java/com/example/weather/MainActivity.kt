package com.example.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    var city: String = "sofia,bg"

    // This key is for weather and it's from openweathermap.org
    val api: String = "08e4af750d42b87ff1af1affee2ce3ee"

    private lateinit var leftIcon: ImageView
    private lateinit var rightIcon: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder

    private val REQUEST_LOCATION_PERMISSION = 1
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        val languagePreferences = getSharedPreferences("languagePreference", Context.MODE_PRIVATE)
        val currentLanguageCode = languagePreferences.getString("language_code", "en").toString()

        setLocale(currentLanguageCode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        leftIcon = findViewById(R.id.left_icon)
        rightIcon = findViewById(R.id.right_icon)

        val toolbarTitle  = findViewById<TextView>(R.id.toolbar_title)
        val changeLanguage = findViewById<LinearLayout>(R.id.languageRow)
        val switchBackground = findViewById<Switch>(R.id.switch_background)
        val inputEditText = findViewById<TextInputEditText>(R.id.edtCity)
        val searchIcon = findViewById<ImageView>(R.id.searchIcon)

        findViewById<TextView>(R.id.languageCode).text = currentLanguageCode

        val sharedPreferencesBackgroundSwitch = getSharedPreferences("backgroundSwitch", Context.MODE_PRIVATE)
        val isSwitchOn = sharedPreferencesBackgroundSwitch.getBoolean("isSwitchOn", false)

        findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
        findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE

        switchBackground.isChecked = isSwitchOn

        setBackground(isSwitchOn)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this, Locale.getDefault())

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION)
        } else {
            turnOnLocationServices()
        }

        switchBackground.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferencesBackgroundSwitch.edit()
            editor.putBoolean("isSwitchOn", isChecked)
            editor.apply()

            setBackground(isChecked)
        }

        changeLanguage.setOnClickListener {
            val languageSelectionDialog = LanguageSelectionDialogFragment()
            languageSelectionDialog.show(supportFragmentManager, "languageSelectionDialog")
        }

        leftIcon.setOnClickListener {
            toolbarTitle.text = "Weather"
            leftIcon.visibility = View.GONE
            rightIcon.visibility = View.VISIBLE

            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.settingsContainer).visibility = View.GONE
        }

        rightIcon.setOnClickListener {

            toolbarTitle.text = getString(R.string.settings)
            rightIcon.visibility = View.GONE
            leftIcon.visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<RelativeLayout>(R.id.settingsContainer).visibility = View.VISIBLE

        }

        searchIcon.setOnClickListener {
            searchCity(inputEditText.text.toString().trim())
        }

        inputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                searchCity(inputEditText.text.toString().trim())
                true
            } else {
                false
            }
        }
    }

    private fun searchCity(cityName: String) {
        lifecycleScope.launch {
            val (_, tempCountry) = findCity(cityName)

            city = "${cityName},$tempCountry".lowercase(Locale.ROOT)

            WeatherTask().execute()
        }
    }

    private fun setBackground(isChecked: Boolean) {
        if (isChecked) {
            findViewById<RelativeLayout>(R.id.mainContainer).setBackgroundResource(R.drawable.gradient_dark_mode_bg)
            findViewById<RelativeLayout>(R.id.settingsContainer).setBackgroundResource(R.drawable.gradient_dark_mode_bg)
        } else {
            findViewById<RelativeLayout>(R.id.mainContainer).setBackgroundResource(R.drawable.gradient_light_mode_bg)
            findViewById<RelativeLayout>(R.id.settingsContainer).setBackgroundResource(R.drawable.gradient_light_mode_bg)
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private suspend fun findCity(cityName: String): Pair<String?, String?> {
        val url = "https://geocode.xyz/$cityName?json=1"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        return withContext(Dispatchers.IO) {
            val response: Response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val jsonObject = JSONObject(responseBody)
                var country = ""
                if (jsonObject.has("standard")) {
                    val standardObject: JSONObject = jsonObject.getJSONObject("standard")
                    country = standardObject.optString("prov", "")
                }
                val city = jsonObject.optString("city", "")
                Pair(city, country)
            } else {
                null to null
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                turnOnLocationServices()
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class WeatherTask : AsyncTask<String, Void, String>(){
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            val response:String? = try{
                URL(
                    "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$api"
                ).readText(
                    Charsets.UTF_8
                )
            }catch (e: Exception){
                null
            }

            return response
        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                val updatedAt:Long = jsonObj.getLong("dt")
                val updatedAtText = getString(R.string.update_at) + ": "  + SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(updatedAt*1000))
                val temp = main.getString("temp")+"°C"
                val tempMin = getString(R.string.min_temp) + ": "  + main.getString("temp_min")+"°C"
                val tempMax = getString(R.string.max_temp) + ": " + main.getString("temp_max")+"°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")

                val sunrise:Long = sys.getLong("sunrise")
                val sunset:Long = sys.getLong("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")
                val bigIcon = weather.getString("icon")

                val address = jsonObj.getString("name")+", "+sys.getString("country")

                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.update_at).text =  updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.ROOT
                    ) else it.toString()
                }
                findViewById<TextView>(R.id.temp).text = temp
                findViewById<TextView>(R.id.temp_min).text = tempMin
                findViewById<TextView>(R.id.temp_max).text = tempMax
                findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise*1000))
                findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset*1000))
                findViewById<TextView>(R.id.wind).text = windSpeed
                findViewById<TextView>(R.id.pressure).text = pressure
                findViewById<TextView>(R.id.humidity).text = "$humidity %"
                findViewById<ImageView>(R.id.big_icon)


                Picasso.get().load("https://openweathermap.org/img/w/$bigIcon.png").into(
                    findViewById<ImageView>(R.id.big_icon)
                )

                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE

            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                println(location)
                if (location != null) {
                    val addresses: List<Address> = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    ) as List<Address>

                    if (addresses.isNotEmpty()) {
                        val address: Address = addresses[0]

                        val cityName: String? = address.locality
                        val countryName: String? = address.countryCode

                        city = "$cityName,$countryName".lowercase(Locale.ROOT)

                        WeatherTask().execute()
                    }
                } else {
                    val locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(10000)
                        .setFastestInterval(5000)
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            val newLocation: Location? = locationResult.lastLocation

                            val addresses: List<Address> = newLocation?.let {
                                geocoder.getFromLocation(
                                    it.latitude,
                                    newLocation.longitude,
                                    1
                                )
                            } as List<Address>

                            if (addresses.isNotEmpty()) {
                                val address: Address = addresses[0]
                                val cityName: String? = address.locality
                                val countryName: String? = address.countryCode
                                city = "$cityName,$countryName".lowercase(Locale.ROOT)
                                WeatherTask().execute()
                            }
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                }
            }
            .addOnFailureListener {
            }
    }

    private fun turnOnLocationServices() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation()
        } else {
            var gpsEnabled: Boolean

            AlertDialog.Builder(this)
                .setMessage("Please turn on GPS to use location services")
                .setPositiveButton("OK") { dialog, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    handler.postDelayed(object: Runnable {
                        override fun run() {
                            gpsEnabled = (getSystemService(LOCATION_SERVICE) as LocationManager)
                                .isProviderEnabled(LocationManager.GPS_PROVIDER)
                            if (gpsEnabled) {
                                getLocation()
                            } else {
                                handler.postDelayed(this, 1000)
                            }
                        }
                    }, 1000)
                }
                .show()
        }
    }
}