package com.forestwave.pdc8g1.forestwave.service;

import com.forestwave.pdc8g1.forestwave.receivers.AlarmReceiver;
import com.google.android.vending.expansion.downloader.impl.DownloaderService;

/**
 * Created by Sylvain on 29/01/15.
 */
public class MyDownloadService extends DownloaderService {
    // You must use the public key belonging to your publisher account
    public static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAo7kS3+rXCYjWCI7VeZ7HOUa6eiYg4+1090Q3psBM71dIwm/vSKk0TEs9CSx63QVOpCT1ok5LdsrKCDKlwL7YBCkKH2nVYIt9ubzhaQmaruuHBZ6EWgCWVQ97NGFdzs9IJakCC2FXcze696zxhW4IgvQvSHwguELaWCOX4cPwFb8fvAMBFANyuCNnBcroDNH+hq6D6Uswj22TmiZm09q774wsHYBx2nqjKxDPrTVf+wvkuQdJf/9AewCyz+51N+iwzgbpnOvbWoHCe2P2DTVX7UuwuFPiS5sSiBKffD4PQAQkz8+xljittF+uwSJNghbGGnoch+gpGE2LdS3U3JMzlwIDAQAB";
    // You should also modify this salt
    public static final byte[] SALT = new byte[] { 1, 42, -12, -1, 54, 98,
            -100, -12, 43, 2, -8, -4, 9, 5, -106, -107, -33, 45, -1, 84
    };

    @Override
    public String getPublicKey() {
        return BASE64_PUBLIC_KEY;
    }

    @Override
    public byte[] getSALT() {
        return SALT;
    }

    @Override
    public String getAlarmReceiverClassName() {
        return AlarmReceiver.class.getName();
    }
}
