package com.pawanhegde.readonkindle.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pawanhegde.readonkindle.R
import com.pawanhegde.readonkindle.preview.PreviewActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val _tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(main_toolbar)

        val viewModel = ViewModelProvider(
                this,
                MainViewModelFactory()
        ).get(MainViewModel::class.java)


        viewModel.url.observe(this, Observer { url -> main_send.isEnabled = isValidUrl(url) })

        main_send.setOnClickListener {
            val previewIntent = Intent(this, PreviewActivity::class.java)
            viewModel.url.value = url_input.text.toString()
            previewIntent.putExtra(Intent.EXTRA_TEXT, viewModel.url.value)
            startActivity(previewIntent)
        }

        url_input.setOnFocusChangeListener { _, _ -> viewModel.url.value = url_input.text.toString() }
    }

    private fun isValidUrl(url: String): Boolean {
//        Log.i(_tag, "url = $url; isNotBlank = ${url.isNotBlank()}; pattern = ${Patterns.WEB_URL}; matchesPattern = ${Patterns.WEB_URL.matcher(url).matches()}")
//        return url.isNotBlank() && Patterns.WEB_URL.matcher(url).matches()
        return true
    }

}
