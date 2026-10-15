# -*- coding: utf-8 -*-
"""Starlight Theater — procedural 7-shot renderer. Pipes frames to ffmpeg."""
import numpy as np
import subprocess, math, os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H, FPS = 1280, 720, 24
CX, CY = W / 2, H / 2
rng = np.random.default_rng(42)

FONT_PATH = "C:/Windows/Fonts/msjh.ttc"
_fonts = {}
def font(sz):
    if sz not in _fonts:
        _fonts[sz] = ImageFont.truetype(FONT_PATH, sz)
    return _fonts[sz]

# ---------- base gradient (deep indigo cosmos) ----------
_yy = np.linspace(0, 1, H)[:, None]
_xx = np.linspace(0, 1, W)[None, :]
BASE = np.zeros((H, W, 3), np.float32)
BASE[..., 0] = 10 + 14 * _yy                      # R
BASE[..., 1] = 8 + 10 * _yy                       # G
BASE[..., 2] = 30 + 26 * _yy                      # B
BASE += (12 * np.exp(-((_xx - 0.5) ** 2 + (_yy - 0.42) ** 2) / 0.18))[..., None]  # center nebula
DX, DY = np.meshgrid(np.arange(W) - CX, np.arange(H) - CY)
R2 = DX ** 2 + DY ** 2

# ---------- subtitle schedule (global timeline) ----------
SUBS = [
    (1.0, 5.6, "這是一座穿越時光隧道的幻影劇院"),
    (10.0, 16.0, "時間，在這個名為星光劇院的空間裡，有了不同的定義"),
    (29.0, 34.0, "當你看到我發出的光，我或許已經不存在"),
    (44.0, 50.5, "星光劇院的那些人，那些事，已不是眼前真實的存在"),
    (51.0, 56.4, "而是如宇宙中已旅行億萬公里的星光，乍現"),
]

def pipe(out):
    return subprocess.Popen([
        "ffmpeg", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24",
        "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "18",
        "-preset", "medium", "-loglevel", "error", out], stdin=subprocess.PIPE)

def emit(proc, img):
    proc.stdin.write(np.clip(img, 0, 255).astype(np.uint8).tobytes())

def splat(img, xs, ys, cols):
    ix = np.clip(np.round(xs).astype(np.int32), 0, W - 1)
    iy = np.clip(np.round(ys).astype(np.int32), 0, H - 1)
    for ch in range(3):
        np.add.at(img[..., ch], (iy, ix), cols[:, ch])

def screen(img, ov):
    img[:] = 255.0 - (255.0 - img) * (255.0 - ov) / 255.0

def blur_screen(img, layer, radius):
    if layer.max() <= 0:
        return
    ov = Image.fromarray(np.clip(layer, 0, 255).astype(np.uint8))
    ov = ov.filter(ImageFilter.GaussianBlur(radius))
    screen(img, np.asarray(ov).astype(np.float32))

def text_overlay(img, txt, y, size, alpha=1.0, color=(255, 240, 210)):
    if alpha <= 0:
        return
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    f = font(size)
    tw = d.textlength(txt, font=f)
    d.text(((W - tw) / 2, y), txt, font=f, fill=(*color, 255))
    glow = ov.filter(ImageFilter.GaussianBlur(8))
    ov = Image.alpha_composite(glow, ov)
    a = np.asarray(ov)[..., 3:4].astype(np.float32) / 255.0 * alpha
    rgb = np.asarray(ov)[..., :3].astype(np.float32)
    img[:] = img * (1 - a) + rgb * a

def draw_subs(img, t_global):
    for st, en, txt in SUBS:
        if st - 0.4 <= t_global <= en + 0.4:
            a = min(1.0, max(0.0, (t_global - st) / 0.4), max(0.0, (en - t_global) / 0.4))
            text_overlay(img, txt, 640, 34, alpha=a)

def fade_env(t_local, dur):
    return min(1.0, t_local / 0.5, max(0.0, (dur - t_local) / 0.5))

# ---------- shared assets ----------
# static background stars
N_BG = 260
bs_x = rng.uniform(0, W, N_BG); bs_y = rng.uniform(0, H, N_BG)
bs_c = rng.uniform(0.25, 0.9, (N_BG, 3)) * np.array([200, 210, 255])
bs_ph = rng.uniform(0, 6.28, N_BG)

def bg_stars(img, t, density=1.0):
    n = int(N_BG * density)
    b = 0.5 + 0.5 * np.sin(1.3 * t + bs_ph[:n])
    splat(img, bs_x[:n], bs_y[:n], bs_c[:n] * b[:, None])

