package com.redcodersgroup.bubbleshooter.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SoundEffectGenerator {
    public static final int SAMPLE_RATE = 22050;

    public static byte[] generatePop(float pitchFactor) {
        int durationMs = 80;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        float startFreq = 750f * pitchFactor;
        float endFreq = 220f * pitchFactor;

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / numSamples;
            float freq = startFreq + (endFreq - startFreq) * t;
            float envelope = (1.0f - t) * (1.0f - t); // quadratic decay

            double angle = 2.0 * Math.PI * freq * (i / (double) SAMPLE_RATE);
            short sample = (short) (Math.sin(angle) * 28000 * envelope);

            buffer[2 * i] = (byte) (sample & 0xFF);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }

    public static byte[] generateShoot() {
        int durationMs = 90;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        float startFreq = 260f;
        float endFreq = 620f;

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / numSamples;
            float freq = startFreq + (endFreq - startFreq) * (float) Math.sqrt(t);
            float envelope = (float) Math.sin(t * Math.PI);

            double angle = 2.0 * Math.PI * freq * (i / (double) SAMPLE_RATE);
            short sample = (short) (Math.sin(angle) * 22000 * envelope);

            buffer[2 * i] = (byte) (sample & 0xFF);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }

    public static byte[] generateBounce() {
        int durationMs = 40;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / numSamples;
            float envelope = (1.0f - t);
            double angle = 2.0 * Math.PI * 900.0 * (i / (double) SAMPLE_RATE);
            short sample = (short) (Math.sin(angle) * 18000 * envelope);

            buffer[2 * i] = (byte) (sample & 0xFF);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }

    public static byte[] generateBomb() {
        int durationMs = 350;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / numSamples;
            float envelope = (1.0f - t);
            double noise = (Math.random() - 0.5) * 2.0;
            double lowFreq = Math.sin(2.0 * Math.PI * 65.0 * (i / (double) SAMPLE_RATE));
            short sample = (short) ((noise * 0.6 + lowFreq * 0.4) * 30000 * envelope);

            buffer[2 * i] = (byte) (sample & 0xFF);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }

    public static byte[] generateWin() {
        int durationMs = 500;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        // C5, E5, G5, C6 arpeggio
        float[] notes = {523.25f, 659.25f, 783.99f, 1046.50f};
        int segmentSamples = numSamples / notes.length;

        for (int i = 0; i < numSamples; i++) {
            int noteIndex = Math.min(notes.length - 1, i / segmentSamples);
            float freq = notes[noteIndex];
            int noteSample = i % segmentSamples;
            float noteT = (float) noteSample / segmentSamples;
            float envelope = (1.0f - noteT * 0.7f);

            double angle = 2.0 * Math.PI * freq * (i / (double) SAMPLE_RATE);
            short sample = (short) (Math.sin(angle) * 24000 * envelope);

            buffer[2 * i] = (byte) (sample & 0xFF);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }

    public static byte[] generatePurchaseSuccess() {
        int durationMs = 460;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        // Metallic coin clicks + ascending bright chime arpeggio (G5, B5, D6, G6, B6)
        float[] chimeNotes = {783.99f, 987.77f, 1174.66f, 1567.98f, 1975.53f};
        int[] noteStartTimesMs = {50, 110, 170, 230, 290};

        for (int i = 0; i < numSamples; i++) {
            float timeSec = (float) i / SAMPLE_RATE;
            int timeMs = (int) (timeSec * 1000f);
            double sampleValue = 0.0;

            // 1. Initial crisp metallic coin clink at t=0ms and t=45ms
            if (timeMs < 60) {
                float tCoin1 = timeMs / 60.0f;
                float envCoin1 = (1.0f - tCoin1) * (1.0f - tCoin1);
                sampleValue += (Math.sin(2.0 * Math.PI * 2200.0 * timeSec) * 0.6
                        + Math.sin(2.0 * Math.PI * 4400.0 * timeSec) * 0.4) * 14000 * envCoin1;
            }
            if (timeMs >= 40 && timeMs < 110) {
                float tCoin2 = (timeMs - 40) / 70.0f;
                float envCoin2 = (1.0f - tCoin2) * (1.0f - tCoin2);
                sampleValue += (Math.sin(2.0 * Math.PI * 2800.0 * timeSec) * 0.6
                        + Math.sin(2.0 * Math.PI * 5600.0 * timeSec) * 0.4) * 15000 * envCoin2;
            }

            // 2. Ascending bell/chime notes with sparkling harmonics
            for (int n = 0; n < chimeNotes.length; n++) {
                int startMs = noteStartTimesMs[n];
                if (timeMs >= startMs) {
                    float noteElapsed = (timeMs - startMs) / 1000.0f;
                    float noteDur = 0.22f;
                    if (noteElapsed < noteDur) {
                        float noteEnv = (float) Math.exp(-noteElapsed * 14.0f);
                        double freq = chimeNotes[n];
                        // Fundamental + sparkle octave harmonic
                        sampleValue += (Math.sin(2.0 * Math.PI * freq * timeSec) * 0.7
                                + Math.sin(2.0 * Math.PI * freq * 2.0 * timeSec) * 0.3) * 18000 * noteEnv;
                    }
                }
            }

            // Clamp sample to 16-bit PCM range
            short sample = (short) Math.max(-32767, Math.min(32767, sampleValue));
            buffer[2 * i] = (byte) (sample & 0xFF);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }

    public static byte[] generateClick() {
        int durationMs = 20;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / numSamples;
            float envelope = (1.0f - t);
            double angle = 2.0 * Math.PI * 1200.0 * (i / (double) SAMPLE_RATE);
            short sample = (short) (Math.sin(angle) * 16000 * envelope);

            buffer[2 * i] = (byte) (sample & 0xFF);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }

    public static byte[] createWavHeader(int pcmDataLength) {
        int totalDataLen = pcmDataLength + 36;
        int byteRate = SAMPLE_RATE * 2; // 16-bit mono

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // Subchunk1Size
        header[20] = 1; header[21] = 0; // AudioFormat = PCM
        header[22] = 1; header[23] = 0; // NumChannels = 1
        header[24] = (byte) (SAMPLE_RATE & 0xff);
        header[25] = (byte) ((SAMPLE_RATE >> 8) & 0xff);
        header[26] = (byte) ((SAMPLE_RATE >> 16) & 0xff);
        header[27] = (byte) ((SAMPLE_RATE >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = 2; header[33] = 0; // BlockAlign = 2 bytes
        header[34] = 16; header[35] = 0; // BitsPerSample = 16
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (pcmDataLength & 0xff);
        header[41] = (byte) ((pcmDataLength >> 8) & 0xff);
        header[42] = (byte) ((pcmDataLength >> 16) & 0xff);
        header[43] = (byte) ((pcmDataLength >> 24) & 0xff);

        return header;
    }
}
