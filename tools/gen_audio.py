#!/usr/bin/env python3
"""Original chiptune-orchestral music and sound effects, synthesised with numpy and encoded to Ogg Vorbis.

Every track is composed here (melody, chords, bass, drums), so all audio is original and free to use.
Run from the repo root: python3 tools/gen_audio.py   (needs numpy and ffmpeg with libvorbis)
"""
import json
import os
import subprocess
import tempfile
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOUNDS = os.path.join(ROOT, "src/main/resources/assets/konocraft/sounds")
SR = 32000
rng = np.random.default_rng(7)

NOTE_INDEX = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8,
              "A": 9, "A#": 10, "Bb": 10, "B": 11}


def freq(note):
    """'C#4' -> Hz. 'R' is a rest."""
    if note == "R":
        return 0.0
    name, octave = note[:-1], int(note[-1])
    n = NOTE_INDEX[name] + 12 * (octave + 1)
    return 440.0 * 2 ** ((n - 69) / 12)


def adsr(n, a=0.01, d=0.08, s=0.7, r=0.08):
    a, d, r = int(a * SR), int(d * SR), int(r * SR)
    env = np.ones(n) * s
    if a > 0:
        env[:min(a, n)] = np.linspace(0, 1, a)[:min(a, n)]
    if d > 0 and a < n:
        seg = np.linspace(1, s, d)
        env[a:a + d] = seg[:max(0, min(d, n - a))]
    if r > 0:
        r = min(r, n)
        env[n - r:] *= np.linspace(1, 0, r)
    return env


def osc(kind, f, n, duty=0.5, vib=0.0, vib_rate=5.5, phase_noise=0.0):
    t = np.arange(n) / SR
    fm = f * (1 + vib * np.sin(2 * np.pi * vib_rate * t) * np.clip(t * 3, 0, 1))
    ph = np.cumsum(fm) / SR
    ph = ph - np.floor(ph)
    if kind == "sine":
        return np.sin(2 * np.pi * ph)
    if kind == "square":
        return np.where(ph < duty, 1.0, -1.0) * 0.6
    if kind == "tri":
        return (2 * np.abs(2 * ph - 1) - 1)
    if kind == "saw":
        return (2 * ph - 1) * 0.6
    if kind == "bell":
        env = np.exp(-t * 3.5)
        return (np.sin(2 * np.pi * ph) + 0.5 * np.sin(2 * np.pi * ph * 2.76) * np.exp(-t * 6) + 0.25 * np.sin(2 * np.pi * ph * 5.4) * np.exp(-t * 9)) * env
    if kind == "organ":
        return (np.sin(2 * np.pi * ph) + 0.5 * np.sin(4 * np.pi * ph) + 0.3 * np.sin(6 * np.pi * ph) + 0.15 * np.sin(8 * np.pi * ph)) * 0.5
    if kind == "brass":
        s = (2 * ph - 1)
        return lowpass(s, 1800) * 0.9
    if kind == "pluck":
        env = np.exp(-t * 7)
        return ((2 * ph - 1) * 0.5 + np.sin(2 * np.pi * ph) * 0.5) * env
    raise ValueError(kind)


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    acc = 0.0
    # vectorised one-pole via lfilter-like recursion in chunks
    out = np.empty_like(x)
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        out[i] = acc
    return out


def fast_lowpass(x, cutoff):
    """Cheap FFT low-pass for long signals."""
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    X *= 1 / (1 + (f / cutoff) ** 4)
    return np.fft.irfft(X, len(x))


def fast_highpass(x, cutoff):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    X *= 1 - 1 / (1 + (f / cutoff) ** 4)
    return np.fft.irfft(X, len(x))


def reverb(x, seconds=1.6, mix=0.25):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    ir = rng.standard_normal(n) * np.exp(-t * 4.5 / seconds)
    ir = fast_lowpass(ir, 5000)
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    size = len(x) + n
    nfft = 1 << (size - 1).bit_length()
    wet = np.fft.irfft(np.fft.rfft(x, nfft) * np.fft.rfft(ir, nfft), nfft)[:len(x)]
    return x * (1 - mix) + wet * mix * 0.8


# ------------------------------------------------------------------------------------------------
# Tiny tracker

