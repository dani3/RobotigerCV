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
import android.widget.ImageButton;
import android.widget.SeekBar;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{
    /* Constants */
    private static final String TAG = "CLIENT";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String MAC_ADDRESS = "00:13:EF:00:0E:5F";
    private static final int REQUEST_ENABLE_BT = 1;
    private static boolean CONNECTED;
    private static boolean MOVING;
    private static boolean ROTATING;

    /* Commands */
    private static final byte UNUSED = 0x00;

    private static final byte MOVE_BACKWARDS = 0x00;
    private static final byte MOVE_RIGHT     = 0x10;
    private static final byte MOVE_LEFT      = 0x20;
    private static final byte MOVE_FORWARD   = 0x30;
    private static final byte HALT           = 0x40;

    private static final byte WRIST = 0x50;

    private static final byte ROTATE_RIGHT = 'e';
    private static final byte ROTATE_LEFT  = 'q';
    private static final byte ROTATE_STOP  = 'z';

    /* Bluetooth Adapter */
    private static BluetoothAdapter mBluetoothAdapter;

    /* Bluetooth device / socket */
    private static BluetoothSocket mSocket;

    private static OutputStream os;

    /* UI elements */
    private CoordinatorLayout mCoordinatorLayout;
    private FloatingActionButton mPowerImageButton;
    private ImageButton mUpArrowImageButton;
    private ImageButton mLeftArrowImageButton;
    private ImageButton mRightArrowImageButton;
    private ImageButton mDownArrowImageButton;
    private ImageButton mRotateRightImageButton;
    private ImageButton mRotateLeftImageButton;

    /* Animators */
    private ObjectAnimator mPulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        CONNECTED = false;
        MOVING    = false;
        ROTATING  = false;

        mCoordinatorLayout      = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mPowerImageButton       = (FloatingActionButton) findViewById(R.id.powerImageButton);
        mUpArrowImageButton     = (ImageButton) findViewById(R.id.upArrowImageButton);
        mDownArrowImageButton   = (ImageButton) findViewById(R.id.downArrowImageButton);
        mLeftArrowImageButton   = (ImageButton) findViewById(R.id.leftArrowImageButton);
        mRightArrowImageButton  = (ImageButton) findViewById(R.id.rightArrowImageButton);
        mRotateLeftImageButton  = (ImageButton) findViewById(R.id.rotateLeft);
        mRotateRightImageButton = (ImageButton) findViewById(R.id.rotateRight);

        SeekBar wristSeekbar = (SeekBar) findViewById(R.id.wrist);

        mUpArrowImageButton.setOnTouchListener(this);
        mRightArrowImageButton.setOnTouchListener(this);
        mLeftArrowImageButton.setOnTouchListener(this);
        mDownArrowImageButton.setOnTouchListener(this);
        mRotateLeftImageButton.setOnTouchListener(this);
        mRotateRightImageButton.setOnTouchListener(this);

        wristSeekbar.setMax(100);
        wristSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                Log.d(TAG, "Wrist: " + (byte) progress);

                _sendCommand(WRIST, (byte) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mPowerImageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!CONNECTED)
                {
                    new BluetoothConnectionTask().execute();
                }
            }
        });

        mPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(mPowerImageButton
                                                        , PropertyValuesHolder.ofFloat("scaleX", 1.2f)
                                                        , PropertyValuesHolder.ofFloat("scaleY", 1.2f));
        mPulseAnimator.setDuration(310);
        mPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mPulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        // Turn on Bluetooth.
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

        if (os != null)
        {
            try
            {
                os.close();

            } catch (IOException e) {
                Log.d(TAG, "Error closing outputStream: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        switch (v.getId())
        {
            case (R.id.upArrowImageButton):
                _move(mUpArrowImageButton, event, MOVE_FORWARD);
                break;

            case (R.id.downArrowImageButton):
                _move(mDownArrowImageButton, event, MOVE_BACKWARDS);
                break;

            case (R.id.leftArrowImageButton):
                _move(mLeftArrowImageButton, event, MOVE_LEFT);
                break;

            case (R.id.rightArrowImageButton):
                _move(mRightArrowImageButton, event, MOVE_RIGHT);
                break;

            case (R.id.rotateLeft):
                _rotate(mRotateLeftImageButton, event, ROTATE_LEFT);
                break;

            case (R.id.rotateRight):
                _rotate(mRotateRightImageButton, event, ROTATE_RIGHT);
                break;
        }

        return true;
    }

    /**
     * Method to move the robot.
     * @param arrow: button actioned.
     * @param event: event triggered.
     * @param command: command to be sent to Arduino.
     */
    private void _move(ImageButton arrow, MotionEvent event, byte command)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (!MOVING)
            {
                MOVING = true;

                arrow.animate().scaleX(1.3f)
                               .scaleY(1.3f)
                               .setDuration(200)
                               .start();
                arrow.setBackgroundTintList(
                        ColorStateList.valueOf(
                                this.getColor(android.R.color.holo_blue_bright)));

                _sendCommand(command, UNUSED);
            }

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            MOVING = false;

            arrow.animate().scaleX(1.0f)
                           .scaleY(1.0f)
                           .setDuration(200)
                           .start();
            arrow.setBackgroundTintList(
                    ColorStateList.valueOf(
                            this.getColor(R.color.colorAccent)));

            // When released, stop the robot.
            _sendCommand(HALT, UNUSED);
        }
    }

    /**
     * Method to rotate the robotic arm.
     * @param arrow: arrow pressed.
     * @param event: event triggered.
     * @param command: command to be sent to Arduino.
     */
    private void _rotate(ImageButton arrow, MotionEvent event, byte command)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (!ROTATING)
            {
                ROTATING = true;

                arrow.animate().scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(200)
                        .start();
                arrow.setBackgroundTintList(
                        ColorStateList.valueOf(
                                this.getColor(android.R.color.holo_blue_bright)));

                _sendCommand(command, UNUSED);
            }

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            ROTATING = false;

            arrow.animate().scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start();
            arrow.setBackgroundTintList(
                    ColorStateList.valueOf(
                            this.getColor(R.color.colorAccent)));

            // When released, stop the rotation.
            _sendCommand(ROTATE_STOP, UNUSED);
        }
    }

    /**
     * Method to send a command to Arduino.
     * @param command: command to be processed by Arduino.
     * @param arg: parameter if needed.
     */
    private void _sendCommand(byte command, byte arg)
    {
        if (CONNECTED)
        {
            try
            {
                os.write(command);
                os.write(arg);
                os.flush();

            } catch (IOException e) {
                Log.e(TAG, "Error sending command: " + e.getMessage());
            }
        }
    }

    /**
     * AsyncTask which connects to the Arduino.
     */
    private class BluetoothConnectionTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected void onPreExecute()
        {
            mPulseAnimator.start();

            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS);

            try
            {
                // MY_UUID is the app's UUID string
                mSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);

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
                // until it succeeds or throws an exception.
                Log.d(TAG, "Connecting...");

                mSocket.connect();

                os = mSocket.getOutputStream();

                CONNECTED = true;

                Log.d(TAG, "Connected!");

            } catch (IOException connectException) {
                Log.e(TAG, "Error connecting to Arduino");

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
                mPowerImageButton.setBackgroundTintList(
                        ColorStateList.valueOf(
                                getColor(R.color.colorLightGreen)));
            } else {
                Snackbar.make(mCoordinatorLayout, "Error connecting", Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}
