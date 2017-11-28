## Android: UDP Audio/Video Streamer
This is a very simple and straight-forward FFmpeg-based application that records video and audio and stream it using *UDP* protocol.

### Disclaimer

This repository contains a sample code intended to demonstrate the capabilities of the FFmpegFrameFilter, OpenCVFrameConverter, AndroidFrameConverter and FFmpegFrameRecorder. The current version is not intended to be used as-is in applications as a library dependency, and will not be maintained as such. Bug fix contributions are welcome, but issues and feature requests will not be addressed.

### Permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
```

### Use example:

#### Using MPV
```bash
mpv udp://x.x.x.x:8090
```

#### Using ffplay
```bash
ffplay udp://192.168.113:8090
```

### Pre-requisites

- Android SDK 26
- Android Build Tools v26.0.2
- Android Support Repository

### Code Samples

- Video and Audio streamer, Video Recorder and OpenCV Face-recognition.

## Credits

This project was based on a Java interface to OpenCV called **JavaCV**.

- [JavaCV][1]

## License

The code supplied here is covered under the MIT Open Source License..

[1]: https://github.com/bytedeco/javacv