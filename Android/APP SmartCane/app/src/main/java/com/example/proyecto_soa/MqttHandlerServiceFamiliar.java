package com.example.proyecto_soa;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandlerServiceFamiliar extends Service implements MqttCallback {
    public static final String ACTION_DATA_ALARM_RECEIVE = "com.example.intentservice.intent.action.DATA_ALARM_RECEIVE";
    public static final String ACTION_DATA_ON_OFF_RECEIVE = "com.example.intentservice.intent.action.DATA_ON_OFF_RECEIVE";
    public static final String ACTION_DATA_STEPS_RECEIVE = "com.example.intentservice.intent.action.DATA_STEPS_RECEIVE";
    public static final String ACTION_CONNECTION_LOST = "com.example.intentservice.intent.action.CONNECTION_LOST";
    public static final String ACTION_DATA_LOCATION_RECEIVE = "com.example.intentservice.intent.action.LOCATION_RECEIVE";

    private MqttClient mqttClient;
    public static boolean isRunning = false;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();

        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (intent != null && intent.hasExtra("CLIENT_ID")) {
                    String clientId = intent.getStringExtra("CLIENT_ID");

                    connectMqtt(clientId);
                }
            }
        });
        thread.start();
        return START_STICKY;
    }

    public void connectMqtt(String clientId) {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            options.setUserName(configMQTT.userName);
            options.setPassword(configMQTT.userPass.toCharArray());
            MemoryPersistence persistence = new MemoryPersistence();

            mqttClient = new MqttClient(configMQTT.mqttServer, clientId, persistence);
            mqttClient.connect(options);

            mqttClient.setCallback(this);

            mqttClient.subscribe(configMQTT.TOPIC_ON_OFF_EMQX);
            mqttClient.subscribe(configMQTT.TOPIC_ALARM_EMQX);
            mqttClient.subscribe(configMQTT.TOPIC_STEP_EMQX);
            mqttClient.subscribe(configMQTT.TOPIC_LOCATION_EMQX);

        } catch (MqttException e) {
            Log.d("Aplicacion", e.getMessage() + "  " + e.getCause());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d("MAIN ACTIVITY", "conexion perdida" + cause.getMessage().toString());

        Intent i = new Intent(ACTION_CONNECTION_LOST);
        sendBroadcast(i);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
               if (topic.equals(configMQTT.TOPIC_ALARM_EMQX)) {
                    String alarm_state = message.toString();

                    Intent i = new Intent(ACTION_DATA_ALARM_RECEIVE);
                    i.putExtra("alarm_state", alarm_state);

                    sendBroadcast(i);
                } else if (topic.equals(configMQTT.TOPIC_ON_OFF_EMQX)) {
                    Log.d("MQTT", "service familiar recibe el mensaje de on-off de MQTT");
                    String alarm_state = message.toString();

                    Intent i = new Intent(ACTION_DATA_ON_OFF_RECEIVE);
                    i.putExtra("cane_state", alarm_state);

                    sendBroadcast(i);
                } else if (topic.equals(configMQTT.TOPIC_STEP_EMQX)) {
                    String steps = message.toString();
                    Log.d("MQTT", "service familiar recibe el mensaje de pasos de MQTT");
                    Log.d("MQTT", steps);

                    Intent i = new Intent(ACTION_DATA_STEPS_RECEIVE);
                    i.putExtra("step_state", steps);

                    sendBroadcast(i);
                } else if (topic.equals(configMQTT.TOPIC_LOCATION_EMQX)) {
                   String location = message.toString();

                   Intent i = new Intent(ACTION_DATA_LOCATION_RECEIVE);
                   i.putExtra("location_state", location);

                   Log.e("MQTT", "Location: " + location);

                   sendBroadcast(i);
               }
            }
        });
        thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }

        isRunning = false;
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
