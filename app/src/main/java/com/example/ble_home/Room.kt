package com.example.ble_home

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.relation.ToMany

@Entity
class Room {

    @Id
    var id: Long = 0

    @Index
    var name: String = ""

    var temperature: Float = 0.0f
    var humidity: Float = 0.0f

    // Связь с историей температуры
    lateinit var temperatureHistory: ToMany<TemperatureRecord>
    lateinit var humidityHistory: ToMany<HumidityRecord>

    // Конструктор по умолчанию
    constructor()

    // Конструктор с параметрами
    constructor(name: String, temperature: Float, humidity: Float) {
        this.name = name
        this.temperature = temperature
        this.humidity = humidity
    }
}