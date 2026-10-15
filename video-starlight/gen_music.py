# -*- coding: utf-8 -*-
"""Synthesize ambient BGM: ethereal pad + tunnel whoosh + ending bell. -> music.wav"""
import numpy as np
from scipy.io import wavfile  # fallback below if scipy missing

SR = 44100
T = 60.0
N = int(SR * T)
t = np.arange(N) / SR
rng = np.random.default_rng(7)

def chord(freqs, t0, t1, amp=1.0):
    out = np.zeros(N)
    i0, i1 = int(t0 * SR), min(int(t1 * SR), N)
    tt = np.arange(i1 - i0) / SR
    dur = t1 - t0
    env = np.minimum(1, tt / 4.0) * np.minimum(1, np.maximum(0, (dur - tt) / 4.0))
    for fq in freqs:
        out[i0:i1] += (np.sin(2 * np.pi * fq * tt)
                       + 0.3 * np.sin(2 * np.pi * fq * 2.01 * tt)
                       + 0.5 * np.sin(2 * np.pi * (fq * 1.003) * tt)) * env * amp
    return out

A2, C3, E3, A3 = 110.0, 130.81, 164.81, 220.0
F2, A2b, C4, F3 = 87.31, 110.0, 261.63, 174.61
C3b, E3b, G3, C4b = 130.81, 164.81, 196.0, 261.63
G2, B2, D3, G3b = 98.0, 123.47, 146.83, 196.0

music = np.zeros(N)
music += chord([A2, E3, A3, C4], 0, 15)          # Am
music += chord([F2, A2b, C4, F3], 15, 30)       # F
music += chord([C3b, G3, C4b, E3 * 2], 30, 45)  # C
music += chord([A2, E3, A3, C4], 45, 58, 0.9)   # Am resolve
music *= 0.028

# low drone
music += 0.05 * np.sin(2 * np.pi * 55 * t) * np.minimum(1, np.maximum(0, (57 - t) / 4))

# airy noise bed
noise = rng.normal(0, 1, N)
k = np.ones(400) / 400
soft_noise = np.convolve(noise, k, mode="same")
music += 0.006 * soft_noise

# tunnel whoosh 0-9s (filtered noise swell)
whoosh_env = np.clip(t / 7.0, 0, 1) * np.clip((9.5 - t) / 1.5, 0, 1)
whoosh_env[t > 9.5] = 0
music += 0.10 * soft_noise * whoosh_env

# distant muffled applause-like texture 28-42s (sparse noise grains)
ap_env = np.clip((t - 28) / 3, 0, 1) * np.clip((42 - t) / 3, 0, 1)
grain = np.convolve(rng.normal(0, 1, N), np.ones(30) / 30, mode="same")
music += 0.008 * grain * ap_env

# bell at 55s
i0 = int(55.0 * SR)
tb = np.arange(N - i0) / SR
bell = (np.sin(2 * np.pi * 523.25 * tb) + 0.5 * np.sin(2 * np.pi * 784.0 * tb)
        + 0.35 * np.sin(2 * np.pi * 1046.5 * tb)) * np.exp(-tb / 2.4)
music[i0:] += 0.16 * bell

# master fade out
fade = np.minimum(1, np.maximum(0, (T - t) / 3.5))
music *= fade * 0.9

# gentle limiter
music = np.tanh(music * 1.1) * 0.85
stereo = np.stack([music, music], axis=1)

try:
    from scipy.io import wavfile
    wavfile.write("music.wav", SR, (stereo * 32767).astype(np.int16))
except ImportError:
    import struct, wave
    w = wave.open("music.wav", "wb")
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((stereo * 32767).astype("<i2").tobytes())
    w.close()
print("music.wav done")
