package com.example.paintlink;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class Login extends Activity {
    private Button button;
    AlertDialog.Builder manualErrorDialog;
    int serverPort = 12345;  // Default server port, unless unsuccessful
    BroadcastThread broadcastThread;
    boolean broadcastSuccessful = false;
    int timeout = 1000;
    byte[] requestBuffer;
    DatagramPacket request;
    DatagramPacket response = new DatagramPacket(new byte[1024], 1024);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Preparing buffer
        requestBuffer = Canvas.getInstance().getPublicKey();

        startBroadcast(new InetSocketAddress("255.255.255.255", serverPort));
    }

    public void completeLogin(SocketAddress serverAddress) {
        Canvas.getInstance().setServerAddress(serverAddress);
        // start Calibration activity
        Intent i = new Intent(Login.this, Calibration.class);
        startActivity(i);
        // finish this
        finish();
    }

    public void startManual() {
        setContentView(R.layout.manual_login);

        if (manualErrorDialog == null)
            manualErrorDialog = new AlertDialog.Builder(Login.this)
                .setTitle(R.string.login_error_title)
                .setPositiveButton(R.string.login_error_broadcast_button, (dialogInterface, i) -> startBroadcast(new InetSocketAddress("255.255.255.255", serverPort)))
                .setNegativeButton(R.string.login_error_retry_button, null);

        button = findViewById(R.id.loginButton);
        EditText ipInput = findViewById(R.id.manualIpInput);
        EditText portInput = findViewById(R.id.manualPortInput);

        button.setOnClickListener(v -> {
            try {
                serverPort = Integer.parseInt(portInput.getText().toString());
                if (ipInput.getText().toString().isEmpty())
                    throw new NetworkOnMainThreadException();
            } catch (NumberFormatException | NetworkOnMainThreadException e) {
                manualErrorDialog.setMessage(String.format(getResources().getText(R.string.login_error_message).toString(), serverPort)).show();
                return;
            }
            startBroadcast(new InetSocketAddress(ipInput.getText().toString(), serverPort));
        });
    }

    public void startBroadcast(InetSocketAddress address) {
        setContentView(R.layout.broadcast_login);

        request = new DatagramPacket(requestBuffer, requestBuffer.length, address);

        broadcastThread = new BroadcastThread();
        broadcastThread.start();

        button = findViewById(R.id.loginButton);
        button.setOnClickListener(v -> {
            if (broadcastSuccessful) {
                completeLogin(response.getSocketAddress());
            } else {
                broadcastThread.interrupt();
                startManual();
            }
        });
    }

    private class BroadcastThread extends Thread {
        public void run() {
            DatagramSocket client;
            try {
                client = new DatagramSocket();
                client.setSoTimeout(timeout);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            while (!Thread.interrupted()) {
                try {
                    client.send(request);
                    Log.d("DHCP", "Sent broadcast discover packet, waiting to get response for " + timeout + "ms...");

                    client.receive(response);
//                response.setPort(1);  // emulates a response
                    // todo: assert response is valid else continue
                    broadcastSuccessful = true;
                    runOnUiThread(this::changeView);
                    Log.d("DHCP", "Success! Received packet from server at " + response.getSocketAddress());
                    break;
                } catch (SocketTimeoutException e) {
                    Log.d("DHCP", "Didn't get response, retrying...");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            client.close();
        }

        private void changeView() {
            ((TextView) findViewById(R.id.loginText)).setText(getResources().getText(R.string.broadcast_login_success));
            findViewById(R.id.loadIcon).setVisibility(View.GONE);
            button.setText(getResources().getText(R.string.login_button_finish));
        }
    }
}
