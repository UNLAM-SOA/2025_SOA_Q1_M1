package com.example.proyecto_soa;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class configMQTT {

    public static final String CLIENT_CANE_ID = "AndroidCaneClient";
    public static final String CLIENT_FAM_ID = "AndroidFamClient";
    public static final String MQTT_SERVER_EMQX = "tcp://broker.emqx.io:1883";
    public static final String USER_NAME_EMQX = "SO Avanzados";
    public static final String USER_PASS_EMQX = "SOA.2019";
    public static final String TOPIC_CMD_EMQX = "soa1c2025/smartcane/cmd";
    public static final String TOPIC_ON_OFF_EMQX = "soa1c2025/smartcane/on_off_status";
    public static final String TOPIC_OBSTACLE_EMQX = "soa1c2025/smartcane/obstacle_status";
    public static final String TOPIC_ALARM_EMQX = "soa1c2025/smartcane/alarm_status";
    public static final String TOPIC_STEP_EMQX = "soa1c2025/smartcane/step_status";
    public static final String TOPIC_LOCATION_EMQX = "soa1c2025/smartcane/location_status";

    public static String mqttServer;
    public static String userName;
    public static String userPass;
    public static String topicCMD;
    public static String topicOnOffState;
    public static String topicObstacle;
    public static String topicAlarm;
    public static String topicSteps;
    public static String topicLocation;

    public static void useServerEMQX()
    {
        mqttServer = MQTT_SERVER_EMQX;

        userName = USER_NAME_EMQX;
        userPass = USER_PASS_EMQX;

        topicCMD = TOPIC_CMD_EMQX;
        topicOnOffState = TOPIC_ON_OFF_EMQX;
        topicObstacle = TOPIC_OBSTACLE_EMQX;
        topicAlarm = TOPIC_ALARM_EMQX;
        topicSteps = TOPIC_STEP_EMQX;
        topicLocation = TOPIC_LOCATION_EMQX;
    }

    private static String clientId;

    public static String getClientId(Context context, String name)
    {
        if (clientId == null)
        {
            SharedPreferences prefs = context.getSharedPreferences("mqtt_prefs", Context.MODE_PRIVATE);
            clientId = prefs.getString("client_id", null);
            if (clientId == null)
            {
                clientId = name + UUID.randomUUID().toString();
                prefs.edit().putString("client_id", clientId).apply();
            }
        }
        return clientId;
    }
}