class Track:
    def __init__(self, bpm, bars, beats_per_bar=4):
        self.bpm = bpm
        self.beat = 60.0 / bpm
        self.bars = bars
        self.bpb = beats_per_bar
        self.n = int(bars * beats_per_bar * self.beat * SR) + SR * 2
        self.buf = np.zeros(self.n)

    def at(self, beat):
        return int(beat * self.beat * SR)

    def note(self, start_beat, beats, f, kind, vol, env=(0.01, 0.08, 0.7, 0.08), **kw):
        if f <= 0:
            return
        n = int(beats * self.beat * SR)
        if n <= 0:
            return
        s = osc(kind, f, n, **kw) * adsr(n, *env) * vol
        i = self.at(start_beat)
        end = min(self.n, i + n)
        self.buf[i:end] += s[:end - i]

    def melody(self, start_beat, seq, kind, vol, env=(0.01, 0.06, 0.75, 0.06), legato=0.92, **kw):
        """seq: list of (note, beats)."""
        b = start_beat
        for note, beats in seq:
            if note != "R":
                self.note(b, beats * legato, freq(note), kind, vol, env, **kw)
            b += beats
        return b

    def chord(self, start_beat, beats, notes, kind, vol, env=(0.08, 0.2, 0.6, 0.2), **kw):
        for nt in notes:
            self.note(start_beat, beats, freq(nt), kind, vol / len(notes) * 1.6, env, **kw)

    def kick(self, beat, vol=0.9):
        n = int(0.25 * SR)
        t = np.arange(n) / SR
        f = 120 * np.exp(-t * 18) + 45
        s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 14) * vol
        i = self.at(beat)
        e = min(self.n, i + n)
        self.buf[i:e] += s[:e - i]

    def snare(self, beat, vol=0.4):
        n = int(0.2 * SR)
        t = np.arange(n) / SR
        s = (rng.standard_normal(n) * 0.7 + np.sin(2 * np.pi * 190 * t) * 0.5) * np.exp(-t * 22) * vol
        i = self.at(beat)
        e = min(self.n, i + n)
        self.buf[i:e] += s[:e - i]

    def hat(self, beat, vol=0.12, open_=False):
        n = int((0.18 if open_ else 0.05) * SR)
        t = np.arange(n) / SR
        noise = rng.standard_normal(n)
        noise = np.diff(np.concatenate([[0], noise]))
        s = noise * np.exp(-t * (14 if open_ else 60)) * vol
        i = self.at(beat)
        e = min(self.n, i + n)
        self.buf[i:e] += s[:e - i]

    def render(self, rev=1.6, mix=0.22, gain=0.85):
        x = self.buf
        x = reverb(x, rev, mix)
        x = x / (np.max(np.abs(x)) + 1e-9) * gain
        # soft fade at the end
        f = int(1.5 * SR)
        x[-f:] *= np.linspace(1, 0, f)
        return x


def parse(s):
    """'C5:1 E5:0.5 R:1' -> [(note, beats)]"""
    out = []
    for tok in s.split():
        n, d = tok.split(":")
        out.append((n, float(d)))
    return out


