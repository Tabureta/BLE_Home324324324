package com.example.ble_home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager // Импортируем GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RoomFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_room, container, false)

        // Получаем RecyclerView из макета
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        Toast.makeText(context, "This is a toast message", Toast.LENGTH_SHORT).show()
        // Устанавливаем GridLayoutManager с 2 столбцами
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Создаем список данных
        val data = arrayOf("Комната 1", "Комната 2", "Комната 3", "Комната 4")

        // Устанавливаем адаптер
        recyclerView.adapter = AdapterRecycler(data)

        return view
    }
}