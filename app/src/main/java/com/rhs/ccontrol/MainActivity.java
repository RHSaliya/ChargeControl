package com.rhs.ccontrol;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Locale;

import eo.view.batterymeter.BatteryMeterView;
import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {

    private TextView stopChargingTV, startChargingTV, chargeTV;
    private SeekBar stopChargingSeekBar, startChargingSeekBar;
    private TextView statusIconTV;
    private Process process;
    private File file;
    private InputStream is;
    private Button btnAction, btnApply;
    private int i;
    private TextView chargeStatusTV, chargeSpeedTV, chargeTempTV;
    private final BatteryBroadcast batteryBroadcast = new BatteryBroadcast();
    private BatteryManager batteryManager;
    private String start, stop;
    private BatteryMeterView batteryMeterView;
    private FrameLayout aboutFrame;
    private final Handler handler = new Handler();
    private boolean isCharging;
    private boolean doubleBackPressFlag = false;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);

        startChargingSeekBar = findViewById(R.id.startChargingSeekBar);
        stopChargingSeekBar = findViewById(R.id.stopChargingSeekBar);
        batteryMeterView = findViewById(R.id.batteryMeterView);
        startChargingTV = findViewById(R.id.startChargingTV);
        stopChargingTV = findViewById(R.id.stopChargingTV);
        chargeStatusTV = findViewById(R.id.chargeStatusTV);
        chargeSpeedTV = findViewById(R.id.chargeSpeedTV);
        chargeTempTV = findViewById(R.id.chargeTempTV);
        statusIconTV = findViewById(R.id.statusIconTV);
        aboutFrame = findViewById(R.id.aboutFrame);
        btnAction = findViewById(R.id.btnAction);
        btnApply = findViewById(R.id.btnApply);
        chargeTV = findViewById(R.id.chargeTV);

        if (!Shell.SU.available()) {
            statusIconTV.setText(Html.fromHtml("<font color='Red'>Root permission required.</font>"));
            statusIconTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            btnAction.setText(getString(R.string.try_battery_alarm));
            btnApply.setEnabled(false);
            btnApply.setAlpha(0.5f);
            set();
        } else if (!Arrays.toString(Build.SUPPORTED_ABIS).contains("arm")) {
            statusIconTV.setText(Html.fromHtml("<font color='Red'>Device Not Supported, Contact Us.</font>"));
            statusIconTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            btnAction.setText(getString(R.string.try_battery_alarm));
            btnApply.setEnabled(false);
            btnApply.setAlpha(0.5f);
            set();
        } else {
            try {
                process = Runtime.getRuntime().exec("su -c pgrep ipc");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while (bufferedReader.readLine() != null) {
                    i++;
                }
                process = Runtime.getRuntime().exec("su -c [ -e /data/adb/modules/IPControl/ipc.conf ] && echo 1 || echo 0");
                if (process.getInputStream().read() == 49) {
                    process = Runtime.getRuntime().exec("su -c cat /data/adb/modules/IPControl/ipc.conf");
                    String str;
                    bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    while ((str = bufferedReader.readLine()) != null) {
                        if (str.contains("thr_")) {
                            if (str.contains("disable")) {
                                stop = str.substring(12);
                                stopChargingSeekBar.setProgress(Integer.parseInt(stop) - 40);
                                stopChargingTV.setText(String.format("%s%%", stop));
                            } else {
                                start = str.substring(11);
                                startChargingSeekBar.setProgress(Integer.parseInt(start) - 30);
                                startChargingTV.setText(String.format("%s%%", start));
                                break;
                            }
                        }
                    }
                    if (i > 1) {
                        btnAction.setText(getString(R.string.stop_service));
                        statusIconTV.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_lock, 0, 0);
                    } else {
                        btnAction.setText(getString(R.string.start_service));
                        statusIconTV.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_lock_open, 0, 0);
                    }
                    setStatus();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        stopChargingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                stopChargingTV.setText(String.format(Locale.ENGLISH, "%d%%", progress + 40));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (startChargingSeekBar.getProgress()-10 >= stopChargingSeekBar.getProgress()){
                    startChargingTV.setText(String.format(Locale.ENGLISH, "%d%%", seekBar.getProgress()+39));
                    startChargingSeekBar.setProgress(stopChargingSeekBar.getProgress()+9);
                }
            }
        });

        startChargingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress-10 >= stopChargingSeekBar.getProgress()){
                    startChargingTV.setText(String.format(Locale.ENGLISH, "%d%%", stopChargingSeekBar.getProgress()+39));
                    startChargingSeekBar.setProgress(stopChargingSeekBar.getProgress()+9);
                }else{
                    startChargingTV.setText(String.format(Locale.ENGLISH, "%d%%", 30 + progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnApply.setEnabled(false);
                try {
                    process = Runtime.getRuntime().exec("su -c ipc --update " + stopChargingTV.getText().toString().replace("%","") + " " + startChargingTV.getText().toString().replace("%",""));
                    stop = stopChargingTV.getText().toString().replace("%", "");
                    start = startChargingTV.getText().toString().replace("%", "");
                    setStatus();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                btnApply.setEnabled(true);
            }
        });

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAction.setEnabled(false);
                try {
                    if (btnAction.getText().equals(getString(R.string.start_service))) {
                        AssetManager am = MainActivity.this.getAssets();
                        process = Runtime.getRuntime().exec("su -c [ -e /bin/ipc ] && echo 1 || echo 0");
                        if (process.getInputStream().read() != 49) {
                            is = am.open("ipc");
                            file = new File(getDir("temp", Context.MODE_PRIVATE), "ipc");
                            FileOutputStream fileOutputStream = new FileOutputStream(file);
                            byte[] buffer = new byte[is.available()];
                            int result = is.read(buffer);
                            if (result != -1) {
                                fileOutputStream.write(buffer);
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            }
                            process = Runtime.getRuntime().exec("su -c  mv " + file.getAbsolutePath() + " /bin/ipc");
                        }

                        process = Runtime.getRuntime().exec("su -c [ -e /data/adb ] && echo 1 || echo 0");
                        if (process.getInputStream().read() != 49) {
                            process = Runtime.getRuntime().exec("su -c  mkdir /data/adb");
                        }
                        process = Runtime.getRuntime().exec("su -c [ -e /data/adb/modules ] && echo 1 || echo 0");
                        if (process.getInputStream().read() != 49) {
                            process = Runtime.getRuntime().exec("su -c  mkdir /data/adb/modules");
                        }
                        process = Runtime.getRuntime().exec("su -c [ -e /data/adb/modules/IPControl ] && echo 1 || echo 0");
                        if (process.getInputStream().read() != 49) {
                            process = Runtime.getRuntime().exec("su -c  mkdir /data/adb/modules/IPControl");
                        }


                        process = Runtime.getRuntime().exec("su -c [ -e /data/adb/modules/IPControl/ipc.conf ] && echo 1 || echo 0");
                        if (process.getInputStream().read() != 49) {
                            file = new File(getDir("temp", Context.MODE_PRIVATE), "ipc.conf");
                            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                            bufferedWriter.write("# Automation status.\nautomation=true\n# Disable threshold.\nthr_disable=80\n# Enable threshold.\nthr_enable=75\n\n# Switch trigger file.\ntrigger=/sys/class/power_supply/battery/batt_slate_mode\n# Positive value of switch.\npos_val=0\n# Negative value of switch.\nneg_val=1");
                            bufferedWriter.flush();
                            bufferedWriter.close();
                            process = Runtime.getRuntime().exec("su -c  mv " + file.getAbsolutePath() + " /data/adb/modules/IPControl/ipc.conf");
                        }
                        process = Runtime.getRuntime().exec("su -c [ -e /data/adb/modules/IPControl/module.prop ] && echo 1 || echo 0");
                        if (process.getInputStream().read() != 49) {
                            file = new File(getDir("temp", Context.MODE_PRIVATE), "module.prop");
                            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                            bufferedWriter.write("id=IPControl\nname=Input Power Control\nversion=3.0.1\nversionCode=35\nauthor=Jaymin Suthar\ndescription=Completely native input power control tool written purely in C++ for performance and stability\n");
                            bufferedWriter.flush();
                            bufferedWriter.close();
                            process = Runtime.getRuntime().exec("su -c  mv " + file.getAbsolutePath() + " /data/adb/modules/IPControl/module.prop");
                        }
                        process = Runtime.getRuntime().exec("su -c  chmod +rwx /bin/ipc");
                        process = Runtime.getRuntime().exec("su -c ipc --daemon launch");
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String str = bufferedReader.readLine();
                        if (str != null && str.contains("handled successfully!")) {
                            i = 2;
                            btnAction.setText(getString(R.string.stop_service));
                            setStatus();
                            statusIconTV.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_lock, 0, 0);
                        } else {
                            i = 1;
                            statusIconTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                            statusIconTV.setText(Html.fromHtml("<font color='Red'>Error, Restart may solve it.</font>"));
                        }
                    } else if (btnAction.getText().equals(getString(R.string.stop_service))) {
                        i = 1;
                        process = Runtime.getRuntime().exec("su -c killall ipc");
                        setStatus();
                        statusIconTV.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_lock_open, 0, 0);
                        btnAction.setText(getString(R.string.start_service));
                    } else {
                        View permissionView = LayoutInflater.from(MainActivity.this).inflate(R.layout.visit_store, null);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.CustomDialogue);
                        builder.setView(permissionView);
                        final AlertDialog dialog = builder.create();
                        dialog.show();
                        TextView cancelTV = dialog.findViewById(R.id.cancelTV);
                        TextView yesTV = dialog.findViewById(R.id.yesTV);

                        cancelTV.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        yesTV.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.rhs.battery")));
                                } catch (android.content.ActivityNotFoundException abe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.rhs.battery")));
                                }
                            }
                        });
                    }
                    btnAction.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void set() {
        stopChargingSeekBar.setProgress(40);
        startChargingSeekBar.setProgress(40);
        startChargingTV.setText("70%");
        stopChargingTV.setText("80%");
    }

    public void onCancelClick(View view) {
        onBackPressed();
    }


    private class BatteryBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryMeterView.setChargeLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));
            chargeTV.setText(String.format(Locale.ENGLISH, "%d%%", intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)));
            if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0) == 2) {
                chargeStatusTV.setText(getString(R.string.charging));
                isCharging=true;
            } else {
                chargeStatusTV.setText(getString(R.string.discharging));
                isCharging=false;
            }
            if (getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("fahrenheit", false)) {
                chargeTempTV.setText(String.format(Locale.ENGLISH, "%.1f °F", intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) * 0.18 + 32));
            } else {
                chargeTempTV.setText(String.format(Locale.ENGLISH, "%.1f °C", intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryBroadcast, filter);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                if (Math.abs(current) > 8000) {
                    current = current / 1000;
                }
                if(isCharging){
                    chargeSpeedTV.setText(String.format(Locale.ENGLISH, "%+dmA", current));
                }else{
                    chargeSpeedTV.setText(String.format(Locale.ENGLISH, "-%dmA", Math.abs(current)));
                }
                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    private void setStatus() {
        if (i > 1) {
            statusIconTV.setText(String.format("Disable %s, Enable %s", stop, start));
        } else {
            statusIconTV.setText(Html.fromHtml("<font color='#aaaaaa'>Disable " + stop + ", Enable " + start + "</font>"));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryBroadcast);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final MenuItem about = menu.findItem(R.id.action_about);
        final MenuItem share = menu.findItem(R.id.action_share);
        final MenuItem review = menu.findItem(R.id.action_review);
        final MenuItem temp = menu.findItem(R.id.action_temp);
        final MenuItem contact = menu.findItem(R.id.action_contact);
        contact.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"RHS.Dev@outlook.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Charge Controller");
                intent.putExtra(Intent.EXTRA_TEXT, "Brand: " + Build.BRAND +
                        "\nDevice: " + Build.DEVICE + "\nSDK: " + Build.VERSION.SDK_INT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(Intent.createChooser(intent, "Choose Email Client"));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        about.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                aboutFrame.setVisibility(View.VISIBLE);
                aboutFrame.setAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.slideup));
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), Color.TRANSPARENT, getResources().getColor(R.color.BGCOLOR));
                        colorAnimation.setDuration(200);
                        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                aboutFrame.setBackgroundColor((int) animator.getAnimatedValue());
                            }
                        });
                        colorAnimation.start();
                    }
                }, 499);
                return false;
            }
        });


        share.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + getPackageName());
                intent.putExtra(Intent.EXTRA_SUBJECT, "Share Battery Alarm");
                startActivity(Intent.createChooser(intent, "Share via"));
                return true;
            }
        });

        if (getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("fahrenheit", false)) {
            temp.setTitle(R.string.temperature_in_c);
        } else {
            temp.setTitle(R.string.temperature_in_f);
        }

        temp.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (temp.getTitle().equals("Temperature in °F")) {
                    temp.setTitle(R.string.temperature_in_c);
                    chargeTempTV.setText(String.format(Locale.ENGLISH, "%.1f °F", Float.parseFloat(chargeTempTV.getText().toString().replace(" °C", "")) * 1.8 + 32));
                    getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("fahrenheit", true).apply();
                } else {
                    temp.setTitle(R.string.temperature_in_f);
                    chargeTempTV.setText(String.format(Locale.ENGLISH, "%.1f °C", (Float.parseFloat(chargeTempTV.getText().toString().replace(" °F", "")) - 32) * 0.56));
                    getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("fahrenheit", false).apply();
                }
                return false;
            }
        });

        review.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                } catch (android.content.ActivityNotFoundException abe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public void onBackPressed() {
        if (aboutFrame.getVisibility() == View.VISIBLE) {
            aboutFrame.setBackgroundColor(Color.TRANSPARENT);
            aboutFrame.setVisibility(View.GONE);
            aboutFrame.setAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.slidedown));
            return;
        }
        if (doubleBackPressFlag) {
            super.onBackPressed();
        } else {
            this.doubleBackPressFlag = true;
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
        }
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackPressFlag = false;
            }
        }, 2000);
    }
}