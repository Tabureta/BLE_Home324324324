package com.example.ble_home

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
class TemperatureRecord {

    @Id
    var id: Long = 0

    var temperature: Float = 0.0f

    var timestamp: Long = 0 // Для сортировки по времени
    var data: Long = 0

    // Конструктор по умолчанию
    constructor()

    // Конструктор с параметрами
    constructor(temperature: Float, timestamp: Long, data : Long) {
        this.temperature = temperature
        this.timestamp = timestamp
        this.data = data
    }
}