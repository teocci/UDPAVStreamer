package com.github.teocci.codesample.udpavstreamer.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Base64;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class provides a variety of basic utility methods that are not
 * dependent on any other classes within the org.jamwiki package structure.
 */
public class Utilities
{
    private static final String TAG = LogHelper.makeLogTag(Utilities.class);

    private static Pattern VALID_IPV4_PATTERN = null;
    private static Pattern VALID_IPV6_PATTERN = null;
    private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}" +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

    static {
        try {
            VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
            VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            //logger.severe("Unable to compile pattern", e);
        }
    }

    /**
     * Determine if the given string is a valid IPv4 or IPv6 address.  This method
     * uses pattern matching to see if the given string could be a valid IP address.
     *
     * @param ipAddress A string that is to be examined to verify whether or not
     *                  it could be a valid IP address.
     * @return <code>true</code> if the string is a value that is a valid IP address,
     * <code>false</code> otherwise.
     */
    public static boolean isIpAddress(String ipAddress)
    {
        return isIpv4Address(ipAddress) || isIpv6Address(ipAddress);
    }

    public static boolean isIpv4Address(String ipAddress)
    {
        Matcher matcher = Utilities.VALID_IPV4_PATTERN.matcher(ipAddress);
        return matcher.matches();
    }

    public static boolean isIpv4Address(InetAddress inetAddress)
    {
        return isIpv4Address(inetAddress.getHostAddress());
    }

    public static boolean isIpv6Address(String ipAddress)
    {
        Matcher matcher = Utilities.VALID_IPV6_PATTERN.matcher(ipAddress);
        return matcher.matches();
    }

    public static String getDeviceID(ContentResolver contentResolver)
    {
        long deviceID = 0;
        final String str = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
        if (str != null) {
            try {
                final BigInteger bi = new BigInteger(str, 16);
                deviceID = bi.longValue();
            } catch (final NumberFormatException ex) {
                /* Nothing critical */
                LogHelper.i(TAG, ex.toString());
            }
        }

        if (deviceID == 0) {
            /* Let's use random number */
            deviceID = new Random().nextLong();
        }

        final byte[] bb = new byte[Long.SIZE / Byte.SIZE];
        for (int index = (bb.length - 1); index >= 0; index--) {
            bb[index] = (byte) (deviceID & 0xFF);
            deviceID >>= Byte.SIZE;
        }

        return Base64.encodeToString(bb, (Base64.NO_WRAP));
    }

    /**
     * Returns the IP address of the first configured interface of the device
     *
     * @param removeIPv6 If true, IPv6 addresses are ignored
     * @return the IP address of the first configured interface or null
     */
    public static String getLocalIpAddress(boolean removeIPv6)
    {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
//                LogHelper.e(TAG, "interface: " + intf.getDisplayName());
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
//                        LogHelper.e(TAG, inetAddress.getHostAddress().toString());
                        if (inetAddress.isSiteLocalAddress() && !inetAddress.isLoopbackAddress() &&
                                !inetAddress.isAnyLocalAddress() &&
                                (!removeIPv6 || isIpv4Address(inetAddress))) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ignore) {}

        return null;
    }
}