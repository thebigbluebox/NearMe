package net.jmodwyer.beacon.beaconPoC.databases;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by tai on 31/01/15.
 */
public class UUIDTable {
    public static final String TABLE_UUID = "uuids";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DATE_CREATION = "date_creation";
    public static final String COLUMN_DATE_LASTUPDATE = "date_lastupdate";
    public static final String COLUMN_STRENGTH = "strength";

    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_UUID
            + "("
            + COLUMN_ID + " integer primary key, "
            + COLUMN_UUID + " text, "
            + COLUMN_NAME + " text, "
            + COLUMN_DATE_CREATION + " date, "
            + COLUMN_DATE_LASTUPDATE + " date, "
            + COLUMN_STRENGTH + " text, "
            + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(UUIDTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_UUID);
        onCreate(database);
    }
}
