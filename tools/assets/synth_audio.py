import argparse
import datetime
import hashlib
import math
import os
import struct
import wave
from pathlib import Path

from audio_utils import (
    append_used_by,
    build_entry_id,
    build_library_path,
    copy_entry_to_project,
    detect_format,
    duration_for_seconds,
    ensure_audio_root,
    load_index,
    save_index,
)


def clamp(value, lo, hi):
    if value < lo:
        return lo
    if value > hi:
        return hi
    return value


def hash_seed(text: str) -> int:
    digest = hashlib.sha256(text.encode("utf-8")).digest()
    return int.from_bytes(digest[:4], "big")


def square(phase):
    return 1.0 if phase % (2.0 * math.pi) < math.pi else -1.0


def adsr(t, a, d, s, r, total):
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


def midi_to_hz(midi):
    return 440.0 * (2.0 ** ((midi - 69) / 12.0))


def make_progression(seed):
    modes = [
        [0, 3, 7, 10],
        [0, 4, 7, 11],
        [0, 4, 7, 10],
        [0, 3, 7, 11],
    ]
    base = 57 + (seed % 12)
    return [
        (base + 0, modes[seed % 4]),
        (base + 5, modes[(seed + 1) % 4]),
        (base + 7, modes[(seed + 2) % 4]),
        (base + 2, modes[(seed + 3) % 4]),
    ]


def synth_bgm_wav(out_path: Path, seconds: int, sample_rate: int, seed: int):
    bpm = 110 + (seed % 41)
    beat = 60.0 / bpm
    bar = beat * 4.0
    frames = int(sample_rate * seconds)
    progression = make_progression(seed)
    melody_scale = [0, 2, 3, 5, 7, 8, 10]
    melody_base = 69 + (seed % 5) - 2

    with wave.open(str(out_path), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)

        p_bass = 0.0
        p_pad1 = 0.0
        p_pad2 = 0.0
        p_mel = 0.0

        for index in range(frames):
            t = index / sample_rate
            chord_index = int(t / bar) % len(progression)
            root_midi, chord = progression[chord_index]

            bass_hz = midi_to_hz(root_midi - 24)
            pad1_hz = midi_to_hz(root_midi + chord[1])
            pad2_hz = midi_to_hz(root_midi + chord[2])

            step = int(t / (beat / 2.0))
            degree = melody_scale[(seed + step) % len(melody_scale)]
            mel_oct = 0 if (step % 8) < 6 else 12
            mel_hz = midi_to_hz(melody_base + degree + mel_oct)

            p_bass += (2.0 * math.pi * bass_hz) / sample_rate
            p_pad1 += (2.0 * math.pi * pad1_hz) / sample_rate
            p_pad2 += (2.0 * math.pi * pad2_hz) / sample_rate
            p_mel += (2.0 * math.pi * mel_hz) / sample_rate

            pad = 0.35 * math.sin(p_pad1) + 0.35 * math.sin(p_pad2)
            bass = 0.45 * square(p_bass)
            mel = 0.28 * square(p_mel)

            bar_pos = t % bar
            kick = 0.0
            snare = 0.0
            hhat = 0.0

            kick_pos = bar_pos % beat
            kick_env = adsr(kick_pos, 0.002, 0.04, 0.0, 0.08, beat)
            if kick_pos < 0.12:
                kick = 0.9 * kick_env * math.sin(2.0 * math.pi * (55.0 - 30.0 * kick_pos) * kick_pos)

            snare_pos = (bar_pos - beat) % (2.0 * beat)
            snare_env = adsr(snare_pos, 0.001, 0.03, 0.0, 0.06, 2.0 * beat)
            if snare_pos < 0.10:
                noise = math.sin(2.0 * math.pi * (seed % 997 + 1) * (t * 7.0))
                snare = 0.45 * snare_env * noise

            hh_pos = bar_pos % (beat / 2.0)
            hh_env = adsr(hh_pos, 0.001, 0.02, 0.0, 0.02, beat / 2.0)
            if hh_pos < 0.06:
                noise2 = math.sin(2.0 * math.pi * (seed % 541 + 3) * (t * 11.0))
                hhat = 0.25 * hh_env * noise2

            mel_pos = t % (beat / 2.0)
            mel_env = adsr(mel_pos, 0.005, 0.03, 0.6, 0.04, beat / 2.0)
            mel *= mel_env

            mix = math.tanh((pad + bass + mel + kick + snare + hhat) * 1.15)
            value = int(clamp(mix, -1.0, 1.0) * 32767)
            wf.writeframes(struct.pack("<h", value))


