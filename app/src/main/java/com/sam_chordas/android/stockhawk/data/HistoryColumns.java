package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.Unique;

/**
 * Created by Saanu_mac on 5/10/16.
 */
public class HistoryColumns {

    @DataType(DataType.Type.INTEGER) @PrimaryKey
    @AutoIncrement
    public static final String _ID = "_id";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String SYMBOL = "symbol";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String OPEN = "open";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String CLOSE = "close";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String HIGH = "high";
    @DataType(DataType.Type.TEXT)
    public static final String LOW = "low";
    @DataType(DataType.Type.TEXT) @Unique (onConflict = ConflictResolutionType.REPLACE) @NotNull
    public static final String DATE = "date";

}
