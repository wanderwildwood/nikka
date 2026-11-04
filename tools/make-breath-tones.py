#!/usr/bin/env python3
"""
Makes the three breathing cues, res/raw/breath_in.ogg, breath_hold.ogg and breath_out.ogg:
small bowls struck softly, each well under the length of the shortest breath (1 s), so one has
died away before the next. In is the highest, out the lowest (the big bowl's own E flat), and a
hold sits between them, quieter and shorter, a tap rather than a strike.

Synthesised like tools/make-bowl.py, so there is no one else's recording to license.

Needs numpy and oggenc (vorbis-tools). Run from anywhere:  python3 tools/make-breath-tones.py
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

RATE = 32000
# (ratio, level, seconds to fall 60 dB, beat in Hz between the pair): a small bowl's partials,
# rung for a much shorter time than the sitting bowl's.
PARTIALS = [
    (1.000, 1.00, 0.90, 0.6),
    (2.740, 0.30, 0.55, 1.2),
    (5.180, 0.08, 0.30, 1.8),
]
HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, '..', 'app', 'src', 'main', 'res', 'raw')


def tone(f0, seconds, level, seed):
    n = int(seconds * RATE)
    t = np.arange(n) / RATE
    rng = np.random.default_rng(seed)
    out = np.zeros(n)
    for ratio, lv, t60, beat in PARTIALS:
        f = f0 * ratio
        decay = np.exp(-6.9078 * t / t60)
        ph = rng.uniform(0, 2 * np.pi, 2)
        out += lv * decay * 0.5 * (np.sin(2 * np.pi * (f - beat / 2) * t + ph[0]) +
                                   np.sin(2 * np.pi * (f + beat / 2) * t + ph[1]))
    rise = int(0.012 * RATE)                   # a padded mallet: no click
    out[:rise] *= 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, rise))
    fall = int(0.25 * RATE)
    out[-fall:] *= np.linspace(1, 0, fall) ** 2
    return out / np.max(np.abs(out)) * level


def write_ogg(samples, name):
    pcm = (samples * 0.70 * 32767).astype(np.int16)
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
    write_ogg(tone(466.16, 0.95, 1.00, 1), 'breath_in')     # B flat 4
    write_ogg(tone(392.00, 0.60, 0.55, 2), 'breath_hold')   # G 4
    write_ogg(tone(311.13, 0.95, 1.00, 3), 'breath_out')    # E flat 4, the sitting bowl's note
