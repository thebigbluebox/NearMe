package net.jmodwyer.beacon.beaconPoC;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;

import net.jmodwyer.beacon.beaconPoC.contentprovider.UUIDContentProvider;
import net.jmodwyer.beacon.beaconPoC.databases.UUIDDatabaseHelper;
import net.jmodwyer.beacon.beaconPoC.databases.UUIDTable;
import net.jmodwyer.ibeacon.ibeaconPoC.R;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Adapted from original code written by D Young of Radius Networks.
 * @author dyoung, jodwyer
 *
 */
public class ScanActivity extends ListActivity implements
                                                        LoaderManager.LoaderCallbacks<Cursor>,
                                                        BeaconConsumer,
														GooglePlayServicesClient.ConnectionCallbacks,
														GooglePlayServicesClient.OnConnectionFailedListener {
    
	// Constant Declaration
	private static final String PREFERENCE_SCANINTERVAL = "scanInterval";
	private static final String PREFERENCE_TIMESTAMP = "timestamp";
	private static final String PREFERENCE_POWER = "power";
	private static final String PREFERENCE_PROXIMITY = "proximity";
	private static final String PREFERENCE_RSSI = "rssi";
	private static final String PREFERENCE_MAJORMINOR = "majorMinor";
	private static final String PREFERENCE_UUID = "uuid";
	private static final String PREFERENCE_INDEX = "index";
    private static final String PREFERENCE_LOCATION = "location";
    private static final String MODE_SCANNING = "STOP SCANNING";
    private static final String MODE_STOPPED = "START SCANNING";
    protected static final String TAG = "ScanActivity";
    private SimpleCursorAdapter adapter;
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private FileHelper fileHelper; 
    private BeaconManager beaconManager;
    private Region region; 
    private int eventNum = 1;
    
    // This StringBuffer will hold the scan data for any given scan.  
    private StringBuffer logString;
   
    // Preferences - will actually have a boolean value when loaded.
    private Boolean index;
    private Boolean location;
    private Boolean uuid;
	private Boolean majorMinor;
	private Boolean rssi;
	private Boolean proximity;
	private Boolean power;
	private Boolean timestamp;
	private String scanInterval;
    
	// LocationClient for Google Play Location Services
	LocationClient locationClient;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanlist);
		verifyBluetooth();
        fillData();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		BeaconScannerApp app = (BeaconScannerApp)this.getApplication();
		beaconManager = app.getBeaconManager();
		//beaconManager.setForegroundScanPeriod(10);

		// Add parser for iBeacons;
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
		beaconManager.bind(this);
		
		region = new Region("myRangingUniqueId", null, null, null);

		fileHelper = app.getFileHelper();
		// Initialise scan button.
		getScanButton().setText(MODE_STOPPED);

	    locationClient = new LocationClient(this, this, this);
    }
    

    public String getCurrentLocation() {
        /** Default "error" value is set for location, will be overwritten with the correct lat and
         *  long values if we're ble to connect to location services and get a reading.
         */
        String location = "Unavailable";
        if (locationClient.isConnected()) {
            Location currentLocation = locationClient.getLastLocation();
            if (currentLocation != null) {
                location = Double.toString(currentLocation.getLatitude()) + "," +
                        Double.toString(currentLocation.getLongitude());
            }
        }
        return location;
     }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBeaconServiceConnect() {}
    
    /**
     * 
     * @param view
     */
	public void onScanButtonClicked(View view) {
		toggleScanState();
	}
	
 	// Handle the user selecting "Settings" from the action bar.
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    	case R.id.Settings:
	            // Show settings
	    		Intent api = new Intent(this, AppPreferenceActivity.class);
	            startActivityForResult(api, 0);
	            return true;
	    	case R.id.action_listfiles:
	    		// Launch list files activity
	    		Intent fhi = new Intent(this, FileHandlerActivity.class);
	            startActivity(fhi);
	            return true;	    			    		
	        default:
	            return super.onOptionsItemSelected(item);
	     }
	 }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i;
    }

    private void fillData() {
        // Fields from the database (projection)
        // Must include the _id column for the adapter to work
        String[] from = new String[] { UUIDTable.COLUMN_NAME , UUIDTable.COLUMN_DATE_CREATION, UUIDTable.COLUMN_STRENGTH}; //need to fill in strength
        // Fields on the UI to which we map
        int[] to = new int[] { R.id.name , R.id.date ,R.id.strength};

        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(this, R.layout.activity_scanrow, null, from, to, 0);

        setListAdapter(adapter);
    }

    // creates a new loader after the initLoader () call
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { UUIDTable.COLUMN_ID, UUIDTable.COLUMN_NAME, UUIDTable.COLUMN_DATE_CREATION, UUIDTable.COLUMN_DATE_LASTUPDATE
        ,UUIDTable.COLUMN_UUID};
        CursorLoader cursorLoader = new CursorLoader(this,
                UUIDContentProvider.CONTENT_URI, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // data is not available anymore, delete reference
        adapter.swapCursor(null);
    }

  //end of where it is
	/**
	 * Start and stop scanning, and toggle button label appropriately.
	 */
	private void toggleScanState() {
		Button scanButton = getScanButton();
		String currentState = scanButton.getText().toString();
		if (currentState.equals(MODE_SCANNING)) {
			stopScanning(scanButton);
            scanButton.setText(MODE_STOPPED);
		} else {
			startScanning(scanButton);
            scanButton.setText(MODE_SCANNING);
		}
	}

	/**
	 * start looking for beacons.
	 */
	private void startScanning(Button scanButton) {
		
		// Set UI elements to the correct state.

		
		// Reset event counter
		eventNum = 1;
		// Get current values for logging preferences
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);		
	    HashMap <String, Object> prefs = new HashMap<String, Object>();
	    prefs.putAll(sharedPrefs.getAll());
	    
	    index = (Boolean)prefs.get(PREFERENCE_INDEX);
        location = (Boolean)prefs.get(PREFERENCE_LOCATION);
	    uuid = (Boolean)prefs.get(PREFERENCE_UUID);
		majorMinor = (Boolean)prefs.get(PREFERENCE_MAJORMINOR);
		rssi = (Boolean)prefs.get(PREFERENCE_RSSI); 
		proximity = (Boolean)prefs.get(PREFERENCE_PROXIMITY);
		power = (Boolean)prefs.get(PREFERENCE_POWER);
		timestamp = (Boolean)prefs.get(PREFERENCE_TIMESTAMP);
		scanInterval = (String)prefs.get(PREFERENCE_SCANINTERVAL);
		
		// Get current background scan interval (if specified)
		if (prefs.get(PREFERENCE_SCANINTERVAL) != null) {
			beaconManager.setBackgroundBetweenScanPeriod(Long.parseLong(scanInterval));
		}
		
