package com.example.proyecto_soa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandlerServiceCane extends Service implements MqttCallback
{
    public static final String ACTION_DATA_OBSTACLE_RECEIVE = "com.example.intentservice.intent.action.DATA_OBSTACLE_RECEIVE";
    public static final String ACTION_CONNECTION_LOST = "com.example.intentservice.intent.action.CONNECTION_LOST";
    private MqttClient mqttClient;
    private int steps = 0;

    public static boolean isRunning = false;

    private final BroadcastReceiver publishReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if ("PUBLISH_MQTT_MESSAGE".equals(intent.getAction()))
            {
                String message = intent.getStringExtra(configMQTT.topicCMD);

                if (message != null)
                {
                    publish(configMQTT.topicCMD, message);
                }
            }
        }
    };

    private final BroadcastReceiver publishStepReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if ("PASOS_ACTUALIZADOS".equals(intent.getAction()))
            {
                steps = intent.getIntExtra("pasos", 0);
                String stringSteps = Integer.toString(steps);

                if (stringSteps != null)
                {
                    publish(configMQTT.TOPIC_STEP_EMQX, stringSteps);
                }
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate()
    {
        super.onCreate();

        isRunning = true;

        IntentFilter filter = new IntentFilter("PUBLISH_MQTT_MESSAGE");
        registerReceiver(publishReceiver, filter);

        IntentFilter filter2 = new IntentFilter("PASOS_ACTUALIZADOS");
        registerReceiver(publishStepReceiver, filter2);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (intent != null && intent.hasExtra("CLIENT_ID"))
                    {
                        String clientId = intent.getStringExtra("CLIENT_ID");
                        connectMqtt(clientId);
                    }
                }
            }
        );

        thread.start();
        return START_STICKY;
    }

    public void connectMqtt(String clientId)
    {
        try
        {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            options.setUserName(configMQTT.userName);
            options.setPassword(configMQTT.userPass.toCharArray());
            MemoryPersistence persistence = new MemoryPersistence();

            mqttClient = new MqttClient(configMQTT.mqttServer, clientId, persistence);
            mqttClient.connect(options);

            mqttClient.setCallback(this);

            mqttClient.subscribe(configMQTT.TOPIC_OBSTACLE_EMQX);
            mqttClient.subscribe(configMQTT.TOPIC_ALARM_EMQX);
        } catch (MqttException e)
        {
            Log.d("Aplicacion", e.getMessage() + "  " + e.getCause());
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message)
    {
        try
        {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(2);
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause)
    {
        Intent i = new Intent(ACTION_CONNECTION_LOST);
        sendBroadcast(i);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception
    {
        Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (topic.equals(configMQTT.TOPIC_OBSTACLE_EMQX))
                    {
                        String pos_obstacle = message.toString();

                        Intent i = new Intent(ACTION_DATA_OBSTACLE_RECEIVE);
                        i.putExtra("pos_obstacle", pos_obstacle);

                        sendBroadcast(i);
                    }  else if (topic.equals(configMQTT.TOPIC_ALARM_EMQX))
                    {
                        String alarm_state = message.toString();

                        if(alarm_state.equals("CANE RELEASED"))
                        {
                            FusedLocationProviderClient fusedLocationClient =
                                    LocationServices.getFusedLocationProviderClient(getApplicationContext());

                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED)
                            {
                                Log.e("MQTT", "No tiene permiso de ubicación");
                                return;
                            }

                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener(location -> {
                                        if (location != null)
                                        {
                                            String message = location.getLatitude() + "," + location.getLongitude();
                                            publish(configMQTT.TOPIC_LOCATION_EMQX, message);
                                            Log.d("MQTT", "Ubicación enviada: " + message);
                                        } else
                                        {
                                            Log.e("MQTT", "No se pudo obtener la ubicación");
                                        }
                                    });
                        }
                    }
                }
            }
        );
        thread.start();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(publishReceiver);
        unregisterReceiver(publishStepReceiver);
        try
        {
            if (mqttClient != null && mqttClient.isConnected())
            {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (MqttException e)
        {
            e.printStackTrace();
        }
        isRunning = false;
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
