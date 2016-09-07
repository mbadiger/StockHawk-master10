package com.sam_chordas.android.stockhawk.ui;

import android.animation.PropertyValuesHolder;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.LineChartView;
import com.db.chart.view.Tooltip;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.BounceEase;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChartFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChartFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = ChartFragment.class.getSimpleName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1_tabtitle;
    private String mParam2_symbol;

    //private OnFragmentInteractionListener mListener;
    private static final int HISTORY_CURSOR_LOADER_ID = 1;

    private LineChartView lineChartView;
    private LineChartView mChart;
    private Cursor mCursor;
    private Runnable mBaseAction;
    private Tooltip mTip;

    public ChartFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param page_title Parameter 1.
     * @param arg_symbol Parameter 2.
     * @return A new instance of fragment ChartFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChartFragment newInstance(String page_title, String arg_symbol) {
        ChartFragment fragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, page_title);
        args.putString(ARG_PARAM2, arg_symbol);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1_tabtitle = getArguments().getString(ARG_PARAM1);
            mParam2_symbol = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.chart_fragment, container, false);

        if (getArguments() != null) {
            mParam1_tabtitle = getArguments().getString(ARG_PARAM1);
            mParam2_symbol = getArguments().getString(ARG_PARAM2);
        }


        lineChartView = (LineChartView) view.findViewById(R.id.linechart);
        mChart = lineChartView;

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle args = new Bundle();
        args.putString("symbol", mParam2_symbol);
        getLoaderManager().initLoader(HISTORY_CURSOR_LOADER_ID, args, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //return null;
        String symbol = args.getString("symbol");
        //String sortOrder = HistoryColumns._ID + " , " + HistoryColumns.DATE + " ASC LIMIT 6 ";
        String sortOrder;


        if (mParam1_tabtitle.equals(getString(R.string.WEEK))) {
            sortOrder = HistoryColumns.DATE + " DESC LIMIT 6 ";
        } else {
            sortOrder = HistoryColumns.DATE + " DESC LIMIT 20 ";
        }

        //String sortOrder = HistoryColumns.DATE + " DESC,  " + HistoryColumns._ID + " LIMIT 6 ";
        return new CursorLoader(getActivity(), QuoteProvider.History.CONTENT_URI,
                new String[]{HistoryColumns._ID, HistoryColumns.SYMBOL, HistoryColumns.OPEN,
                        HistoryColumns.CLOSE, HistoryColumns.HIGH, HistoryColumns.LOW, HistoryColumns.DATE},
                HistoryColumns.SYMBOL + " = ?",
                new String[]{symbol},
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        mCursor = data;

      /*  Cursor c = getContentResolver().query(QuoteProvider.History.CONTENT_URI,
                new String[] { HistoryColumns.SYMBOL }, HistoryColumns.SYMBOL + "= ?",
                new String[] { symbol }, null);*/

       /* while ( mCursor.moveToNext()) {
            Log.v("MHB", "MHB the first value is " + mCursor.getString(0));
            Log.v("MHB", "MHB the second value is " + mCursor.getString(1));
            Log.v("MHB", "MHB the third value is " +mCursor.getString(2));
        }*/

        Log.v(LOG_TAG, "The cursor data size is " + data.getCount());
        showMyChart(mBaseAction, data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    @Override
    public void onResume() {
        super.onResume();

        Bundle args = new Bundle();
        args.putString("symbol", mParam2_symbol);
        getLoaderManager().restartLoader(HISTORY_CURSOR_LOADER_ID, args, this);
    }


    public void showMyChart(Runnable action, Cursor data) {

        // Tooltip
        mTip = new Tooltip(getActivity(), R.layout.linechart_tooltip, R.id.value);

        ((TextView) mTip.findViewById(R.id.value))
                .setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf"));

        mTip.setVerticalAlignment(Tooltip.Alignment.BOTTOM_TOP);
        mTip.setDimensions((int) Tools.fromDpToPx(65), (int) Tools.fromDpToPx(25));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            mTip.setEnterAnimation(PropertyValuesHolder.ofFloat(View.ALPHA, 1),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f)).setDuration(200);

            mTip.setExitAnimation(PropertyValuesHolder.ofFloat(View.ALPHA, 0),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0f)).setDuration(200);

            mTip.setPivotX(Tools.fromDpToPx(65) / 2);
            mTip.setPivotY(Tools.fromDpToPx(25));
        }

        mChart.setTooltips(mTip);

        // Data
        LineSet dataset = new LineSet();
        ArrayList<Float> stockPrices = new ArrayList<Float>();

        data.moveToFirst();

        float highprice;
        float lowprice;
        double minrange = 1;
        double maxrange = 1;

        if (data != null && data.moveToLast()) {

            Boolean first_time_label = true;

            do {

                String symbol = data.getString(data.getColumnIndex(
                        HistoryColumns.SYMBOL));
                String date = data.getString(data.getColumnIndex(
                        HistoryColumns.DATE));

                highprice = data.getFloat(data.getColumnIndex(HistoryColumns.HIGH));
                lowprice = data.getFloat(data.getColumnIndex(HistoryColumns.LOW));


                stockPrices.add(highprice);
                stockPrices.add(lowprice);

                String label;

                label = date;

                if (mParam1_tabtitle.equals(getString(R.string.MONTH))) {

                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date stockDate;
                    try {
                        stockDate = dateFormat.parse(date);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(stockDate);

                        if (first_time_label == true) {
                            first_time_label = false;

                            label = Integer.toString(calendar.get(Calendar.MONTH) + 1) + "-" + Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
                        } else {
                            label = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
                        }


                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                dataset.addPoint(new Point(label, lowprice));
                dataset.addPoint(" ", highprice);

            } while (data.moveToPrevious());

            Collections.sort(stockPrices);

            minrange = stockPrices.get(0);
            maxrange = stockPrices.get(stockPrices.size() - 1);


            maxrange = Math.ceil(maxrange);


            if (((int) (maxrange) - (int) (minrange)) <= 2) {
                maxrange = maxrange + 2;
            }


            dataset.setColor(Color.parseColor("#758cbb"))
                    .setFill(Color.parseColor("#2d374c"))
                    .setDotsColor(Color.parseColor("#758cbb"))
                    .setThickness(4);


            mChart.addData(dataset);


            // Draw the chart now
            mChart.setBorderSpacing(Tools.fromDpToPx(10))
                    .setAxisBorderValues((int) minrange, (int) maxrange, 1)
                    .setAxisLabelsSpacing(1.0f)
                    .setLabelsColor(Color.parseColor("#ffffff"))
                    .setXAxis(true)
                    .setAxisColor(Color.parseColor("#84FFFF"))
                    .setAxisThickness(0.5f)
                    .setFontSize(35)
                    .setYAxis(true);

            mBaseAction = action;
            Runnable chartAction = new Runnable() {
                @Override
                public void run() {
                    //mBaseAction.run();
                /*mTip.prepare(mChart.getEntriesArea(0).get(3), mValues[0][3]);
                mChart.showTooltip(mTip, true);*/
                }
            };

            Animation anim = new Animation()
                    .setEasing(new BounceEase())
                    .setEndAction(chartAction);

            //mChart.show(anim);
            mChart.show();

        }
    }

}
