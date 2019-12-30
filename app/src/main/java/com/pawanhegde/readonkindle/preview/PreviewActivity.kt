package com.pawanhegde.readonkindle.preview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.pawanhegde.readonkindle.R
import com.pawanhegde.readonkindle.entities.Content
import com.pawanhegde.readonkindle.main.MainActivity
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.bottom_sheet_preview.*
import java.io.File

class PreviewActivity : AppCompatActivity() {
    private val _tag: String = "PreviewActivity"

    private lateinit var viewModel: PreviewViewModel
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e(_tag, "--- ON CREATE CALLED ---")
        setContentView(R.layout.activity_preview)

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_preview)

        val url = intent.getStringExtra(Intent.EXTRA_TEXT)

        if (url?.isNotBlank() == false) {
            Log.e(_tag, "Preview activity launched without a URL")
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(mainActivityIntent)
            finish()
        }

        viewModel = ViewModelProvider(
                this,
                PreviewViewModelFactory(application, url!!)
        ).get(PreviewViewModel::class.java)

        viewModel.content.observe(this, Observer {
            val content = it
            preview_web_view.loadData(content.simplifiedContent, "text/html", "UTF-8")
            preview_send.setOnClickListener {
                if (preview_target_email.text.isNullOrBlank()) {
                    Log.w(_tag, "The target email is ${preview_target_email.text}")
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    preview_target_email.error = "Missing email address"
                    return@setOnClickListener
                }

                Log.i(_tag, "Target email: ${preview_target_email.text}")
                sendPage(content)
            }
            preview_send.isEnabled = true
            preview_status.visibility = GONE
            preview_progress_bar.visibility = GONE
            preview_web_view.visibility = VISIBLE
        })

        val title: MediatorLiveData<String> = MediatorLiveData()
        title.addSource(viewModel.url) { displayTitle.text = it }
        title.addSource(viewModel.content) {
            displayTitle.text = it.title
        }
        title.observe(this, Observer { displayTitle.text = it })

        updateUrl(url)

        val preferences = this.getPreferences(Context.MODE_PRIVATE)
        val kindleEmail = preferences.getString(getString(R.string.target_email), "")
        val bottomSheetPreference = preferences.getInt(getString(R.string.bottom_sheet_preference), BottomSheetBehavior.STATE_EXPANDED)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_SETTLING
        preview_target_email.setText(kindleEmail)
        bottomSheetBehavior.state = bottomSheetPreference
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val url = intent?.getStringExtra(Intent.EXTRA_TEXT)
        Log.e(_tag, "--- ON NEW INTENT CALLED [${url}]---")

        updateUrl(url)
    }

    override fun onPause() {
        super.onPause()

        val kindleEmail = preview_target_email.text?.toString()
        val collapseSheet = bottomSheetBehavior.state

        val preferences = this.getPreferences(Context.MODE_PRIVATE)
        with(preferences.edit()) {
            putString(getString(R.string.target_email), kindleEmail)
            putInt(getString(R.string.bottom_sheet_preference), collapseSheet)
            apply()
        }
    }

    private fun updateUrl(url: String?) {
        url?.let {
            if (viewModel.url.value != it) {
                viewModel.url.postValue(it)
            }
        }
    }

    private fun sendPage(content: Content) {
        val sentToKindle = File(this.cacheDir, "sent_to_kindle/${content.title}.html")

        sentToKindle.parentFile?.delete()

        sentToKindle.parentFile?.mkdirs()
        sentToKindle.createNewFile()

        sentToKindle.writeText(content.simplifiedContent, Charsets.UTF_8)

        val contentUri = FileProvider.getUriForFile(applicationContext, "com.pawanhegde.readonkindle", sentToKindle)

        val findIntent = Intent(Intent.ACTION_SENDTO)
        findIntent.data = Uri.fromParts("mailto", "abc@gmail.com", null)
        val emailActivities = applicationContext.packageManager.queryIntentActivities(findIntent, 0)
        val packageNames = emailActivities.map { it.activityInfo.packageName }

        Log.d(_tag, "Sending to the email id: ${preview_target_email.text}")

        val shareIntents = packageNames.map { packageName ->
            application.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.`package` = packageName
            shareIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(preview_target_email.text.toString()))
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sending the page ${content.title}")
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.type = contentResolver.getType(contentUri)

            shareIntent
        }

        Log.d(_tag, "The intent to send is ${shareIntents.first().extras}")

        val chooserIntent = Intent.createChooser(shareIntents.last(), "Choose the app")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, shareIntents.dropLast(1).toTypedArray())
        startActivity(chooserIntent)

        Log.i(_tag, "Started chooser intent")
        finish()
    }
}