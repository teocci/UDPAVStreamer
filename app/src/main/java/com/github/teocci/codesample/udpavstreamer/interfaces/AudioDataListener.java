package com.github.teocci.codesample.udpavstreamer.interfaces;

import java.nio.ShortBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-01
 */

public interface AudioDataListener
{
    void onSampleReady(ShortBuffer audioData);
}
