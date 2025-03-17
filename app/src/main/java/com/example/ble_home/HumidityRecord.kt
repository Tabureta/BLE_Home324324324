package com.example.ble_home

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
class HumidityRecord {
    @Id
    var id: Long = 0

    var humidity: Float = 0.0f

    var timestamp: Long = 0 // Для сортировки по времени
    var data: Long = 0

    // Конструктор по умолчанию
    constructor()

    // Конструктор с параметрами
    constructor(humidity: Float, timestamp: Long, data: Long) {
        this.humidity = humidity
        this.timestamp = timestamp
        this.data = data
    }
}