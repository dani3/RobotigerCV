package com.robotiger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "CLIENT BLUETOOTH";
    private static final boolean D = true;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream os = null;
    private InputStream is = null;

    private static final UUID MY_UUID =
            UUID.fromString( "00001101-0000-1000-8000-00805F9B34FB" );

    private static final String macAddress = "00:13:EF:00:0E:5F";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if ( D )
            Log.e(TAG, "-- Creating Activty --");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter( );
        if ( mBluetoothAdapter == null )
        {
            Toast.makeText(this
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();

            finish( );
            return;
        }

        if ( !mBluetoothAdapter.isEnabled( ) )
            mBluetoothAdapter.enable( );

        if ( D )
            Log.e( TAG , "-- Done (Bluetooth activated) --");
    }

    @Override
    public void onStart( )
    {
        super.onStart( );
        if ( D )
            Log.e( TAG, "-- Starting --");

    } // onStart

    @Override
    public void onResume( )
    {
        super.onResume( );
        if ( D )
        {
            Log.e( TAG, "-- Resuming --");
            Log.e( TAG, "-- Attempting to connect --" );
        }

        BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice( macAddress );

        try
        {
            btSocket = mDevice.createRfcommSocketToServiceRecord( MY_UUID );

        } catch ( IOException e ) {
            Log.e ( TAG, "-- Unable to create socket --" );
        }

        mBluetoothAdapter.cancelDiscovery( );

        try {
            btSocket.connect( );
            os = btSocket.getOutputStream( );

            Log.e( TAG , "-- Bluetooth connection established --");

            Toast.makeText( this
                    , "Connection established"
                    , Toast.LENGTH_SHORT ).show();

        } catch ( IOException e ) {
            try {
                btSocket.close( );

            } catch ( IOException e1 ) {
                Log.e( TAG, "-- Unable to close socket --" );
            }
        } // catch

        if ( D )
            Log.e( TAG, "-- About to send something to Arduino --");

    } // onResume

    @Override
    public void onPause( )
    {
        super.onPause( );

        if ( D )
            Log.e( TAG, "-- Pausing activity --" );

        if ( os != null )
        {
            try {
                btSocket.close( );
                os.flush( );

            } catch ( IOException e ) {
                Log.e( TAG, "-- Unable to close socket --" );
            }
        } // if

        if ( this.isFinishing( ) )
            mBluetoothAdapter.disable( );

    } // onPause

    @Override
    public void onDestroy( )
    {
        super.onDestroy( );
        if ( D )
            Log.e( TAG, " -- Destroying activity --" );

    } // onDestroy
}
