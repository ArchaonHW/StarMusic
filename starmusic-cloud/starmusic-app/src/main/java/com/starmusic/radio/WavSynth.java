package com.starmusic.radio;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 依頻道 id 產生可循環播放的柔和琶音 WAV（22050Hz / 16-bit / mono）。
 * 雛形階段的佔位串流，讓前端播放器有真實音訊可播。
 */
final class WavSynth {

    private static final int SAMPLE_RATE = 22050;
    private static final int SECONDS = 24;
    private static final double VOLUME = 0.22;

    // 五聲音階（A 小調）：各頻道取不同子集與速度
    private static final double[] SCALE = {220.0, 261.63, 293.66, 329.63, 392.0, 440.0, 523.25};

    private WavSynth() {
    }

    static byte[] render(long channelId) {
        int total = SAMPLE_RATE * SECONDS;
        double[] pcm = new double[total];

        int noteLen = SAMPLE_RATE * 3; // 每音 3 秒
        List<Double> notes = List.of(
                SCALE[(int) (channelId % SCALE.length)],
                SCALE[(int) ((channelId + 2) % SCALE.length)],
                SCALE[(int) ((channelId + 4) % SCALE.length)],
                SCALE[(int) ((channelId + 1) % SCALE.length)]);

        for (int i = 0; i < total; i++) {
            double t = (double) i / SAMPLE_RATE;
            int noteIdx = (i / noteLen) % notes.size();
            double freq = notes.get(noteIdx);
            double pos = i % noteLen;
            // 音頭/音尾淡化，避免爆音
            double env = Math.min(1.0, Math.min(pos, noteLen - pos) / (SAMPLE_RATE * 0.4));
            double s = Math.sin(2 * Math.PI * freq * t)
                    + 0.35 * Math.sin(2 * Math.PI * freq * 2 * t)
                    + 0.15 * Math.sin(2 * Math.PI * freq * 3 * t);
            // 緩慢顫音讓音色不呆板
            s *= 0.8 + 0.2 * Math.sin(2 * Math.PI * 0.5 * t);
            pcm[i] = s * env * VOLUME;
        }

        // 頭尾各 0.5 秒淡化，讓 loop 接縫平順
        int fade = SAMPLE_RATE / 2;
        for (int i = 0; i < fade; i++) {
            double g = (double) i / fade;
            pcm[i] *= g;
            pcm[total - 1 - i] *= g;
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(44 + total * 2);
            DataOutputStream out = new DataOutputStream(bos);
            int dataSize = total * 2;
            out.writeBytes("RIFF");
            writeIntLE(out, 36 + dataSize);
            out.writeBytes("WAVE");
            out.writeBytes("fmt ");
            writeIntLE(out, 16);
            writeShortLE(out, 1); // PCM
            writeShortLE(out, 1); // mono
            writeIntLE(out, SAMPLE_RATE);
            writeIntLE(out, SAMPLE_RATE * 2);
            writeShortLE(out, 2); // block align
            writeShortLE(out, 16); // bits
            out.writeBytes("data");
            writeIntLE(out, dataSize);
            for (double v : pcm) {
                writeShortLE(out, (int) Math.max(-32768, Math.min(32767, v * 32767)));
            }
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeIntLE(DataOutputStream out, int v) throws IOException {
        out.writeByte(v & 0xFF);
        out.writeByte((v >> 8) & 0xFF);
        out.writeByte((v >> 16) & 0xFF);
        out.writeByte((v >> 24) & 0xFF);
    }

    private static void writeShortLE(DataOutputStream out, int v) throws IOException {
        out.writeByte(v & 0xFF);
        out.writeByte((v >> 8) & 0xFF);
    }
}
