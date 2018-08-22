package com.neberox.app.animatedstar;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.RelativeLayout;

import com.neberox.library.animatedstar.StarRatingBar;

import static android.widget.RelativeLayout.CENTER_IN_PARENT;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RelativeLayout mainLayout = findViewById(R.id.mainView);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dpToPx(200), dpToPx(200));
        params.addRule(CENTER_IN_PARENT);

        StarRatingBar bar = new StarRatingBar(this);
        mainLayout.addView(bar, params);

        bar.setAnimDuration(200); //(Optional)
        bar.setFillColor(Color.WHITE); //(Optional)
        bar.setSelectedColor(Color.parseColor("#f2b01e")); //(Optional)
        bar.setStrokeColor(Color.LTGRAY); //(Optional)
        bar.setStrokeWidth(1); //(Optional)
        bar.setTextPadding(0.5f); //(Optional)
        bar.setTextColor(Color.LTGRAY); //(Optional)
        bar.setSelectedTextColor(Color.WHITE); //(Optional)
        bar.setTextSize(18); //(Optional)

        bar.regenerateStar();  //(Compulsory)
    }

    private int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
