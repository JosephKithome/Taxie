package com.example.taxie;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.github.kmenager.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;

import Common.Common;
import Helper.CustomInforindow;
import Model.Notification;
import Model.FCMResponse;
import Model.Riders;
import Model.Sender;
import Model.Token;
import Remote.IFCMService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private AppBarConfiguration mAppBarConfiguration;
    SupportMapFragment mapFragment;
    private  static  final int MY_PERMISSION_REQUEST_CODE = 8000;
    private static  final  int PLAY_SERVICE_RE_REQUEST =8001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL= 5000;
    private static int FASTEST_INTERVAL = 3000;
    private  static int DISPLACEMENT = 10;

   MaterialAnimatedSwitch location_switch;
    //car animation
    private List<LatLng> polyLineList;
    private Marker mRiderMarker;
    private Float v;
    private  double lat, lng;
    private Handler handler;
    private LatLng startPosition, endPosition,currentPosition;
    private  int index,next;
    private Button btnGo;
    private EditText mEditText;
    private AutocompleteSupportFragment places;
    private String destination;
    private PolylineOptions polyLineOptions, blackPolyLineOptions;
    private Polyline blackPolyLine,greyPolyLine;
    private GoogleMap mMap;

    //Bottomsheet
    ImageView imgExpandable;
    BottomSheetRiderFragment mBottomSheetRiderFragment;
    Button btnPickupRequest;

    boolean isDriverFound = false;
    String driverId ="";
    int radius =1; //km
    int distance = 1; //km
    private static final int LIMIT =3;

    //send Alert
    IFCMService ifcmService;
    DatabaseReference driversAvailable;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        ifcmService = Common.getIFCMService();
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        mapFragment=(SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //init view
        imgExpandable=(ImageView)findViewById(R.id.imageExpandable);
        mBottomSheetRiderFragment =BottomSheetRiderFragment.newInstance("Rider Bottom sheet");
        imgExpandable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetRiderFragment.show(getSupportFragmentManager(),mBottomSheetRiderFragment.getTag());
            }
        });
        btnPickupRequest =(Button)findViewById(R.id.btnPickupRequest);
        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isDriverFound)
                    requestPickupHere(FirebaseAuth.getInstance().getCurrentUser().getUid());
                else 
                    sendRequestTodriver(driverId);

            }
        });


        setUpLocation();
        updateFirebaseTokenOnSignin();
    }

    private void updateFirebaseTokenOnSignin() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens =db.getReference(Common.tokens_tbl);
        Token token =  new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);
    }
    private void sendRequestTodriver(String driverId) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference(Common.tokens_tbl);
        tokens.orderByKey().equalTo(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot postSnapshot :snapshot.getChildren()){
                            Token token = postSnapshot.getValue(Token.class); // getting token object from database with key

                            // make raw payload - convert lat lng to json
                            String json_lat_lng = new Gson().toJson(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                            Notification notification =new Notification("DEVSOFTDevJoseph",json_lat_lng); // sent it to driver and we will deserialize it again
                            Sender content = new Sender(notification,token.getToken()); // send this data to token in cloud

                            ifcmService.sendMessage(content)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                            if (response.body() != null && response.body().success == 1)

                                                Toast.makeText(HomeActivity.this, "Request sent!", Toast.LENGTH_SHORT).show();


                                            else
                                                Toast.makeText(HomeActivity.this, "failed!!", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                                            Log.e(t.getMessage(),"ERROR");

                                        }
                                    });

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void requestPickupHere(String uid) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();


        String userId = firebaseUser.getUid();
        DatabaseReference ref = firebaseDatabase.getReference(Common.pickup_request_tbl);
        GeoFire geoFire = new GeoFire(ref);


        geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()),
                new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (mRiderMarker.isVisible()){
                    mRiderMarker.remove();

                    //Add new Marker
                    mRiderMarker = mMap.addMarker(new MarkerOptions()
                            .title("Pickup Here")
                            .snippet("")
                            .position(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLatitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    mRiderMarker.showInfoWindow();
                    btnPickupRequest.setText("Getting Your Driver");
                    findDriver();

                }
//                Toast.makeText(HomeActivity.this,"You are Active",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void findDriver()  {
        DatabaseReference drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gfDrivers =new GeoFire(drivers);
        GeoQuery geoQuery =gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude())
        ,radius);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //if driver found
                if (!isDriverFound){
                    isDriverFound = true;
                    driverId =key;
                    btnPickupRequest.setText("Request Ride now");
                    Toast.makeText(HomeActivity.this, " "+ key, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //if still not found driver increase distance
                if (!isDriverFound)
                {
                    radius++;
                    findDriver();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED){
                    if (checkPlayServices())
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
//                        if (location_switch.isChecked())
                            displayLocation();
                    }
                }
        }
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            //Request Runtime Permissions
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }
        else{
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
//                if (location_switch.isChecked())
                    displayLocation();
            }
            }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation !=null){
//            if (location_switch.isChecked()){
            driversAvailable =FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
            driversAvailable.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    loadAvailableDrivers();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
                final  double latitude = mLastLocation.getLatitude();
                final double longitude = mLastLocation.getLongitude();

                //update to firebase

                        // Add Marker
                        if (mRiderMarker != null)
                            mRiderMarker.remove();//remove  already marker
                        mRiderMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude,longitude))
                                .title("You"));

                        //move camera to this position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));
                        loadAvailableDrivers();

                        //draw  animation rotate marker
//                        rotateMarker(mCurrent,-360,mMap);
            }

        else{
            Log.d("DEVSOFTDEV", "Cannot get your Current Location");
        }

    }

    private void loadAvailableDrivers() {
        mMap.clear();

        // after clearing the map just add our location again
        mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()))
                      .title("You"));

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl);
        GeoFire geoFire = new GeoFire(driverLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude())
        ,distance);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                //user key to get  email from table users

                FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                //Because Rider and user model are the same so we can use rider model to get user here
                                Riders customer = snapshot.getValue(Riders.class);

                                //Add driver to map
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(location.latitude, location.longitude))
                                        .flat(true)
                                        .title(customer.getName())
                                        .snippet("Phone:" + customer.getPhone())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance <=LIMIT) //distance find for about 3km
                {
                    distance++;
                    loadAvailableDrivers();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS){
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this, PLAY_SERVICE_RE_REQUEST).show();
            else{
                Toast.makeText(this, "Your device is not supported", Toast.LENGTH_SHORT).show();
                finish();

            }
            return  false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap =googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInforindow(this));

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, (com.google.android.gms.location.LocationListener) this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation =location;
        displayLocation();

    }
}