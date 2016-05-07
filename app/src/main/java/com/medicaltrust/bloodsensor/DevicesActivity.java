package com.medicaltrust.bloodsensor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.drawable.Drawable;

import android.os.Bundle;
import android.os.Handler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DevicesActivity extends Activity
{
    // Intent
    static final int DISCOVERABLE_TIMEOUT = 300;
    static final int DEFAULT_DATA_FORMAT = 8;
  
    static final int REQUEST_ENABLE_BT = 0;
    static final int REQUEST_DISCOVER = 1;
    static final int REQUEST_RELOAD_MEASUREMENTS = 2;


    PairedListAdapter mPairedAdapter;

    List<PairedDevice> mPairedItems;

    BluetoothAdapter mBtAdapter;
    Handler mHandler;
    Context mContext;

    BroadcastReceiver mRegistered;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice dev =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mPairedAdapter.add(new PairedDevice(dev.getName(),
                                                        dev.getAddress()));
                }
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.devices);

        mHandler = new Handler();
        mContext = this;

        mRegistered = null;
    
        mPairedItems = new ArrayList<PairedDevice>();
    
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            alert("Error", "Failed to adapt to bluetooth.", "Exit");
            return;
        }

        // touch and start thread
        prepareBluetoothScreen(mBtAdapter);

    } // OnCreate
  
    @Override
    public void onDestroy ()
    {
        if (mRegistered != null) unregisterReceiver(mRegistered);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode,
                                     Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQUEST_ENABLE_BT: break;
        case REQUEST_DISCOVER: 
            if (resultCode != RESULT_CANCELED)
                // Discovery is heavyweight procedure,
                // so use cancelDiscovery() to cancel ongoing discovery.
                mBtAdapter.startDiscovery();
            break;
        }
    } // onActivityResult
  
    private void prepareBluetoothScreen (BluetoothAdapter adapter)
    {
        /* ask if enable bluetooth
         * accept : RESULT_OK
         * fail   : RESULT_CANCELED
         */
        if (!adapter.isEnabled()) { // skip if bluetooth has already enabled
            final Intent intent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }

        // set discovering switch
        Button bt = (Button)findViewById(R.id.discover);
        bt.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) { discoverDevices(); }
        });

        // set start button
        bt = (Button)findViewById(R.id.start);
        bt.setOnClickListener(new View.OnClickListener() {
           public void onClick (View v) { startBluetooth(); }
        });

        class DefaultColors {
            public int text = 0; public Drawable background = null;
            public boolean isNew () { return text == 0 && background == null; }
            public void set (int t, Drawable b) { text = t; background = b; }
        }
        final DefaultColors defaultColors = new DefaultColors();

        ListView lv = (ListView)findViewById(R.id.devices);

        lv.setOnItemClickListener (new AdapterView.OnItemClickListener() {
            public void onItemClick (AdapterView<?> a, View v,
                                     int position, long id)
            {
                final PairedDevice i =
                    (PairedDevice)a.getItemAtPosition(position);
                final TextView tv1 =
                    (TextView)v.findViewById(R.id.paired_device);
                final TextView tv2 =
                    (TextView)v.findViewById(R.id.paired_addr);

                if (defaultColors.isNew ())
                    defaultColors.set(tv1.getCurrentTextColor(),
                                      tv1.getBackground());

                if (mPairedItems.contains(i)) {
                    mPairedItems.remove(i);
                    v.setBackgroundDrawable(defaultColors.background);
                    tv1.setTextColor(defaultColors.text);
                    tv2.setTextColor(defaultColors.text);
                } else {
                    mPairedItems.add(i);
                    v.setBackgroundResource(R.color.mcby1);
                    tv1.setTextColor(R.color.mcby1_string);
                    tv2.setTextColor(R.color.mcby1_string);
                }
            }
        });

        // get already paired devices
        List<PairedDevice> items = getPairedDevices(adapter);
        mPairedAdapter =
            new PairedListAdapter(mContext, R.layout.paired_list, items);
    
        lv.setAdapter(mPairedAdapter);
    } // prepareBluetoothScreen

    // get already paired devices
    private List<PairedDevice> getPairedDevices (BluetoothAdapter adapter)
    {
        List<PairedDevice> items = new ArrayList<PairedDevice>();
    
        Set<BluetoothDevice> paired = adapter.getBondedDevices();
        for (BluetoothDevice dev : paired)
            items.add(new PairedDevice(dev.getName(), dev.getAddress()));

        return items;
    }

    private void discoverDevices () {
        /* Register the BroadcastReceiver
         * Unregister will be occured in onDestroy
         */
        if (mRegistered != null) return;
    
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        mRegistered = mReceiver;

        // Make the local device discoverable to other divices. 
        Intent discoverableIntent =
            new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent
        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                  DISCOVERABLE_TIMEOUT);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVER);
    }

    private void startBluetooth () {
        if (mPairedItems.isEmpty()) {
            alert("Info", "Select at least one devices.", "OK");
            return;
        }
        if (!testToConnect(mPairedItems)) return;
    
        Intent intent = new Intent(mContext, MeasurementActivity.class);

        intent.putParcelableArrayListExtra(Config.DevicesNameLabel,
                                           (ArrayList)mPairedItems);

        startActivityForResult(intent, REQUEST_RELOAD_MEASUREMENTS);
    }

    /* Try to connect with bluetooth devices and disconnect immediately.
     * This cause PIN code dialogs to appear for connection. */
    private boolean testToConnect (List<PairedDevice> devices) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            alert("Info", "bluetooth adapter not available.", "OK");
            return false;
        }

        device: for (PairedDevice pd: devices) {
            BluetoothDevice device = adapter.getRemoteDevice(pd.getAddr());
            BluetoothSocket socket = null;
            try {
                socket = device.createRfcommSocketToServiceRecord(Config.MY_UUID);
            } catch (IOException e) {
                alert("Error", "failed to prepare client socket.", "OK");
                return false;
            }
            for (int i = 0; i < Config.TryLimit; i++) {
                if (i != 0)
                    try {
                        Thread.sleep(Config.TryInterval);
                    } catch (InterruptedException e) { }
                try {
                    socket.connect();
                } catch (IOException e) {
                    continue;
                }
                try {
                    socket.close();
                } catch (IOException e) { }
                continue device;
            }
            return false;
        }
        return true;
    }

    // user interface
    private void alert (String title, String mes, String button)
    {
        android.util.Log.d("DevicesActivity", title+" "+mes);
        final AlertDialog a = new AlertDialog.Builder(this).create();
        a.setTitle(title);
        a.setMessage(mes);
        a.setButton(button, new DialogInterface.OnClickListener() {
            public void onClick (DialogInterface dialog, int which) {}
        });
        mHandler.post(new Runnable() {
            public void run () { a.show(); }
        });
    }

    class PairedListAdapter extends ArrayAdapter<PairedDevice>
    {
        class ViewHolder
        {
            TextView mName;
            TextView mAddr;
      
            public ViewHolder (View v) {
                mName = (TextView)v.findViewById(R.id.paired_device);
                mAddr = (TextView)v.findViewById(R.id.paired_addr);
            }
            public void setView (PairedDevice i) {
                mName.setText(i.getName());
                mAddr.setText(i.getAddr());
            }
        }

        int mLayout;
    
        public PairedListAdapter (Context context, int layout,
                                  List<PairedDevice> items)
        {
            super(context, layout, items);
            mLayout = layout;
        }

        @Override
        public View getView (int position, View contentView,
                             ViewGroup viewGroup)
        {
            View view;
            ViewHolder holder; // reference of view's member

            if (contentView == null) {
                // create view and define holder as tag
                LayoutInflater inflater =
                    (LayoutInflater)getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
                view = inflater.inflate(mLayout, null);
                holder = new ViewHolder(view);
                view.setTag(holder);
            } else {
                // view has been already constructed
                view = contentView;
                holder = (ViewHolder)view.getTag();
            }

            holder.setView(getItem(position));

            return view;
        }
    } // PairedListAdapter

}
