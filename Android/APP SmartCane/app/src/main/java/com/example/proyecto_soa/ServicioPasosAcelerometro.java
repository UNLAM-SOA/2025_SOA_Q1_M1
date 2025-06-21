package com.example.proyecto_soa;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class ServicioPasosAcelerometro extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int stepCount = 0;
    private static final float THRESHOLD = 10.0f;
    private static final int TIME_BETWEEN_STEPS_MS = 300;
    private long lastStepTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e("ServicioPasos", "Acelerómetro no disponible");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
                long now = System.currentTimeMillis();

                if (magnitude > THRESHOLD && (now - lastStepTime > TIME_BETWEEN_STEPS_MS)) {
                    stepCount++;
                    lastStepTime = now;

                    Intent i = new Intent("PASOS_ACTUALIZADOS");
                    i.putExtra("pasos", stepCount);
                    sendBroadcast(i);
                }
            }
        }).start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}