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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class ActivityCane extends AppCompatActivity
{
    private TextView txtObstacle;
    private Button cmdTurnOff;
    private Button cmdTurnOn;
    private Button cmdSteps;
    public Intent intentActivitySteps;
    public IntentFilter filterReceive;
    public IntentFilter filterConnectionLost;
    private OperationReceiver receiver = new OperationReceiver();
    private ConnectionLost connectionLost = new ConnectionLost();
    private MediaPlayer mediaPlayer;
    private boolean soundEnabled = true;
    private TextView iconToggleSound;
    private String obstacle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.cane_activity);

        txtObstacle = (TextView) findViewById(R.id.txt_obstacle);
        cmdTurnOff = (Button) findViewById(R.id.button_off);
        cmdTurnOn = (Button) findViewById(R.id.button_on);
        cmdSteps = (Button) findViewById(R.id.button_steps);

        iconToggleSound = (TextView) findViewById(R.id.icon_toggle_sound);

        iconToggleSound.setOnClickListener(buttonListeners);
        cmdTurnOff.setOnClickListener(buttonListeners);
        cmdTurnOn.setOnClickListener(buttonListeners);
        cmdSteps.setOnClickListener(buttonListeners);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100
            );
        }

        if(!MqttHandlerServiceCane.isRunning)
        {
            Intent intent = new Intent(this, MqttHandlerServiceCane.class);
            String clientId = configMQTT.getClientId(getApplicationContext(), configMQTT.CLIENT_CANE_ID);
            intent.putExtra("CLIENT_ID", clientId);
            intent.putExtra("ROL", getIntent().getStringExtra("ROL"));
            startService(intent);
        }

        Intent intent2 = new Intent(this, StepsWithAccelerometerService.class);
        startService(intent2);

        configureBroadcastReceiver();
    }

    private View.OnClickListener buttonListeners = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            if (view.getId() == R.id.button_on)
            {
                Intent intent_on = new Intent("PUBLISH_MQTT_MESSAGE");
                intent_on.putExtra(configMQTT.topicCMD, "ON");
                sendBroadcast(intent_on);
            } else if (view.getId() == R.id.button_off)
            {
                Intent intent_off = new Intent("PUBLISH_MQTT_MESSAGE");
                intent_off.putExtra(configMQTT.topicCMD, "OFF");
                sendBroadcast(intent_off);
            } else if (view.getId() == R.id.button_steps)
            {
                intentActivitySteps = new Intent(ActivityCane.this, ActivitySteps.class);
                startActivity(intentActivitySteps);
            } else if (view.getId() == R.id.icon_toggle_sound)
            {
                soundEnabled = !soundEnabled;
                String soundState = soundEnabled ? "🔊" : "🔇";
                iconToggleSound.setText(soundState);
            } else
            {
                Toast.makeText(getApplicationContext(), "Error en Listener de los Botones", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(connectionLost);

        if (mediaPlayer != null)
        {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void configureBroadcastReceiver()
    {
        filterReceive = new IntentFilter(MqttHandlerServiceCane.ACTION_DATA_OBSTACLE_RECEIVE);
        filterConnectionLost = new IntentFilter(MqttHandlerServiceCane.ACTION_CONNECTION_LOST);

        filterReceive.addCategory(Intent.CATEGORY_DEFAULT);
        filterConnectionLost.addCategory(Intent.CATEGORY_DEFAULT);

        registerReceiver(receiver, filterReceive);
        registerReceiver(connectionLost, filterConnectionLost);
    }

    public class ConnectionLost extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            Toast.makeText(getApplicationContext(), "Conexion Perdida", Toast.LENGTH_SHORT).show();
        }
    }

    public class OperationReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            String posObstacleText = intent.getStringExtra("pos_obstacle");
            txtObstacle.setText(posObstacleText);

            if(!obstacle.equals(posObstacleText))
            {
                obstacle = posObstacleText;
                if(soundEnabled)
                {
                    playSound(context, posObstacleText);
                }
            }
        }
    }

    void playSound(Context context, String posObstacleText)
    {
        if (posObstacleText != null)
        {
            posObstacleText = posObstacleText.toLowerCase();
            int soundResId = -1;

            if (posObstacleText.contains("obstacle_right"))
            {
                soundResId = R.raw.obstaculo_derecha;
            } else if (posObstacleText.contains("obstacle_left"))
            {
                soundResId = R.raw.obstaculo_izquierda;
            } else if (posObstacleText.contains("obstacle_both"))
            {
                soundResId = R.raw.obstaculo_frente;
            }
            if (soundResId != -1)
            {
                if (mediaPlayer != null)
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context, soundResId);
                mediaPlayer.start();
            }
        }
    }
}
