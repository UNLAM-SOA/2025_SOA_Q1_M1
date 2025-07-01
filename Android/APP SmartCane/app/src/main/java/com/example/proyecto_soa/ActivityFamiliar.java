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

public class ActivityFamiliar extends AppCompatActivity {
    private TextView txt_state;
    private TextView txt_alarm;
    private TextView txt_pasos;
    private int pasos = 0;
    private float lat;
    private float lon;

    public IntentFilter filterStepReceive;
    public IntentFilter filterAlarmReceive;
    public IntentFilter filterStateReceive;
    public IntentFilter filterConncetionLost;
    public IntentFilter filterLocationReceive;
    private ReceptorStateOperacion receiverState = new ReceptorStateOperacion();
    private ReceptorAlarmOperacion receiverAlarm = new ReceptorAlarmOperacion();
    private ReceptorStepOperacion receiverStep = new ReceptorStepOperacion();
    private ReceptorLocationOperacion receiverLocation = new ReceptorLocationOperacion();
    private ActivityFamiliar.ConnectionLost connectionLost = new ActivityFamiliar.ConnectionLost();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_familiar);

        txt_pasos = (TextView) findViewById(R.id.id_txt_pasos_familiar);
        txt_state = (TextView) findViewById(R.id.id_txt_estado_baston);
        txt_alarm = (TextView) findViewById(R.id.id_txt_alarma_baston);

        if(!MqttHandlerServiceFamiliar.isRunning){
            Intent intent = new Intent(this, MqttHandlerServiceFamiliar.class);
            String clientId = configMQTT.getClientId(getApplicationContext(), configMQTT.CLIENT_FAM_ID);
            intent.putExtra("CLIENT_ID", clientId);
            intent.putExtra("ROL", getIntent().getStringExtra("ROL"));
            startService(intent);
        }

        configurarBroadcastReciever();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Intent intent = new Intent(this, MqttHandlerServiceFamiliar.class);
        //stopService(intent);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void configurarBroadcastReciever() {

        filterStepReceive = new IntentFilter(MqttHandlerServiceFamiliar.ACTION_DATA_STEPS_RECEIVE);
        filterAlarmReceive = new IntentFilter(MqttHandlerServiceFamiliar.ACTION_DATA_ALARM_RECEIVE);
        filterStateReceive = new IntentFilter(MqttHandlerServiceFamiliar.ACTION_DATA_ON_OFF_RECEIVE);
        filterConncetionLost = new IntentFilter(MqttHandlerServiceFamiliar.ACTION_CONNECTION_LOST);
        filterLocationReceive = new IntentFilter(MqttHandlerServiceFamiliar.ACTION_DATA_LOCATION_RECEIVE);

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

    public class ConnectionLost extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            Toast.makeText(getApplicationContext(), "Conexion Perdida", Toast.LENGTH_SHORT).show();
        }
    }

    public class ReceptorStateOperacion extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String estado = intent.getStringExtra("cane_state");
            txt_state.setText(estado);
        }
    }

    public class ReceptorAlarmOperacion extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String alarma = intent.getStringExtra("alarm_state");
            txt_alarm.setText(alarma);
        }
    }

    public class ReceptorStepOperacion extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Log.d("MQTT", "Llegaron los pasos a la activity familiar");
            String steps = intent.getStringExtra("step_state");
            Log.d("MQTT", steps);
            if (steps != null) {
                pasos = Integer.parseInt(steps);
            } else {
                pasos = -1;
            }
            txt_pasos.setText("Pasos: " + pasos);
        }
    }

    public class ReceptorLocationOperacion extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String loc = intent.getStringExtra("location_state");

            if (loc != null && loc.contains(",")) {
                String[] parts = loc.split(",");
                try {
                    double lat = Double.parseDouble(parts[0]);
                    double lon = Double.parseDouble(parts[1]);

                    Uri uri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);

                    mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(mapIntent); // Android mostrará apps compatibles

                } catch (NumberFormatException e) {
                    Log.e("ReceptorLocation", "Formato inválido: " + loc);
                }
            } else {
                Log.e("ReceptorLocation", "Ubicación inválida recibida: " + loc);
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        configurarBroadcastReciever();
        txt_pasos.setText("Pasos: " + pasos);
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