# theater silhouette target points (built once)
def build_theater():
    m = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(m)
    cx = CX
    d.polygon([(cx - 260, 300), (cx + 260, 300), (cx, 200)], fill=255)      # pediment
    d.rectangle([cx - 280, 300, cx + 280, 330], fill=255)                   # entablature
    for i in range(5):                                                    # columns
        x = cx - 240 + i * 120
        d.rectangle([x - 14, 330, x + 14, 520], fill=255)
    d.rectangle([cx - 300, 520, cx + 300, 545], fill=255)                   # base
    d.rectangle([cx - 320, 545, cx + 320, 562], fill=255)
    d.rectangle([cx - 90, 330, cx + 90, 430], fill=255)                     # marquee band
    # marquee letters (separate mask for staggered glow)
    lm = Image.new("L", (W, H), 0)
    ld = ImageDraw.Draw(lm)
    f = font(44)
    txt = "星光劇院"
    tw = ld.textlength(txt, font=f)
    ld.text((cx - tw / 2, 355), txt, font=f, fill=255)
    arr = np.asarray(m) - np.asarray(lm)
    ys, xs = np.where(arr > 128)
    sel = rng.choice(len(xs), size=min(3200, len(xs)), replace=False)
    tpts = np.stack([xs[sel], ys[sel]], axis=1).astype(np.float32)
    lys, lxs = np.where(np.asarray(lm) > 60)
    lpts = np.stack([lxs, lys], axis=1).astype(np.float32)
    letter_x_ranges = [(cx - tw / 2) + tw * i / 4 for i in range(5)]
    return tpts, lpts, letter_x_ranges, np.asarray(m), np.asarray(lm)

TPTS, LPTS, LXR, THEATER_MASK, LETTER_MASK = build_theater()

# ---------- Shot 1: light tunnel (9s) ----------
def shot1(proc, t0):
    n = 700
    ang = rng.uniform(0, 6.283, n)
    rad = rng.uniform(20, 240, n)
    z = rng.uniform(0.05, 1.0, n)
    cols = np.where(rng.random((n, 1)) < 0.7,
                    np.array([255, 220, 160]), np.array([160, 190, 255])).astype(np.float32)
    px_prev = np.full(n, CX); py_prev = np.full(n, CY)
    dur = 9.0
    overlay = Image.new("RGB", (W, H))
    for f in range(int(dur * FPS)):
        t = f / FPS; tg = t0 + t
        img = BASE.copy()
        z -= (0.55 + 0.35 * t / dur) / FPS
        ang += 0.25 / FPS
        proj = rad / np.maximum(z, 0.03)
        xs = CX + np.cos(ang) * proj
        ys = CY + np.sin(ang) * proj * 0.62
        fresh = (z <= 0.05) | (xs < -40) | (xs > W + 40) | (ys < -40) | (ys > H + 40)
        overlay_arr = np.zeros((H, W, 3), np.float32)
        od = ImageDraw.Draw(overlay)
        od.rectangle([0, 0, W, H], fill=0)
        b = np.clip(1.6 - z, 0.15, 1.4)
        for i in np.where(~fresh)[0]:
            od.line([px_prev[i], py_prev[i], xs[i], ys[i]], fill=tuple((cols[i] * b[i]).astype(int)), width=1)
        px_prev, py_prev = xs.copy(), ys.copy()
        k = fresh.sum()
        if k:
            ang[fresh] = rng.uniform(0, 6.283, k)
            rad[fresh] = rng.uniform(20, 240, k)
            z[fresh] = rng.uniform(0.85, 1.0, k)
            px_prev[fresh] = CX + np.cos(ang[fresh]) * rad[fresh]
            py_prev[fresh] = CY + np.sin(ang[fresh]) * rad[fresh] * 0.62
        blur_screen(img, np.asarray(overlay).astype(np.float32), 1.2)
        # growing center glow
        g = (90 + 200 * (t / dur)) * np.exp(-R2 / (2 * (60 + 90 * t / dur) ** 2))
        img += np.stack([g, g * 0.85, g * 0.55], axis=-1)
        bg_stars(img, tg, 0.4)
        draw_subs(img, tg)
        img *= fade_env(t, dur)
        emit(proc, img)

