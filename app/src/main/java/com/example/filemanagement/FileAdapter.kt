package com.example.filemanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FileAdapter(private val context: AppCompatActivity, private val files: List<FileItem>) :
    ArrayAdapter<FileItem>(context, R.layout.file_item, files) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val fileItem = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.file_item, parent, false)

        val fileNameTextView: TextView = view.findViewById(R.id.fileNameTextView)
        fileNameTextView.text = fileItem?.name

        return view
    }
}