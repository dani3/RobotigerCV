package com.robotiger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.triggertrap.seekarc.SeekArc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{
    /* Constants */
    private static final String TAG = "CLIENT";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String MAC_ADDRESS = "00:13:EF:00:0E:5F";

    /* Flags */
    private static boolean CONNECTED;
    private static boolean CONNECTING;
    private static boolean SENDING;
    private static boolean MOVING;

    /* Commands */
    private static final short UNUSED = 0x00;

    private static final short RESTART = 0xFF;

    private static final short MOVE_BACKWARDS = 0x80;
    private static final short MOVE_RIGHT     = 0x81;
    private static final short MOVE_LEFT      = 0x82;
    private static final short MOVE_FORWARD   = 0x83;
    private static final short HALT           = 0x84;

    private static final short ROTATE_WRIST = 0xA0;
    private static final short ELBOW        = 0xB0;
    private static final short SHOULDER     = 0xC0;
    private static final short WRIST        = 0xD0;
    private static final short HAND         = 0xE0;

    /* Bluetooth Adapter */
    private static BluetoothAdapter mBluetoothAdapter;

    /* Bluetooth device / socket */
    private static BluetoothSocket mSocket;
    private static OutputStream os;

    /* Amimator */
    YoYo.YoYoString mPulse;

    /* UI elements */
    private TextView mStatusTextView;
    private CircleImageView mStatusIndicator;
    private ImageView mForwardImageButton;
    private ImageView mLeftImageButton;
    private ImageView mRightImageButton;
    private ImageView mBackwardsImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        CONNECTED  = false;
        CONNECTING = false;
        SENDING    = false;
        MOVING     = false;

        //_initMovementViews();
        _initViews();
        _initMovementViews();
        _startAnimation();
        _initSeekbars();
        _initBluetooth();
    }

    /**
     * Metodo que realiza la animacion de entrada.
     */
    private void _startAnimation()
    {
        View circle_header         = findViewById(R.id.header_circles);
        View header                = findViewById(R.id.arm_header);
        View shoulder              = findViewById(R.id.shoulder_layout);
        View shoulder_bottom       = findViewById(R.id.shoulder_bottom);
        View elbow                 = findViewById(R.id.elbow_layout);
        View elbow_bottom          = findViewById(R.id.elbow_bottom);
        View wrist                 = findViewById(R.id.wrist_layout);
        View wrist_bottom          = findViewById(R.id.wrist_bottom);
        View wrist_rotation        = findViewById(R.id.wrist_rotation_layout);
        View wrist_rotation_bottom = findViewById(R.id.wrist_rotation_bottom);
        View hand                  = findViewById(R.id.hand_layout);
        View hand_bottom           = findViewById(R.id.hand_bottom);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_top_rotate);
        animation.setFillAfter(true);

        circle_header.startAnimation(animation);
        header.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_top));

        Animation animation1 = AnimationUtils.loadAnimation(this, R.anim.fade_in_top);
        animation1.setStartOffset(75);
        shoulder.startAnimation(animation1);
        shoulder_bottom.startAnimation(animation1);

        Animation animation2 = AnimationUtils.loadAnimation(this, R.anim.fade_in_top);
        animation2.setStartOffset(150);
        elbow.startAnimation(animation2);
        elbow_bottom.startAnimation(animation2);

        Animation animation3 = AnimationUtils.loadAnimation(this, R.anim.fade_in_top);
        animation3.setStartOffset(225);
        wrist.startAnimation(animation3);
        wrist_bottom.startAnimation(animation3);

        Animation animation4 = AnimationUtils.loadAnimation(this, R.anim.fade_in_top);
        animation4.setStartOffset(300);
        wrist_rotation.startAnimation(animation4);
        wrist_rotation_bottom.startAnimation(animation4);

        Animation animation5 = AnimationUtils.loadAnimation(this, R.anim.fade_in_top);
        animation5.setStartOffset(375);
        hand.startAnimation(animation5);
        hand_bottom.startAnimation(animation5);

        View rotation           = findViewById(R.id.arm_rotation);
        View inner_circle       = findViewById(R.id.inner_circle);
        View degrees            = findViewById(R.id.degrees_layout);
        View movement_container = findViewById(R.id.movement_container);

        rotation.setAlpha(0.0f);
        rotation.animate().alpha(1.0f).setStartDelay(75).start();

        inner_circle.setAlpha(0.0f);
        inner_circle.animate().alpha(0.5f).setStartDelay(150).start();

        degrees.setAlpha(0.0f);
        degrees.animate().alpha(1.0f).setStartDelay(225);

        movement_container.setAlpha(0.0f);
        movement_container.animate().alpha(1.0f).setStartDelay(290);
    }

    /**
     * Metodo para inicializar otras vistas
     */
    private void _initViews()
    {
        findViewById(R.id.header_semicircle).getBackground().setLevel(5000);
        ((TextView)findViewById(R.id.arm_rotation_degrees)).setText("0");

        mStatusIndicator = (CircleImageView) findViewById(R.id.status_indicator);
        mStatusTextView  = (TextView) findViewById(R.id.status_text);

        if (!CONNECTING && !CONNECTED)
        {
            mStatusTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    new BluetoothConnectionTask().execute();
                }
            });
        }
    }

    /**
     * Metodo para inicializar las flechas.
     */
    private void _initMovementViews()
    {
        mForwardImageButton   = (ImageView) findViewById(R.id.forward);
        mBackwardsImageButton = (ImageView) findViewById(R.id.backward);
        mLeftImageButton      = (ImageView) findViewById(R.id.left);
        mRightImageButton     = (ImageView) findViewById(R.id.right);

        mForwardImageButton.setOnTouchListener(this);
        mBackwardsImageButton.setOnTouchListener(this);
        mLeftImageButton.setOnTouchListener(this);
        mRightImageButton.setOnTouchListener(this);
    }

    /**
     * Metodo para inicializar las Seekbars.
     */
    private void _initSeekbars()
    {
        SeekArc armRotationSeekbar = (SeekArc) findViewById(R.id.arm_rotation);
        armRotationSeekbar.setProgress(180);
        armRotationSeekbar.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener()
        {
            @Override
            public void onProgressChanged(SeekArc seekArc, int i, boolean b)
            {
                int degrees =
                    (int)((float) 135 / (float) 180 * (float) (i - 180) + (float) 135);

                if (i >= 180) {
                    degrees -= 135;
                } else {
                    degrees = 135 - degrees;
                }

                ((TextView)findViewById(R.id.arm_rotation_degrees)).setText(Integer.toString(degrees));
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {}

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {}
        });

        SeekBar wristSeekbar         = (SeekBar) findViewById(R.id.wrist);
        SeekBar elbowSeekbar         = (SeekBar) findViewById(R.id.elbow);
        SeekBar shoulderSeekbar      = (SeekBar) findViewById(R.id.shoulder);
        SeekBar wristRotationSeekbar = (SeekBar) findViewById(R.id.wrist_rotation);
        SeekBar handSeekbar          = (SeekBar) findViewById(R.id.hand);

        wristRotationSeekbar.setMax(100);
        wristRotationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                _sendCommand(ROTATE_WRIST, (short) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        wristSeekbar.setMax(100);
        wristSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                _sendCommand(WRIST, (short) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        handSeekbar.setMax(100);
        handSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                _sendCommand(HAND, (short) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        elbowSeekbar.setMax(100);
        elbowSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                _sendCommand(ELBOW, (short) (100 - progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        shoulderSeekbar.setMax(100);
        shoulderSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                _sendCommand(SHOULDER, (short) (100 - progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * Metodo que realiza el setup del Bluetooth.
     */
    private void _initBluetooth()
    {
        if (!CONNECTED)
        {
            // Turn on Bluetooth.
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            mBluetoothAdapter.enable();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if ((mBluetoothAdapter != null) && (mBluetoothAdapter.isEnabled()))
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
    public void onBackPressed()
    {
        super.onBackPressed();

        _sendCommand(RESTART, UNUSED);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        switch (v.getId())
        {
            case (R.id.forward):
                _move(mForwardImageButton, event, MOVE_FORWARD);
                break;

            case (R.id.backward):
                _move(mBackwardsImageButton, event, MOVE_BACKWARDS);
                break;

            case (R.id.left):
                _move(mLeftImageButton, event, MOVE_LEFT);
                break;

            case (R.id.right):
                _move(mRightImageButton, event, MOVE_RIGHT);
                break;
        }

        return true;
    }

    /**
     * Method to move the robot.
     * @param view: button actioned.
     * @param event: event triggered.
     * @param command: command to be sent to Arduino.
     */
    @SuppressWarnings("deprecation")
    private void _move(ImageView view, MotionEvent event, short command)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (!MOVING)
            {
                MOVING = true;

                view.animate().scaleX(1.2f)
                              .scaleY(1.2f)
                              .setDuration(200)
                              .start();

                view.setBackgroundTintList(getResources().getColorStateList(R.color.white_selector));

                _sendCommand(command, UNUSED);
            }

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            MOVING = false;

            view.animate().scaleX(1.0f)
                          .scaleY(1.0f)
                          .setDuration(200)
                          .start();

            view.setBackgroundTintList(getResources().getColorStateList(R.color.white_dark));

            // When released, stop the robot.
            _sendCommand(HALT, UNUSED);
        }
    }

    /**
     * Method to send a command to Arduino.
     * @param command: command to be processed by Arduino.
     * @param arg: parameter if needed.
     */
    private void _sendCommand(short command, short arg)
    {
        if ((CONNECTED) && (!SENDING))
        {
            SENDING = true;

            try
            {
                Log.d(TAG, "Sending command: " + Integer.toString(command, 16) + " " + arg);

                os.write(command);
                os.write(arg);
                os.flush();

            } catch (IOException e) {
                Log.e(TAG, "Error sending command: " + e.getMessage());

            } finally {
                SENDING = false;
            }
        }
    }

    /**
     * AsyncTask which connects to the Arduino.
     */
    private class BluetoothConnectionTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        @SuppressWarnings("deprecation")
        protected void onPreExecute()
        {
            CONNECTING = true;

            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS);

            mStatusTextView.setText(getResources().getString(R.string.status_connecting));

            mPulse = YoYo.with(Techniques.Pulse).repeat(2000).playOn(mStatusIndicator);
            mStatusIndicator.setImageResource(android.R.color.holo_blue_bright);

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
        @SuppressWarnings("deprecation")
        protected void onPostExecute(Void v)
        {
            if (CONNECTED)
            {
                mStatusIndicator.setImageResource(android.R.color.holo_green_light);
                mStatusTextView.setText(getResources().getString(R.string.status_connected));

            } else {
                mStatusIndicator.setImageResource(android.R.color.holo_red_light);
                mStatusTextView.setText(getResources().getString(R.string.status_disconnected));
            }

            CONNECTING = false;
        }
    }
}
