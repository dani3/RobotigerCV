package com.robotiger;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageButton;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{
    /* Constants */
    private static final String TAG = "CLIENT";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String MAC_ADDRESS = "00:13:EF:00:0E:5F";
    private static final int REQUEST_ENABLE_BT = 1;
    private static boolean CONNECTED;
    private static boolean PRESSED;

    /* Bluetooth Adapter */
    private static BluetoothAdapter mBluetoothAdapter;

    /* UI elements */
    private CoordinatorLayout mCoordinatorLayout;
    private FloatingActionButton mPowerImageButton;
    private ImageButton mUpArrowImageButton;
    private ImageButton mLeftArrowImageButton;
    private ImageButton mRightArrowImageButton;
    private ImageButton mDownArrowImageButton;

    /* Animators */
    private ObjectAnimator mPulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        CONNECTED = false;
        PRESSED = false;

        mCoordinatorLayout     = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mPowerImageButton      = (FloatingActionButton) findViewById(R.id.powerImageButton);
        mUpArrowImageButton    = (ImageButton) findViewById(R.id.upArrowImageButton);
        mDownArrowImageButton  = (ImageButton) findViewById(R.id.downArrowImageButton);
        mLeftArrowImageButton  = (ImageButton) findViewById(R.id.leftArrowImageButton);
        mRightArrowImageButton = (ImageButton) findViewById(R.id.rightArrowImageButton);

        mUpArrowImageButton.setOnTouchListener(this);
        mRightArrowImageButton.setOnTouchListener(this);
        mLeftArrowImageButton.setOnTouchListener(this);
        mDownArrowImageButton.setOnTouchListener(this);

        mPowerImageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!CONNECTED)
                {
                    new ConnectThread().execute();
                }
            }
        });

        mPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(mPowerImageButton
                                                        , PropertyValuesHolder.ofFloat("scaleX", 1.2f)
                                                        , PropertyValuesHolder.ofFloat("scaleY", 1.2f));
        mPulseAnimator.setDuration(310);
        mPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mPulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        // Turn on Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        switch (v.getId())
        {
            case (R.id.upArrowImageButton):
                _animateArrow(mUpArrowImageButton, event);
                break;

            case (R.id.downArrowImageButton):
                _animateArrow(mDownArrowImageButton, event);
                break;

            case (R.id.leftArrowImageButton):
                _animateArrow(mLeftArrowImageButton, event);
                break;

            case (R.id.rightArrowImageButton):
                _animateArrow(mRightArrowImageButton, event);
                break;
        }

        return true;
    }

    private void _animateArrow(ImageButton arrow, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (!PRESSED)
            {
                PRESSED = true;

                arrow.animate().scaleX(1.3f)
                               .scaleY(1.3f)
                               .setDuration(200)
                               .start();
                arrow.setBackgroundTintList(
                        ColorStateList.valueOf(
                                this.getColor(android.R.color.holo_blue_bright)));
            }

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            PRESSED = false;

            arrow.animate().scaleX(1.0f)
                           .scaleY(1.0f)
                           .setDuration(200)
                           .start();
            arrow.setBackgroundTintList(
                    ColorStateList.valueOf(
                            this.getColor(R.color.colorAccent)));
        }
    }

    /**
     * AsyncTask which connects to the Arduino
     */
    private class ConnectThread extends AsyncTask<Void, Void, Void>
    {
        private BluetoothSocket mSocket;
        private BluetoothDevice mDevice;

        @Override
        protected void onPreExecute()
        {
            mPulseAnimator.start();

            mDevice = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS);

            try
            {
                // MY_UUID is the app's UUID string
                mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "Error creating RFCOMM socket");
            }
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.d(TAG, "Connecting...");

                mSocket.connect();

                CONNECTED = true;

                Log.d(TAG, "Connected!");

            } catch (IOException connectException) {
                Log.e(TAG, "Error connectig to the Arduino");

                try
                {
                    mSocket.close();

                } catch (IOException closeException) {
                    Log.e(TAG, "Error closing the socket");
                }

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v)
        {
            mPulseAnimator.cancel();

            if (CONNECTED)
            {
                Snackbar.make(mCoordinatorLayout, "Conectado", Snackbar.LENGTH_SHORT).show();

                mPowerImageButton.setBackgroundTintList(
                        ColorStateList.valueOf(
                                getColor(R.color.colorLightGreen)));
            } else {
                Snackbar.make(mCoordinatorLayout, "Error al conectar", Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}