# ---------- Shot 2: theater condenses from particles (9s) ----------
def shot2(proc, t0):
    n = len(TPTS)
    start = np.stack([rng.uniform(0, W, n), rng.uniform(0, H, n)], axis=1)
    ph = rng.uniform(0, 6.28, n)
    jit = rng.uniform(3, 30, (n, 2))
    dur = 9.0
    letter_t = [5.6, 6.2, 6.8, 7.4]
    letter_done = np.zeros(4)
    for f in range(int(dur * FPS)):
        t = f / FPS; tg = t0 + t
        img = BASE.copy()
        bg_stars(img, tg)
        p = min(1.0, t / 5.5)
        p = p * p * (3 - 2 * p)  # smoothstep
        pos = start + (TPTS - start) * p + np.sin(2 * t + ph)[:, None] * jit * (1 - p) * 0.5
        layer = np.zeros((H, W, 3), np.float32)
        cols = np.tile(np.array([255, 200, 120], np.float32), (n, 1))
        cols[:, 2] += 60 * np.sin(3 * t + ph)
        splat(layer, pos[:, 0], pos[:, 1], cols * (0.4 + 0.8 * p))
        blur_screen(img, layer, 1.6)
        # silhouette glow rising after converge
        glow_a = min(1.0, max(0, (t - 4.5) / 3.0))
        if glow_a > 0:
            m = np.asarray(Image.fromarray(THEATER_MASK).filter(ImageFilter.GaussianBlur(10))).astype(np.float32)
            glow = np.stack([m * 0.55, m * 0.34, m * 0.12], -1) * glow_a
            screen(img, glow)
        # marquee letters flicker on
        for i in range(4):
            a = min(1.0, max(0, (t - letter_t[i]) / 0.5))
            flicker = a * (0.75 + 0.25 * math.sin(40 * t + i * 9)) if a < 1 else 1.0
            if flicker > 0:
                sel = (LPTS[:, 0] >= LXR[i]) & (LPTS[:, 0] < LXR[i + 1])
                l = np.zeros((H, W, 3), np.float32)
                splat(l, LPTS[sel, 0], LPTS[sel, 1],
                      np.tile(np.array([255, 232, 175], np.float32) * flicker * 1.5, (sel.sum(), 1)))
                blur_screen(img, l, 2.2)
        draw_subs(img, tg)
        img *= fade_env(t, dur)
        emit(proc, img)

# ---------- Shot 3: doors open, golden beam (9s) ----------
def shot3(proc, t0):
    n = 170
    mx = rng.uniform(0, 1, n); my = rng.uniform(0, H, n)
    msp = rng.uniform(6, 20, n); mph = rng.uniform(0, 6.28, n)
    dur = 9.0
    for f in range(int(dur * FPS)):
        t = f / FPS; tg = t0 + t
        img = BASE.copy() * 0.5
        p = min(1.0, t / 4.0); p = p * p * (3 - 2 * p)
        gap = 8 + 150 * p                      # door gap half-width
        sigma = gap * 0.9
        # golden beam: horizontal gaussian through gap, vertical falloff downward
        beam = np.exp(-(DX ** 2) / (2 * sigma ** 2)) * np.exp(-((DY + 160) ** 2) / (2 * 380 ** 2))
        beam = beam * (140 + 240 * p)
        img += np.stack([beam, beam * 0.72, beam * 0.32], -1)
        # door panels: darken sides, amber rim at edges
        edge = np.abs(np.abs(DX) - gap)
        rim = np.exp(-edge ** 2 / (2 * 6 ** 2)) * 130 * p
        img += np.stack([rim, rim * 0.6, rim * 0.2], -1)
        dark = (np.abs(DX) > gap).astype(np.float32)
        img *= (1 - 0.55 * dark[..., None])
        # dust motes in beam
        yy = (my + msp * t) % H
        xx = CX + (mx - 0.5) * gap * 3.4 + 14 * np.sin(0.8 * t + mph)
        tw = 0.5 + 0.5 * np.sin(3 * t + mph)
        layer = np.zeros((H, W, 3), np.float32)
        splat(layer, xx, yy, np.tile(np.array([255, 225, 170], np.float32), (n, 1)) * tw[:, None] * (30 + 90 * p) / 130)
        blur_screen(img, layer, 1.4)
        bg_stars(img, tg, 0.15)
        draw_subs(img, tg)
        img *= fade_env(t, dur)
        emit(proc, img)

