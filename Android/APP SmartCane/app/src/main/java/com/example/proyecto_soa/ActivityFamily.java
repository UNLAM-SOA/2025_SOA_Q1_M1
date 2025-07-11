package com.example.proyecto_soa;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityFamily extends AppCompatActivity {
    private TextView txtState;
    private TextView txtAlarm;
    private TextView textSteps;
    private int steps = 0;

    public IntentFilter filterStepReceive;
    public IntentFilter filterAlarmReceive;
    public IntentFilter filterStateReceive;
    public IntentFilter filterConncetionLost;
    public IntentFilter filterLocationReceive;
    private ReceptorStateOperation receiverState = new ReceptorStateOperation();
    private ReceptorAlarmOperation receiverAlarm = new ReceptorAlarmOperation();
    private ReceptorStepOperation receiverStep = new ReceptorStepOperation();
    private ReceptorLocationOperation receiverLocation = new ReceptorLocationOperation();
    private ActivityFamily.ConnectionLost connectionLost = new ActivityFamily.ConnectionLost();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_familiar);

        textSteps = (TextView) findViewById(R.id.id_txt_family_steps);
        txtState = (TextView) findViewById(R.id.id_txt_cane_status);
        txtAlarm = (TextView) findViewById(R.id.id_txt_cane_alarm);

        if(!MqttHandlerServiceFamily.isRunning)
        {
            Intent intent = new Intent(this, MqttHandlerServiceFamily.class);
            String clientId = configMQTT.getClientId(getApplicationContext(), configMQTT.CLIENT_FAM_ID);
            intent.putExtra("CLIENT_ID", clientId);
            intent.putExtra("ROL", getIntent().getStringExtra("ROL"));
            startService(intent);
        }

        configureBroadcastReceiver();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void configureBroadcastReceiver()
    {
        filterStepReceive = new IntentFilter(MqttHandlerServiceFamily.ACTION_DATA_STEPS_RECEIVE);
        filterAlarmReceive = new IntentFilter(MqttHandlerServiceFamily.ACTION_DATA_ALARM_RECEIVE);
        filterStateReceive = new IntentFilter(MqttHandlerServiceFamily.ACTION_DATA_ON_OFF_RECEIVE);
        filterConncetionLost = new IntentFilter(MqttHandlerServiceFamily.ACTION_CONNECTION_LOST);
        filterLocationReceive = new IntentFilter(MqttHandlerServiceFamily.ACTION_DATA_LOCATION_RECEIVE);

        filterAlarmReceive.addCategory(Intent.CATEGORY_DEFAULT);
        filterStateReceive.addCategory(Intent.CATEGORY_DEFAULT);
        filterStepReceive.addCategory(Intent.CATEGORY_DEFAULT);
        filterConncetionLost.addCategory(Intent.CATEGORY_DEFAULT);
        filterLocationReceive.addCategory(Intent.CATEGORY_DEFAULT);

        registerReceiver(receiverStep, filterStepReceive);
        registerReceiver(receiverState, filterStateReceive);
        registerReceiver(receiverAlarm, filterAlarmReceive);
        registerReceiver(connectionLost, filterConncetionLost);
        registerReceiver(receiverLocation, filterLocationReceive);
    }

    public class ConnectionLost extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            Toast.makeText(getApplicationContext(), "Conexion Perdida", Toast.LENGTH_SHORT).show();
        }
    }

    public class ReceptorStateOperation extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            String caneState = intent.getStringExtra("cane_state");
            txtState.setText(caneState);
        }
    }

    public class ReceptorAlarmOperation extends BroadcastReceiver
    {

        public void onReceive(Context context, Intent intent)
        {
            String alarmState = intent.getStringExtra("alarm_state");
            txtAlarm.setText(alarmState);
        }
    }

    public class ReceptorStepOperation extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            String steps = intent.getStringExtra("step_state");
            if (steps != null)
            {
                ActivityFamily.this.steps = Integer.parseInt(steps);
            } else
            {
                ActivityFamily.this.steps = -1;
            }
            textSteps.setText("Pasos: " + ActivityFamily.this.steps);
        }
    }

    public class ReceptorLocationOperation extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            String loc = intent.getStringExtra("location_state");

            if (loc != null && loc.contains(","))
            {
                String[] parts = loc.split(",");
                try
                {
                    double lat = Double.parseDouble(parts[0]);
                    double lon = Double.parseDouble(parts[1]);

                    Uri uri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);

                    mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(mapIntent);

                } catch (NumberFormatException e)
                {
                    Log.e("ReceptorLocation", "Formato inválido: " + loc);
                }
            } else
            {
                Log.e("ReceptorLocation", "Ubicación inválida recibida: " + loc);
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        configureBroadcastReceiver();
        textSteps.setText("Pasos: " + steps);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverAlarm);
        unregisterReceiver(receiverState);
        unregisterReceiver(receiverStep);
        unregisterReceiver(connectionLost);
    }
}
