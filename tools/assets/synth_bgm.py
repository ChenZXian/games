import argparse
import datetime
import hashlib
import json
import math
import os
import wave
import struct

def _hash_seed(s):
    h = hashlib.sha256(s.encode("utf-8")).digest()
    return int.from_bytes(h[:4], "big")

def _clamp(x, lo, hi):
    if x < lo:
        return lo
    if x > hi:
        return hi
    return x

def _square(phase):
    return 1.0 if phase % (2.0 * math.pi) < math.pi else -1.0

def _adsr(t, a, d, s, r, total):
    if t < 0.0:
        return 0.0
    if t < a:
        return t / a if a > 0 else 1.0
    t2 = t - a
    if t2 < d:
        if d <= 0:
            return s
        return 1.0 - (1.0 - s) * (t2 / d)
    t3 = t2 - d
    sustain_time = total - (a + d + r)
    if sustain_time < 0:
        sustain_time = 0
    if t3 < sustain_time:
        return s
    t4 = t3 - sustain_time
    if t4 < r:
        if r <= 0:
            return 0.0
        return s * (1.0 - (t4 / r))
    return 0.0

def _midi_to_hz(midi):
    return 440.0 * (2.0 ** ((midi - 69) / 12.0))

def _make_progression(seed):
    modes = [
        [0, 3, 7, 10],
        [0, 4, 7, 11],
        [0, 4, 7, 10],
        [0, 3, 7, 11],
    ]
    base = 57 + (seed % 12)
    prog = [
        (base + 0, modes[seed % 4]),
        (base + 5, modes[(seed + 1) % 4]),
        (base + 7, modes[(seed + 2) % 4]),
        (base + 2, modes[(seed + 3) % 4]),
    ]
    return prog

def synth_wav(out_path, seconds, sr, seed):
    bpm = 110 + (seed % 41)
    beat = 60.0 / bpm
    bar = beat * 4.0
    total = seconds
    frames = int(sr * seconds)

    prog = _make_progression(seed)
    melody_scale = [0, 2, 3, 5, 7, 8, 10]
    mel_base = 69 + (seed % 5) - 2

    with wave.open(out_path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sr)

        p_bass = 0.0
        p_pad1 = 0.0
        p_pad2 = 0.0
        p_mel = 0.0

        for i in range(frames):
            t = i / sr

            chord_idx = int(t / bar) % len(prog)
            root_midi, chord = prog[chord_idx]

            bass_midi = root_midi - 24
            bass_hz = _midi_to_hz(bass_midi)

            pad1_hz = _midi_to_hz(root_midi + chord[1])
            pad2_hz = _midi_to_hz(root_midi + chord[2])

            step = int(t / (beat / 2.0))
            mel_degree = melody_scale[(seed + step) % len(melody_scale)]
            mel_oct = 0 if (step % 8) < 6 else 12
            mel_hz = _midi_to_hz(mel_base + mel_degree + mel_oct)

            p_bass += (2.0 * math.pi * bass_hz) / sr
            p_pad1 += (2.0 * math.pi * pad1_hz) / sr
            p_pad2 += (2.0 * math.pi * pad2_hz) / sr
            p_mel += (2.0 * math.pi * mel_hz) / sr

            pad = 0.35 * math.sin(p_pad1) + 0.35 * math.sin(p_pad2)
            bass = 0.45 * _square(p_bass)
            mel = 0.28 * _square(p_mel)

            bar_pos = t % bar
            kick = 0.0
            sn = 0.0
            hhat = 0.0

            kick_pos = bar_pos % beat
            kick_env = _adsr(kick_pos, 0.002, 0.04, 0.0, 0.08, beat)
            if kick_pos < 0.12:
                kick = 0.9 * kick_env * math.sin(2.0 * math.pi * (55.0 - 30.0 * kick_pos) * kick_pos)

            sn_pos = (bar_pos - beat) % (2.0 * beat)
            sn_env = _adsr(sn_pos, 0.001, 0.03, 0.0, 0.06, 2.0 * beat)
            if sn_pos < 0.10:
                n = math.sin(2.0 * math.pi * (seed % 997 + 1) * (t * 7.0))
                sn = 0.45 * sn_env * n

            hh_pos = bar_pos % (beat / 2.0)
            hh_env = _adsr(hh_pos, 0.001, 0.02, 0.0, 0.02, beat / 2.0)
            if hh_pos < 0.06:
                n2 = math.sin(2.0 * math.pi * (seed % 541 + 3) * (t * 11.0))
                hhat = 0.25 * hh_env * n2

            mel_pos = (t % (beat / 2.0))
            mel_env = _adsr(mel_pos, 0.005, 0.03, 0.6, 0.04, beat / 2.0)
            mel *= mel_env

            mix = pad + bass + mel + kick + sn + hhat
            mix = math.tanh(mix * 1.15)

            v = int(_clamp(mix, -1.0, 1.0) * 32767)
            wf.writeframes(struct.pack("<h", v))

def _load_index(path):
    if not os.path.exists(path):
        return {"version": 1, "entries": []}
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def _save_index(path, data):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    os.replace(tmp, path)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-GameId", required=True)
    ap.add_argument("-Tag", default="default")
    ap.add_argument("-LibraryRoot", default="shared_assets/bgm")
    ap.add_argument("-Seconds", type=int, default=16)
    ap.add_argument("-SampleRate", type=int, default=44100)
    ap.add_argument("-AssignProject", default="")
    args = ap.parse_args()

    game_id = args.GameId.strip()
    tag = args.Tag.strip().lower()
    lib_root = args.LibraryRoot.strip()

    seed = _hash_seed(game_id + ":" + tag)
    today = datetime.date.today().isoformat()

    files_dir = os.path.join(lib_root, "files")
    os.makedirs(files_dir, exist_ok=True)

    fname = f"{tag}_{seed:08x}.wav"
    out_wav = os.path.join(files_dir, fname)

    if not os.path.exists(out_wav):
        synth_wav(out_wav, args.Seconds, args.SampleRate, seed)

    idx_path = os.path.join(lib_root, "index.json")
    idx = _load_index(idx_path)

    exists = False
    for e in idx.get("entries", []):
        if e.get("file") == fname:
            exists = True
            break

    if not exists:
        idx.setdefault("entries", []).append({
            "file": fname,
            "tag": tag,
            "generated": True,
            "seed": f"{seed:08x}",
            "seconds": args.Seconds,
            "sample_rate": args.SampleRate,
            "retrieved_at": today
        })
        _save_index(idx_path, idx)

    if args.AssignProject:
        proj = args.AssignProject
        dst_dir = os.path.join(proj, "app", "src", "main", "assets", "audio")
        os.makedirs(dst_dir, exist_ok=True)
        dst = os.path.join(dst_dir, "bgm.wav")
        with open(out_wav, "rb") as rf:
            data = rf.read()
        with open(dst, "wb") as wf:
            wf.write(data)

if __name__ == "__main__":
    main()
