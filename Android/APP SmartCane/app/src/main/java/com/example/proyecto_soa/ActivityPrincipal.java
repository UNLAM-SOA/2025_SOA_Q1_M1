package com.example.proyecto_soa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityPrincipal extends AppCompatActivity {
    private Button cmdBaston;
    private Button cmdFamiliar;
    public Intent intent_next_activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        configMQTT.useServerEMQX();

        cmdBaston = (Button) findViewById(R.id.id_baston);
        cmdFamiliar = (Button) findViewById(R.id.id_familiar);

        cmdBaston.setOnClickListener(botonesListeners);
        cmdFamiliar.setOnClickListener(botonesListeners);
    }

    private View.OnClickListener botonesListeners = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.id_baston) {
                intent_next_activity = new Intent(ActivityPrincipal.this, ActivityBaston.class);
                intent_next_activity.putExtra("ROL", "BASTON");
                startActivity(intent_next_activity);
            } else if (view.getId() == R.id.id_familiar) {
                intent_next_activity = new Intent(ActivityPrincipal.this, ActivityFamiliar.class);
                intent_next_activity.putExtra("ROL", "FAMILIAR");
                startActivity(intent_next_activity);
            } else
                Toast.makeText(getApplicationContext(), "Error en Listener de botones", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = new Intent(this, ServicioPasosAcelerometro.class);
        stopService(intent);

        Intent intent2 = new Intent(this, MqttHandlerServiceFamiliar.class);
        stopService(intent2);

        Intent intent3 = new Intent(this, MqttHandlerServiceBaston.class);
        stopService(intent3);
    }
}
