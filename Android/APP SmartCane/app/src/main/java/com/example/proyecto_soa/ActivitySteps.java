package com.example.proyecto_soa;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ActivitySteps extends AppCompatActivity
{
    private TextView txtSteps;
    private Button cmdBack;
    private int steps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_steps);

        txtSteps = (TextView) findViewById(R.id.step_count);
        cmdBack = (Button) findViewById(R.id.button_back);

        cmdBack.setOnClickListener(buttonListeners);
    }

    private View.OnClickListener buttonListeners = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            if (view.getId() == R.id.button_back)
                finish();
            else
                Toast.makeText(getApplicationContext(), "Error en Listener de botones", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
    }

    private BroadcastReceiver stepReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            steps = intent.getIntExtra("pasos", 0);
            txtSteps.setText("Pasos: " + steps);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(stepReceiver, new IntentFilter("PASOS_ACTUALIZADOS"));
        txtSteps.setText("Pasos: " + steps);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(stepReceiver);
    }
}