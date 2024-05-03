package com.example.paintlink.extras;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

public class EncryptionTest extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Generate key pair and signer
        KeyPair pair;
        Signature signer;
        try {
//            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
//            gen.initialize(64);
//            pair = gen.generateKeyPair();
            pair = KeyPairGenerator.getInstance("EC").generateKeyPair();
            //                                        /\
            // keyGen.initialize(new ECGenParameterSpec("secp256r1"));
            // keyGen.initialize(70);  // signature length
            signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(pair.getPrivate());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        // Message to be signed
        byte[] message = "Hello, Python server!".getBytes();

        // Sign the message
        byte[] signatureBytes;
        try {
            signer.update(message);
            signatureBytes = signer.sign();
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }

        // Convert public key to byte array
        byte[] publicKeyBytes = pair.getPublic().getEncoded();
        Log.d("bananananana", byteArrayToHex(publicKeyBytes));
        Log.d("bananananana", byteArrayToHex(signatureBytes));
    }


    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
