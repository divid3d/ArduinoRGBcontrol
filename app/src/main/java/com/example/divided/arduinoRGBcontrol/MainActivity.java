package com.example.divided.arduinoRGBcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;
import com.flask.colorpicker.builder.ColorWheelRendererBuilder;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;
import com.white.progressview.HorizontalProgressView;
import com.xw.repo.BubbleSeekBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import es.dmoral.toasty.Toasty;
import mehdi.sakout.fancybuttons.FancyButton;

public class MainActivity extends AppCompatActivity {

    private static boolean isDoubleBackToExitPressedOnce = false;
    private static boolean isDeviceOn = false;
    private static boolean isBluetoothConnected = false;
    private static boolean isTooltipOpened = false;

    final private int REQUEST_ENABLE_BT = 1;
    final private int BAR_ANIMATION_DELAY = 250;
    final private int VIBRATION_TIME = 15;

    BluetoothSPP bt;
    TextView mRedValue;
    TextView mGreenValue;
    TextView mBlueValue;
    TextView mConnectionStatus;
    TextView mSelectedColorHexText;
    TextView mSelectedColorHsvText;
    TextView mRenderMode;
    FancyButton mRandomButton;
    FancyButton mTurnOn;
    FancyButton mConnectButton;
    ToolTipView myToolTipView;
    ToolTipRelativeLayout mToolTipRelativeLayout;
    ToolTip mToolTip;
    BubbleSeekBar mDensityBar;
    HorizontalProgressView mRedIndicator;
    HorizontalProgressView mGreenIndicator;
    HorizontalProgressView mBlueIndicator;
    Vibrator mVibrator;
    Switch mSwitch;
    ColorPickerView mColorPicker;
    BroadcastReceiver mBluetoothAdapterStatusReceiver;
    RGBdiod rgbDiod = new RGBdiod(255, 255, 255);


    private static void vibrate(Vibrator vibrator, int time) {

        if (vibrator.hasVibrator()) {
            vibrator.vibrate(time);
        }
    }

    private static byte convertUint8ToByte(int uint8) {

        return (byte) (uint8 & 0xff);
    }

    private static ColorStateList getColorOfDialogSelectors(RGBdiod RGBdiod, float darnknessFactor) {

        int[][] states = new int[][]{
                new int[]{}
        };
        int[] colors = new int[]{
                RGBdiod.getDarkerColor(darnknessFactor).getColorInt(),
        };
        return new ColorStateList(states, colors);
    }

    private void sendDataOverBluetooth(int RGB[]) {

        final byte[] byteData = new byte[4];

        byteData[0] = convertUint8ToByte(RGB[0]);
        byteData[1] = convertUint8ToByte(RGB[1]);
        byteData[2] = convertUint8ToByte(RGB[2]);
        byteData[3] = convertUint8ToByte(0); // Special value to trigger Arduino buffer

        bt.send(byteData, false);

    }

    private void changeUiColor(Activity activity, int color) {

        activity.getWindow().setStatusBarColor(rgbDiod.getDarkerColor(0.8f).getColorInt());
        mRandomButton.setFocusBackgroundColor(rgbDiod.getDarkerColor(0.6f).getColorInt());
        mTurnOn.setFocusBackgroundColor(rgbDiod.getDarkerColor(0.6f).getColorInt());
        mConnectButton.setFocusBackgroundColor(rgbDiod.getDarkerColor(0.6f).getColorInt());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
        }

