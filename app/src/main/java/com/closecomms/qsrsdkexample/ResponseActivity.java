package com.closecomms.qsrsdkexample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class ResponseActivity extends AppCompatActivity {

    //================================================================================
    // MEMBER CONSTANTS
    //================================================================================

    public final static String ARG_RESPONSE = "response";

    //================================================================================
    // MEMBER VARIABLES
    //================================================================================

    private String mRepsonseJson;

    //================================================================================
    // VIEWS
    //================================================================================

    @BindView(R.id.response)
    TextView mResponse;

    //================================================================================
    // LIFECYCLE METHODS
    //================================================================================

    public static void start(Context context, String response) {
        Intent intent = new Intent(context, ResponseActivity.class);
        intent.putExtra(ARG_RESPONSE, response);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response);
        Timber.d("NETWORK sdk started");
        ButterKnife.bind(this);
        mResponse.setMovementMethod(new ScrollingMovementMethod());
        getIntentData();
    }

    //================================================================================
    // DATA
    //================================================================================

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            mRepsonseJson = intent.getStringExtra(ARG_RESPONSE);
            populateForm();
        }
    }

    //================================================================================
    // UI
    //================================================================================

    private void populateForm() {
        mResponse.setText(mRepsonseJson);
    }
}
