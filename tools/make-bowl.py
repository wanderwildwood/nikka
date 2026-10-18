#!/usr/bin/env python3
"""
Makes the app's two bell sounds, res/raw/bowl.ogg (one strike) and res/raw/bowl_end.ogg (three),
synthesised here rather than recorded, so there is no one else's recording to licence.

A singing bowl is a few partials that ring for a long time and die away at different rates: the
lowest the longest, the highest within a second or two. Each partial is really two modes a hair
apart, because no bowl is perfectly round, and that pair beating slowly against itself is the
shimmer a bowl has and a sine wave does not. The strike is a padded mallet: a soft 10 ms rise
and a little low thud, no click.

Needs numpy and oggenc (vorbis-tools). Run from anywhere:  python3 tools/make-bowl.py
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

RATE = 32000
F0 = 311.0                                 # E flat 4: low enough to be soft, high enough
                                           # for a phone's small speaker to carry
# (ratio to F0, level, seconds to fall 60 dB, beat in Hz between the pair)
PARTIALS = [
    (1.000, 1.00, 9.0, 0.55),
    (2.740, 0.42, 5.5, 1.10),
    (5.180, 0.16, 2.6, 1.70),
    (8.320, 0.05, 1.3, 2.30),
]
STRIKE = 9.0                                # seconds of one strike, fade included
HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, '..', 'app', 'src', 'main', 'res', 'raw')


def strike(seconds=STRIKE, seed=1):
    n = int(seconds * RATE)
    t = np.arange(n) / RATE
    rng = np.random.default_rng(seed)
    out = np.zeros(n)
    for ratio, level, t60, beat in PARTIALS:
        f = F0 * ratio
        decay = np.exp(-6.9078 * t / t60)
        phase = rng.uniform(0, 2 * np.pi, 2)
        pair = 0.5 * (np.sin(2 * np.pi * (f - beat / 2) * t + phase[0]) +
                      np.sin(2 * np.pi * (f + beat / 2) * t + phase[1]))
        out += level * decay * pair
    # the padded mallet: a short low thud, filtered noise, well under the ring
    thud_n = int(0.04 * RATE)
    thud = rng.standard_normal(thud_n)
    kernel = np.ones(24) / 24
    thud = np.convolve(thud, kernel, mode='same') * np.hanning(thud_n * 2)[thud_n:] * 0.08
    out[:thud_n] += thud
    # soft rise, so the strike has no click; a long fade at the end so it never stops short
    rise = int(0.010 * RATE)
    out[:rise] *= 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, rise))
    fall = int(1.5 * RATE)
    out[-fall:] *= np.linspace(1, 0, fall) ** 2
    return out


def write_ogg(samples, name):
    peak = np.max(np.abs(samples))
    pcm = (samples / peak * 0.70 * 32767).astype(np.int16)   # about -3 dB
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + '.wav')
        with wave.open(wav, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        out = os.path.join(RAW, name + '.ogg')
        subprocess.run(['oggenc', '-Q', '-q', '3', '--discard-comments', '-o', out, wav], check=True)
        print(out, os.path.getsize(out), 'bytes')


if __name__ == '__main__':
    os.makedirs(RAW, exist_ok=True)
    write_ogg(strike(), 'bowl')
    # The end of a sitting: three strikes, each one let ring a while before the next.
    gap = 4.5
    total = np.zeros(int((2 * gap + STRIKE) * RATE))
    for i in range(3):
        s = strike(seed=i + 1) * (1.0 if i == 0 else 0.92)
        at = int(i * gap * RATE)
        total[at:at + len(s)] += s
    write_ogg(total, 'bowl_end')