# ---------- Shot 4: phantom audience + stage (11s) ----------
def shot4(proc, t0):
    n_orb = 46
    ox = rng.uniform(60, W - 60, n_orb); oy = rng.uniform(H * 0.52, H * 0.94, n_orb)
    orad = rng.uniform(9, 24, n_orb); oph = rng.uniform(0, 6.28, n_orb)
    oper = rng.uniform(4, 9, n_orb)
    n_dust = 220
    dx_ = rng.uniform(0, W, n_dust); dy_ = rng.uniform(0, H, n_dust)
    dsp = rng.uniform(10, 34, n_dust)
    n_d = 10
    dur = 11.0
    for f in range(int(dur * FPS)):
        t = f / FPS; tg = t0 + t
        img = BASE.copy() * 0.6
        bg_stars(img, tg, 0.5)
        layer = np.zeros((H, W, 3), np.float32)
        # spotlight cones to stage
        cones = Image.new("RGB", (W, H), 0)
        cd = ImageDraw.Draw(cones)
        cd.polygon([(CX - 320, 0), (CX - 60, 0), (CX + 40, H * 0.45), (CX - 180, H * 0.45)], fill=(46, 40, 26))
        cd.polygon([(CX + 320, 0), (CX + 60, 0), (CX - 40, H * 0.45), (CX + 180, H * 0.45)], fill=(46, 40, 26))
        blur_screen(img, np.asarray(cones).astype(np.float32), 14)
        # stage glow (pulsing ellipse)
        e = np.exp(-(((DX / 300) ** 2 + ((DY + H * 0.31) / 90) ** 2)))
        pulse = 0.75 + 0.25 * math.sin(1.6 * t)
        img += np.stack([e * 110 * pulse, e * 62 * pulse, e * 26 * pulse], -1)
        # dancer: particle cluster orbiting stage center
        for i in range(n_d):
            a = t * (1.3 + i * 0.07) + i * 0.63
            rx = 46 + 26 * math.sin(0.9 * t + i)
            xs = CX + math.cos(a) * rx
            ys = H * 0.40 + math.sin(a) * rx * 0.42 - 8 * abs(math.sin(a * 1.4))
            s = np.zeros((H, W, 3), np.float32)
            splat(s, [xs], [ys], np.array([[300, 240, 170]], np.float32))
            blur_screen(img, s, 3.2)
        # audience bokeh orbs appearing / fading (phantoms)
        orbs = Image.new("RGB", (W, H), 0)
        od = ImageDraw.Draw(orbs)
        for i in range(n_orb):
            ph2 = (t + oph[i]) % oper[i]
            a = min(1.0, ph2 / 1.2, (oper[i] - ph2) / 1.2)
            if a <= 0:
                continue
            sway = 6 * math.sin(0.6 * t + oph[i])
            c = int(70 * a * (0.7 + 0.3 * math.sin(2 * t + oph[i])))
            od.ellipse([ox[i] + sway - orad[i], oy[i] - orad[i], ox[i] + sway + orad[i], oy[i] + orad[i]],
                       fill=(c, int(c * 0.8), int(c * 0.45)))
        blur_screen(img, np.asarray(orbs).astype(np.float32), 6)
        # rising dust
        yy = (dy_ - dsp * t) % H
        tw = 0.4 + 0.6 * np.abs(np.sin(1.7 * t + dx_))
        splat(layer, dx_, yy, np.tile(np.array([200, 190, 160], np.float32), (n_dust, 1)) * tw[:, None] * 0.35)
        blur_screen(img, layer, 1.0)
        draw_subs(img, tg)
        img *= fade_env(t, dur)
        emit(proc, img)

# ---------- Shot 5: bright one shatters into rising light (8s) ----------
def shot5(proc, t0):
    n = 1600
    burst = False
    px = np.zeros(n); py = np.zeros(n); vx = np.zeros(n); vy = np.zeros(n)
    pcol = np.zeros((n, 3)); plife = np.zeros(n)
    dur = 8.0; tb = 3.6
    for f in range(int(dur * FPS)):
        t = f / FPS; tg = t0 + t
        img = BASE.copy()
        bg_stars(img, tg, 0.35)
        if t < tb:
            # pulsing warm orb
            br = 1.0 + 0.25 * math.sin(5 * t)
            sig = 34 + 8 * math.sin(2.2 * t)
            g = 150 * br * np.exp(-R2 / (2 * sig ** 2))
            g2 = 90 * br * np.exp(-R2 / (2 * (sig * 3.2) ** 2))
            img += np.stack([g + g2, g * 0.82 + g2 * 0.6, g * 0.5 + g2 * 0.3], -1)
        else:
            if not burst:
                burst = True
                px[:] = CX + rng.normal(0, 30, n)
                py[:] = CY + rng.normal(0, 30, n)
                sp = rng.uniform(40, 320, n)
                ang = rng.uniform(-3.14, 0, n)          # upward hemisphere
                vx = np.cos(ang) * sp * 0.55
                vy = np.sin(ang) * sp
                pcol = np.where(rng.random((n, 1)) < 0.65,
                                np.array([255, 215, 150]), np.array([170, 200, 255])).astype(np.float32)
                plife = rng.uniform(2.2, 4.5, n)
            dt = 1 / FPS
            tt = t - tb
            vy -= 130 * dt                              # accelerate upward
            px += vx * dt; py += vy * dt
            life = np.clip(1 - tt / plife, 0, 1)
            twk = 0.6 + 0.4 * np.sin(22 * t + px)
            layer = np.zeros((H, W, 3), np.float32)
            splat(layer, px, py, pcol * (life * twk)[:, None] * 2.2)
            blur_screen(img, layer, 1.1)
            flash = 230 * math.exp(-tt * 5.0) * np.exp(-R2 / (2 * 95 ** 2))
            img += np.stack([flash, flash * 0.85, flash * 0.5], -1)
            after = max(0.0, 1 - tt / 2.2) * 46
            g = after * np.exp(-R2 / (2 * 120 ** 2))
            img += np.stack([g, g * 0.7, g * 0.35], -1)
        draw_subs(img, tg)
        img *= fade_env(t, dur)
        emit(proc, img)

