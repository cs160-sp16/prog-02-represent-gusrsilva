package gusrsilva.represent;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener
        , GoogleApiClient.ConnectionCallbacks, ShakeEventListener.ShakeListener {


    private TextView mTextView;
    public static String TAG = "Represent!", zipCode = "00000";
    private static GoogleApiClient mWatchApiClient;
    private List<Node> nodes = new ArrayList<>();
    private static int repNumber = -1;
    private ShakeEventListener shaker;
    private CardAdapter adapter;
    public static String KEY_ZIP_CODE = "zip_code";
    private final String PATH_REP_NUM = "/rep_num", PATH_ZIP_CODE = "/zip_code";
    private static final int ACTION_SEND_REP_NUM = 0, ACTION_SEND_RANDOM_ZIP = 1;
    private static int watchAction = -1;
    private long lastShake = System.currentTimeMillis(), SHAKE_TIME_THRESHOLD = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final GridViewPager pager = (GridViewPager)findViewById(R.id.pager);
        //PagerAdapter adapter = new PagerAdapter(getApplicationContext(), getFragmentManager());

        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if( ConnectionResult.SUCCESS != result ){
            Log.d(TAG, "Showing update dialog");
            // Show appropriate dialog
            Dialog d = GooglePlayServicesUtil.getErrorDialog(result, this, 0);
            d.show();
        }

        ArrayList<Rep> repList = new ArrayList<>();
        Rep rep1 = new Rep();
        rep1.setRepType("Senator");
        rep1.setName("Barbara Boxer");
        rep1.setParty("Democrat");
        rep1.setEmail("Sen.Boxer@opencongress.org");
        rep1.setWebsite("www.boxer.senate.gov");
        rep1.setImageResource(R.drawable.rep1);
        rep1.setColor(ContextCompat.getColor(getApplicationContext(), R.color.dem_blue));

        Rep rep2 = new Rep();
        rep2.setRepType("Senator");
        rep2.setName("Dianne Feinstein");
        rep2.setParty("Democrat");
        rep2.setImageResource(R.drawable.rep2);
        rep2.setColor(ContextCompat.getColor(getApplicationContext(), R.color.dem_blue));

        Rep rep3 = new Rep();
        rep3.setRepType("Representative");
        rep3.setName("Paul Cook");
        rep3.setParty("Republican");
        rep3.setImageResource(R.drawable.rep3);
        rep3.setColor(ContextCompat.getColor(getApplicationContext(), R.color.rep_red));

        Rep dummy = new Rep(); dummy.setName("dummy"); //Dummy to make room for 2012 election view

        repList.add(rep1);repList.add(rep2);repList.add(rep3);repList.add(dummy);
        adapter = new CardAdapter(repList, getApplicationContext());
        pager.setAdapter(adapter);


        if(mWatchApiClient == null || !mWatchApiClient.isConnected())
        {
            mWatchApiClient = new GoogleApiClient.Builder(this)
                    .addApi( Wearable.API )
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        shaker = new ShakeEventListener(this);
        zipCode = getIntent().getStringExtra(KEY_ZIP_CODE);
        if(zipCode == null)
            zipCode = "00000";
    }

    @Override
    public void onPause()
    {
        shaker.unregisterListener();
        super.onPause();
    }

    @Override
    public void onResume()
    {
        shaker.registerListener();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mWatchApiClient != null)
            mWatchApiClient.disconnect();
    }

    public static void cardClicked(int pos)
    {
        Log.d("Represent!", "cardClicked() called");
        repNumber = pos;
        watchAction = ACTION_SEND_REP_NUM;
        mWatchApiClient.disconnect();
        mWatchApiClient.connect();
    }

    @Override //alternate method to connecting: no longer create this in a new thread, but as a callback
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected() called! Watch Action: " + watchAction);
        if(watchAction == ACTION_SEND_REP_NUM) {
            Wearable.NodeApi.getConnectedNodes(mWatchApiClient)
                    .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                        @Override
                        public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                            nodes = getConnectedNodesResult.getNodes();
                            //Log.d("T", "found nodes");
                            //when we find a connected node, we populate the list declared above
                            //finally, we can send a message
                            sendMessage(PATH_REP_NUM, repNumber + "");
                            Log.d(TAG, "Sent Rep number: " + repNumber);
                        }
                    });
        }
        else if(watchAction == ACTION_SEND_RANDOM_ZIP)
        {
            Wearable.NodeApi.getConnectedNodes(mWatchApiClient)
                    .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                        @Override
                        public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                            nodes = getConnectedNodesResult.getNodes();
                            //Log.d("T", "found nodes");
                            //when we find a connected node, we populate the list declared above
                            //finally, we can send a message
                            sendMessage(PATH_ZIP_CODE,  zipCode);
                            Log.d(TAG, "Sent Zip Code: " + zipCode);
                        }
                    });
            //restartActivity();
        }
        else
            Log.d(TAG, "Invalid Watch Action Specified");
    }

    @Override //we need this to implement GoogleApiClient.ConnectionsCallback
    public void onConnectionSuspended(int i) {}


    private void sendMessage(final String path, final String text ) {
        for (Node node : nodes) {
            Wearable.MessageApi.sendMessage(
                    mWatchApiClient, node.getId(), path, text.getBytes());
        }
        if(path == PATH_REP_NUM)
        {
            Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.OPEN_ON_PHONE_ANIMATION);
            startActivity(intent);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed! " + connectionResult.toString());
    }

    @Override
    public void onShake() { //TODO: Maybe use current time to make sure shakes are at least 1 second apart

        //Log.d(TAG, "onShake()");

        long thisShake = System.currentTimeMillis();
        long difference = thisShake - lastShake;

        if(difference > SHAKE_TIME_THRESHOLD)
        {
            lastShake = thisShake;
            zipCode = generateRandomZip();
            watchAction = ACTION_SEND_RANDOM_ZIP;
            Log.d(TAG, "thisShake: " + thisShake + "   lastShake: " + lastShake + "   diff: " + difference);
            Log.d(TAG, "from onShake sending random zip: " + zipCode);
            adapter.notifyDataSetChanged();
            mWatchApiClient.disconnect();
            mWatchApiClient.connect();

            Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
            startActivity(intent);

            if(adapter.mChart != null) {
                adapter.setData(2, 100);
                adapter.mChart.invalidate();
                adapter.mChart.notifyDataSetChanged();
            }
        }

    }

    @Override
    public void onLittleShake() {
        //Log.d(TAG, "onLittleShake");
        //Toast.makeText(getApplicationContext(), "Little Shake!", Toast.LENGTH_SHORT).show();

    }

    private String generateRandomZip()
    {
        Random rand = new Random(System.currentTimeMillis());
        int next = rand.nextInt(899999) + 10000;
        return String.valueOf(next);
    }

    private void restartActivity()
    {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}
