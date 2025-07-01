package com.example.proyecto_soa;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityPasos extends AppCompatActivity {
    private TextView txt_pasos;
    private Button cmdVolver;
    private int pasos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("DEBUG", "SegundaActivity iniciada");
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_secundaria);

        txt_pasos = (TextView) findViewById(R.id.Cant_pasos);
        cmdVolver = (Button) findViewById(R.id.button_volver);

        cmdVolver.setOnClickListener(botonesListeners);
    }

    private View.OnClickListener botonesListeners = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.button_volver)
                finish();
            else
                Toast.makeText(getApplicationContext(), "Error en Listener de botones", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    private BroadcastReceiver pasoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pasos = intent.getIntExtra("pasos", 0);
            txt_pasos.setText("Pasos: " + pasos);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(pasoReceiver, new IntentFilter("PASOS_ACTUALIZADOS"));
        txt_pasos.setText("Pasos: " + pasos);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(pasoReceiver);
    }
}