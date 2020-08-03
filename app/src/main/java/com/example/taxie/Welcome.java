package com.example.taxie;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.kmenager.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import Common.Common;
import Remote.IGoogleAPI;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Welcome extends FragmentActivity implements OnMapReadyCallback ,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private GoogleMap mMap;
    private  static  final int MY_PERMISSION_REQUEST_CODE = 8000;
    private static  final  int PLAY_SERVICE_RE_REQUEST =8001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private  Location mLastLocation;

    private static int UPDATE_INTERVAL= 5000;
    private static int FASTEST_INTERVAL = 3000;
    private  static int DISPLACEMENT = 10;

    DatabaseReference drivers;
    Marker mCurrent;
    GeoFire geoFire;

    MaterialAnimatedSwitch location_switch;

    SupportMapFragment mapFragment;

    //car animation
    private List<LatLng> polyLineList;
    private Marker PickupLocationMarker;
    private Float v;
    private  double lat, lng;
    private Handler handler;
    private LatLng startPosition, endposition,currentPosition;
    private  int index,next;
//    private Button btnGo;
    private PlaceAutocompleteFragment places;
    private String destination;
    private PolylineOptions polyLineOptions, blackPolyLineOptions;
    private Polyline blackPolyLine,greyPolyLine;
    private IGoogleAPI mService;
    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
            if (index < polyLineList.size()-1)
            {
                index ++;
                next =index +1;
            }
            if (index < polyLineList.size()-1){
                startPosition = polyLineList.get(index);
                endposition = polyLineList.get(next);

            }
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    v =valueAnimator.getAnimatedFraction();
                    lng = v * endposition.longitude+(1-v)*startPosition.longitude;
                    lat = v * endposition.latitude +(1-v)* startPosition.latitude;
                    LatLng newPos =new LatLng(lat,lng);
                    PickupLocationMarker.setPosition(newPos);
                    PickupLocationMarker.setAnchor(0.5f, 0.5f);
                    PickupLocationMarker.setRotation(getBearing(startPosition,newPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                            .target(newPos)
                            .zoom(15.5f)
                            .build()
                    ));
                }
            });
            valueAnimator.start();
            handler.postDelayed(this,3000);

        }
    };

    private float getBearing(LatLng startPosition, LatLng endPosition) {
        double lat = Math.abs(startPosition.latitude-endPosition.latitude);
        double lng = Math.abs(startPosition.longitude- endPosition.longitude);
        if (startPosition.latitude <  endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lat/lng)));
        else  if (startPosition.latitude >=  endPosition.latitude && startPosition.longitude > endPosition.longitude)
            return (float)(Math.toDegrees((90-Math.atan(lat/lng)))+90);
        else  if (startPosition.latitude >= endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lat/lng))+180);
        else  if (startPosition.latitude <  endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lat/lng)))+270);

        return -1;

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
       mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //init view
        location_switch =(MaterialAnimatedSwitch)findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {
                if (isOnline){
                    startLocationUpdates();
                    displayLocation();
//                    Snackbar.make(mapFragment.getView(), "You are online",Snackbar.LENGTH_SHORT)
//                            .show();
                    Toast.makeText(Welcome.this, "You are online", Toast.LENGTH_SHORT).show();
                }else{
                    stopLocationUpdates();
                    mCurrent.remove();
                    mMap.clear();
                    handler.removeCallbacks(drawPathRunnable);
//                    Snackbar.make(mapFragment.getView(),"You are offline", Snackbar.LENGTH_SHORT)
//
                    Toast.makeText(Welcome.this, "You are offline", Toast.LENGTH_SHORT).show();
                }

            }
        });
        //Geo fire
        polyLineList = new ArrayList<>();

        //places Api
//        btnGo =(Button)findViewById(R.id.btnGo);
        places =(PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.places_autocomplete_fragment);
       places.setOnPlaceSelectedListener(new PlaceSelectionListener() {
           @Override
           public void onPlaceSelected(Place place) {
               if (location_switch.isChecked())
               {
                   destination = place.getAddress().toString();
                   destination = destination.replace("","+");
                   getDirection();
               }
               else {
                   Toast.makeText(Welcome.this, "offline please go ONLINE", Toast.LENGTH_SHORT).show();
               }
                    
           }

           @Override
           public void onError(Status status) {
               Toast.makeText(Welcome.this, ""+status.toString(), Toast.LENGTH_SHORT).show();

           }
       });
