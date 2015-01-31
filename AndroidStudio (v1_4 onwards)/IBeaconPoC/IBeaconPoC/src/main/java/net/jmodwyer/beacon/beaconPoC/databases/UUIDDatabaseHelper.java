package net.jmodwyer.beacon.beaconPoC.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by tai on 31/01/15.
 */
public class UUIDDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "categories.db";
    private static final int DATABASE_VERSION = 1;

    public UUIDDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        UUIDTable.onCreate(database);
    }

    // Method is called during an upgrade of the database,
    // e.g. if you increase the database version
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
                          int newVersion) {
        UUIDTable.onUpgrade(database, oldVersion, newVersion);
    }
}
