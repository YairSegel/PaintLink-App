package com.example.paintlink;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.madrapps.pikolo.ColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;

public class SensorPaint extends Activity {
    private SensorManager sensorManager;
    private Canvas canvasHandler;
    private ImageButton paintButton;
    private int color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_paint);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        canvasHandler = Canvas.getInstance();
        color = getColor(R.color.default_color_picker);

        paintButton = findViewById(R.id.paintButton);

        AlertDialog.Builder recalibrationDialog = new AlertDialog.Builder(SensorPaint.this)
                .setTitle(R.string.recalibrate_dialog_title)
                .setPositiveButton(R.string.recalibrate_dialog_relog_button, (dialogInterface, i) -> {
                    Log.d("Activity Change", "SensorPaint -> Login [Logging]");
                    startActivity(new Intent(SensorPaint.this, Login.class));
                    finish();
                })
                .setNegativeButton(R.string.recalibrate_dialog_recalibrate_button, (dialogInterface, i) -> {
                    Log.d("Activity Change", "SensorPaint -> Calibration [Recalibrating]");
                    startActivity(new Intent(SensorPaint.this, Calibration.class));
                    finish();
                });

        findViewById(R.id.recalibrateButton).setOnClickListener(v -> recalibrationDialog.show());

        String[] requestLimitingStringOptions = getResources().getStringArray(R.array.request_limiting_options);
        Spinner requestLimitingSpinner = findViewById(R.id.requestLimitingSpinner);
        requestLimitingSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, requestLimitingStringOptions));
        requestLimitingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            String item;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                item = parent.getSelectedItem().toString();
                item = item.substring(0, item.indexOf(" "));
                canvasHandler.setRequestsPerSecond((item.equals("Unlimited")) ? 1000 : Integer.parseInt(item));
                Log.d("Set Request Limit", parent.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final ColorPicker colorPicker = findViewById(R.id.colorPicker);
        colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override
            public void onColorSelected(int newColor) {
                color = newColor;
                paintButton.getBackground().setColorFilter(newColor, PorterDuff.Mode.MULTIPLY);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        canvasHandler.startCommunication();
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        canvasHandler.endCommunication();
        sensorManager.unregisterListener(sensorEventListener);
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        private final float[] rotationMatrix = new float[9], rotatedVector = new float[3];

        @Override
        public void onSensorChanged(SensorEvent event) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, rotatedVector);
            canvasHandler.setPointBuffer(rotatedVector, color, paintButton.isPressed());
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
}
