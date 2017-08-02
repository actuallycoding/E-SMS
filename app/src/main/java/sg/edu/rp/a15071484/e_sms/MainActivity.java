package sg.edu.rp.a15071484.e_sms;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private TextView tv;
    private LocationRequest mLocationRequest;
    private Button btnAdd, btnSend, btnDelete;
    private EditText etPhone;
    private ListView lv;
    private ArrayList<String> alNum = new ArrayList<String>();
    private ArrayAdapter<String> aaNum;
    private DBHelper dbh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.textView);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnDelete = (Button)findViewById(R.id.btnDeleteAll);
        etPhone = (EditText) findViewById(R.id.editTextPhone);
        lv = (ListView) findViewById(R.id.lv);
        dbh = new DBHelper(MainActivity.this);
        alNum = dbh.getAllNotes();
        aaNum = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, alNum);
        lv.setAdapter(aaNum);
        getLocation();
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbh.removeAllItems();
                alNum.clear();
                aaNum.notifyDataSetChanged();
            }
        });
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = etPhone.getText().toString();
                dbh.insertNote(number);
                alNum.add(number);
                aaNum.notifyDataSetChanged();
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = "Currently need help @ " + tv.getText().toString();
                String toNumbers = "";
                for (String s : alNum) {
                    toNumbers = toNumbers + s + ";";
                }
                toNumbers = toNumbers.substring(0, toNumbers.length() - 1);

                Uri sendSmsTo = Uri.parse("smsto:" + toNumbers);
                Intent intent = new Intent(
                        android.content.Intent.ACTION_SENDTO, sendSmsTo);
                intent.putExtra("sms_body", msg);
                startActivity(intent);
            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()

        {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {
                String selectedNum = alNum.get(position).toString();
                AlertDialog.Builder myBuilder = new AlertDialog.Builder(MainActivity.this);
                myBuilder.setTitle("Delete");
                myBuilder.setMessage("Are you sure you want to delete this number: " + selectedNum + " ?");
                myBuilder.setCancelable(false);
                myBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        alNum.remove(position);
                        dbh.deleteNote(position);
                        alNum = dbh.getAllNotes();
                        aaNum.notifyDataSetChanged();
                    }
                });
                myBuilder.setNeutralButton("Cancel", null);
                AlertDialog myDialog = myBuilder.create();
                myDialog.show();
            }
        });
    }

    public void getLocation() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest
                .PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setSmallestDisplacement(10);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        int permissionCheck_Coarse = PermissionChecker.checkSelfPermission(
                MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionCheck_Fine = PermissionChecker.checkSelfPermission(
                MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck_Coarse != PermissionChecker.PERMISSION_GRANTED
                || permissionCheck_Fine != PermissionChecker.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);

            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        getLocation();

        if (mLocation != null) {

            Log.d("test", mLocation.getLatitude() + "," + mLocation.getLongitude());
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {

                HttpRequest request = new HttpRequest("http://maps.googleapis.com/maps/api/geocode/json?latlng=" + mLocation.getLatitude() + "," + mLocation.getLongitude() + "&sensor=true");
                request.setMethod("GET");
                request.execute();

                /******************************/
                try {
                    String jsonString = request.getResponse();
                    Log.d("JsonString", "jsonString: " + jsonString);

                    JSONObject jsonObj = (JSONObject) new JSONTokener(jsonString).nextValue();
                    Log.d("address", jsonObj.getJSONArray("results") + "");
                    JSONArray address = jsonObj.getJSONArray("results");
                    String currentAddress = address.getJSONObject(0).getString("formatted_address");
                    Log.d("LOL", currentAddress);
                    tv.setText(currentAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "No network connection available.", Toast.LENGTH_SHORT).show();
            }


        } else {
            Toast.makeText(this, "Location not Detected",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //the detected location is given by the variable location in the signature

        Log.d("test", location.getLatitude() + "," + location.getLongitude());
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {

            HttpRequest request = new HttpRequest("http://maps.googleapis.com/maps/api/geocode/json?latlng=" + location.getLatitude() + "," + location.getLongitude() + "&sensor=true");
            request.setMethod("GET");
            request.execute();

            /******************************/
            try {
                String jsonString = request.getResponse();
                Log.d("JsonString", "jsonString: " + jsonString);

                JSONObject jsonObj = (JSONObject) new JSONTokener(jsonString).nextValue();
                Log.d("address", jsonObj.getJSONArray("results") + "");
                JSONArray address = jsonObj.getJSONArray("results");
                String currentAddress = address.getJSONObject(0).getString("formatted_address");
                Log.d("LOL", currentAddress);
                tv.setText(currentAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "No network connection available.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                            mLocationRequest, this);
                } else {
                    // permission denied... notify user
                    Toast.makeText(MainActivity.this, "Permission not granted",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

