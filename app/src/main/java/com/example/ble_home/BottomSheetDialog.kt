package com.example.ble_home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetDialog : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(
            R.layout.bottom_sheet_layout,
            container, false
        )

        val connect_devices_button = v.findViewById<Button>(R.id.connect_devices)
        val create_room_button = v.findViewById<Button>(R.id.create_room)

        connect_devices_button.setOnClickListener {
            // Показываем Toast
            Toast.makeText(
                activity,
                "First Button Clicked", Toast.LENGTH_SHORT
            ).show()

            // Запускаем новую активность
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)

            // Закрываем BottomSheet
            dismiss()
        }

        create_room_button.setOnClickListener {
            Toast.makeText(
                activity,
                "Second Button Clicked", Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }
        return v
    }
}
