package com.closecomms.qsrsdkexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.closecomms.qsrcorelibraryclient.CoreManager;
import com.closecomms.qsrcorelibraryclient.authentication.AuthenticationListener;
import com.closecomms.qsrcorelibraryclient.data.models.Branch;
import com.closecomms.qsrcorelibraryclient.data.models.Message;
import com.closecomms.qsrcorelibraryclient.data.models.NotificationOffer;
import com.closecomms.qsrcorelibraryclient.onboarder.NetworkManager;
import com.jakewharton.rxbinding3.view.RxView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

import static com.closecomms.qsrcorelibraryclient.beacon.BeaconProcessor.BEACON_MESSAGE;

public class MainActivity extends AppCompatActivity implements AuthenticationListener, NetworkManager.InternetListener {

    //================================================================================
    // MEMBER VARIABLES
    //================================================================================

    private CoreManager mCoreManager;
    private CompositeDisposable mDisposable;

    //================================================================================
    // VIEWS
    //================================================================================

    @BindView(R.id.btn_authenticate)
    Button mAuthenticate;

    @BindView(R.id.btn_detect_beacons)
    Button mDetectBeacons;

    @BindView(R.id.btn_unlock_wifi)
    Button mUnlockWifi;

    @BindView(R.id.btn_nearest_stores)
    Button mNearestStores;

    @BindView(R.id.btn_valid_location)
    Button mIsValidLocation;

    @BindView(R.id.btn_post_app_event)
    Button mPostEvent;

    @BindView(R.id.event_name)
    EditText mEventName;

    @BindView(R.id.txt_response)
    TextView mResponse;

    @BindView(R.id.user_id)
    EditText mUserID;

    @BindView(R.id.btn_authenticate_with_id)
    Button mAuthenticateWithUserId;

    @BindView(R.id.lat)
    EditText mLat;

    @BindView(R.id.lng)
    EditText mLng;

    @BindView(R.id.btn_get_deal)
    Button mGetFlashDeals;