def sfx_defaults(role: str):
    mapping = {
        "ui_click": {"seconds": 0.10, "freq_start": 1200.0, "freq_end": 780.0, "wave": "sine"},
        "ui_confirm": {"seconds": 0.18, "freq_start": 840.0, "freq_end": 1180.0, "wave": "triangle"},
        "ui_back": {"seconds": 0.16, "freq_start": 980.0, "freq_end": 560.0, "wave": "triangle"},
        "attack": {"seconds": 0.14, "freq_start": 180.0, "freq_end": 110.0, "wave": "noise"},
        "hit": {"seconds": 0.12, "freq_start": 220.0, "freq_end": 90.0, "wave": "noise"},
        "collect": {"seconds": 0.24, "freq_start": 760.0, "freq_end": 1320.0, "wave": "sine"},
        "warning": {"seconds": 0.32, "freq_start": 520.0, "freq_end": 520.0, "wave": "pulse"},
        "upgrade": {"seconds": 0.30, "freq_start": 680.0, "freq_end": 1480.0, "wave": "triangle"},
        "win": {"seconds": 0.42, "freq_start": 720.0, "freq_end": 1560.0, "wave": "sine"},
        "fail": {"seconds": 0.40, "freq_start": 520.0, "freq_end": 140.0, "wave": "triangle"},
    }
    return mapping.get(role, {"seconds": 0.18, "freq_start": 880.0, "freq_end": 620.0, "wave": "sine"})


def synth_sfx_wav(out_path: Path, role: str, sample_rate: int, seed: int):
    settings = sfx_defaults(role)
    seconds = settings["seconds"]
    frames = int(sample_rate * seconds)
    start_freq = settings["freq_start"]
    end_freq = settings["freq_end"]
    wave_name = settings["wave"]

    with wave.open(str(out_path), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)

        phase = 0.0
        for index in range(frames):
            t = index / sample_rate
            progress = index / max(1, frames - 1)
            freq = start_freq + (end_freq - start_freq) * progress
            phase += (2.0 * math.pi * freq) / sample_rate
            env = adsr(t, 0.004, 0.04, 0.45, max(0.02, seconds * 0.45), seconds)

            if wave_name == "triangle":
                sample = 2.0 * abs(2.0 * ((phase / (2.0 * math.pi)) % 1.0) - 1.0) - 1.0
            elif wave_name == "pulse":
                sample = 1.0 if math.sin(phase) >= 0 else -0.35
            elif wave_name == "noise":
                noise_phase = math.sin((seed % 997 + 1) * (t * 13.0))
                sample = 0.65 * noise_phase + 0.35 * math.sin(phase)
            else:
                sample = math.sin(phase)

            if role in ("collect", "upgrade", "win"):
                harmonic = math.sin(phase * 2.0) * 0.2
                sample += harmonic

            if role in ("warning",):
                pulse = math.sin(2.0 * math.pi * 6.0 * t)
                sample *= 0.65 + 0.35 * abs(pulse)

            mix = math.tanh(sample * env * 1.2)
            value = int(clamp(mix, -1.0, 1.0) * 32767)
            wf.writeframes(struct.pack("<h", value))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--game-id", required=True)
    ap.add_argument("--type", required=True)
    ap.add_argument("--role", default="")
    ap.add_argument("--tag", default="default")
    ap.add_argument("--library-root", default="shared_assets/audio")
    ap.add_argument("--seconds", type=int, default=16)
    ap.add_argument("--sample-rate", type=int, default=44100)
    ap.add_argument("--assign-project", default="")
    args = ap.parse_args()

    game_id = args.game_id.strip()
    audio_type = args.type.strip().lower()
    role = (args.role.strip().lower() or ("play" if audio_type == "bgm" else "generic"))
    tag = args.tag.strip().lower()
    library_root = Path(args.library_root)

    if audio_type not in ("bgm", "sfx"):
        raise SystemExit("Unsupported audio type")

    ensure_audio_root(library_root)
    seed = hash_seed(f"{game_id}:{audio_type}:{role}:{tag}")
    unique_token = f"{seed:08x}"
    entry_id = build_entry_id(audio_type, role, tag, unique_token)
    file_name = f"{entry_id}.wav"
    relative_path = build_library_path(audio_type, file_name)
    output_path = library_root / relative_path
    output_path.parent.mkdir(parents=True, exist_ok=True)

    if not output_path.exists():
        if audio_type == "bgm":
            synth_bgm_wav(output_path, args.seconds, args.sample_rate, seed)
        else:
            synth_sfx_wav(output_path, role, args.sample_rate, seed)

    index_path = library_root / "index.json"
    data = load_index(index_path)
    existing_ids = {item.get("id", "") for item in data.get("audio", []) if isinstance(item, dict)}

    if entry_id not in existing_ids:
        data.setdefault("audio", []).append({
            "id": entry_id,
            "file": relative_path.replace("\\", "/"),
            "type": audio_type,
            "role": role,
            "tags": [tag],
            "style": tag,
            "loop": audio_type == "bgm",
            "duration_sec": duration_for_seconds(args.seconds if audio_type == "bgm" else sfx_defaults(role)["seconds"]),
            "used_by": [],
            "source": "synth_audio",
            "retrieved_at": datetime.date.today().isoformat(),
            "license": "",
            "license_url": "",
            "generated": True,
            "format": detect_format(file_name),
        })

    entry = next(item for item in data.get("audio", []) if item.get("id", "") == entry_id)

    if args.assign_project:
        dst = copy_entry_to_project(entry, library_root, Path(args.assign_project))
        append_used_by(entry, game_id)
        print(f"AUDIO_ASSIGNED_PROJECT={args.assign_project}")
        print(f"AUDIO_TARGET={dst.as_posix()}")

    save_index(index_path, data)
    print(f"AUDIO_SYNTH_ID={entry_id}")
    print(f"AUDIO_SYNTH_FILE={relative_path}")


if __name__ == "__main__":
    main()
