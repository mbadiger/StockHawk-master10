package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;


public class StockDetail extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph1);

        String symbol = getIntent().getExtras().getString("symbol");
        Bundle args = new Bundle();
        args.putString("symbol", symbol);

        //Now try to set up the history data in the detail view.
        Intent mServiceIntent = new Intent(this, StockIntentService.class);
        mServiceIntent.putExtra("tag", "history");
        mServiceIntent.putExtra("symbol", symbol);
        startService(mServiceIntent);


        //Setup the Tabs to display graph by Week/Month
        TabLayout tabLayout = (TabLayout) findViewById(R.id.mytabs);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

        MyPageAdapter mAdapter = new MyPageAdapter(getSupportFragmentManager(), args);
        viewPager.setAdapter(mAdapter);

        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }


}
