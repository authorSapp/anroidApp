package com.simpleapp.dev.android_app;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import tw.com.prolific.driver.pl2303.PL2303Driver;


public class MainActivity extends AppCompatActivity {

    private final static boolean SHOW = false;
    final String TAG = "android_app";
    private String log = "";
    private final String initString = "ZZZ";
    int[][] parametersAndroidApp = {
            {24, 40, 16, 4, 0, 10, 1, 0}, //1 - [closed data]
            {20, 40, 24, 4, 0, 13, 1, 40}, //2 - [closed data]
            {24, 40, 22, 4, 0, 13, 2, 0}, //3 - [closed data]
            {24, 40, 16, 4, 0, 10, 3, 0}, //4 - [closed data]
            {24, 40, 16, 4, 0, 10, 4, 0}, //5 - [closed data]
    };

    private PL2303Driver mSerial;
    private Button mOpen, mInit, mWrite;
    private TextView myTextViewLog, myTextViewData;

//    private PL2303Driver.BaudRate mBaudrate = PL2303Driver.BaudRate.B9600;
//    private PL2303Driver.DataBits mDataBits = PL2303Driver.DataBits.D8;
//    private PL2303Driver.Parity mParity = PL2303Driver.Parity.NONE;
//    private PL2303Driver.StopBits mStopBits = PL2303Driver.StopBits.S1;
//    private PL2303Driver.FlowControl mFlowControl = PL2303Driver.FlowControl.OFF;

    ListView lvMain;
    String[] names;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOpen = (Button) findViewById(R.id.btnOpen);
        mOpen.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                openUsbSerial();
            }
        });

        mInit = (Button) findViewById(R.id.btnInit);
        mInit.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initAndroidApp();
            }
        });


        mWrite = (Button) findViewById(R.id.btnWrite);
        mWrite.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                writeDataToAndroidApp();
            }
        });


        myTextViewLog = (TextView) findViewById(R.id.myTextLog);
        myTextViewData = (TextView) findViewById(R.id.myTextData);
        log("In onCreate", SHOW);

        lvMain = (ListView) findViewById(R.id.lvMain);
        // устанавливаем режим выбора пунктов списка
        lvMain.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // Создаем адаптер, используя массив из файла ресурсов
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.names,
                android.R.layout.simple_list_item_single_choice);
        lvMain.setAdapter(adapter);
        // получаем массив из файла ресурсов
        names = getResources().getStringArray(R.array.names);
//        log(names[lvMain.getCheckedItemPosition()]);

        mSerial = new PL2303Driver((UsbManager) getSystemService(Context.USB_SERVICE),
                this, "com.example.lilit.AndroidApp.USB_PERMISSION");
//        log("mSerial - " + mSerial);

        if (!mSerial.PL2303USBFeatureSupported()) {
            Toast.makeText(this, "No Support USB host API", Toast.LENGTH_SHORT)
                    .show();
            log("No Support USB host API");
            mSerial = null;
        } else {
            log("Support USB host API");
        }

        log("Out onCreate", SHOW);
    }

    @Override
    protected void onDestroy() {
        log("In onDestroy", SHOW);
        if(mSerial != null) {
            mSerial.end();
            mSerial = null;
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        log("In onResume", SHOW);
        super.onResume();

        String action =  getIntent().getAction();
//        Log.d(TAG, "onResume: " + action);
//        log("onResume: " + action);

        if(!mSerial.isConnected()) {
            if( !mSerial.enumerate() ) {
                Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();
                return;
            } else {
//                Log.d(TAG, "onResume: enumerate succeeded!");
                log("onResume: enumerate succeeded!");
            }
        }
        Toast.makeText(this, "attached", Toast.LENGTH_SHORT).show();

        log("Out onResume", SHOW);
    }

    private void openUsbSerial() {
        if(mSerial == null)
            return;

        if (mSerial.isConnected()) {
            if (!mSerial.InitByDefualtValue()) {
                if(!mSerial.PL2303Device_IsHasPermission()) {
                    Toast.makeText(this, "cannot open, maybe no permission", Toast.LENGTH_SHORT).show();
                }
                if(mSerial.PL2303Device_IsHasPermission() && (!mSerial.PL2303Device_IsSupportChip())) {
                    Toast.makeText(this, "cannot open, maybe this chip has no support, please use PL2303HXD / RA / EA chip.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "connected " , Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initAndroidApp() {
        if(mSerial == null)
            return;

        if(!mSerial.isConnected())
            return;

        int res = mSerial.write(initString.getBytes());
        if (res < 0) {
            Toast.makeText(this, "Init data error: " + res, Toast.LENGTH_SHORT).show();
        }
    }

    private void writeDataToAndroidApp() {
        if(mSerial == null)
            return;

        if(!mSerial.isConnected())
            return;
        String rawData = getRawDataFromFile();
        rawData = rawData.substring(0, 240) + parametersToChar(lvMain.getCheckedItemPosition()) + rawData.substring(248);
        myTextViewData.setText(rawData + " | " + rawData.length());

        int res = 1;
        for (int i = 0; i < rawData.length()/16; i++) {
            res = mSerial.write(rawData.substring(0 + 16*i, 16 + 16*i).getBytes());
//            log(rawData.substring(0 + 16*i, 16 + 16*i) + " | " + res);

            if (res < 0) {
                Toast.makeText(this, "Transfer data error: " + res, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
//                e.printStackTrace();
                log("Thread.sleep - InterruptedException");
            }
            res = mSerial.write(rawData.substring((rawData.length() / 16) * 16, rawData.length()).getBytes());
            if (res < 0) {
                Toast.makeText(this, "Tail transfer data error: " + res, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Write data success ", Toast.LENGTH_LONG).show();
    }

    private String parametersToChar(int position) {
        String result = "";
        for (int i = 0; i < parametersAndroidApp[position].length; i++) {
            result += (char)parametersAndroidApp[position][i];
        }
        return result;
    }

    private String getRawDataFromFile() {

        int readedCharFromFile = 0;
        int flagByte = 0;
        String oneByte = "";
        String result = "";

//        InputStream fin = new FileInputStream(fileName);
        InputStream fin = this.getResources().openRawResource(R.raw.data_array);

        while (readedCharFromFile != -1) {
            if (flagByte == 2) {
                result += (char)Integer.parseInt(oneByte, 16);
                flagByte = 0;
                oneByte = "";
            } else {
                try {
                    readedCharFromFile = fin.read();
                } catch (IOException e) {
//                    e.printStackTrace();
                    log("Trouble read file - IOException");

                }
                if (readedCharFromFile == 13 || readedCharFromFile == 10) continue;
                flagByte++;
                oneByte += (char)readedCharFromFile;
            }
        }
        try {
            fin.close();
        } catch (IOException e) {
//            e.printStackTrace();
            log("Trouble close file - IOException");

        }
        return result;
    }

    private void log(String logMessage) {
        this.log += logMessage + "\n";
        myTextViewLog.setText(this.log);
    }

    private void log(String logMessage, boolean show) {
        if (show) {
            this.log += logMessage + "\n";
            myTextViewLog.setText(this.log);
        }
    }

}
