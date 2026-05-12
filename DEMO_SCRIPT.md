# GyanSetu — Demo Video Script (90 seconds)

A shot-by-shot breakdown of the hackathon demo video. Aim for **90 seconds total**
(judges watch a lot of videos — keep it tight). All Gujarati voice-overs include
an English equivalent in parentheses you can read instead.

## Equipment

- **Android phone**: Pixel 7+ / OnePlus / any 6 GB-RAM device with API 26+
- **Screen recorder**: Android's built-in (Quick Settings → Screen record), or scrcpy
  + OBS on a laptop for cleaner output
- **Audio**: built-in mic is fine; record VO separately and overlay in editing
- **Editor**: DaVinci Resolve (free), iMovie, or CapCut

## Pre-recording checklist

- [ ] Sideload `gemma-4-E2B-it.litertlm` (~2.5 GB) into `/storage/emulated/0/Android/data/com.gyansetu/files/` — model already present so you skip the download bar
- [ ] Phone in airplane mode — visible in the status bar, kills the "wait, is it cheating with cloud?" question
- [ ] Volume up — TTS is part of the demo
- [ ] Stars / streak persisted from a couple of practice quiz runs (looks more lived-in)
- [ ] Object on the desk for the camera scan — a printed picture of a cow or a real fruit is fine; a stuffed animal works great
- [ ] Charge above 50% so the battery icon isn't a distraction
- [ ] Disable system notifications

## Storyboard

