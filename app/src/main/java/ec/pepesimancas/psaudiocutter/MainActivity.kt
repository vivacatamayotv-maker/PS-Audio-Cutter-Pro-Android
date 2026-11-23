package ec.pepesimancas.psaudiocutter

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var picker: ValueCallback<Array<Uri>>? = null
    private var output: OutputStream? = null
    private var outputUri: Uri? = null
    private var outputFile: File? = null
    private val lock = Any()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xff0b1525.toInt()
        if (Build.VERSION.SDK_INT <= 28 &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 71)

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.setBackgroundColor(0xff0b1525.toInt())
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                picker?.onReceiveValue(null)
                picker = callback
                return try {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/mp4"))
                    }
                    startActivityForResult(intent, 501)
                    true
                } catch (e: Exception) {
                    picker = null
                    callback?.onReceiveValue(null)
                    false
                }
            }
        }
        web.addJavascriptInterface(Bridge(), "AndroidBridge")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Selector de archivos compatible desde Android 8")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 501) {
            picker?.onReceiveValue(if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null)
            picker = null
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun beginFile(rawName: String, mime: String): Boolean = synchronized(lock) {
            closeFile(false)
            if (Build.VERSION.SDK_INT <= 28 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Autoriza el almacenamiento", Toast.LENGTH_LONG).show() }
                return@synchronized false
            }
            val name = rawName.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(130)
            if (name.isBlank() || !(name.endsWith(".mp3") || name.endsWith(".zip"))) return@synchronized false
            return@synchronized try {
                if (Build.VERSION.SDK_INT >= 29) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, mime)
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PS Audio Cutter")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    outputUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    output = outputUri?.let { contentResolver.openOutputStream(it) }
                } else {
                    val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PS Audio Cutter")
                    folder.mkdirs()
                    var path = File(folder, name)
                    var index = 1
                    while (path.exists()) path = File(folder, "${index++}_$name")
                    outputFile = path
                    output = FileOutputStream(path)
                }
                output != null
            } catch (e: Exception) {
                closeFile(false)
                false
            }
        }

        @JavascriptInterface
        fun appendChunk(base64: String): Boolean = synchronized(lock) {
            try {
                val stream = output ?: return@synchronized false
                stream.write(Base64.decode(base64, Base64.DEFAULT))
                true
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun finishFile(): Boolean = synchronized(lock) {
            val ok = output != null
            closeFile(ok)
            ok
        }

        @JavascriptInterface
        fun cancelFile() = synchronized(lock) { closeFile(false) }
    }

    private fun closeFile(keep: Boolean) {
        try { output?.close() } catch (e: Exception) { }
        output = null
        if (!keep) {
            outputUri?.let { try { contentResolver.delete(it, null, null) } catch (e: Exception) { } }
            outputFile?.delete()
        } else if (Build.VERSION.SDK_INT >= 29) {
            outputUri?.let {
                val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                try { contentResolver.update(it, values, null, null) } catch (e: Exception) { }
            }
        }
        outputUri = null
        outputFile = null
    }

    override fun onDestroy() {
        synchronized(lock) { closeFile(false) }
        web.removeJavascriptInterface("AndroidBridge")
        web.destroy()
        super.onDestroy()
    }
}