//        btnGo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                destination = places.getText().toString();
//                destination.replace("","+");
//                Log.d("DEVSOFTDEV",destination);
//
//                getDirection();
//            }
//        });

        drivers = FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire =new GeoFire(drivers);

        setUpLocation();
        mService = Common.getGoogleAPI();

    }

    private void getDirection() {
        currentPosition = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
        String requestApi;
        try {
            requestApi ="https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference = less_driving&"+
                    "origin="+currentPosition.latitude+ ","+ currentPosition.longitude+"&"+
                    "destination="+ destination+ "&"+
                    "key="+getResources().getString(R.string.google_direction_api);

            Log.d("DEVSOFTDEV",requestApi); //print url for debug
            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                for (int i=0; i<jsonArray.length();i++){
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly= route.getJSONObject("overview_polyline");
                                    String polyline =poly.getString("points");
                                    polyLineList = decodePoly(polyline);
                                    //adjusting bounds
                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                    for (LatLng latLng:polyLineList)
                                        builder.include(latLng);
                                    LatLngBounds bounds = builder.build();

                                    CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,2);
                                    mMap.animateCamera(mCameraUpdate);

                                    polyLineOptions = new PolylineOptions();
                                    polyLineOptions.color(Color.GRAY);
                                    polyLineOptions.width(5);
                                    polyLineOptions.startCap(new SquareCap());
                                    polyLineOptions.endCap(new SquareCap());
                                    polyLineOptions.jointType(JointType.ROUND);
                                    polyLineOptions.addAll(polyLineList);
                                    greyPolyLine = mMap.addPolyline(polyLineOptions);

                                    blackPolyLineOptions = new PolylineOptions();
                                    blackPolyLineOptions.color(Color.BLACK);
                                    blackPolyLineOptions.width(5);
                                    blackPolyLineOptions.startCap(new SquareCap());
                                    blackPolyLineOptions.endCap(new SquareCap());
                                    blackPolyLineOptions.jointType(JointType.ROUND);
                                    blackPolyLine = mMap.addPolyline(blackPolyLineOptions);

                                    mMap.addMarker(new MarkerOptions().position(polyLineList.get(polyLineList.size()-1))
                                    .title("Pickup station"));

                                    //add animation
                                    ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0,100);
                                    polyLineAnimator.setDuration(2000);
                                    polyLineAnimator.setInterpolator(new LinearInterpolator());
                                    polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                            List<LatLng>points = greyPolyLine.getPoints();
                                            int parcentValue =(int)valueAnimator.getAnimatedValue();
                                            int size =points.size();
                                            int newPoints = (int) (size * (parcentValue/100.0f));
                                            List<LatLng> p = points.subList(0,newPoints);
                                            blackPolyLine.setPoints(p);

                                        }
                                    });
                                    polyLineAnimator.start();
                                    PickupLocationMarker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));

                                    handler = new Handler();
                                    index = 1;
                                    next =1;
                                    handler.postDelayed(drawPathRunnable,3000);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(Welcome.this,""+t.getMessage(),Toast.LENGTH_SHORT).show();

                        }
                    });



        }catch (Exception e){
            e.printStackTrace();
        }


    }
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    // press control O because we need  to override on permission request method


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED){
                    if (checkPlayServices())
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
                        if (location_switch.isChecked())
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
            if (checkPlayServices())
            {
                buildGoogleApiClient();
                createLocationRequest();
                if (location_switch.isChecked())
                    displayLocation();
            }
        }

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

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation !=null){
            if (location_switch.isChecked()){
                final  double latitude = mLastLocation.getLatitude();
                final double longitude = mLastLocation.getLongitude();

                //update to firebase
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        // Add Marker
                        if (mCurrent != null)
                            mCurrent.remove();//remove  already marker
                        mCurrent = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude,longitude))
                                .title("You"));

                        //move camera to this position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));

                        //draw  animation rotate marker
//                        rotateMarker(mCurrent,-360,mMap);
                    }
                });
            }
        }
        else{
            Log.d("ERROR", "Cannot get your Current Location");
        }


    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation =mCurrent.getRotation();
        final long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float)elapsed/duration);
                float rot = t*i+(1-t)*startRotation;
                mCurrent.setRotation(-rot >180?rot/2:rot);
                if (t<1.0 ){
                    handler.postDelayed(this,16);
                }
            }
        });

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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();

    }

//    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

//    @Override
    public void onProviderEnabled(String s) {

    }

//    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}