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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

public class Canvas {  // TODO: make sure the app doesn't crash when the screen rotates (or disable rotation so that doesn't happen)
    private final static Canvas INSTANCE = new Canvas();

    public static Canvas getInstance() {
        return INSTANCE;
    }

    private int requestsPerSecond = 100;
    private double left, right, top, bottom;
    private boolean canvasCrossingSouth = false;

    private InetSocketAddress serverAddress;
    private final ByteBuffer pointBuffer = ByteBuffer.allocate(500).order(ByteOrder.BIG_ENDIAN); // 72 is max signature length + 20 for info
    private final KeyPair keyPair;
    private final Signature signer;


    private Canvas() {
        try {
            keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
            signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(keyPair.getPrivate());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getPublicKey() {
        return keyPair.getPublic().getEncoded();
    }

    private CommunicationThread activeCommunicationThread;

    public void startCommunication() {
        if (activeCommunicationThread != null) activeCommunicationThread.interrupt();
        activeCommunicationThread = new CommunicationThread();
        activeCommunicationThread.start();
    }

    public void endCommunication() {
        activeCommunicationThread.interrupt();
        activeCommunicationThread = null;
    }


    public void setRequestsPerSecond(int newRequestsPerSecond) {
        requestsPerSecond = newRequestsPerSecond;
    }

    public void setServerAddress(SocketAddress address) {
        serverAddress = (InetSocketAddress) address;
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


    public void setPointBuffer(float[] rotatedVector, int color, boolean pressed) {
        if (canvasCrossingSouth)
            pointBuffer.putFloat(0, norm(left, right, (float) (rotatedVector[0] + ((rotatedVector[0] < 0) ? Math.PI : -Math.PI))));
        else pointBuffer.putFloat(0, norm(left, right, rotatedVector[0]));
        pointBuffer.putFloat(4, norm(top, bottom, rotatedVector[1]));
        pointBuffer.putInt(8, color);
        pointBuffer.putInt(12, pressed ? 1 : 0);

        byte[] signature = generateSignature(Arrays.copyOfRange(pointBuffer.array(), 0, 16));
        pointBuffer.putInt(16, signature.length);
        for (int i = 0; i < signature.length; i++)
            pointBuffer.put(i + 20, signature[i]);
//        pointBuffer.put(signature);  // doesn't work!
    }

    private byte[] generateSignature(byte[] message) {
        try {
            signer.update(message);
            return signer.sign();
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private float norm(double minimum, double maximum, float value) {
        // Norm the value between 0 and 1 relative to range[min, max]
        // values over the edge will be snapped to it
        return (float) min(1, max(0, value - minimum) / (maximum - minimum));
    }

    private class CommunicationThread extends Thread {
        private DatagramChannel client;

        public void run() {
            try {
                client = DatagramChannel.open();
                client.bind(null);
            } catch (IOException e) {
                Log.e("Communication Error", "Failed to open a client channel!");
                Thread.currentThread().interrupt();
            }
            while (!Thread.interrupted()) {
                try {
                    if (requestsPerSecond != 0) Thread.sleep(1000 / requestsPerSecond);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sendRequest();
            }
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void sendRequest() {
            try {
                pointBuffer.rewind();
                client.send(pointBuffer, serverAddress);
            } catch (IOException e) {
                Log.e("Communication Error", "Failed to send request, oh well");
            }
        }
    }
}
