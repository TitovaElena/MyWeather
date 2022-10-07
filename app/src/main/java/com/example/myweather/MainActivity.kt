package com.example.myweather

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val baseUrl: String = "http://api.openweathermap.org/"
    private val apiKey: String = "8636fb8922bfc84a32396048ff4bd088"

    private fun keyProcessing() {
        val cityNameEditText: TextInputEditText = findViewById(R.id.cityNameET)
        val cityNameTextView: TextView = findViewById(R.id.cityNameTV)
        val btnNext: Button = findViewById(R.id.btnNext)

        btnNext.setOnClickListener {
            val cityName = cityNameEditText.text.toString()
            cityNameTextView.text = cityName
            launch {
                val loc = locDetermination(cityName)
                tempNow(loc.lat, loc.lon)
                weatherForecast(loc.lat, loc.lon)
            }
        }
    }

    private suspend inline fun <reified T : Any> request(url: String): T {
        val client = HttpClient(CIO)
        val response: HttpResponse = client.get("$baseUrl$url&appid=$apiKey")
        val responseBody: String = response.body()
        val format = Json { ignoreUnknownKeys = true }
        val obj = format.decodeFromString<T>(responseBody)

        client.close()

        return obj
    }

    //Определение местоположения (долготы и широты) по названию города
    private suspend fun locDetermination(cityName: String): Location {
        val urlGeo = "geo/1.0/direct?q=${cityName}&limit=1"
        val loc = request<List<Location>>(urlGeo)

        return loc[0]
    }

    //Вывод текущей погоды
    @SuppressLint("SetTextI18n")
    private suspend fun tempNow(latitude: Float, longitude: Float){
        val temp: TextView = findViewById(R.id.temperatureNow)
        val weather: TextView = findViewById(R.id.weatherNow)

        val urlCurrentWeather = "data/2.5/weather?lat=${latitude}" +
                "&lon=${longitude}&lang=ru&units=metric"
        val obj = request<WeatherIndicatorsNow>(urlCurrentWeather)

        val tempNow = roundingToBigDecimal(obj.main.temp)
        val weatherNow = obj.weather[0].description

        temp.text = "${tempNow}°C "
        weather.text = weatherNow
    }

    //Вывод данных о погоде по часам в указанном городе
    @SuppressLint("SetTextI18n", "WrongViewCast", "CutPasteId")
    private suspend fun weatherForecast(latitude: Float, longitude: Float) {
        val urlWeatherData = "data/2.5/forecast?lat=${latitude}" +
                "&lon=${longitude}&lang=ru&units=metric"
        val obj = request<WeatherData>(urlWeatherData)

        hourlyForecast(obj)
        dailyForecast(obj)
    }

    @SuppressLint("SetTextI18n")
    private fun hourlyForecast(obj: WeatherData) {
        val hourlyForecastList: MutableList<HourlyWeather> = mutableListOf()
        val hourlyForecastTbl: TableLayout = findViewById(R.id.hourlyWeatherTable)
        val param = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        val timeRow = TableRow(this)
        val tempRow = TableRow(this)
        val textSize = 24F
        val gravity = Gravity.CENTER_HORIZONTAL

        for (item in obj.list.take(9)) {
            val hours = changeDataFormat(item.dt_txt, "HH:mm")
            val hourlyTemp = roundingToBigDecimal(item.main.temp)
            hourlyForecastList.add(HourlyWeather(hours.toString(), hourlyTemp.toString()))
        }

        hourlyForecastTbl.removeAllViews()

        for (item in hourlyForecastList) {

            fillRow("${item.hour} ", timeRow, param, textSize, gravity)
            fillRow("${item.tempHour}°C ", tempRow, param, textSize, gravity)


        }
        hourlyForecastTbl.addView(timeRow)
        hourlyForecastTbl.addView(tempRow)
    }

    @SuppressLint("SetTextI18n")
    private fun dailyForecast(obj: WeatherData) {

        val dailyWeatherTbl: TableLayout = findViewById(R.id.dailyWeatherTable)
        val param = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        val textSize = 18F
        val gravity = Gravity.END

        val dailyForecastMap: MutableMap<String, DailyForecast> = mutableMapOf()

        for(item in obj.list) {
            val date = changeDataFormat(item.dt_txt, "dd.MM.yyyy")
            val dateStr = date.toString()
            if (!dailyForecastMap.contains(dateStr)){
                dailyForecastMap[dateStr] = DailyForecast(dateStr, item.weather[0].description, item.main.temp, item.main.temp)
                continue
            }else{
                if(dailyForecastMap[dateStr]?.minTemp!! > item.main.temp){
                    dailyForecastMap[dateStr]?.minTemp = item.main.temp
                }
                if(dailyForecastMap[dateStr]?.maxTemp!! < item.main.temp){
                    dailyForecastMap[dateStr]?.maxTemp = item.main.temp
                }
            }

        }
        println(dailyForecastMap)

        dailyWeatherTbl.removeAllViews()

        for (item in dailyForecastMap.values) {
            val dailyWeatherRow = TableRow(this)
            val tempMax = roundingToBigDecimal(item.maxTemp)
            val tempMin = roundingToBigDecimal(item.minTemp)

            fillRow("${item.date} ", dailyWeatherRow, param, textSize, gravity)
            fillRow("${item.weather} ", dailyWeatherRow, param, textSize, gravity)
            fillRow("$tempMax / ", dailyWeatherRow, param, textSize, gravity)
            fillRow("$tempMin°C", dailyWeatherRow, param, textSize, gravity)

            dailyWeatherTbl.addView(dailyWeatherRow)
        }
    }

    private fun changeDataFormat(dateTxt: String, outputPattern: String): String? {
        val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTxt, pattern)
        val formatter = DateTimeFormatter.ofPattern(outputPattern)

        return formatter.format(localDateTime)
    }

    private fun fillRow (text: String, tableRow: TableRow, params: TableRow.LayoutParams,
                         textSize: Float, gravity: Int) {
        val textView = TextView(this)
        textView.text = text
        textView.textSize = textSize
        textView.gravity = gravity
        tableRow.addView(textView, params)
    }

    private fun roundingToBigDecimal(number: Float): BigDecimal? {
        return number.toBigDecimal().setScale(0, RoundingMode.DOWN)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        keyProcessing()
    }
}