| t (sec) | What's on screen | Voice-over (Gujarati / English) | Production notes |
|---|---|---|---|
| 0:00–0:05 | **Title card**: orange gradient, owl mascot, "GyanSetu — જ્ઞાનસેતુ" centered. Subtitle "AI tutor that works without internet." | "Meet GyanSetu — the offline AI tutor for primary school children in rural India." | 5-second hold. Music: light, upbeat (royalty-free). |
| 0:05–0:12 | **Stock footage** of a rural Indian classroom (Pexels/Pixabay search "rural school India" — credit in description). Cut to a B-roll of "no signal" Wi-Fi icon. | "250 million rural Indians live in connectivity-poor areas. Today's AI tutors can't reach them." | Stock B-roll with subtle dimming. Voice-over urgent but not bleak. |
| 0:12–0:18 | **Phone in hand**, airplane-mode icon visible in status bar. Cold-launch GyanSetu — splash screen plays bobbing-owl animation. | "GyanSetu runs Gemma 4 entirely on-device. No cloud, no subscriptions, no signal needed." | Hold the phone steady. The airplane-mode icon is the key visual proof. |
| 0:18–0:22 | Splash → tap "ચાલો શરૂ કરીએ!" → loading bar fills (already-warm Gemma model, finishes in ~3 seconds). Lands on Home with 5 chunky tiles. | "Model loads from local storage in under 5 seconds. Built on Google AI Edge LiteRT." | Speed up the loading bar 1.5× in the edit if it feels slow. |
| 0:22–0:35 | Tap **Ask** tile. Tap mic button. Speak in Gujarati: *"સૂર્ય શું છે?"* (What is the sun?). UI shows "🔴 સાંભળી રહ્યું છે · Listening…" Then "વિચારી રહ્યું છું…" Then a Gemma 4 answer streams in token-by-token in both languages. TTS reads it aloud. | "Voice-in goes straight to Gemma 4's native audio — no separate speech-to-text. The answer streams in Gujarati and English. Read aloud by the device's offline voice." | This is the **money shot**. Pause the music here so the streaming + TTS speaks for itself. |
| 0:35–0:42 | Same Ask flow, but ask: *"ગાય શું ખાય છે?"* (What does a cow eat?). When Gemma's response comes back, **highlight the yellow tool-chip** that appears: `🔧 query_syllabus(topic=animals, name=cow) → cow/ગાય; eats grass…` | "When questions need accurate curriculum data, GyanSetu acts as an agent — calling local tools backed by SQLite, no network." | **Zoom in 1.3× on the tool chip** during this cut so judges can read it. |
| 0:42–0:52 | Tap **Camera Scan** tile. Phone preview shows a real cow picture (or stuffed animal). Tap shutter button. Brief "🔍 ઓળખી રહ્યું છે…" overlay. Result card slides up: **COW · ગાય · /kaʊ/** with a one-line story. Tap "🔊 સાંભળો" — TTS reads it. | "Multimodal scan. Gemma 4 vision identifies the object, returns the bilingual name, phonetic pronunciation, and a story for the kid." | Use a clear, bright object on a plain background for highest recognition reliability. |
| 0:52–1:02 | Back to Home, tap **GK Quiz** (જ્ઞાન રમત). One question appears (e.g. "How many days in a week?"). Tap the correct answer (સાત / Seven). ✓ pops in. **Cut to results screen** with confetti and "3 / 5 ⭐". | "And it's playable. Quizzes, matching games, achievements — built around the curriculum, not the gimmicks." | Don't show all 5 questions — cut from question 1 directly to the results screen. |
| 1:02–1:12 | Cut to the **DEVPOST.md prize-track section** scrolling up, or four side-by-side hero shots of the screens. | "Built for Future of Education, Digital Equity, and the LiteRT prize tracks. Apache 2.0. Fully open source." | Use the screenshots in `docs/`. |
| 1:12–1:25 | **Closing card**: GyanSetu logo, owl mascot waving, three URLs: GitHub, Devpost, model download. Subtitle: "Apache 2.0 · CC-BY 4.0 · Made for rural Gujarat." | "GyanSetu. The bridge between knowledge and the children who need it most. Try it now." | Mascot does the wing-flap animation from the splash. Music swells slightly, then fades. |
| 1:25–1:30 | **Black** with author credit (`Pratik Patel · @pratik227`) + repo URL (`github.com/pratik227/gyansetu`) + Devpost URL. | (silence) | Hold for 5s for slow readers. |

## Voice-over recording tips

- Record in a closet or under a blanket for best vocal isolation
- Use a phone with a Voice Memos app — Apple's is fine, Samsung's Voice Recorder is fine
- Record 2 takes per line; you'll thank yourself in editing
- Speak **one word slower than you think you should**
- Bilingual narration is a strength, not a weakness — say each line in Gujarati then translate, OR Gujarati for the kid-facing screens and English for the technical sections

## What NOT to do in the video

- ❌ Don't say "ChatGPT" or other competitors by name; let the offline angle speak for itself
- ❌ Don't show the model download bar (sideload the model first)
- ❌ Don't film with the phone propped against a coffee cup — get a tripod, even a $10 phone holder is fine
- ❌ Don't use copyrighted music — Pixabay Music, Free Music Archive, YouTube Audio Library are all free
- ❌ Don't overshoot 90 seconds — judges literally have a hundred to watch

## Backup: 30-second teaser version

If the main video is delayed, ship this short version first:

| t | Shot |
|---|---|
| 0:00–0:03 | Title card |
| 0:03–0:08 | Airplane mode icon + cold launch + loading bar |
| 0:08–0:18 | Voice question in Gujarati → streaming answer + TTS |
| 0:18–0:24 | Camera scan → bilingual result card |
| 0:24–0:30 | Closing logo + "GyanSetu — Gemma 4 offline AI tutor" + URL |

## File deliverables

- `gyansetu-demo.mp4` — 90-second main video, 1080×1920 (vertical) or 1920×1080 (landscape, preferred for Devpost)
- `gyansetu-teaser.mp4` — optional 30-second teaser (vertical for X / Reels)
- `cover.png` — Devpost video thumbnail, 1280×720, owl mascot + "GyanSetu" + "Offline AI tutor"

## Music suggestions (royalty-free)

- "Inspiring Cinematic Ambient" — Pixabay
- "Ukulele Happy Whistling" — Pixabay (good for the "kid-friendly" feel)
- "Tech Background" — YouTube Audio Library

Keep music quiet under VO (–18 dB), louder during transitions (–8 dB), full during the closing card (–4 dB).
