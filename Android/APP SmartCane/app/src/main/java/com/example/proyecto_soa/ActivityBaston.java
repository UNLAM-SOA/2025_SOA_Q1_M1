package com.example.proyecto_soa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class ActivityBaston extends AppCompatActivity {
    private TextView txt_obstacle;
    private Button cmdApagar;
    private Button cmdEncender;
    private Button cmdPasos;
    public Intent intent_activity2;
    public IntentFilter filterReceive;
    public IntentFilter filterConncetionLost;
    private ReceptorOperacion receiver = new ReceptorOperacion();
    private ConnectionLost connectionLost = new ConnectionLost();
    private MediaPlayer mediaPlayer;
    private boolean sonidoHabilitado = true;
    private TextView iconToggleSound;
    private String obstacle="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_baston);

        txt_obstacle = (TextView) findViewById(R.id.txt_obstacle);
        cmdApagar = (Button) findViewById(R.id.button_off);
        cmdEncender = (Button) findViewById(R.id.button_on);
        cmdPasos = (Button) findViewById(R.id.button_pasos);

        iconToggleSound=(TextView)findViewById(R.id.icon_toggle_sound);

        iconToggleSound.setOnClickListener(botonesListeners);
        cmdApagar.setOnClickListener(botonesListeners);
        cmdEncender.setOnClickListener(botonesListeners);
        cmdPasos.setOnClickListener(botonesListeners);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100
            );
        }

        if(!MqttHandlerServiceBaston.isRunning){
            Intent intent = new Intent(this, MqttHandlerServiceBaston.class);
            String clientId = configMQTT.getClientId(getApplicationContext(), configMQTT.CLIENT_BASTON_ID);
            intent.putExtra("CLIENT_ID", clientId);
            intent.putExtra("ROL", getIntent().getStringExtra("ROL"));
            startService(intent);
        }

        Intent intent2 = new Intent(this, ServicioPasosAcelerometro.class);
        startService(intent2);

        configurarBroadcastReciever();
    }

    private View.OnClickListener botonesListeners = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.button_on) {
                Intent intent_on = new Intent("PUBLISH_MQTT_MESSAGE");
                intent_on.putExtra(configMQTT.topicEstado, "ON");
                Log.d("baston", "Envía broadcast");
                sendBroadcast(intent_on);
                Log.d("baston", "Ya mando broadcast");
            } else if (view.getId() == R.id.button_off) {
                Intent intent_off = new Intent("PUBLISH_MQTT_MESSAGE");
                intent_off.putExtra(configMQTT.topicEstado, "OFF");
                sendBroadcast(intent_off);
            } else if (view.getId() == R.id.button_pasos) {
                intent_activity2 = new Intent(ActivityBaston.this, ActivityPasos.class);
                startActivity(intent_activity2);
                Log.d("DEBUG", "Botón presionado, iniciando SegundaActivity");
            } else if (view.getId() == R.id.icon_toggle_sound) {
                sonidoHabilitado = !sonidoHabilitado;
                String estado = sonidoHabilitado ? "🔊" : "🔇";
                iconToggleSound.setText(estado);
            }else
                Toast.makeText(getApplicationContext(), "Error en Listener de botones", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(connectionLost);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void configurarBroadcastReciever() {

        filterReceive = new IntentFilter(MqttHandlerServiceBaston.ACTION_DATA_OBSTACLE_RECEIVE);
        filterConncetionLost = new IntentFilter(MqttHandlerServiceBaston.ACTION_CONNECTION_LOST);

        filterReceive.addCategory(Intent.CATEGORY_DEFAULT);
        filterConncetionLost.addCategory(Intent.CATEGORY_DEFAULT);

        registerReceiver(receiver, filterReceive);
        registerReceiver(connectionLost, filterConncetionLost);

    }

    public class ConnectionLost extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), "Conexion Perdida", Toast.LENGTH_SHORT).show();
        }
    }

    public class ReceptorOperacion extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("pos_obstacle");
            txt_obstacle.setText(text);

            if(!obstacle.equals(text))
            {
                obstacle=text;
                if(sonidoHabilitado) {
                    playSound(context, text);
                }
            }
        }
    }

    void playSound(Context context, String text) {
        if (text != null) {
            text = text.toLowerCase();
            int soundResId = -1;
            if (text.contains("obstacle_right")) {
                soundResId = R.raw.obstaculo_derecha;
            } else if (text.contains("obstacle_left")) {
                soundResId = R.raw.obstaculo_izquierda;
            } else if (text.contains("obstacle_both")) {
                soundResId = R.raw.obstaculo_frente;
            }
            if (soundResId != -1) {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context, soundResId);
                mediaPlayer.start();
            }
        }
    }
}
