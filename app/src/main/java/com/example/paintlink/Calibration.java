package com.example.paintlink;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class Calibration extends Activity {
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;

    private final float[] lrtb = {0, 0, 0, 0}, lrtb_accuracy = {0, 0, 0, 0};
    private int current = -1;
    private final int minimum_accuracy = 100;

    private TextView text;
    private Button button;
    private boolean inProcess = false;
    private String[] possibleTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        text = findViewById(R.id.calibrationText);
        possibleTexts = getResources().getStringArray(R.array.calibration_sides);

        button = findViewById(R.id.calibrationButton);
        button.setOnClickListener(v -> {
            inProcess = true;
            button.setText("In process, continue");
        });
        next_edge();
    }

    private void move_on() {
        // Processing lrtb
        for (int i = 0; i < lrtb.length; i++) lrtb[i] /= lrtb_accuracy[i];
        Canvas.getInstance().setEdges(lrtb[0], lrtb[1], lrtb[2], lrtb[3]);
        // start Paint activity
        Intent i = new Intent(Calibration.this, SensorPaint.class);
//        source activity:
//        i.putExtra("Canvas Position", lrtb);
//        recipient activity:
//        float[] lrtb = getIntent().getFloatArrayExtra("Canvas Position");

        startActivity(i);
        // finish this
        finish();
    }

    private void next_edge() {
        inProcess = false;
        current++;
        if (current > 3) {
            move_on();
            return;
        }
        text.setText(possibleTexts[current]);
        button.setText("Start");
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        private final float[] rotationMatrix = new float[9], rotatedVector = new float[3];

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!inProcess) return;
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, rotatedVector);
            lrtb[current] += rotatedVector[current / 2];
            lrtb_accuracy[current]++;
            if (lrtb_accuracy[current] >= minimum_accuracy) next_edge();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d("Accuracy", "CHANGE!!! " + accuracy);
        }
    };
}
