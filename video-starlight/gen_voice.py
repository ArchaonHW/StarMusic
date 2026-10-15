import asyncio
import edge_tts

SEGMENTS = [
    ("seg1", "這是一座穿越時光隧道的幻影劇院。"),
    ("seg2", "時間，在這個名為星光劇院的空間裡，有了不同的定義。"),
    ("seg3", "當你看到我發出的光，我或許已經不存在。"),
    ("seg4", "星光劇院的那些人，那些事，已不是眼前真實的存在。"),
    ("seg5", "而是如宇宙中已旅行億萬公里的星光，乍現。"),
]

VOICE = "zh-TW-YunJheNeural"  # 低沉男聲
RATE = "-12%"
PITCH = "-5Hz"

async def main():
    for name, text in SEGMENTS:
        tts = edge_tts.Communicate(text, VOICE, rate=RATE, pitch=PITCH)
        await tts.save(f"voice_{name}.mp3")
        print(name, "done")

asyncio.run(main())
