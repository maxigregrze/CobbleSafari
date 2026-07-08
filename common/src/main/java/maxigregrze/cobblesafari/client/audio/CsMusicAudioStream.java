package maxigregrze.cobblesafari.client.audio;

import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * OGG Vorbis decoded in memory and seekable to an arbitrary sample via {@code STBVorbis}
 * ({@code stb_vorbis_seek}). Both mono and stereo are supported natively — the OpenAL format
 * follows the file's real channel count, no downmix. <b>Not thread-safe</b>: a single reader
 * thread (the owning {@link CsMusicVoice} pump) may call {@link #readPcm}/{@link #seekMs}.
 *
 * <p>Only the compressed OGG is kept resident ({@link #encoded}); PCM is produced incrementally,
 * so stereo does not double memory.</p>
 */
final class CsMusicAudioStream implements AutoCloseable {

    private ByteBuffer encoded;   // native copy of the OGG bytes (memAlloc — must be freed)
    private long handle;          // stb_vorbis handle
    final int format;             // AL_FORMAT_MONO16 / AL_FORMAT_STEREO16
    final int sampleRate;
    final int channels;
    final long durationMs;        // -1 if unknown

    CsMusicAudioStream(byte[] oggBytes) {
        ByteBuffer buf = MemoryUtil.memAlloc(oggBytes.length);
        buf.put(oggBytes);
        buf.flip();
        this.encoded = buf;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer err = stack.mallocInt(1);
            long h = STBVorbis.stb_vorbis_open_memory(buf, err, null);
            if (h == 0L) {
                throw new IllegalStateException("stb_vorbis_open_memory failed (error " + err.get(0) + ")");
            }
            this.handle = h;
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(h, info);
            this.channels = info.channels();
            this.sampleRate = info.sample_rate();
            int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(h);
            this.durationMs = sampleRate > 0 ? (long) totalSamples * 1000L / sampleRate : -1L;
            // Mono ET stéréo : le format suit le nb de canaux réel du fichier, aucun downmix.
            this.format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
        } catch (RuntimeException e) {
            MemoryUtil.memFree(buf);
            this.encoded = null;
            throw e;
        }
    }

    /** Fills {@code dst} with interleaved 16-bit PCM. Returns bytes written (0 = end of stream). */
    int readPcm(ByteBuffer dst) {
        if (handle == 0L) {
            return 0;
        }
        int frameBytes = channels * 2;
        if (dst.remaining() < frameBytes) {
            return 0;
        }
        ShortBuffer shorts = dst.asShortBuffer();
        int frames = STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, shorts);
        if (frames <= 0) {
            return 0;
        }
        int bytes = frames * frameBytes;
        dst.position(dst.position() + bytes);
        return bytes;
    }

    void seekMs(long ms) {
        if (handle == 0L) {
            return;
        }
        long sample = ms * sampleRate / 1000L;
        STBVorbis.stb_vorbis_seek(handle, (int) Math.max(0L, sample));
    }

    @Override
    public void close() {
        if (handle != 0L) {
            STBVorbis.stb_vorbis_close(handle);
            handle = 0L;
        }
        if (encoded != null) {
            MemoryUtil.memFree(encoded);
            encoded = null;
        }
    }
}
