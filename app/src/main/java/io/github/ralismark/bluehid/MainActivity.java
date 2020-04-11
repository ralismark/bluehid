package io.github.ralismark.bluehid;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothHidDeviceCallback;
import android.bluetooth.BluetoothInputHost;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Range;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {


    /* see https://www.usb.org/sites/default/files/documents/hid1_11.pdf */

    // 4 buttons, 1 X/Y stick
    //
    //   7 6 5 4 3 2 1 0
    // [ - - - - 4 3 2 1 ] - buttons
    // [  X axis         ]
    // [  Y axis         ]

    private static final byte[] descriptor = new byte[]{

            // HID descriptor
            0x09, // bLength
            0x21, // bDescriptorType - HID
            0x11, 0x01, // bcdHID (little endian - 1.11)
            0x00, // bCountryCode
            0x01, // bNumDescriptors (min 1)
            0x22, // bDescriptorType - Report
            0x30, 0x00, // wDescriptorLength (48)

            // Report descriptor
            0x05, 0x01,        // USAGE_PAGE (Generic Desktop)
            0x09, 0x05,        // USAGE (Game Pad)
            (byte) 0xa1, 0x01, // COLLECTION (Application)
            (byte) 0xa1, 0x00, //   COLLECTION (Physical)
            0x05, 0x09,        //     USAGE_PAGE (Button)
            0x19, 0x01,        //     USAGE_MINIMUM (Button 1)
            0x29, 0x04,        //     USAGE_MAXIMUM (Button 4)
            0x15, 0x00,        //     LOGICAL_MINIMUM (0)
            0x25, 0x01,        //     LOGICAL_MAXIMUM (1)
            0x75, 0x01,        //     REPORT_SIZE (1)
            (byte) 0x95, 0x04, //     REPORT_COUNT (4)
            (byte) 0x81, 0x02, //     INPUT (Data,Var,Abs)
            0x75, 0x04,        //     REPORT_SIZE (4)
            (byte) 0x95, 0x01, //     REPORT_COUNT (1)
            (byte) 0x81, 0x03, //     INPUT (Cnst,Var,Abs)
            0x05, 0x01,        //     USAGE_PAGE (Generic Desktop)
            0x09, 0x30,        //     USAGE (X)
            0x09, 0x31,        //     USAGE (Y)
            0x15, (byte) 0x81, //     LOGICAL_MINIMUM (-127)
            0x25, 0x7f,        //     LOGICAL_MAXIMUM (127)
            0x75, 0x08,        //     REPORT_SIZE (8)
            (byte) 0x95, 0x02, //     REPORT_COUNT (2)
            (byte) 0x81, 0x02, //     INPUT (Data,Var,Abs)
            (byte) 0xc0,       //   END_COLLECTION
            (byte) 0xc0        // END_COLLECTION

    };

    private static final int REQUEST_ENABLE_BT = 99;

    private static final String TAG = "MainActivity";

    private BluetoothInputHost mBtHidDevice;
    private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mBtDevice;

    private Vibrator vibrator;

    private void info(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
        Log.d(TAG, msg);
    }

    private void getProxy() {
        mBtAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            @SuppressLint("NewApi")
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.INPUT_HOST) {
                    info("Got HID device");
                    mBtHidDevice = (BluetoothInputHost) proxy;


                    BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                            "BlueHID",
                            "Android HID hackery",
                            "Android",
                            (byte) 0x00,
                            descriptor
                    );

                    mBtHidDevice.registerApp(sdp, null, null, new BluetoothHidDeviceCallback() {
                        @Override
                        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
                            Log.v(TAG, "onGetReport: device=" + device + " type=" + type
                                    + " id=" + id + " bufferSize=" + bufferSize);
                        }

                        @Override
                        public void onConnectionStateChanged(BluetoothDevice device, final int state) {
                            Log.v(TAG, "onConnectionStateChanged: device=" + device + " state=" + state);
                            if (device.equals(mBtDevice)) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TextView status = findViewById(R.id.status);
                                        if (state == BluetoothProfile.STATE_DISCONNECTED) {
                                            status.setText(R.string.status_disconnected);
                                            mBtDevice = null;
                                        } else if (state == BluetoothProfile.STATE_CONNECTING) {
                                            status.setText(R.string.status_connecting);
                                        } else if (state == BluetoothProfile.STATE_CONNECTED) {
                                            status.setText(R.string.status_connected);
                                        } else if (state == BluetoothProfile.STATE_DISCONNECTING) {
                                            status.setText(R.string.status_disconnecting);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.INPUT_HOST) {
                    info("Lost HID device");
                }
            }
        }, BluetoothProfile.INPUT_HOST);
    }

    private ImageButton[] buttons;
    private JoystickView joystick;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = getSystemService(Vibrator.class);

        // initialise the joystick
        joystick = findViewById(R.id.joystick);
        joystick.setTag(R.id.tag_x_pos, 0.0);
        joystick.setTag(R.id.tag_y_pos, 0.0);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                double rad = Math.toRadians(angle);
                double dist = strength / 100.0;

                // vibrate if crossing an axis
