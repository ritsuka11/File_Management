package com.example.filemanagement

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var fileListView: ListView
    private lateinit var currentDirectory: File
    private lateinit var adapter: FileAdapter
    private val requestCodePermission = 123

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fileListView = findViewById(R.id.fileListView)

        currentDirectory = Environment.getExternalStorageDirectory()
        val files = getListOfFiles(currentDirectory)

        adapter = FileAdapter(this, files)
        fileListView.adapter = adapter

        fileListView.setOnItemClickListener { _, _, position, _ ->
            val selectedFile = files[position]
            if (selectedFile.isDirectory) {
                showContentsOfDirectory(File(currentDirectory, selectedFile.name))
            } else {
                val file = File(currentDirectory, selectedFile.name)
                when {
                    file.extension.equals("txt", ignoreCase = true) -> showTextFileContent(file)
                    file.extension.equals("bmp", ignoreCase = true) ||
                            file.extension.equals("jpg", ignoreCase = true) ||
                            file.extension.equals("png", ignoreCase = true) -> showImageFile(file)
                    else -> {
                        // Handle other file types if needed
                        // ...
                    }
                }
            }
        }

        registerForContextMenu(fileListView)

        requestStoragePermission()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission granted, proceed with your logic
                // ...
            } else {
                // Permission not granted, request it
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    requestCodePermission
                )
            }
        } else {
            // No need for runtime permission on lower Android versions
            // Proceed with your logic
            // ...
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermission) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with your logic
                // ...
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showContentsOfDirectory(directory: File) {
        currentDirectory = directory
        val files = getListOfFiles(directory)
        adapter.clear()
        adapter.addAll(files)
    }

    private fun getListOfFiles(directory: File): List<FileItem> {
        val files = directory.listFiles()
        val fileItemList = mutableListOf<FileItem>()

        files?.forEach {
            fileItemList.add(FileItem(it.name, it.isDirectory))
        }

        return fileItemList
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_new_folder -> {
                showNewFolderDialog()
                true
            }
            R.id.menu_new_text_file -> {
                createNewTextFile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showNewFolderDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Folder")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Create") { _, _ ->
            val folderName = input.text.toString()
            val newFolder = File(currentDirectory, folderName)

            if (!newFolder.exists()) {
                newFolder.mkdir()
                showContentsOfDirectory(currentDirectory)
            } else {
                Toast.makeText(this, "Folder already exists", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showTextFileContent(file: File) {
        val content = file.readText(Charset.defaultCharset())
        showFileContent("Text File Content", content)
    }

    private fun showImageFile(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW)
        val type = file.getMimeType()
        intent.setDataAndType(uri, type)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }

    private fun showFileContent(title: String, content: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(content)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun createNewTextFile() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Text File")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Create") { _, _ ->
            val fileName = input.text.toString()
            val newFile = File(currentDirectory, "$fileName.txt")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore.createWriteRequest for Android Q and above
                createTextFileWithMediaStore(fileName)
            } else {
                // Fallback to the traditional file creation for older versions
                createTextFileWithoutMediaStore(newFile)
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun createTextFileWithMediaStore(fileName: String) {
        val resolver = contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.txt")
            put(MediaStore.Images.Media.MIME_TYPE, "text/plain")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val contentUri =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        contentUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    // Write your text content here
                    val content = "Hello, this is a new text file!"
                    outputStream.write(content.toByteArray())
                    outputStream.close()
                    showContentsOfDirectory(currentDirectory)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTextFileWithoutMediaStore(newFile: File) {
        try {
            newFile.createNewFile()
            showContentsOfDirectory(currentDirectory)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun File.getMimeType(): String {
        return when (extension.toLowerCase(Locale.ROOT)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "bmp" -> "image/bmp"
            "txt" -> "text/plain"
            // Add more mime types as needed
            else -> "*/*"
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        val info = menuInfo as AdapterView.AdapterContextMenuInfo
        val selectedFile = adapter.getItem(info.position)

        if (selectedFile?.isDirectory == true) {
            inflater.inflate(R.menu.context_menu_folder, menu)
        } else {
            inflater.inflate(R.menu.context_menu_file, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val selectedFile = adapter.getItem(info.position)

        when (item.itemId) {
            R.id.context_rename -> showRenameDialog(selectedFile)
            R.id.context_copy -> showCopyDialog(selectedFile)
            R.id.context_delete -> showDeleteDialog(selectedFile)
        }

        return super.onContextItemSelected(item)
    }

    private fun showRenameDialog(selectedFile: FileItem?) {
        selectedFile?.let {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Rename ${it.name}")

            val input = EditText(this)
            input.setText(it.name)
            builder.setView(input)

            builder.setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString()
                renameFile(File(currentDirectory, it.name), newName)
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }
    }

    private fun showCopyDialog(selectedFile: FileItem?) {
        selectedFile?.let {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Copy ${it.name} to...")

            val input = EditText(this)
            builder.setView(input)

            builder.setPositiveButton("Copy") { _, _ ->
                val destinationDirectoryName = input.text.toString()
                val destinationDirectory = File(currentDirectory, destinationDirectoryName)

                if (destinationDirectory.exists() && destinationDirectory.isDirectory) {
                    copyFile(File(currentDirectory, it.name), destinationDirectory)
                } else {
                    Toast.makeText(this, "Invalid destination directory", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }
    }

    private fun showDeleteDialog(selectedFile: FileItem?) {
        selectedFile?.let {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Deletion")
            builder.setMessage("Are you sure you want to delete ${it.name}?")

            builder.setPositiveButton("Delete") { _, _ ->
                deleteFile(File(currentDirectory, it.name))
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }
    }

    private fun renameFile(oldFile: File, newName: String) {
        val newFile = File(oldFile.parent, newName)
        if (oldFile.renameTo(newFile)) {
            showContentsOfDirectory(currentDirectory)
        } else {
            Toast.makeText(this, "Error renaming file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyFile(sourceFile: File, destinationDirectory: File) {
        val destinationFile = File(destinationDirectory, sourceFile.name)
        try {
            sourceFile.copyTo(destinationFile)
            showContentsOfDirectory(currentDirectory)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error copying file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFile(file: File) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to delete ${file.name}?")

        builder.setPositiveButton("Delete") { _, _ ->
            if (file.delete()) {
                showContentsOfDirectory(currentDirectory)
            } else {
                Toast.makeText(this, "Error deleting file", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}