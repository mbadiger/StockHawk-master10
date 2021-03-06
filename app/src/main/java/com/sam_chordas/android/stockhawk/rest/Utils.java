package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        final String OWM_MESSAGE_CODE = "cod";

        try {

            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {

                // do we have an error?

                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");


                    if (jsonObject.isNull("Bid")) {

                        return batchOperations;
                    }
                    //if ( jsonObject )
                    //
                    batchOperations.add(buildBatchOperation(jsonObject));
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");


                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }

        change = change.substring(1, change.length());

        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));

            String bidprice = jsonObject.getString("Bid");

            if (bidprice == null) {
                //Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
                return builder.build();
            }

            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }


    public static boolean isStockDataEmpty(String JSON) {


        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(JSON);

            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");

                    return (jsonObject.isNull("Bid"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return (false);
    }

    public static ArrayList historyJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        final String OWM_MESSAGE_CODE = "cod";

        try {

            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {

                // do we have an error?

                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");


                    if (jsonObject.isNull("symbol")) {

                        return batchOperations;
                    }

                    batchOperations.add(buildHistoryBatchOperation(jsonObject));
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");


                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            if (jsonObject != null) {
                                batchOperations.add(buildHistoryBatchOperation(jsonObject));
                            }

                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }


    public static ContentProviderOperation buildHistoryBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.History.CONTENT_URI);
        try {

            String msymbol = jsonObject.getString("Symbol");

            builder.withValue(HistoryColumns.SYMBOL, jsonObject.getString("Symbol"));

            String stockDate = jsonObject.getString("Date");

            if (stockDate == null) {
                //Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
                Log.v(LOG_TAG, "There is no History data");
                return builder.build();
            }

            builder.withValue(HistoryColumns.OPEN, truncateBidPrice(jsonObject.getString("Open")));
            builder.withValue(HistoryColumns.CLOSE, truncateBidPrice(
                    jsonObject.getString("Close")));
            builder.withValue(HistoryColumns.HIGH, truncateBidPrice(jsonObject.getString("High")));
            builder.withValue(HistoryColumns.LOW, truncateBidPrice(jsonObject.getString("Low")));
            builder.withValue(HistoryColumns.DATE, jsonObject.getString("Date"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

}
