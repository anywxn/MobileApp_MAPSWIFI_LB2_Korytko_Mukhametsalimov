package com.example.wifi_map

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Установка ключа (замените на ваш реальный ключ из кабинета разработчика)
        MapKitFactory.setApiKey("af09eda9-2250-4586-8e00-def0c0a591f5")

        // 2. Инициализация библиотеки перед созданием интерфейса
        MapKitFactory.initialize(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 3. Находим карту в разметке
        mapView = findViewById(R.id.mapview)

        // 4. Перемещаем камеру на начальную точку (например, центр города)
        // Координаты: широта, долгота, зум, поворот, наклон
        mapView.map.move(
            CameraPosition(Point(55.751244, 37.618423), 11.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 5f),
            null
        )

        // 5. Пример добавления метки Wi-Fi
        val mapObjects = mapView.map.mapObjects.addCollection()
        val wifiPoint = Point(55.751244, 37.618423) // Координаты для метки

        val placemark = mapObjects.addPlacemark(wifiPoint)
        placemark.setText("Бесплатный Wi-Fi")
    }

    // Обязательные методы жизненного цикла для Яндекс Карт:
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}