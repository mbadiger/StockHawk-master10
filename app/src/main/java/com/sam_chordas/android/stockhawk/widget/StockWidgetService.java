package com.sam_chordas.android.stockhawk.widget;


import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViewsService;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StockWidgetService extends RemoteViewsService {
    public StockWidgetService() {
    }


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockWidgetFactory(getApplicationContext(), intent);
    }


}
