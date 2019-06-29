package com.pawanhegde.readonkindle.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pawanhegde.readonkindle.R
import com.pawanhegde.readonkindle.preview.PreviewActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val previewIntent = Intent(this, PreviewActivity::class.java)
        previewIntent.putExtra(Intent.EXTRA_TEXT, "https://www.newyorker.com/news/daily-comment/donald-trumps-royal-treatment-queen-elizabeth-theresa-may-britain-brexit")
        startActivity(previewIntent)
    }
}
