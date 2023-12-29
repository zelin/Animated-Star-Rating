package com.neberox.app.animatedstar

import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import androidx.activity.ComponentActivity
import com.neberox.library.animatedstar.StarRatingBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rating)
        findViewById<StarRatingBar>(R.id.star_rating).setOnRatingChangeListener { rating ->
            Log.d("Rating", rating.toString())
        }
    }
}