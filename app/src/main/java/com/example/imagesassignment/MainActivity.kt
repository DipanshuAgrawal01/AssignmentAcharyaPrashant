package com.example.imagesassignment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        imageAdapter = ImageAdapter(this)
        recyclerView.adapter = imageAdapter

        loadImages()
    }

    private fun loadImages() {
        scope.launch {
            val imageUrls = fetchImageUrls()
            if (imageUrls != null) {
                imageAdapter.setImageUrls(imageUrls)
            } else {
                Toast.makeText(applicationContext, "Failed to load images", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private suspend fun fetchImageUrls(): List<String>? = withContext(Dispatchers.IO) {

        try {
            val apiUrl = "https://acharyaprashant.org/api/v2/content/misc/media-coverages?limit=100"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                bufferedReader.close()
                connection.disconnect()

                // Parse JSON response
                val gson = Gson()
                val dataArray = gson.fromJson(response.toString(), Array<ImageItem>::class.java)

                // Construct image URLs
                val imageUrls = mutableListOf<String>()
                dataArray.forEach { data ->
                    val thumbnail = data.thumbnail
                    val imageUrl = "${thumbnail?.domain}/${thumbnail?.basePath}/0/${thumbnail?.key}"
                    imageUrls.add(imageUrl)
                }

                return@withContext imageUrls
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private inner class ImageAdapter(private val context: Context) :
        RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
        private var imageUrls: List<String> = emptyList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun getItemCount(): Int {
            return imageUrls.size
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.bind(imageUrls[position])
        }

        fun setImageUrls(imageUrls: List<String>) {
            this.imageUrls = imageUrls
            notifyDataSetChanged()
        }

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.imageView)
            private var currentJob: Job? = null

            fun bind(imageUrl: String) {
                currentJob?.cancel()
                currentJob = scope.launch {
                    val bitmap = loadImage(imageUrl)
                    imageView.setImageBitmap(bitmap)
                }
            }

            private suspend fun loadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
                val cacheFile = File(context.cacheDir, url.hashCode().toString())
                try {
                    if (cacheFile.exists()) {
                        FileInputStream(cacheFile).use {
                            return@withContext BitmapFactory.decodeStream(it)
                        }
                    } else {
                        val bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                        FileOutputStream(cacheFile).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        return@withContext bitmap
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext null
            }
        }
    }
}