//                double prev_x = (double) joystick.getTag(R.id.tag_x_pos);
//                double prev_y = (double) joystick.getTag(R.id.tag_y_pos);
//                double x = dist * Math.cos(rad);
//                double y = dist * Math.sin(rad);
//
//                if((prev_x < -0.15 && x >= -0.15) || (prev_x > 0.15 && x <= 0.15) ||
//                        (prev_y < -0.15 && y >= -0.15) || (prev_y > 0.15 && y <= 0.15)) {
//                    vibrator.vibrate(VibrationEffect.createOneShot(40, 50));
//                }

                joystick.setTag(R.id.tag_x_pos, dist * Math.cos(rad));
                joystick.setTag(R.id.tag_y_pos, dist * Math.sin(rad));
                sendReport();
            }
        });

        // get buttons ready
        buttons = new ImageButton[]{
                findViewById(R.id.button_1),
                findViewById(R.id.button_2),
                findViewById(R.id.button_3),
                findViewById(R.id.button_4),
        };
        for (ImageButton button : buttons) {
            button.setTag(R.id.tag_pressed, false);
            button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Do something
                            if(!(boolean) v.getTag(R.id.tag_pressed)) {
                                vibrator.vibrate(VibrationEffect.createOneShot(40, 50));
                            }
                            v.setTag(R.id.tag_pressed, true);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // No longer down
                            v.setTag(R.id.tag_pressed, false);
                            break;
                        default:
                            return false;
                    }
                    sendReport();
                    return false;
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get bluetooth enabled before continuing
        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            btListDevices();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mBtHidDevice != null) {
            btConnect(null); // disconnect
            Spinner btList = findViewById(R.id.devices);
            btList.setSelection(0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                btListDevices();
            } else {
                final MainActivity activity = this;

                // TODO handle if the user doesn't like bluetooth
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth Required")
                        .setMessage("Bluetooth is required to run this app. Try again?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.finishAndRemoveTask(); // exit
                            }
                        })
                        .show();
            }
        }
    }

    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();

    private void btListDevices() {
        getProxy(); // need bluetooth to have been enabled first

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Add devices to adapter
        List<String> names = new ArrayList<>();

        // add empty
        names.add("(disconnected)");
        mDevices.add(null);

        for (BluetoothDevice btDev : pairedDevices) {
            names.add(btDev.getName());
            mDevices.add(btDev);
        }

        Spinner btList = findViewById(R.id.devices);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        btList.setAdapter(adapter);

        btList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice dev = mDevices.get(position);
                btConnect(dev);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO handle this
            }
        });
    }

    private void btConnect(BluetoothDevice device) {
        Log.i(TAG, "btConnect: device=" + device);

        // disconnect from everything else
        for (BluetoothDevice btDev : mBtHidDevice.getDevicesMatchingConnectionStates(new int[]{
                BluetoothProfile.STATE_CONNECTING,
                BluetoothProfile.STATE_CONNECTED
        })) {
            mBtHidDevice.disconnect(btDev);
        }

        if (device != null) {
            mBtDevice = device;
            mBtHidDevice.connect(device);
        }
    }

    static int reportIndex = 0;

    private void sendReport() {
        // get button state
        byte state = 0;
        for (int i = 0; i < 4; ++i) {
            if ((boolean) buttons[i].getTag(R.id.tag_pressed)) {
                state |= (1 << i);
            }
        }

        // get joystick state
        Range<Integer> bounds = new Range<>(-127, 127);
        int adjX = bounds.clamp((int) ((double) joystick.getTag(R.id.tag_x_pos) * 127));
        int adjY = bounds.clamp((int) ((double) joystick.getTag(R.id.tag_y_pos) * -127));

        Log.d(TAG, "sendReport(): " + state + " " + adjX + " " + adjY);
        TextView reportIndicator = (TextView)findViewById(R.id.reportCount);
        reportIndicator.setText("#" + reportIndex++);

        for (BluetoothDevice btDev : mBtHidDevice.getConnectedDevices()) {
            mBtHidDevice.sendReport(btDev, 0, new byte[]{
                    state,
                    (byte) adjX,
                    (byte) adjY,
            });
        }
    }
}
