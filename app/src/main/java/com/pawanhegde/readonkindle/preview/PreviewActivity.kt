package com.pawanhegde.readonkindle.preview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.pawanhegde.readonkindle.R
import com.pawanhegde.readonkindle.main.MainActivity
import com.pawanhegde.readonkindle.models.ReadableDocument
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.bottom_sheet_preview.*
import java.io.File

class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        if (!intent.hasExtra(Intent.EXTRA_TEXT)) {
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(mainActivityIntent)
            finish()
        }

        val viewModel: PreviewViewModel by lazy {
            ViewModelProviders.of(
                    this,
                    PreviewViewModelFactory(intent.getStringExtra(Intent.EXTRA_TEXT))
            ).get(PreviewViewModel::class.java)
        }

        viewModel.readableDocument.observe(this, Observer<ReadableDocument> {
            val readableDocument = it
            preview_web_view.loadData(readableDocument.htmlContent, "text/html", "UTF-8")
            preview_send.setOnClickListener { sendPage(readableDocument) }
            preview_send.isEnabled = true
            preview_status.visibility = GONE
            preview_progress_bar.visibility = GONE
            preview_web_view.visibility = VISIBLE
        })
    }

    override fun onStart() {
        super.onStart()

        val preferences = this.getPreferences(Context.MODE_PRIVATE)
        val kindleEmail = preferences.getString(getString(R.string.target_email), "")

        if (!kindleEmail.isNullOrBlank()) {
            preview_target_email.setText(kindleEmail)
        }
    }

    override fun onStop() {
        super.onStop()

        val kindleEmail = preview_target_email.text?.toString()

        if (!kindleEmail.isNullOrBlank()) {
            val preferences = this.getPreferences(Context.MODE_PRIVATE)
            with(preferences.edit()) {
                putString(getString(R.string.target_email), kindleEmail)
                apply()
            }
        }
    }

    private fun sendPage(document: ReadableDocument) {
        val sentToKindle = File(this.cacheDir, "sent_to_kindle/${document.title}.html")

        if (sentToKindle.parentFile.exists()) {
            sentToKindle.parentFile.delete()
        }
        sentToKindle.parentFile.mkdirs()
        sentToKindle.createNewFile()

        sentToKindle.writeText(document.htmlContent, Charsets.UTF_8)

        val contentUri = FileProvider.getUriForFile(applicationContext, "com.pawanhegde.readonkindle", sentToKindle)

        val findIntent = Intent(Intent.ACTION_SENDTO)
        findIntent.data = Uri.fromParts("mailto", "abc@gmail.com", null)
        val emailActivities = applicationContext.packageManager.queryIntentActivities(findIntent, 0)
        val packageNames = emailActivities.map { it.activityInfo.packageName }

        println("Sending to the email id: ${preview_target_email.text}")

        val shareIntents = packageNames.map {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.`package` = it
            shareIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(preview_target_email.text.toString()))
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sending the page ${document.title}")
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.type = contentResolver.getType(contentUri)

            shareIntent
        }

        println("The intent to send is ${shareIntents.first().extras}")

        val chooserIntent = Intent.createChooser(shareIntents.last(), "Choose the app")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, shareIntents.dropLast(1).toTypedArray())
        startActivity(chooserIntent)
        finish()
    }
}