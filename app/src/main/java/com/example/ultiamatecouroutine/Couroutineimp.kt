package com.example.ultiamatecouroutine

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class Couroutineimp : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView
    private lateinit var playVideoButton: Button
    private lateinit var pdfTextView: TextView
    private lateinit var pdfImageView: ImageView
    private lateinit var imageUrlInput: EditText
    private lateinit var videoUrlInput: EditText
    private lateinit var pdfUrlInput: EditText
    private lateinit var loadImageButton: Button
    private lateinit var loadPdfButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.allcouroutinedesign)

        // Initialize UI components
        imageView = findViewById(R.id.coroutineImage)
        videoView = findViewById(R.id.videoView)
        playVideoButton = findViewById(R.id.buttonPlayVideo)
        pdfTextView = findViewById(R.id.pdfTextView)
        pdfImageView = findViewById(R.id.pdfImageView)
        imageUrlInput = findViewById(R.id.imageUrlInput)
        videoUrlInput = findViewById(R.id.videoUrlInput)
        pdfUrlInput = findViewById(R.id.pdfUrlInput)
        loadImageButton = findViewById(R.id.loadImageButton)
        loadPdfButton = findViewById(R.id.loadPdfButton)

        // Load image on button click
        loadImageButton.setOnClickListener {
            val imageUrl = imageUrlInput.text.toString()
            loadImage(imageUrl)
        }

        // Handle video playback on button click
        playVideoButton.setOnClickListener {
            val videoUrl = videoUrlInput.text.toString()
            playVideo(videoUrl)
        }

        // Load and render PDF on button click
        loadPdfButton.setOnClickListener {
            val pdfUrl = pdfUrlInput.text.toString()
            loadPdf(pdfUrl)
        }
    }

    private fun loadImage(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val imageData = fetchImage(url)
            if (imageData != null) {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private suspend fun fetchImage(url: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        }
    }

    private fun playVideo(url: String) {
        lifecycleScope.launch {
            val videoUri = fetchVideoUrl(url)
            withContext(Dispatchers.Main) {
                videoUri?.let {
                    videoView.setVideoURI(it)
                    videoView.start()
                }
            }
        }
    }

    private suspend fun fetchVideoUrl(url: String): Uri? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Uri.parse(url)
            } else {
                null
            }
        }
    }

    private fun loadPdf(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                pdfTextView.text = "Downloading PDF..."
            }

            val pdfFile = downloadPdf(url)
            if (pdfFile != null) {
                withContext(Dispatchers.Main) {
                    pdfTextView.text = "PDF downloaded, rendering the first page..."
                }

                val pdfBitmap = renderPdf(pdfFile)
                pdfBitmap?.let {
                    withContext(Dispatchers.Main) {
                        pdfImageView.setImageBitmap(it)
                        pdfTextView.text = "PDF rendered successfully"
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    pdfTextView.text = "Failed to download PDF"
                }
            }
        }
    }

    private suspend fun downloadPdf(url: String): File? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val pdfFile = File(cacheDir, "Downloaded_pdf.pdf")
                val fos = FileOutputStream(pdfFile)

                response.body?.byteStream()?.use { inputStream ->
                    fos.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                pdfFile
            } else {
                null
            }
        }
    }

    private suspend fun renderPdf(file: File): Bitmap? {
        return withContext(Dispatchers.IO) {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val page = pdfRenderer.openPage(0)

            val width = page.width
            val height = page.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pdfRenderer.close()
            bitmap
        }
    }
}
