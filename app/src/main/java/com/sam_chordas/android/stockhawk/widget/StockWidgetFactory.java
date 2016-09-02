package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.ui.StockDetail;

/**
 * Created by Saanu_mac on 8/31/16.
 */
public class StockWidgetFactory implements RemoteViewsService.RemoteViewsFactory{


    private Cursor data = null;
    private Context mContext;
    int mWidgetId;

    public StockWidgetFactory(Context context, Intent intent) {
        mContext = context;
        mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        //do nothing
    }

    @Override
    public void onDataSetChanged() {

        if (data != null) {
            data.close();
        }
        //final long identityToken = Binder.clearCallingIdentity();

        data = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);

        //Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {
        if (data != null) {
            data.close();
            data = null;
        }
    }

    @Override
    public int getCount() {
        //return data == null ? 0 : data.getCount();
        return data.getCount();
    }


    @Override
    public RemoteViews getViewAt(int position) {
        if (position == AdapterView.INVALID_POSITION ||
                data == null || !data.moveToPosition(position)) {
            return null;
        }

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.stock_widget_list_item);
        if (data.moveToPosition(position)) {



            rv.setTextViewText(R.id.stock_symbol,
                    data.getString(data.getColumnIndex(QuoteColumns.SYMBOL)));
            rv.setTextViewText(R.id.bid_price,
                    data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE)));
            rv.setTextViewText(R.id.change,
                    data.getString(data.getColumnIndex(QuoteColumns.CHANGE)));
        }

        String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));

        //Now try to set up the history data in the detail view.
        Intent mServiceIntent = new Intent(mContext, StockIntentService.class);
        mServiceIntent.putExtra("tag", "history");
        mServiceIntent.putExtra("symbol", symbol);
        mContext.startService(mServiceIntent);


        Intent fillIntent = new Intent(mContext, StockDetail.class);
        fillIntent.putExtra("symbol", symbol);
        rv.setOnClickFillInIntent(R.id.widget_list_item, fillIntent);

        return rv;
    }


    @Override
    public RemoteViews getLoadingView() {
        //return new RemoteViews(mContext.getPackageName(), R.layout.stock_widget_list_item);
        return null;

    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        //if (data.moveToPosition(position))
          //  return data.getLong(0);
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
