package com.example.paintlink.extras;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.paintlink.Calibration;
import com.example.paintlink.Canvas;
import com.example.paintlink.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class BroadcastLogin extends Activity {
    private Button button;
    boolean successful = false;

    int timeout = 1000;
    byte[] bytes = new byte[1024];
    DatagramPacket request;
    DatagramPacket response = new DatagramPacket(bytes, 1024);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.broadcast_login);

        button = findViewById(R.id.loginButton);
        button.setOnClickListener(v -> {
            if (successful) {
                Canvas.getInstance().setServerAddress(response.getSocketAddress());
                // start Calibration activity
                Intent i = new Intent(BroadcastLogin.this, Calibration.class);
                startActivity(i);
                // finish this
                finish();
            } else {
                DHCPThread.interrupt();
                // start ManualLogin activity
                Intent i = new Intent(BroadcastLogin.this, ManualLogin.class);
                startActivity(i);
                // finish this
                finish();
            }
        });

        // todo: prep request properly
        ByteBuffer.wrap(bytes).put("Where are you?".getBytes());
        request = new DatagramPacket(bytes, 1024, new InetSocketAddress("255.255.255.255", 12345));

        DHCPThread.start();
    }

    Thread DHCPThread = new Thread(() -> {
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
                runOnUiThread(this::announceSuccess);
                Log.d("DHCP", "Success! Received packet from server at " + response.getSocketAddress());
                break;
            } catch (SocketTimeoutException e) {
                Log.d("DHCP", "Didn't get response, retrying...");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        client.close();
    });

    private void announceSuccess() {
        successful = true;
        ((TextView) findViewById(R.id.loginText)).setText(getResources().getText(R.string.broadcast_login_success));
        findViewById(R.id.loadIcon).setVisibility(View.GONE);
        button.setText(getResources().getText(R.string.login_button_finish));
    }
}
