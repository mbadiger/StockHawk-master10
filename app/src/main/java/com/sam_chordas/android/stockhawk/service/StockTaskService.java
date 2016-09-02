package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();
    public static final String ACTION_DATA_UPDATED =
            "com.sam_chordas.android.stockhawk.service.ACTION_DATA_UPDATED";

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;
    private Handler handler;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
        handler = new Handler();

    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (params.getTag().equals("history")) {
            String stockInput = params.getExtras().getString("symbol");
            return (getHistoricalData(stockInput));
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;


        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {

                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;

                if (Utils.isStockDataEmpty(getResponse)) {
                    sendMessage();
                    //Toast toast = Toast.makeText(mContext, "Symbol couldn't be added", Toast.LENGTH_LONG);
                    //toast.show();
                }

                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }

                    ArrayList stockList = Utils.quoteJsonToContentVals(getResponse);

                    if (stockList.isEmpty()) {
                        //Toast.makeText(this, "Symbol couldn't be added", Toast.LENGTH_SHORT).show();
                    } else {

                        //mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                        //  Utils.quoteJsonToContentVals(getResponse));
                        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                                stockList);
                    }
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Update the app widgets if we add any new stock or if data is refetched
        updateWidget();

        return result;
    }

    private void updateWidget() {

        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(mContext.getPackageName());
        mContext.sendBroadcast(dataUpdatedIntent);
    }

    private int getHistoricalData(String symbol) {

        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Load historic stock data
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = new Date();

            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(currentDate);
            calEnd.add(Calendar.DATE, 0);

            Calendar calStart = Calendar.getInstance();
            calStart.setTime(currentDate);
            calStart.add(Calendar.MONTH, -1);

            String startDate = dateFormat.format(calStart.getTime());
            String endDate = dateFormat.format(calEnd.getTime());

            // Base URL for the Yahoo query
            //urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            //urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where startDate =\"2009-09-11\" and endDate =\"2009-10-11\" and symbol "
            //        + "in (", "UTF-8") );


            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where startDate =", "UTF-8"));
            urlStringBuilder.append(URLEncoder.encode("\"" + startDate + "\"", "UTF-8"));
            urlStringBuilder.append(URLEncoder.encode(" and endDate = ", "UTF-8"));
            urlStringBuilder.append(URLEncoder.encode("\"" + endDate + "\"", "UTF-8"));
            urlStringBuilder.append(URLEncoder.encode(" and symbol in ( ", "UTF-8"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            //urlStringBuilder.append(URLEncoder.encode("YHOO", "UTF-8"));
            urlStringBuilder.append(URLEncoder.encode("\"" + symbol + "\")", "UTF-8"));
            //urlStringBuilder.append(
            //      URLEncoder.encode("\"YHOO\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {

                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;

      /*  if (Utils.isStockDataEmpty(getResponse)) {

          sendMessage();
          //Toast toast = Toast.makeText(mContext, "Symbol couldn't be added", Toast.LENGTH_LONG);
          //toast.show();
        }*/

                ArrayList stockList = Utils.historyJsonToContentVals(getResponse);

                //ContentValues[] cvArray = new ContentValues[stockList.size()];
                //stockList.toArray(cvArray);
                //mContext.getContentResolver().bulkInsert(QuoteProvider.History.CONTENT_URI, stockList);


                if (stockList.isEmpty()) {

                    //Toast.makeText(this, "Symbol couldn't be added", Toast.LENGTH_SHORT).show();
                } else {

                    //mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    //  Utils.quoteJsonToContentVals(getResponse));
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            stockList);
                }
                //mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                //      stockList);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }

        }
        return result;
    }

    private void sendMessage() {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("custom-event-name");
        // You can also include some extra data.
        intent.putExtra("message", "This is my message!");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