# ---------- Shot 6: theater shrinks into a star (8s) ----------
def shot6(proc, t0):
    n = len(TPTS)
    ph = rng.uniform(0, 6.28, n)
    dur = 8.0
    for f in range(int(dur * FPS)):
        t = f / FPS; tg = t0 + t
        img = BASE.copy()
        p = min(1.0, t / 5.2); p = p * p * (3 - 2 * p)
        s = 1.0 + (0.015 - 1.0) * p                     # shrink to ~1.5%
        cx = CX + (CX - CX) * p
        px = CX + (TPTS[:, 0] - CX) * s
        py = CY + 30 + (TPTS[:, 1] - (CY + 30)) * s
        layer = np.zeros((H, W, 3), np.float32)
        tw = 0.7 + 0.3 * np.sin(6 * t + ph)
        cols = np.tile(np.array([255, 205, 130], np.float32), (n, 1)) * tw[:, None]
        splat(layer, px, py, cols * (1.1 - 0.6 * p))
        blur_screen(img, layer, 1.4 + 2.0 * p)
        # emerges as a star with flare
        star_a = min(1.0, max(0, (t - 4.4) / 1.2))
        if star_a > 0:
            g = 120 * star_a * np.exp(-R2 / (2 * 26 ** 2))
            flare = 60 * star_a * np.exp(-(np.abs(DX) + np.abs(DY)) / 26)
            img += np.stack([g + flare, (g + flare) * 0.8, (g + flare) * 0.45], -1)
        bg_stars(img, tg, 0.35 + 0.65 * p)
        draw_subs(img, tg)
        img *= fade_env(t, dur)
        emit(proc, img)

# ---------- Shot 7: star flickers out, title card (6s) ----------
def shot7(proc, t0):
    dur = 6.0
    for f in range(int(dur * FPS)):
        t = f / FPS; tg = t0 + t
        img = BASE.copy() * 0.5
        bg_stars(img, tg, 0.7)
        # star pulses twice then dies by t=3.0
        if t < 3.0:
            br = (0.55 + 0.45 * math.sin(9 * t)) * max(0.0, 1 - t / 3.2)
            g = 150 * br * np.exp(-R2 / (2 * 22 ** 2))
            fl = 70 * br * np.exp(-(np.abs(DX) + np.abs(DY)) / 30)
            img += np.stack([g + fl, (g + fl) * 0.82, (g + fl) * 0.5], -1)
            img *= (0.25 + 0.75 * br)
        # title fades in
        ta = min(1.0, max(0, (t - 2.6) / 1.4))
        if ta > 0:
            text_overlay(img, "星 光 劇 院", 300, 74, alpha=ta)
            sa = min(1.0, max(0, (t - 3.6) / 1.4))
            if sa > 0:
                text_overlay(img, "穿越時光的光影，為你而亮", 410, 30, alpha=sa,
                             color=(190, 200, 235))
        img *= fade_env(t, dur)
        emit(proc, img)

SHOTS = [
    ("shot1.mp4", shot1, 0.0),
    ("shot2.mp4", shot2, 9.0),
    ("shot3.mp4", shot3, 18.0),
    ("shot4.mp4", shot4, 27.0),
    ("shot5.mp4", shot5, 38.0),
    ("shot6.mp4", shot6, 46.0),
    ("shot7.mp4", shot7, 54.0),
]

if __name__ == "__main__":
    import sys
    only = sys.argv[1] if len(sys.argv) > 1 else None
    for name, fn, t0 in SHOTS:
        if only and only not in name:
            continue
        print(f"rendering {name} ...", flush=True)
        p = pipe(name)
        fn(p, t0)
        p.stdin.close(); p.wait()
        print(f"  done {name}")
    print("all done")
