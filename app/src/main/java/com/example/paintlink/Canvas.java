package com.example.paintlink;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

public class Canvas {
    private final static Canvas INSTANCE = new Canvas();

    // Private constructor suppresses generation of a (public) default constructor
    public static Canvas getInstance() {
        return INSTANCE;
    }

    private int requestsPerSecond = 100;
    private double left, right, top, bottom;
    private boolean canvasCrossingSouth = false;
    private boolean changed = true;  // todo

    public boolean active = false;
    private DatagramChannel client;
    private InetSocketAddress serverAddress;
    private final ByteBuffer pointBuffer = ByteBuffer.allocate(16);


    private Canvas() {
        pointBuffer.order(ByteOrder.BIG_ENDIAN);
        try {
            client = DatagramChannel.open();
            client.bind(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Thread UDPThread = new Thread(() -> {
        while (!Thread.interrupted()) {
            try {
                if (requestsPerSecond != 0) Thread.sleep(1000 / requestsPerSecond);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!(active && changed)) continue;
            sendRequest();
        }
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    });

    public void startCommunication() {
//        if (serverAddress == null) return;
        if (!UDPThread.isAlive()) UDPThread.start();
        active = true;
    }

    public void pauseCommunication() {
        active = false;
    }

    public void endCommunication() {
        UDPThread.interrupt();
    }

    public void setEdges(float left, float right, float top, float bottom) {
        if (left > right) {
            canvasCrossingSouth = true;
            this.left = left - Math.PI;
            this.right = right + Math.PI;
        } else {
            this.left = left;
            this.right = right;
        }
        this.top = top;
        this.bottom = bottom;
        Log.d("Canvas", "Edges were set to (LR-TB): " + left + ", " + right + ", " + top + ", " + bottom);
    }

    public void setServerAddress(String ip, int port) {
        serverAddress = new InetSocketAddress(ip, port);
    }

    public void setServerAddress(SocketAddress address) {
        serverAddress = (InetSocketAddress) address;
    }

    private void sendRequest() {
        try {
            pointBuffer.rewind();
            client.send(pointBuffer, serverAddress);
        } catch (IOException e) {
            Log.d("com error", "Failed to send request, oh well");
        }
    }

    public void setPointBuffer(float[] rotatedVector, int color, boolean pressed) {
        if (canvasCrossingSouth)
            pointBuffer.putFloat(0, norm(left, right, (float) (rotatedVector[0] + ((rotatedVector[0] < 0) ? Math.PI : -Math.PI))));
        else pointBuffer.putFloat(0, norm(left, right, rotatedVector[0]));
        pointBuffer.putFloat(4, norm(top, bottom, rotatedVector[1]));
        pointBuffer.putInt(8, color);
        pointBuffer.putInt(12, pressed ? 1 : 0);
//        Log.d("Future UDP Request", pressed + " of " + color + " in relative point ???");
    }

    public void setRequestsPerSecond(int newRequestsPerSecond) {
        requestsPerSecond = newRequestsPerSecond;
    }

    private float norm(double minimum, double maximum, float value) {
        // Norm the value between 0 and 1 relative to range[min, max]
        // values over the edge will be snapped to it
        return (float) min(1, max(0, value - minimum) / (maximum - minimum));
    }
}