def triad(root, kind="maj", octave=4):
    base = NOTE_INDEX[root] + 12 * (octave + 1)
    intervals = {"maj": (0, 4, 7), "min": (0, 3, 7), "dim": (0, 3, 6), "sus": (0, 5, 7), "7": (0, 4, 7, 10), "m7": (0, 3, 7, 10), "maj7": (0, 4, 7, 11)}[kind]
    names = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
    return [names[(base + i) % 12] + str((base + i) // 12 - 1) for i in intervals]


def bass_note(root, octave=2):
    return root + str(octave)


# ------------------------------------------------------------------------------------------------
# Compositions

def axel_theme():
    """Cheerful town theme in F major."""
    t = Track(112, 32)
    prog = [("F", "maj"), ("C", "maj"), ("D", "min"), ("A#", "maj"), ("F", "maj"), ("C", "maj"), ("A#", "maj"), ("C", "maj")]
    melA = parse("A5:1 C6:0.5 A5:0.5 G5:1 F5:1 | E5:1.5 F5:0.5 G5:2 | F5:1 D5:0.5 F5:0.5 A5:1 G5:1 | F5:1.5 D5:0.5 C5:2 "
                 "| A5:1 C6:0.5 D6:0.5 C6:1 A5:1 | G5:1 E5:1 C5:2 | D5:1 F5:1 A#5:1 A5:1 | G5:3 R:1".replace("|", ""))
    melB = parse("C6:1.5 A#5:0.5 A5:1 G5:1 | A5:1 F5:1 C5:2 | D5:1 E5:1 F5:1 G5:1 | A5:3 R:1 "
                 "| C6:1.5 D6:0.5 C6:1 A#5:1 | A5:1 G5:1 F5:2 | E5:1 G5:1 C6:1 E5:1 | F5:3 R:1".replace("|", ""))
    for rep in range(4):
        base = rep * 32
        mel = melA if rep % 2 == 0 else melB
        t.melody(base, mel, "square", 0.32, duty=0.25, vib=0.004)
        if rep >= 1:
            t.melody(base, [(n[:-1] + str(int(n[-1]) - 1) if n != "R" else n, d) for n, d in mel], "tri", 0.10)
        for i, (root, k) in enumerate(prog):
            b = base + i * 4
            t.chord(b, 4, triad(root, k, 4), "saw", 0.16, env=(0.05, 0.3, 0.5, 0.3))
            for q in range(4):
                bn = bass_note(root, 2) if q % 2 == 0 else bass_note(triad(root, k, 2)[2][:-1], 2)
                t.note(b + q, 0.9, freq(bn), "tri", 0.42)
            # light percussion
            for q in range(4):
                if q in (0, 2):
                    t.kick(b + q, 0.45)
                else:
                    t.snare(b + q, 0.18)
                t.hat(b + q + 0.5, 0.06)
        # little bell sparkle on top
        for i in range(8):
            if i % 2 == 0:
                t.note(base + i * 4 + 3.5, 0.5, freq(triad(prog[i][0], prog[i][1], 6)[1]), "bell", 0.12)
    return t.render(1.3, 0.2)


def fields_theme():
    """Adventurous travelling theme in G major."""
    t = Track(128, 32)
    prog = [("G", "maj"), ("D", "maj"), ("E", "min"), ("C", "maj")] * 2
    mel = parse("D5:0.5 G5:0.5 B5:1 A5:0.5 G5:0.5 A5:1 | B5:1 D6:1 B5:1 A5:1 | G5:0.5 E5:0.5 G5:1 B5:1 A5:1 | G5:2 E5:1 D5:1 "
                "| D5:0.5 G5:0.5 B5:1 C6:1 B5:1 | A5:1 F#5:1 D5:2 | E5:1 G5:1 B5:1 E6:1 | D6:3 R:1".replace("|", ""))
    mel2 = parse("B5:1 C6:1 D6:2 | C6:1 B5:1 A5:2 | B5:1 A5:1 G5:1 E5:1 | G5:2 A5:2 "
                 "| B5:1 C6:1 D6:1 E6:1 | D6:1 B5:1 A5:2 | G5:1 A5:1 B5:1 C6:1 | B5:2 G5:2".replace("|", ""))
    for rep in range(4):
        base = rep * 32
        t.melody(base, mel if rep % 2 == 0 else mel2, "square", 0.30, duty=0.5 if rep % 2 else 0.25, vib=0.005)
        for i, (root, k) in enumerate(prog):
            b = base + i * 4
            notes = triad(root, k, 4)
            for q in range(8):
                t.note(b + q * 0.5, 0.45, freq(notes[q % 3]), "pluck", 0.14)
            for q in range(4):
                t.note(b + q, 0.45, freq(bass_note(root, 2)), "tri", 0.45)
                t.note(b + q + 0.5, 0.45, freq(bass_note(root, 3)), "tri", 0.3)
                t.kick(b + q, 0.5) if q % 2 == 0 else t.snare(b + q, 0.22)
                t.hat(b + q + 0.5, 0.07)
    return t.render(1.1, 0.18)


def castle_theme():
    """Haunted castle: slow D minor organ with a harpsichord arpeggio."""
    t = Track(84, 24)
    prog = [("D", "min"), ("A#", "maj"), ("G", "min"), ("A", "maj"), ("D", "min"), ("F", "maj"), ("G", "min"), ("A", "7")]
    mel = parse("A4:2 F4:1 E4:1 | D4:2 R:1 A4:1 | A#4:2 A4:1 G4:1 | E4:3 R:1 | D5:2 C5:1 A#4:1 | A4:2 F4:2 | G4:1 A4:1 A#4:1 D5:1 | C#5:4".replace("|", ""))
    for rep in range(3):
        base = rep * 32
        for i, (root, k) in enumerate(prog):
            b = base + i * 4
            t.chord(b, 4, triad(root, k, 3), "organ", 0.32, env=(0.3, 0.5, 0.8, 0.6))
            notes = triad(root, k, 4)
            for q in range(8):
                t.note(b + q * 0.5, 0.4, freq(notes[[0, 1, 2, 1][q % 4] if len(notes) > 2 else 0]), "pluck", 0.12)
            t.note(b, 4, freq(bass_note(root, 1)), "sine", 0.35, env=(0.2, 0.5, 0.8, 0.5))
        t.melody(base, mel, "sine", 0.25, env=(0.08, 0.2, 0.7, 0.3), vib=0.01, vib_rate=4.5)
        t.note(base + 31, 1, freq("D3"), "bell", 0.25)
    return t.render(2.6, 0.35)


def afterlife_theme():
    """Ethereal hall of goddess Eris: bells over soft pads, Eb major."""
    t = Track(70, 16)
    prog = [("D#", "maj7"), ("A#", "maj"), ("C", "min"), ("G#", "maj7")] * 2
    for rep in range(2):
        base = rep * 32
        for i, (root, k) in enumerate(prog):
            b = base + i * 4
            t.chord(b, 4, triad(root, k, 4), "sine", 0.3, env=(1.0, 0.5, 0.8, 1.0), vib=0.003)
            notes = triad(root, k, 5)
            for q, idx in enumerate([0, 2, 1, 3 % len(notes), 2, 1]):
                t.note(b + q * 0.66, 1.5, freq(notes[idx % len(notes)]), "bell", 0.14)
            t.note(b, 4, freq(bass_note(root, 2)), "sine", 0.22, env=(0.5, 0.5, 0.8, 0.8))
    return t.render(3.2, 0.45, 0.75)


def battle_theme(boss=False):
    """Fast minor battle music. The boss variant is heavier."""
    bpm = 150 if boss else 144
    t = Track(bpm, 32)
    prog = [("D", "min"), ("A#", "maj"), ("C", "maj"), ("A", "maj")] * 2 if boss else [("A", "min"), ("F", "maj"), ("G", "maj"), ("E", "maj")] * 2
    mel = parse("D5:0.5 D5:0.5 F5:0.5 A5:0.5 G5:1 F5:1 | A#5:1 A5:0.5 G5:0.5 F5:2 | E5:0.5 F5:0.5 G5:1 C6:1 A#5:1 | A5:3 C#5:1 "
                "| D5:0.5 F5:0.5 A5:1 D6:1 C6:1 | A#5:1 D6:1 F6:2 | E6:1 D6:0.5 C6:0.5 A#5:1 G5:1 | A5:4".replace("|", "")) if boss else \
        parse("A4:0.5 C5:0.5 E5:1 D5:0.5 C5:0.5 B4:1 | C5:1 A4:1 F4:2 | G4:0.5 B4:0.5 D5:1 G5:1 F5:1 | E5:3 G#4:1 "
              "| A4:0.5 C5:0.5 E5:1 A5:1 G5:1 | F5:1 A5:1 C6:2 | B5:1 A5:0.5 G5:0.5 F5:1 D5:1 | E5:4".replace("|", ""))
    for rep in range(4):
        base = rep * 32
        t.melody(base, mel, "brass" if boss else "square", 0.34, duty=0.25, vib=0.006)
        if rep % 2 == 1:
            t.melody(base, [(n[:-1] + str(int(n[-1]) - 1) if n != "R" else n, d) for n, d in mel], "square", 0.12, duty=0.5)
        for i, (root, k) in enumerate(prog):
            b = base + i * 4
            t.chord(b, 4, triad(root, k, 3), "saw", 0.2, env=(0.02, 0.2, 0.6, 0.1))
            for q in range(8):
                t.note(b + q * 0.5, 0.42, freq(bass_note(root, 2 if q % 2 == 0 else 3)), "saw" if boss else "tri", 0.38)
            for q in range(4):
                t.kick(b + q, 0.8)
                if q % 2 == 1:
                    t.snare(b + q, 0.38)
                t.hat(b + q + 0.5, 0.09)
                if boss:
                    t.kick(b + q + 0.75, 0.4)
            if boss and i % 2 == 1:
                t.note(b + 3.5, 0.5, freq("D6"), "bell", 0.18)
    return t.render(0.9, 0.15, 0.9)


def destroyer_theme():
    """The final battle: an epic C minor march with brass and timpani."""
    t = Track(138, 32)
    prog = [("C", "min"), ("G#", "maj"), ("A#", "maj"), ("G", "maj"), ("C", "min"), ("F", "min"), ("G#", "maj"), ("G", "7")]
    melA = parse("G4:1.5 C5:0.5 D#5:1 G5:1 | G#5:1 G5:0.5 F5:0.5 D#5:2 | F5:1 D#5:0.5 D5:0.5 A#4:1 D5:1 | G4:3 R:1 "
                 "| C5:0.5 D5:0.5 D#5:1 G5:1 C6:1 | A#5:1 G#5:1 F5:2 | D#5:1 F5:1 G5:1 G#5:1 | G5:2 B4:2".replace("|", ""))
    melB = parse("C6:2 G5:1 D#5:1 | G#5:2 F5:2 | A#5:1 G#5:1 G5:1 F5:1 | G5:4 "
                 "| D#6:1.5 D6:0.5 C6:1 G5:1 | G#5:1 C6:1 F6:2 | D#6:1 D6:1 C6:1 B5:1 | C6:4".replace("|", ""))
    for rep in range(4):
        base = rep * 32
        mel = melA if rep < 2 else melB
        t.melody(base, mel, "brass", 0.4, env=(0.03, 0.1, 0.8, 0.1), vib=0.006)
        t.melody(base, [(n[:-1] + str(int(n[-1]) + 1) if n != "R" else n, d) for n, d in mel], "square", 0.08, duty=0.25)
        for i, (root, k) in enumerate(prog):
            b = base + i * 4
            t.chord(b, 4, triad(root, k, 3), "saw", 0.24, env=(0.05, 0.3, 0.7, 0.2))
            t.chord(b, 4, triad(root, k, 4), "organ", 0.12, env=(0.2, 0.3, 0.7, 0.3))
            for q in range(8):
                t.note(b + q * 0.5, 0.4, freq(bass_note(root, 1 if q % 2 == 0 else 2)), "saw", 0.4)
            # march drums
            for q in range(4):
                t.kick(b + q, 0.9)
                t.snare(b + q + 0.5, 0.18)
                if q == 3:
                    t.snare(b + q + 0.25, 0.25)
                    t.snare(b + q + 0.75, 0.3)
            t.note(b, 1, freq(bass_note(root, 2)), "sine", 0.5, env=(0.005, 0.3, 0.2, 0.3))
    return t.render(1.4, 0.2, 0.92)


def ending_theme():
    """Warm, slightly bittersweet celebration in C major."""
    t = Track(96, 32)
    prog = [("C", "maj"), ("G", "maj"), ("A", "min"), ("E", "min"), ("F", "maj"), ("C", "maj"), ("F", "maj"), ("G", "maj")]
    mel = parse("E5:1 G5:1 C6:1.5 B5:0.5 | B5:1 A5:1 G5:2 | A5:1 C6:1 E6:1 D6:1 | B5:3 G5:1 "
                "| A5:1 C6:1 A5:1 F5:1 | G5:1 E5:1 C5:2 | D5:1 F5:1 A5:1 C6:1 | B5:2 D6:2".replace("|", ""))
    for rep in range(4):
        base = rep * 32
        t.melody(base, mel, "sine" if rep == 0 else "square", 0.3 if rep == 0 else 0.24, duty=0.25, vib=0.006, env=(0.03, 0.1, 0.75, 0.15))
        for i, (root, k) in enumerate(prog):
            b = base + i * 4
            t.chord(b, 4, triad(root, k, 4), "saw", 0.13, env=(0.15, 0.3, 0.6, 0.4))
            notes = triad(root, k, 5)
            for q in range(4):
                t.note(b + q, 0.9, freq(notes[q % 3]), "bell", 0.08)
                t.note(b + q, 0.9, freq(bass_note(root, 2)), "tri", 0.36)
            if rep >= 1:
                t.kick(b, 0.4)
                t.kick(b + 2, 0.4)
                t.snare(b + 1, 0.15)
                t.snare(b + 3, 0.15)
    return t.render(1.8, 0.25)


# ------------------------------------------------------------------------------------------------
# Sound effects

def sfx_explosion():
    n = int(3.5 * SR)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    boom = fast_lowpass(noise, 300) * np.exp(-t * 1.6) * 2.5
    crack = fast_highpass(noise, 1500) * np.exp(-t * 9) * 0.6
    sub = np.sin(2 * np.pi * np.cumsum(70 * np.exp(-t * 2) + 28) / SR) * np.exp(-t * 1.8) * 1.4
    rumble = fast_lowpass(rng.standard_normal(n), 120) * np.exp(-t * 0.9) * 2.0
    x = boom + crack + sub + rumble
    x[:int(0.01 * SR)] *= np.linspace(0, 1, int(0.01 * SR))
    return reverb(x, 2.5, 0.3)


def sfx_chant():
    n = int(2.2 * SR)
    x = np.zeros(n)
    t = np.arange(n) / SR
    for i, f in enumerate([440, 554, 659, 880, 1108, 1318]):
        start = int(i * 0.18 * SR)
        m = n - start
        x[start:] += osc("bell", f, m) * 0.4
    x += osc("sine", 220, n, vib=0.01) * np.clip(t, 0, 1) * np.exp(-t * 0.6) * 0.3
    x += fast_highpass(rng.standard_normal(n), 4000) * np.clip(t * 0.5, 0, 1) * 0.08
    return reverb(x, 2.0, 0.4)


def sfx_alarm():
    n = int(4.0 * SR)
    x = np.zeros(n)
    for k in range(3):
        start = int(k * 1.2 * SR)
        m = n - start
        tt = np.arange(m) / SR
        bell = (np.sin(2 * np.pi * 523 * tt) + 0.6 * np.sin(2 * np.pi * 523 * 2.4 * tt) + 0.4 * np.sin(2 * np.pi * 523 * 3.9 * tt)
                + 0.3 * np.sin(2 * np.pi * 262 * tt)) * np.exp(-tt * 1.8)
        x[start:] += bell * 0.5
    return reverb(x, 2.0, 0.3)


def sfx_blip():
    n = int(0.035 * SR)
    return osc("square", 880, n, duty=0.5) * adsr(n, 0.002, 0.01, 0.5, 0.01) * 0.6


def sfx_level_up():
    t = Track(240, 2)
    t.melody(0, parse("C5:0.5 E5:0.5 G5:0.5 C6:1.5"), "square", 0.4, duty=0.25)
    t.melody(0, parse("C6:0.5 E6:0.5 G6:0.5 C7:1.5"), "bell", 0.2)
    return t.render(1.0, 0.25, 0.8)[:int(1.6 * SR)]


def sfx_holy():
    n = int(1.8 * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for f in (523, 659, 784, 1047, 1319):
        x += osc("sine", f, n, vib=0.004) * 0.18
    x *= adsr(n, 0.15, 0.3, 0.7, 0.8)
    for i in range(10):
        start = int(rng.random() * 1.2 * SR)
        m = int(0.5 * SR)
        x[start:start + m] += osc("bell", 1500 + rng.random() * 2500, m)[:len(x[start:start + m])] * 0.1
    return reverb(x, 2.2, 0.45)


def sfx_croak():
    n = int(0.6 * SR)
    t = np.arange(n) / SR
    f = 95 + 25 * np.sin(2 * np.pi * 3 * t)
    x = osc("saw", 1, n)
    ph = np.cumsum(f) / SR
    x = (2 * (ph - np.floor(ph)) - 1)
    gate = (np.sin(2 * np.pi * 9 * t) > -0.2).astype(float)
    x = fast_lowpass(x * gate, 900) * adsr(n, 0.01, 0.1, 0.8, 0.15) * 1.6
    return x


def sfx_gulp():
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    f = 400 * np.exp(-t * 6) + 60
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 5)
    x += fast_lowpass(rng.standard_normal(n), 500) * np.exp(-t * 10) * 0.6
    return x


def sfx_whoosh():
    n = int(0.6 * SR)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    env = np.sin(np.pi * t / t[-1]) ** 2
    return fast_highpass(fast_lowpass(noise, 2500), 400) * env * 0.8


def sfx_cry():
    """A comedic descending 'waaah' (trombone-like) for Aqua's tantrums."""
    n = int(1.4 * SR)
    t = np.arange(n) / SR
    f = 520 * np.exp(-t * 0.5) * (1 + 0.04 * np.sin(2 * np.pi * 7 * t))
    ph = np.cumsum(f) / SR
    x = (2 * (ph - np.floor(ph)) - 1)
    x = fast_lowpass(x, 1400) * adsr(n, 0.05, 0.1, 0.9, 0.3)
    return x * 1.2


def sfx_coin():
    t = Track(300, 1)
    t.melody(0, parse("B6:0.5 E7:1.5"), "square", 0.3, duty=0.5)
    return t.render(0.6, 0.15, 0.6)[:int(0.5 * SR)]


# ------------------------------------------------------------------------------------------------

def write_ogg(x, path, quality=4):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    x = np.clip(x / (np.max(np.abs(x)) + 1e-9) * 0.92, -1, 1)
    pcm = (x * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", str(quality), path], check=True)
    os.unlink(wav_path)
    print("wrote", os.path.relpath(path, ROOT), f"{len(x) / SR:.1f}s")


MUSIC = {
    "axel": axel_theme, "fields": fields_theme, "castle": castle_theme, "afterlife": afterlife_theme,
    "battle": lambda: battle_theme(False), "boss": lambda: battle_theme(True), "destroyer": destroyer_theme, "ending": ending_theme,
}
SFX = {
    "fx/explosion_big": sfx_explosion, "fx/chant": sfx_chant, "fx/alarm": sfx_alarm, "fx/blip": sfx_blip, "fx/level_up": sfx_level_up,
    "fx/holy": sfx_holy, "entity/toad_croak": sfx_croak, "entity/toad_gulp": sfx_gulp, "entity/cabbage_whoosh": sfx_whoosh,
    "voice/aqua_cry": sfx_cry, "fx/coin": sfx_coin,
}
EVENTS = {
    "fx.explosion_big": "fx/explosion_big", "fx.chant": "fx/chant", "fx.alarm": "fx/alarm", "fx.blip": "fx/blip", "fx.level_up": "fx/level_up",
    "fx.holy": "fx/holy", "entity.toad.croak": "entity/toad_croak", "entity.toad.gulp": "entity/toad_gulp",
    "entity.cabbage.whoosh": "entity/cabbage_whoosh", "voice.aqua_cry": "voice/aqua_cry", "fx.coin": "fx/coin",
}


def main():
    import sys
    only = sys.argv[1:]
    for name, fn in MUSIC.items():
        if only and name not in only:
            continue
        write_ogg(fn(), os.path.join(SOUNDS, "music", name + ".ogg"), 3)
    for name, fn in SFX.items():
        if only and name.split("/")[-1] not in only and "sfx" not in only:
            continue
        write_ogg(fn(), os.path.join(SOUNDS, name + ".ogg"), 5)
    sounds = {}
    for name in MUSIC:
        sounds["music." + name] = {"category": "music", "sounds": [{"name": "konocraft:music/" + name, "stream": True}]}
    for ev, file in EVENTS.items():
        sounds[ev] = {"sounds": ["konocraft:" + file], "subtitle": "subtitles.konocraft." + ev}
    with open(os.path.join(ROOT, "src/main/resources/assets/konocraft/sounds.json"), "w") as fh:
        json.dump(sounds, fh, indent=2)


if __name__ == "__main__":
    main()
