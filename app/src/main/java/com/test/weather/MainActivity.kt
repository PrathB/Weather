package com.test.weather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.permissionx.guolindev.PermissionX
import com.test.weather.databinding.ActivityMainBinding
import com.test.weather.models.WeatherResponse
import com.test.weather.network.WeatherService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog : Dialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        PermissionX.init(this)
            .permissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(deniedList, "Please grant permission to access" +
                        " precise location in App Settings", "OK", "Cancel")
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    if(isLocationEnabled()){
                        requestLocationData()
                    }
                    else{
                        showEnableLocationDialog()
                    }
                }else{
                    Toast.makeText(this, "Permission Denied: $deniedList", Toast.LENGTH_LONG).show()
                }
            }

    }

    private fun showEnableLocationDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Location Services Disabled")
        alertDialog.setMessage("Please enable location services to use this feature.")
        alertDialog.setPositiveButton("Enable") { _, _ ->
            // Open the device settings to enable location
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(settingsIntent)
            finish()
        }
        alertDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.show()
    }

    private fun isLocationEnabled() : Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE)
                as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000).apply {
            setMinUpdateDistanceMeters(200F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            val mLastLocation: Location? = p0.lastLocation
            val latitude = mLastLocation?.latitude
            val longitude = mLastLocation?.longitude

            Log.i("Latitude: ","$latitude")
            Log.i("Longitude: ","$longitude")

            getLocationWeatherDetails(latitude!!,longitude!!)
        }
    }

    private fun getLocationWeatherDetails(latitude : Double, longitude : Double){
        if(Constants.isNetworkAvailable(this)){
            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service: WeatherService = retrofit.create<WeatherService>(WeatherService::class.java)
            val listCall : Call<WeatherResponse> = service
                .getWeather(latitude,longitude,Constants.METRIC_UNIT,Constants.API_KEY)

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    hideProgressDialog()
                    if(response.isSuccessful) {
                        val weatherData: WeatherResponse? = response.body()
                        setupUI(weatherData!!)
                        Log.i("Response:","$weatherData")
                    }else{
                        val rc = response.code()
                        Log.e("Errorr:","$rc")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideProgressDialog()
                    Log.e("Errorr:","${t.message}")
                }
            }
            )
        }
        else{
            Toast.makeText(this,"No internet connection",Toast.LENGTH_LONG).show()
        }
    }

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog(){
        if(mProgressDialog!=null){
            mProgressDialog!!.dismiss()
            mProgressDialog = null
        }
    }

    private fun setupUI(weatherData : WeatherResponse){
        for(weather in weatherData.weather){
            var desc : String = weather.description
            desc = desc.uppercase()
            binding?.tvDescription?.text = desc

            when(weather.icon){
                "01d" -> {
                    binding?.ivMain?.setImageResource(R.drawable.sunny)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_clear_day))
                }
                "01n" -> {
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_clear_night))
                }

                "02d" -> {
                    binding?.ivMain?.setImageResource(R.drawable.cloud)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_cloudy_day))
                }

                "02n" -> {
                    binding?.ivMain?.setImageResource(R.drawable.cloud)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_cloudy_night))
                }
                "03d" -> {
                    binding?.ivMain?.setImageResource(R.drawable.cloud)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_cloudy_day))
                }
                "03n" -> {
                    binding?.ivMain?.setImageResource(R.drawable.cloud)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_cloudy_night))
                }
                "04d" -> {
                    binding?.ivMain?.setImageResource(R.drawable.cloud)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_cloudy_day))
                }
                "04n" -> {
                    binding?.ivMain?.setImageResource(R.drawable.cloud)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_cloudy_night))
                }
                "09d" -> {
                    binding?.ivMain?.setImageResource(R.drawable.rain)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_cloudy_day))
                }
                "09n" -> {
                    binding?.ivMain?.setImageResource(R.drawable.rain)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_cloudy_night))
                }
                "010d" -> {
                    binding?.ivMain?.setImageResource(R.drawable.rain)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_thunderstorm))
                }
                "010n" -> {
                    binding?.ivMain?.setImageResource(R.drawable.rain)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_thunderstorm))
                }
                "011d" -> {
                    binding?.ivMain?.setImageResource(R.drawable.storm)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_thunderstorm))
                }
                "011n" -> {
                    binding?.ivMain?.setImageResource(R.drawable.storm)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_thunderstorm))
                }
                "013d" -> {
                    binding?.ivMain?.setImageResource(R.drawable.snowflake)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_thunderstorm))
                }
                "013n" ->{
                    binding?.ivMain?.setImageResource(R.drawable.snowflake)
                    binding?.llMain?.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_thunderstorm))
                }
            }
        }
        binding?.tvCityName?.text = weatherData.name
        binding?.tvTemp?.text = buildString {
            append(weatherData.main.temp.toInt().toString())
            append("°ᶜ")
        }
        binding?.tvMaxMinValue?.text = buildString {
            append(weatherData.main.temp_max.toInt())
            append("°/")
            append(weatherData.main.temp_min.toInt())
            append("° C")
        }
        binding?.tvFeelsLikeValue?.text = buildString {
            append(weatherData.main.feels_like.toInt())
            append("°C")
        }

        binding?.tvWindSpeed?.text = buildString {
            append(weatherData.wind.speed)
            append(" m/s")
        }
        binding?.tvHumidity?.text = buildString {
            append(weatherData.main.humidity)
            append(" %")
        }
        binding?.tvPressure?.text = buildString {
            append(weatherData.main.pressure)
            append(" hPa")
        }
        binding?.tvVisibility?.text = buildString {
            append(weatherData.visibility.toDouble()/1000)
            append(" Km")
        }
        binding?.tvSunrise?.text = unixConverter(weatherData.sys.sunrise)
        binding?.tvSunset?.text = unixConverter(weatherData.sys.sunset)
    }

    private fun unixConverter(timex: Long) : String{
        val date = Date(timex*1000L)
        val sdf = SimpleDateFormat("hh:mm a", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}