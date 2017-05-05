package com.example.testble;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;




import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;

public class BluetoothControlActivity extends Activity {
    private final static String TAG = BluetoothControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private HolloBluetooth mble;
    private Context context;

    private SeekBar volume;
    private SeekBar octave;
    private ScrollView scrollView;

    private Handler mHandler;
    public String received;

    public String A0Value;
    public String A1Value;
    public String A2Value;
    public String A3Value;


    private static final int MSG_DATA_CHANGE = 0x11;

    TextView A0Input;
    TextView A1Input;
    TextView A2Input;
    TextView A3Input;
    TextView voltext;
    TextView oct;


    public String toSend;

    StringBuilder output = new StringBuilder();

    private PdUiDispatcher dispatcher;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(sampleRate, 1, 2, 8, true);

        dispatcher = new PdUiDispatcher();

        PdBase.setReceiver(dispatcher);

        dispatcher.addListener("ble", receiver);
        PdBase.subscribe("ble");

    }


    public void sendPatchData(String receive, String value) {

        sendFloatPD(receive, Float.parseFloat(value));

        Log.e(receive, value);

    }

    public void sendFloatPD(String receiver, Float value) {
        PdBase.sendFloat(receiver, value);
    }

    public void sendBangPD(String receiver) {
        PdBase.sendBang(receiver);
    }


    private void loadPDPatch(String patchName) throws IOException {
        File dir = getFilesDir();
        try {
            IoUtils.extractZipResource(getResources().openRawResource(R.raw.synth), dir, true);
            File pdPatch = new File(dir, patchName);
            PdBase.openPatch(pdPatch.getAbsolutePath());
        } catch (IOException e) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        A0Input = (TextView) findViewById(R.id.A0Input);
        A1Input = (TextView) findViewById(R.id.A1Input);
        A2Input = (TextView) findViewById(R.id.A2Input);
        A3Input = (TextView) findViewById(R.id.A3Input);
        voltext = (TextView) findViewById(R.id.voltext);
        oct = (TextView) findViewById(R.id.oct);

        Switch onOffSwitch = (Switch) findViewById(R.id.onOffSwitch);

        Button button1 = (Button) findViewById(R.id.Note1);
        Button button2 = (Button) findViewById(R.id.Note2);
        Button button3 = (Button) findViewById(R.id.Note3);
        Button button4 = (Button) findViewById(R.id.Note4);
        Button button5 = (Button) findViewById(R.id.Note5);
        Button button6 = (Button) findViewById(R.id.Note6);
        Button button7 = (Button) findViewById(R.id.Note7);
        Button button8 = (Button) findViewById(R.id.Note8);


        ToggleButton toggle1 = (ToggleButton) findViewById(R.id.toggle1);
        ToggleButton toggle2 = (ToggleButton) findViewById(R.id.toggle2);
        ToggleButton toggle3 = (ToggleButton) findViewById(R.id.toggle3);


        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        context = this;

        mble = HolloBluetooth.getInstance(getApplicationContext());


        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        int color = msg.arg1;
                        String strData = (String) msg.obj;
                        SpannableStringBuilder builder = new SpannableStringBuilder(strData);

                        //ForegroundColorSpan ，BackgroundColorSpan
                        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
                        String string;
                        int num;
                        switch (color) {
                            case Color.BLUE: //send

                                builder.setSpan(colorSpan, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.RED:    //error
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.BLACK: //tips
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;

                            default: //receive
                                addLogText(strData, Color.BLACK, strData.length());


                                for (int i = 0; i < strData.length(); i++) {
                                    if (strData.charAt(i) == 'A' || strData.charAt(i) == 'B' || strData.charAt(i) == 'C' || strData.charAt(i) == 'D') {
                                        received = output.toString();
                                        sensorParse();
                                        output.delete(0, output.length());
                                        output.append(strData.charAt(i));

                                    } else {
                                        output.append(strData.charAt(i));
                                    }
                                }

                                break;
                        }

                        break;

                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };

        new Handler().post(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++) {
                    if (mble.connectDevice(mDeviceAddress, bleCallBack))
                        break;

                    try {
                        Thread.sleep(10, 0);
                    } catch (Exception e) {

                    }
                }
                if (i == 5) {

                    return;
                }

                try {
                    Thread.sleep(10, 0);
                } catch (Exception e) {

                }


                if (mble.wakeUpBle()) {

                } else {

                }

            }
        });

        try {
            initPD();
            loadPDPatch("synth.pd"); // This is the name of the patch in the zip

            new Handler().post(new Runnable() {
                @Override
                public void run() {

                    if (!mble.sendData("start")) {
                    }

                }
            });


        } catch (IOException e) {
            finish();
        }

        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                float val = (isChecked) ? 1.0f : 0.0f; // value = (get value of isChecked, if true val = 1.0f, if false val = 0.0f)
                sendFloatPD("onOff", val); //send value to patch, receiveEvent names onOff

            }
        });