//		logToDisplay("Scanning...");
		
		// Initialise scan log
		logString = new StringBuffer();
		
		//Start scanning again.
        beaconManager.setRangeNotifier(new RangeNotifier() {
        	@Override 
        	public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        		if (beacons.size() > 0) {
        			Iterator <Beacon> beaconIterator = beacons.iterator();
        			while (beaconIterator.hasNext()) {
        				Beacon beacon = beaconIterator.next();
        				logBeaconData(beacon);
        			}
        		}
        	}
        });

        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {   
        	// TODO - OK, what now then?
        }	

	}

	/**
	 * Stop looking for beacons.
	 */
	private void stopScanning(Button scanButton) {
		try {
			beaconManager.stopRangingBeaconsInRegion(region);
		} catch (RemoteException e) {
				// TODO - OK, what now then?
		}
//		String scanData = logString.toString();
//		if (scanData.length() > 0) {
//			// Write file
//			//fileHelper.createFile(scanData);
//			// Display file created message.
//			Toast.makeText(getBaseContext(),
//					"File saved to:" + getFilesDir().getAbsolutePath(),
//					Toast.LENGTH_SHORT).show();
//			scanButton.setText(MODE_STOPPED);
//		} else {
//			// We didn't get any data, so there's no point writing an empty file.
//			Toast.makeText(getBaseContext(),
//					"No data captured during scan, output file will not be created.",
//					Toast.LENGTH_SHORT).show();
			scanButton.setText(MODE_STOPPED);
//		}
	}

	/**
	 * 
	 * @return reference to the start/stop scanning button
	 */
	private Button getScanButton() {
		return (Button)findViewById(R.id.scanButton);
	}
	
    /**
     * 
     * @param beacon The detected beacon
     */
	private void logBeaconData(Beacon beacon) {
        //UUID values
        Uri UUIDuri;
        ContentValues uuidValues = new ContentValues();
        uuidValues.put(UUIDTable.COLUMN_NAME, "Blood Services");
        uuidValues.put(UUIDTable.COLUMN_UUID, beacon.getId1().toString());
        uuidValues.put(UUIDTable.COLUMN_STRENGTH, BeaconHelper.getProximityString(beacon.getDistance()));
        Log.i("FUCK",beacon.getId1().toString());
        String[] categoryProjection = { UUIDTable.COLUMN_UUID};
        if (getContentResolver().query(Uri.parse(UUIDContentProvider.CONTENT_URI + beacon.getId1().toString()), categoryProjection, null, null , null) == null && beacon.getId1() != null) {

            UUIDuri = getContentResolver().insert(UUIDContentProvider.CONTENT_URI, uuidValues);
            //gets the new id from the given gategory uri
        } else {
            // Update category
            getContentResolver().update(Uri.parse(UUIDContentProvider.CONTENT_URI  + beacon.getId1().toString()), uuidValues, null, null);
        }

        fillData();
	}
    

//    private void logToDisplay(final String line) {
//    	runOnUiThread(new Runnable() {
//    	    public void run() {
//    	    	EditText editText = (EditText)ScanActivity.this
//    					.findViewById(R.id.scanText);
//    	    }
//    	});
//    }
    
 	private void verifyBluetooth() {

		try {
			if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Bluetooth not enabled");			
				builder.setMessage("Please enable bluetooth in settings and restart this application.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						finish();
			            System.exit(0);					
					}					
				});
				builder.show();
			}			
		}
		catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Bluetooth LE not available");			
			builder.setMessage("Sorry, this device does not support Bluetooth LE.");
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					finish();
		            System.exit(0);					
				}
				
			});
			builder.show();
			
		}
		
	}

    /* Location services code follows */

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        locationClient.connect();
    }

    @Override
    protected void onStop() {
        // Disconnect the client.
        locationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Uncomment the following line to display the connection status.
        // Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

         /* Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Toast.makeText(getBaseContext(),
                    "Location services not available, cannot track device location.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    /*
     * Handle results returned to the FragmentActivity
     * by Google Play services
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
             switch (resultCode) {
                case Activity.RESULT_OK :
                 /*
                  * TODO - Try the request again
                  */
                  break;
             }
        }
    }

}

