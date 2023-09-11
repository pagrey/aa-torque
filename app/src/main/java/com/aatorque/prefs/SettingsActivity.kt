package com.aatorque.prefs

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aatorque.stats.App
import com.aatorque.stats.R
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import java.io.File
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.lifecycleScope
import com.aatorque.datastore.UserPreference
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private var mCredential: GoogleAccountCredential? = null
    private var mCurrentAuthorizationIntent: Intent? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val app = application as App
        mCredential = app.googleCredential
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment, SettingsFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.action_export_dashboards -> {
                // Export the file to a user provided location
                exportFile()
                true
            }
            R.id.action_import_dashboards -> {
                openFile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFile() {
        importFileLauncher.launch(EXPORT_MIME)
    }

    private fun exportFile() {
        val file = File(filesDir, "datastore/user_prefs.pb")

        if (file.exists() && file.canRead()) {
            // Launch the activity result with the contract input
            exportFileLauncher.launch(file.name)
        } else {
            // Show a toast message that the file is not available
            Toast.makeText(this, R.string.file_not_available, Toast.LENGTH_SHORT).show()
        }
    }

    private val exportFileLauncher = registerForActivityResult(ExportFileContract()) { uri: Uri? ->
        // This lambda will be executed when the activity result returns
        var msg = if (uri != null) {
            // Get the content resolver and open an output stream to the URI
            val resolver = contentResolver
            resolver.openOutputStream(uri)?.use { outputStream ->
                // Get the file to be exported and open an input stream from it
                val file = File(filesDir, "datastore/user_prefs.pb")
                file.inputStream().use { inputStream ->
                    // Copy the bytes from the input stream to the output stream
                    inputStream.copyTo(outputStream)
                }
            }
            // Show a toast message that the file was exported successfully
            R.string.file_exported_successfully
        } else {
            // Show a toast message that the export failed
            R.string.export_failed
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


    private val importFileLauncher = registerForActivityResult(ImportFileContract()) { uri: Uri? ->
        if (uri != null) {
            val inStream = contentResolver.openInputStream(uri)
            this@SettingsActivity.lifecycleScope.launch {
                this@SettingsActivity.applicationContext.dataStore.updateData {
                    return@updateData UserPreference.parseFrom(inStream)
                }
            }
        }
    }

    class ExportFileContract() : ActivityResultContract<String, Uri?>() {

        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = EXPORT_MIME
                // Suggest a file name based on the original file name
                putExtra(Intent.EXTRA_TITLE, "dashboards.pb")
            }
        }

        // This function parses the activity result and returns the URI of the user selected location
        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (resultCode == Activity.RESULT_OK) {
                intent?.data
            } else {
                null
            }
        }
    }

    class ImportFileContract() : ActivityResultContract<String, Uri?>() {

        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = input
            }
        }

        // This function parses the activity result and returns the URI of the user selected location
        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (resultCode == Activity.RESULT_OK) {
                intent?.data
            } else {
                null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }


    override fun onResume() {
        super.onResume()
        val permissionsToRequest: MutableList<String> = ArrayList()
        if (ContextCompat.checkSelfPermission(this, PERMISSION_CAR_VENDOR_EXTENSION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(PERMISSION_CAR_VENDOR_EXTENSION)
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS)
            return
        }

    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val args = Bundle()
        args.putCharSequence("title", pref.title)
        args.putString("prefix", pref.key)
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment!!
        )
        fragment.arguments = args
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.settings_fragment, fragment)
            .addToBackStack(null)
            .commit()
        return true
    }


    companion object {
        private const val TAG = "SettingsActivity"
        private const val REQUEST_PERMISSIONS = 0
        private const val PERMISSION_CAR_VENDOR_EXTENSION =
            "com.google.android.gms.permission.CAR_VENDOR_EXTENSION"
        const val PREF_LOCATION = "useGoogleGeocoding"
        const val EXPORT_MIME = "application/octet-stream"
    }
}

