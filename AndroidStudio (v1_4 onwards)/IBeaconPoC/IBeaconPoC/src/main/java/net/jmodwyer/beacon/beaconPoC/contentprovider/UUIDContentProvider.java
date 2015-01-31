package net.jmodwyer.beacon.beaconPoC.contentprovider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import net.jmodwyer.beacon.beaconPoC.databases.UUIDDatabaseHelper;
import net.jmodwyer.beacon.beaconPoC.databases.UUIDTable;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by tai on 31/01/15.
 */
public class UUIDContentProvider extends ContentProvider{
    private UUIDDatabaseHelper uuiddatabase;

    // used for the UriMacher
    private static final int COMPLETEUUID = 10;
    private static final int UUID_ID = 20;
    private static final int UUID = 30;

    private static final String AUTHORITY = "net.jmodwyer.beaconPoC.contentprovider.uuid";

    private static final String BASE_PATH = "uuid";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/beaconPoC";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/uuid";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, COMPLETEUUID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", UUID_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/UUID/#", UUID);
    }



    @Override
    public boolean onCreate() {
        uuiddatabase = new UUIDDatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(UUIDTable.TABLE_UUID);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case COMPLETEUUID:
                break;
            case UUID_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(UUIDTable.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            case UUID:
                queryBuilder.appendWhere(UUIDTable.COLUMN_UUID + "="
                        + uri.getLastPathSegment());
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = uuiddatabase.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = uuiddatabase.getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case COMPLETEUUID:
                id = sqlDB.insert(UUIDTable.TABLE_UUID, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = uuiddatabase.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
            case COMPLETEUUID:
                rowsDeleted = sqlDB.delete(UUIDTable.TABLE_UUID, selection,
                        selectionArgs);
                break;
            case UUID_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(UUIDTable.TABLE_UUID,
                            UUIDTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(UUIDTable.TABLE_UUID,
                            UUIDTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            case UUID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(UUIDTable.TABLE_UUID,
                            UUIDTable.COLUMN_UUID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(UUIDTable.TABLE_UUID,
                            UUIDTable.COLUMN_UUID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = uuiddatabase.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case COMPLETEUUID:
                rowsUpdated = sqlDB.update(UUIDTable.TABLE_UUID,
                        values,
                        selection,
                        selectionArgs);
                break;
            case UUID_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(UUIDTable.TABLE_UUID,
                            values,
                            UUIDTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(UUIDTable.TABLE_UUID,
                            values,
                            UUIDTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case UUID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(UUIDTable.TABLE_UUID,
                            values,
                            UUIDTable.COLUMN_UUID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(UUIDTable.TABLE_UUID,
                            values,
                            UUIDTable.COLUMN_UUID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
    private void checkColumns(String[] projection) {
        String[] available = { UUIDTable.COLUMN_DATE_LASTUPDATE,
                UUIDTable.COLUMN_DATE_CREATION, UUIDTable.COLUMN_ID,
                UUIDTable. COLUMN_NAME };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