    //================================================================================
    // LIFECYCLE METHODS
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Timber.d("NETWORK sdk started");
        ButterKnife.bind(this);
        initSDK();
    }

    private void initSDK() {
        Timber.d("NETWORK sdk initialising");
        mCoreManager = new CoreManager(this, AppConstants.CONST_SDK_KEY, this, this, 1000, "https://api.stage.venuenow.tech/", "https://api.s4l.tech/bmsapi/");
        Timber.d("NETWORK sdk version:" + mCoreManager.getVersion());
    }

    private void initListeners() {
        mDisposable = new CompositeDisposable();

        // Note that these calls are using RXJava. You can use whatever you want to initiate the method on CoreManager

        // Authenticate the sdk.  This also sets the API token which is needed to make subsequent calls
        mDisposable.add(
                RxView.clicks(mAuthenticate)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .subscribe(result -> {
                                    mResponse.setText("Authenticating");
                                    mCoreManager.authenticateSDK();
                                },
                                throwable -> Timber.d("Error attempting authenication %s", throwable.getMessage()))
        );

        mDisposable.add(
                RxView.clicks(mUnlockWifi)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                                    Timber.d("WIFI unlocking");
                                    mResponse.setText("Unlocking Wi-Fi");
                                    mUnlockWifi.setEnabled(false);
                                    mCoreManager.startWiFiOnboarding("Free Subway Wifi", "Subway Wi-Fi App", "5Gq:tmmV>P:,yEHj", NetworkManager.ONBOARDING_TYPE.WPA);
                                },
                                throwable -> Timber.d("Error attempting to unlock Wi-Fi %s", throwable.getMessage()))
        );

        // Start search for beacons
        mDisposable.add(
                RxView.clicks(mDetectBeacons)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .subscribe(result -> {
                                    mResponse.setText("Detecting beacons");
                                    registerForBeaconBroadcasts();
                                },
                                throwable -> Timber.d("Error attempting beacon detection %s", throwable.getMessage()))
        );

        // Get nearest stores
        mDisposable.add(
                RxView.clicks(mNearestStores)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .flatMap(view -> {
                            mResponse.setText("Getting stores");
                            return mCoreManager.APIService().observableNearestStoresToLocation(51.0, -113, 50);
                        })
                        .subscribe(stores -> {
                                    if (stores != null && stores.size() > 0) {
                                        StringBuilder storeList = new StringBuilder();
                                        for (Branch branch : stores) {
                                            storeList.append(branch.getZip());
                                            storeList.append(System.getProperty("line.separator"));
                                        }
                                        ResponseActivity.start(this, "Stores:" + storeList.toString());
                                        //mResponse.setText("Stores:" + storeList.toString());
                                    } else {
                                        // mResponse.setText("No stores found");
                                    }
                                },
                                throwable -> Timber.d("Error getting nearest stores %s", throwable.getMessage()))
        );

        // Get valid location for offer
        mDisposable.add(
                RxView.clicks(mIsValidLocation)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .flatMap(view -> {
                            mResponse.setText("Checking for valid location for offer");
                            return mCoreManager.APIService().observableIsInLocation(51.0, -113.0);
                        })
                        .subscribe(validLocation -> {
                                    if (validLocation != null) {
                                        ResponseActivity.start(this, getString(R.string.is_in_valid_location) + validLocation.isInRange());
                                    }
                                },
                                throwable -> Timber.d("Error getting nearest stores %s", throwable.getMessage()))
        );


        // Post an app event. For example, if a user clicks on an offer or navigates to another feature
        mDisposable.add(
                RxView.clicks(mPostEvent)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .flatMap(view -> {
                            mResponse.setText("Sending app event");
                            return mCoreManager.APIService().observablePostAppEvent("TEST EVENT", mEventName.getText().toString());
                        })
                        .subscribe(responseBodyResponse -> {
                                    mResponse.setText("App event " + mEventName.getText().toString() + " sent successfully");
                                },
                                throwable -> Timber.d("Error getting nearest stores %s", throwable.getMessage()))
        );


        // Authenticate with user id
        mDisposable.add(
                RxView.clicks(mAuthenticateWithUserId)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .subscribe(result -> {
                                    mResponse.setText("Authenticating with user id");
                                    mCoreManager.authenticateSDKWithClientLogin(mUserID.getText().toString());
                                },
                                throwable -> Timber.d("Error attempting authenication with user id %s", throwable.getMessage()))
        );

        // Get nearest stores
        mDisposable.add(
                RxView.clicks(mNearestStores)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .flatMap(view -> {
                            mResponse.setText("Getting stores");
                            return mCoreManager.APIService().observableNearestStoresToLocation(51.0, -113, 50);
                        })
                        .subscribe(stores -> {
                                    if (stores != null && stores.size() > 0) {
                                        StringBuilder storeList = new StringBuilder();
                                        for (Branch branch : stores) {
                                            storeList.append(branch.getZip());
                                            storeList.append(System.getProperty("line.separator"));
                                        }
                                        ResponseActivity.start(this, "Stores:" + storeList.toString());
                                    } else {
                                         mResponse.setText("No stores found");
                                    }
                                },
                                throwable -> Timber.d("Error getting nearest stores %s", throwable.getMessage()))
        );

        mDisposable.add(
                RxView.clicks(mGetFlashDeals)
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .flatMap(view -> {
                            mResponse.setText("Getting stores");
                            return mCoreManager.APIService().observableCampaignOffersSentToUser(Double.parseDouble(mLat.getText().toString()), Double.parseDouble(mLng.getText().toString()));
                        })
                        .subscribe(offers -> {
                                    if (offers != null && offers.size() > 0) {
                                        StringBuilder offerList = new StringBuilder();
                                        for (NotificationOffer offer : offers) {
                                            offerList.append(offer.getTitle());
                                            offerList.append(System.getProperty("line.separator"));
                                        }
                                        ResponseActivity.start(this, getString(R.string.status_offers) + offerList.toString());
                                    } else {
                                        mResponse.setText("No offers found for user");
                                    }
                                },
                                throwable -> Timber.d("Error getting offers %s", throwable.getMessage()))
        );

    }

    @Override
    protected void onStart() {
        super.onStart();
        initListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDisposable.clear();
    }

    //================================================================================
    // WIFI ON-BOARDING EVENTS
    //================================================================================

    @Override
    public void internetConnectionAvailable(Boolean connected) {
        if (connected) {
            mUnlockWifi.setEnabled(true);
            mResponse.setText("Wi-Fi connected. Authenticating");
            mCoreManager.authenticateSDK();
        }
    }

    @Override
    public void connectionState(NetworkManager.CONNECTION_STATE connectionState) {
        if (connectionState != null) {
            mResponse.setText("Wi-Fi connection state:" + connectionState.toString());
            mResponse.setText(connectionState.toString());
        }
    }

    @Override
    public void connectionIssue() {
        mUnlockWifi.setEnabled(true);
        mResponse.setText("Wi-Fi  connectionIssue - Access Point connection failed");
    }

    @Override
    public void apMacAddress(String mac) {
        mResponse.setText("Wi-Fi  apMacAddress - Got the ap mac address");
    }

    //================================================================================
    // AUTHENTICATION EVENTS
    //================================================================================

    @Override
    public void error(String s, int i) {
        mResponse.setText("Authentication Error. Code: " + i);
    }

    @Override
    public void success() {
        mResponse.setText("Authentication Succeeded");
    }

    //================================================================================
    // BEACON METHODS
    //================================================================================

    /**
     * Our handler for received Intents. This will be called whenever an Intent is
     * fired from the beacon service
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Message message = intent.getParcelableExtra(BEACON_MESSAGE);
            mResponse.setText("Beacon Message received:" + message.getTitle() + "---" + message.getBody());
        }
    };

    /**
     * Registers the app or service to receive the broadcasted beacon message
     */
    private void registerForBeaconBroadcasts() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(mCoreManager.getBeaconIntentFilter()));
    }
}
