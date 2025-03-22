package com.example.ble_home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.objectbox.BoxStore

lateinit var boxStore: BoxStore

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        boxStore = MyObjectBox.builder()
            .androidContext(this)
            .build()

        val roomBox = boxStore.boxFor(Room::class.java)

        val fragments : ArrayList<RoomFragment> = ArrayList()
        val rooms = roomBox.all
        rooms.forEach { room ->
            fragments.add(RoomFragment())
        }


        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // Устанавливаем Toolbar как ActionBar
        supportActionBar?.title = "Комнаты" // Заголовок Toolbar

        val buttonAdd = findViewById<View>(R.id.button_add)
        buttonAdd.setOnClickListener {
            // Логика для добавления новой комнаты
            val bottomSheet =
                BottomSheetDialog()
            bottomSheet.show(
                supportFragmentManager,
                "ModalBottomSheet"
            )
        }

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)



        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        // Связываем TabLayout с ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "Tab ${position + 1}" // Устанавливаем текст для вкладки
        }.attach()
    }

    private fun addNewRoom() {

        val roomBox = boxStore.boxFor(Room::class.java)

// Создаем объект Room
        val room = Room(name = "Living Room", temperature = 22.5f, humidity = 45.0f)
        roomBox.put(room)
        val rooms = roomBox.all
        rooms.forEach { room ->
            Log.d("RoomInfo", "Room: ${room.name}, Temp: ${room.temperature}°C")
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        boxStore.close()
    }
}