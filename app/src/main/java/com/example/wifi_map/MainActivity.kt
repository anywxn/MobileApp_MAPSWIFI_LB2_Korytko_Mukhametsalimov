package com.example.wifi_map

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import android.app.AlertDialog
import android.widget.EditText
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject

class MainActivity : AppCompatActivity() {

    data class WifiPoint(
        val lat: Double,
        val lon: Double,
        val name: String
    )

    private lateinit var mapView: MapView
    private lateinit var wifiPoints: MapObjectCollection
    private val PREFS_NAME = "wifi_prefs"
    private val KEY_POINTS = "points"


    private val inputListener = object : InputListener{
        override fun onMapTap(map: Map, point: Point){
            //Ничего не делает
        }

        override fun onMapLongTap(map: Map, point: Point) {
            showAddDialog(point)
        }
    }

    private val tapListener = MapObjectTapListener { mapObject, _ ->
        val placemark = mapObject as PlacemarkMapObject

        val name = placemark.userData as? String ?: "Wi-Fi"
        val point = placemark.geometry

        showPointDialog(placemark, point, name)
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Установка ключа (замените на ваш реальный ключ из кабинета разработчика)
        MapKitFactory.setApiKey("af09eda9-2250-4586-8e00-def0c0a591f5")

        // 2. Инициализация библиотеки перед созданием интерфейса
        MapKitFactory.initialize(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 3. Находим карту в разметке
        mapView = findViewById(R.id.mapview)
        wifiPoints = mapView.map.mapObjects.addCollection()

        val startPoint = Point(55.751244, 37.618423)

        // 4. Перемещаем камеру на начальную точку (например, центр города)
        // Координаты: широта, долгота, зум, поворот, наклон
        mapView.map.move(
            CameraPosition(startPoint, 11.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 5f),
            null
        )

        // 5. Пример добавления метки Wi-Fi
        addWifiPoint(startPoint, "Бесплатный Wi-Fi")
        mapView.map.addInputListener(inputListener)

        loadPoints().forEach {
            val point = Point(it.lat, it.lon)
            addWifiPoint(point, it.name)
        }

    }

    private fun addWifiPoint(point: Point, title: String = "Wi-Fi") {
        wifiPoints.addPlacemark().apply {
            geometry = point

            setIcon(
                ImageProvider.fromResource(
                    this@MainActivity,
                    android.R.drawable.ic_menu_mylocation
                )
            )

            setText(title, TextStyle())

            userData = title   //  сохраняем название
            addTapListener(tapListener) //  обработчик
        }
    }

    private fun showAddDialog(point: Point) {
        val editText = EditText(this)
        editText.hint = "Название Wi-Fi"

        AlertDialog.Builder(this)
            .setTitle("Добавить точку")
            .setView(editText)
            .setPositiveButton("Добавить") { _, _ ->
                val name = editText.text.toString().ifEmpty { "Wi-Fi" }

                addWifiPoint(point, name)
                savePoint(point, name)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun savePoint(point: Point, name: String) {
        val existing = loadPoints().toMutableList()
        existing.add(WifiPoint(point.latitude, point.longitude, name))
        saveAllPoints(existing)
    }


    private fun showPointDialog(
        placemark: PlacemarkMapObject,
        point: Point,
        name: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("Точка Wi-Fi")
            .setMessage("Название: $name")
            .setPositiveButton("Редактировать") { _, _ ->
                showEditDialog(placemark, point, name)
            }
            .setNeutralButton("Удалить") { _, _ ->
                deletePoint(placemark, point, name)
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showEditDialog(
        placemark: PlacemarkMapObject,
        point: Point,
        oldName: String
    ) {
        val editText = EditText(this)
        editText.setText(oldName)

        AlertDialog.Builder(this)
            .setTitle("Редактировать точку")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = editText.text.toString().ifEmpty { "Wi-Fi" }

                updatePoint(placemark, point, oldName, newName)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updatePoint(
        placemark: PlacemarkMapObject,
        point: Point,
        oldName: String,
        newName: String
    ) {
        // обновляем на карте
        placemark.setText(newName, TextStyle())
        placemark.userData = newName

        // обновляем в памяти
        val points = loadPoints().toMutableList()

        for (i in points.indices) {
            val p = points[i]

            if (p.lat == point.latitude &&
                p.lon == point.longitude &&
                p.name == oldName
            ) {
                points[i] = WifiPoint(p.lat, p.lon, newName)
                break
            }
        }

        saveAllPoints(points)
    }

    private fun deletePoint(
        placemark: PlacemarkMapObject,
        point: Point,
        name: String
    ) {
        // 1. Удаляем с карты
        wifiPoints.remove(placemark)

        // 2. Удаляем из памяти
        val points = loadPoints().toMutableList()

        val iterator = points.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()

            if (p.lat == point.latitude &&
                p.lon == point.longitude &&
                p.name == name
            ) {
                iterator.remove()
                break
            }
        }

        saveAllPoints(points)
    }

    private fun saveAllPoints(points: List<WifiPoint>) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = com.google.gson.Gson()

        val json = gson.toJson(points)
        prefs.edit().putString(KEY_POINTS, json).apply()
    }

    private fun loadPoints(): List<WifiPoint> {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = com.google.gson.Gson()

        val json = prefs.getString(KEY_POINTS, null) ?: return emptyList()

        val type = com.google.gson.reflect.TypeToken
            .getParameterized(List::class.java, WifiPoint::class.java)
            .type

        return gson.fromJson(json, type)
    }




    // Обязательные методы жизненного цикла для Яндекс Карт:
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.map.removeInputListener(inputListener)
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}