        String colorInHex = String.format("#%06X", (0xFFFFFF & rgbDiod.getColorWithInvertedLightness().getColorInt()));
        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"" + colorInHex + "\">" + "Arduino RGB control" + "</font>"));
        mSelectedColorHexText.setText("HEX: " + String.format(Locale.ENGLISH, "#%06X", rgbDiod.getColorInt()));
        mSelectedColorHsvText.setText("HSV: " + String.format(Locale.ENGLISH, "%d", (int) rgbDiod.getColorHSV()[0]) + "°, "
                + String.format(Locale.ENGLISH, "%d", (int) ((rgbDiod.getColorHSV()[1] / 1.0) * 100)) + "%, "
                + String.format(Locale.ENGLISH, "%d", (int) ((rgbDiod.getColorHSV()[2] / 1.0) * 100)) + "%");

    }

    private void initBluetooth() {

        BluetoothAdapter bluetoothAdapter;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toasty.error(getApplicationContext()
                    , getString(R.string.toastBluetoothUnavailableText)
                    , Toast.LENGTH_LONG
                    , true)
                    .show();
            finishAffinity();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                bt = new BluetoothSPP(getApplicationContext());
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                Toasty.info(getApplicationContext()
                        , getString(R.string.toastBluetoothEnableText)
                        , Toast.LENGTH_LONG
                        , true
                )
                        .show();
            } else {
                mConnectButton.setText(getString(R.string.mConnectButtonBluetoothDisableText));
                mConnectionStatus.setText(R.string.mConnectionStatusBluetoothDisableText);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                bt = new BluetoothSPP(getApplicationContext());
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mToolTipRelativeLayout = findViewById(R.id.activity_main_tooltipRelativeLayout);
        mSelectedColorHexText = findViewById(R.id.mSelectedHex);
        mRenderMode = findViewById(R.id.mRenderMode);
        mSelectedColorHsvText = findViewById(R.id.mSelectedHsv);
        mRandomButton = (FancyButton) findViewById(R.id.randomColorButton);
        mRandomButton.setEnabled(false);
        mTurnOn = findViewById(R.id.mTurnOn);
        mTurnOn.setEnabled(false);
        mConnectButton = findViewById(R.id.mConnectButton);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mToolTip = new ToolTip()
                .withColor(Color.WHITE)
                .withTextColor(Color.BLACK)
                .withShadow()
                .withAnimationType(ToolTip.AnimationType.FROM_TOP);
        mConnectionStatus = findViewById(R.id.btStatus);
        mDensityBar = findViewById(R.id.mDensityBar);
        mRedValue = findViewById(R.id.mR);
        mGreenValue = findViewById(R.id.mG);
        mBlueValue = findViewById(R.id.mB);
        mRedIndicator = findViewById(R.id.mRedIndicator);
        mRedIndicator.setTextVisible(false);
        mGreenIndicator = findViewById(R.id.mGreenIndicator);
        mGreenIndicator.setTextVisible(false);
        mBlueIndicator = findViewById(R.id.mBlueIndicator);
        mBlueIndicator.setTextVisible(false);
        mColorPicker = findViewById(R.id.color_picker_view);
        mColorPicker.setDensity(mDensityBar.getProgress());
        mSwitch = findViewById(R.id.mSwitch);

        mBluetoothAdapterStatusReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:

                            Toasty.info(getApplicationContext()
                                    , getString(R.string.toastBluetoothDisabledText)
                                    , Toast.LENGTH_LONG
                                    , true
                            )
                                    .show();
                            mConnectButton.setText(getString(R.string.mConnectButtonEnableBluetoothText));
                            mConnectionStatus.setTextColor(Color.WHITE);
                            mConnectionStatus.setText(getString(R.string.mConnectionStatusWaitText));
                            break;

                        case BluetoothAdapter.STATE_ON:

                            Toasty.info(getApplicationContext()
                                    , getString(R.string.toastBluetoothEnableText)
                                    , Toast.LENGTH_LONG
                                    , true
                            )
                                    .show();
                            mConnectButton.setText(getString(R.string.mConnectButtonConnectText));
                            mConnectionStatus.setTextColor(Color.WHITE);
                            mConnectionStatus.setText(getString(R.string.mConnectionStatusWaitText));
                            break;
                    }
                }
            }
        };

        registerReceiver(mBluetoothAdapterStatusReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        mConnectButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (BluetoothAdapter.getDefaultAdapter().getState() == BluetoothAdapter.STATE_OFF) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else if (BluetoothAdapter.getDefaultAdapter().getState() == BluetoothAdapter.STATE_ON) {
                            if (isBluetoothConnected) {
                                bt.disconnect();
                            } else {
                                final BluetoothDevice[] bluetoothBoundedDevices = bt.getBluetoothAdapter().getBondedDevices().toArray(
                                        new BluetoothDevice[bt.getBluetoothAdapter().getBondedDevices().size()]);

                                List<String> devicesToShow = new ArrayList<>();

                                for (BluetoothDevice bluetoothBoundedDevice : bluetoothBoundedDevices) {
                                    devicesToShow.add(bluetoothBoundedDevice.getName() + "\n" + bluetoothBoundedDevice.getAddress());
                                }

                                final ColorStateList dialogSelectorsColors = getColorOfDialogSelectors(rgbDiod, 0.5f);
                                final int dialogButtonRippleColor = rgbDiod
                                        .getDarkerColor(0.8f)
                                        .getColorWithModifiedSaturation(0.1f)
                                        .getColorInt();
                                final int dialogBackgroundColor = rgbDiod.getColorWithModifiedSaturation(0.1f).getColorInt();

                                new MaterialDialog.Builder(MainActivity.this)
                                        .title(R.string.selectDeviceDialogSelectdeviceText)
                                        .items(devicesToShow)
                                        .negativeColor(Color.BLACK)
                                        .titleGravity(GravityEnum.CENTER)
                                        .positiveColor(Color.BLACK)
                                        .contentColor(Color.BLACK)
                                        .itemsColor(Color.BLACK)
                                        .choiceWidgetColor(dialogSelectorsColors)
                                        .buttonRippleColor(dialogButtonRippleColor)
                                        .backgroundColor(dialogBackgroundColor)
                                        .itemsCallbackSingleChoice(-1,
                                                new MaterialDialog.ListCallbackSingleChoice() {

                                                    @Override
                                                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                                                        bt.connect(bluetoothBoundedDevices[which].getAddress());
                                                        bt.setBluetoothConnectionListener(
                                                                new BluetoothSPP.BluetoothConnectionListener() {

                                                                    @Override
                                                                    public void onDeviceConnected(String name, String address) {

                                                                        Toasty.success(getApplicationContext()
                                                                                , "Connected to " + bt.getConnectedDeviceName() + " (" + bt.getConnectedDeviceAddress() + ")"
                                                                                , Toast.LENGTH_SHORT
                                                                                , true)
                                                                                .show();
                                                                        mConnectionStatus.setTextColor(Color.GREEN);
                                                                        mConnectionStatus.setText(getString(R.string.mConnectionStatusConnectedText));
                                                                        mConnectButton.setText(getString(R.string.mConnectButtonDisconnectText));
                                                                        mRandomButton.setEnabled(true);
                                                                        mTurnOn.setEnabled(true);
                                                                        isBluetoothConnected = true;
                                                                    }

                                                                    @Override
                                                                    public void onDeviceDisconnected() {

                                                                        Toasty.error(getApplicationContext()
                                                                                , "Disconnected from " + bt.getConnectedDeviceName() + " (" + bt.getConnectedDeviceAddress() + ")"
                                                                                , Toast.LENGTH_SHORT
                                                                                , true)
                                                                                .show();
                                                                        mConnectionStatus.setTextColor(Color.RED);
                                                                        mConnectionStatus.setText(R.string.mConnectionStatusDisconnectedText);
                                                                        mConnectButton.setText(getString(R.string.mConnectButtonConnectText));
                                                                        mRandomButton.setEnabled(false);
                                                                        mTurnOn.setEnabled(false);
                                                                        isBluetoothConnected = false;
                                                                    }

                                                                    @Override
                                                                    public void onDeviceConnectionFailed() {

                                                                        Toasty.warning(getApplicationContext()
                                                                                , getString(R.string.toastConnectionFailedText)
                                                                                , Toast.LENGTH_SHORT
                                                                                , true)
                                                                                .show();
                                                                        mConnectionStatus.setTextColor(0xffffa500);
                                                                        mConnectionStatus.setText(R.string.mConnectionStatusFailedText);
                                                                        isBluetoothConnected = false;
                                                                    }
                                                                });
                                                        return true;
                                                    }
                                                })
                                        .positiveText(R.string.selectDeviceDialogConnectText)
                                        .negativeText(R.string.selectDeviceDialogCancelText)
                                        .show();
                            }
                        }
                    }
                });

        mRandomButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (isDeviceOn) {
                            vibrate(mVibrator, VIBRATION_TIME);
                        }

                        float[] randomHsv = new float[3];
                        Color.colorToHSV(rgbDiod.getRandomColor().getColorInt(), randomHsv);
                        randomHsv[2] = 1.0f; // maximum lightness for better visual effect
                        int outputColor = Color.HSVToColor(randomHsv);

                        mColorPicker.setColor(outputColor, false);
                        rgbDiod.setColor(mColorPicker.getSelectedColor());

                        changeUiColor(MainActivity.this, rgbDiod.getColorInt());

                        mRedValue.setText(String.format(Locale.ENGLISH, "%d", rgbDiod.getRedColor()));
                        mRedIndicator.setProgressInTime(
                                mRedIndicator.getProgress()
                                , (int) (((long) rgbDiod.getColorRGB()[0] / 255.0) * 100.0)
                                , BAR_ANIMATION_DELAY);
                        mGreenValue.setText(String.format(Locale.ENGLISH, "%d", rgbDiod.getGreenColor()));
                        mGreenIndicator.setProgressInTime(
                                mGreenIndicator.getProgress()
                                , (int) (((long) rgbDiod.getColorRGB()[1] / 255.0) * 100.0)
                                , BAR_ANIMATION_DELAY);
                        mBlueValue.setText(String.format(Locale.ENGLISH, "%d", rgbDiod.getBlueColor()));
                        mBlueIndicator.setProgressInTime(
                                mBlueIndicator.getProgress()
                                , (int) (((long) rgbDiod.getColorRGB()[2] / 255.0) * 100.0)
                                , BAR_ANIMATION_DELAY);

                        if (isDeviceOn) {
                            sendDataOverBluetooth(rgbDiod.getColorRGB());
                        }
                    }
                });

        mConnectionStatus.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (bt.getServiceState() == BluetoothState.STATE_CONNECTED && !isTooltipOpened) {
                            mToolTip.withText("Name: " + bt.getConnectedDeviceName() + "\n" + "Address: " + bt.getConnectedDeviceAddress());
                            myToolTipView = mToolTipRelativeLayout.showToolTipForView(mToolTip, findViewById(R.id.btStatus));
                            myToolTipView.setOnToolTipViewClickedListener(
                                    new ToolTipView.OnToolTipViewClickedListener() {

                                        @Override
                                        public void onToolTipViewClicked(ToolTipView toolTipView) {
                                            myToolTipView.remove();
                                            isTooltipOpened = false;
                                        }
                                    });
                            isTooltipOpened = true;
                        } else if (isTooltipOpened) {
                            myToolTipView.remove();
                            isTooltipOpened = false;
                        }
                    }
                });

        mDensityBar.setOnProgressChangedListener(

                new BubbleSeekBar.OnProgressChangedListener() {

                    @Override
                    public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

                        mColorPicker.setDensity(progress);
                        mColorPicker.setVisibility(View.GONE);
                        mColorPicker.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

                    }

                    @Override
                    public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

                    }
                });

        mTurnOn.setOnClickListener(

                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        isDeviceOn = !isDeviceOn;

                        if (isDeviceOn) {
                            vibrate(mVibrator, VIBRATION_TIME);
                            mTurnOn.setText(getString(R.string.mTurnOnButtonOffText));

                            sendDataOverBluetooth(rgbDiod.getColorRGB());
                        } else {
                            vibrate(mVibrator, VIBRATION_TIME);
                            mTurnOn.setText(getString(R.string.mTurnOnButtonOnText));
                            int[] offRGB = {0, 0, 0};

                            sendDataOverBluetooth(offRGB);
                        }
                    }
                });

        mSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mColorPicker.setRenderer(ColorWheelRendererBuilder.getRenderer(ColorPickerView.WHEEL_TYPE.FLOWER));
                            mColorPicker.setVisibility(View.GONE);
                            mColorPicker.setVisibility(View.VISIBLE);
                            mRenderMode.setText(R.string.mRenderModeFlowerRendererText);
                        } else {
                            mColorPicker.setRenderer(ColorWheelRendererBuilder.getRenderer(ColorPickerView.WHEEL_TYPE.CIRCLE));
                            mColorPicker.setVisibility(View.GONE);
                            mColorPicker.setVisibility(View.VISIBLE);
                            mRenderMode.setText(R.string.mRenderModeCircleRendererText);
                        }
                    }
                });

        mColorPicker.addOnColorChangedListener(
                new OnColorChangedListener() {

                    @Override
                    public void onColorChanged(int i) {

                        rgbDiod.setColor(i);
                        if (isBluetoothConnected && isDeviceOn) {
                            vibrate(mVibrator, VIBRATION_TIME);
                        }

                        mRedValue.setText(String.format(Locale.ENGLISH, "%d", rgbDiod.getRedColor()));
                        mRedIndicator.setProgressInTime(
                                mRedIndicator.getProgress()
                                , (int) (((long) rgbDiod.getRedColor() / 255.0) * 100.0)
                                , BAR_ANIMATION_DELAY);
                        mGreenValue.setText(String.format(Locale.ENGLISH, "%d", rgbDiod.getGreenColor()));
                        mGreenIndicator.setProgressInTime(
                                mGreenIndicator.getProgress()
                                , (int) (((long) rgbDiod.getGreenColor() / 255.0) * 100.0)
                                , BAR_ANIMATION_DELAY);
                        mBlueValue.setText(String.format(Locale.ENGLISH, "%d", rgbDiod.getBlueColor()));
                        // mBlueValue.setText(Integer.toString(rgbDiod.getBlueColor()));
                        mBlueIndicator.setProgressInTime(
                                mBlueIndicator.getProgress()
                                , (int) (((long) rgbDiod.getBlueColor() / 255.0) * 100.0)
                                , BAR_ANIMATION_DELAY);

                        changeUiColor(MainActivity.this, rgbDiod.getColorInt());

                        if (isDeviceOn) {
                            sendDataOverBluetooth(rgbDiod.getColorRGB());
                        }
                    }
                });

        changeUiColor(MainActivity.this, Color.WHITE);
        initBluetooth();
    }

    @Override
    public void onBackPressed() {

        if (isDoubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        isDoubleBackToExitPressedOnce = true;

        Toasty.info(this, getString(R.string.toastClickBackAgainText), Toast.LENGTH_SHORT, true).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                isDoubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (bt != null) {
            if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                bt.disconnect();
            } else if (bt.isServiceAvailable()) {
                bt.stopService();
            }
        }
        unregisterReceiver(mBluetoothAdapterStatusReceiver);
    }
}
