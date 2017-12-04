package com.github.teocci.codesample.udpavstreamer.utils;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class Config
{
    public static final String LOG_PREFIX = "[AVStreamer]";

    public static final int AUDIO_SAMPLE_RATE = 8000;
//    public static final int SAMPLE_RATE = 44100; // Hertz
    public static final int SAMPLE_INTERVAL = 20; // Milliseconds
    public static final int SAMPLE_SIZE = 2; // Bytes
    public static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
}
