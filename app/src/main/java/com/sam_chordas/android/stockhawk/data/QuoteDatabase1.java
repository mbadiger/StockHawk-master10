package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by sam_chordas on 10/5/15.
 */
@Database(version = QuoteDatabase1.VERSION)
public class QuoteDatabase1 {
  private QuoteDatabase1(){}

  public static final int VERSION = 16;

  @Table(QuoteColumns.class) public static final String QUOTES = "quotes";
  @Table(HistoryColumns.class) public static final String HISTORY = "history";
}
