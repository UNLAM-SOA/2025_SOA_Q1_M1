package com.example.proyecto_soa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityMain extends AppCompatActivity
{
    private Button cmdCane;
    private Button cmdFamily;
    public Intent intentNextActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        configMQTT.useServerEMQX();

        cmdCane = (Button) findViewById(R.id.id_cane);
        cmdFamily = (Button) findViewById(R.id.id_family);

        cmdCane.setOnClickListener(buttonListeners);
        cmdFamily.setOnClickListener(buttonListeners);
    }

    private View.OnClickListener buttonListeners = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            if (view.getId() == R.id.id_cane)
            {
                intentNextActivity = new Intent(ActivityMain.this, ActivityCane.class);
                intentNextActivity.putExtra("ROL", "BASTON");
                startActivity(intentNextActivity);
            } else if (view.getId() == R.id.id_family)
            {
                intentNextActivity = new Intent(ActivityMain.this, ActivityFamily.class);
                intentNextActivity.putExtra("ROL", "FAMILIAR");
                startActivity(intentNextActivity);
            } else
            {
                Toast.makeText(getApplicationContext(), "Error en Listener de botones", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        Intent intent = new Intent(this, StepsWithAccelerometerService.class);
        stopService(intent);

        Intent intent2 = new Intent(this, MqttHandlerServiceFamily.class);
        stopService(intent2);

        Intent intent3 = new Intent(this, MqttHandlerServiceCane.class);
        stopService(intent3);
    }
}