/////////////////////Seek Bar/////////////////////////////////////////////////////////////////////////////////////

        volume = (SeekBar) findViewById(R.id.volume);
        octave = (SeekBar) findViewById(R.id.octave);

        volume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    float vol = 99;
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        vol = progress;

                        voltext.setText("Volume: " + Float.toString(vol) + "%");
                        sendFloatPD("volume", vol / 100f);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }

                });


        octave.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    float number = 0;
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        number = progress;

                        oct.setText("Octave: " + Float.toString(number+4));
                        sendFloatPD("octave", (number+1)*2);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }

                });


        //-----------------------------------------CLICK LISTENER FOR BUTTONS and TOGGLES--------------------------------------------------------------------------------------//
        //-----------------------------------------CLICK LISTENER FOR BUTTONS and TOGGLES--------------------------------------------------------------------------------------//
        //-----------------------------------------CLICK LISTENER FOR BUTTONS and TOGGLES--------------------------------------------------------------------------------------//
        //-----------------------------------------CLICK LISTENER FOR BUTTONS and TOGGLES--------------------------------------------------------------------------------------//
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
                    public void onClick(View view) {

                sendBangPD("button1");
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendBangPD("button2");
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendBangPD("button2");
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendBangPD("button3");
            }
        });
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendBangPD("button4");
            }
        });
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendBangPD("button5");
            }
        });
        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendBangPD("button6");
            }
        });
        button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendBangPD("button7");
            }
        });
        button8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendBangPD("button8");
            }
        });





                toggle1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                float val = (isChecked) ? 1.0f : 0.0f;
                if (isChecked) {
                    sendFloatPD("toggle1", val);
                } else {
                    sendFloatPD("toggle1", val);
                }
            }
        });

        toggle2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                float val = (isChecked) ? 1.0f : 0.0f;
                if (isChecked) {
                    sendFloatPD("toggle2", val);
                } else {
                    sendFloatPD("toggle2", val);
                }
            }
        });

        toggle3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                float val = (isChecked) ? 1.0f : 0.0f;
                if (isChecked) {
                    sendFloatPD("toggle3", val);
                } else {
                    sendFloatPD("toggle3", val);
                }
            }
        });


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        return super.onMenuItemSelected(featureId, item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.menu_refresh).setActionView(null);

        return super.onCreateOptionsMenu(menu);
    }

    void sensorParse() {

        if (received.length() > 0) {
            if (received.charAt(0) == 'A') {
                A0Value = received.substring(1);
                Log.i("A0", A0Value);
                A0Input.setText(A0Value);
                sendPatchData("a_input_0", A0Value);
            } else if (received.charAt(0) == 'B') {
                A1Value = received.substring(1);
                Log.i("A1", A1Value);
                A1Input.setText(A1Value);
                sendPatchData("a_input_1", A1Value);
            } else if (received.charAt(0) == 'C') {
                A2Value = received.substring(1);
                Log.i("A2", A2Value);
                A2Input.setText(A2Value);
                sendPatchData("a_input_2", A2Value);
            } else if (received.charAt(0) == 'D') {
                A3Value = received.substring(1);
                Log.i("A3", A3Value);
                A3Input.setText(A3Value);
                sendPatchData("a_input_3", A3Value);
            }
        }
    }

    void addLogText(final String log, final int color, int byteLen) {
        Message message = new Message();
        message.what = MSG_DATA_CHANGE;
        message.arg1 = color;
        message.arg2 = byteLen;
        message.obj = log;
        mHandler.sendMessage(message);
    }

    HolloBluetooth.OnHolloBluetoothCallBack bleCallBack = new HolloBluetooth.OnHolloBluetoothCallBack() {

        @Override
        public void OnHolloBluetoothState(int state) {
            if (state == HolloBluetooth.HOLLO_BLE_DISCONNECTED) {
                onBackPressed();
            }
        }

        @Override
        public void OnReceiveData(byte[] recvData) {
            addLogText(ConvertData.bytesToHexString(recvData, false), Color.rgb(139, 0, 255), recvData.length);


        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PdAudio.startAudio(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PdAudio.stopAudio();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mble.disconnectDevice();
        Log.d(TAG, "destroy");
        mble.disconnectLocalDevice();
        Log.d(TAG, "destroyed");
    }

    private PdReceiver receiver = new PdReceiver() {

        private void pdPost(final String msg) {
            Log.e("RECEIVED:", msg);


            while (!mble.sendData(msg)) {
                //  Log.e("BLEWRITE","ERROR");
            }

            sendFloatPD("stop", 1.0f);

        }


        @Override
        public void print(String s) {
            Log.i("PRINT", s);
        }

        @Override
        public void receiveBang(String source) {
            //pdPost("bang");
        }

        @Override
        public void receiveFloat(String source, float x) {

        }

        @Override
        public void receiveList(String source, Object... args) {

        }

        @Override
        public void receiveMessage(String source, String symbol, Object... args) {
            //  pdPost("list: " + Arrays.toString(args));
            toSend = symbol + ",";
            for (int i = 0; i < args.length; i++) {
                toSend += args[i].toString();
                if (i != args.length - 1) {
                    toSend += ",";
                } else {
                    toSend += ";";
                }
            }
            toSend = toSend.replace(".0", "");
            sendFloatPD("start", 1.0f);
            pdPost(toSend);

        }

        @Override
        public void receiveSymbol(String source, String symbol) {
            //pdPost("symbol: " + symbol);
        }
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("BluetoothControl Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
