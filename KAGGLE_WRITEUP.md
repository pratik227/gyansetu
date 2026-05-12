# GyanSetu — જ્ઞાનસેતુ

### The first AI tutor that actually works in rural Gujarat — because it doesn't need the internet.

> **Tracks targeted:** LiteRT Prize · Future of Education · Digital Equity & Inclusivity · Main Track
>
> **Status:** On-device, offline, bilingual (and 5-language ready), multimodal — running **Gemma 4 E2B/E4B** via Google AI Edge **LiteRT-LM** on a $100 Android phone.

🎬 **Demo video:** [`docs/gyansetu-demo.mp4`](docs/gyansetu-demo.mp4) (50-second walkthrough)
🎞 **Teaser GIF:** [`docs/gyansetu-teaser.gif`](docs/gyansetu-teaser.gif)
🌐 **Web demo (PWA):** open [`index.html`](index.html) in any mobile browser → "Add to Home Screen"
📱 **Android APK:** [`android/`](android/) — Kotlin/Compose, ~6,350 LOC, LiteRT-LM direct integration
📜 **License:** CC-BY 4.0 (per §2.5 Winner License)

![Splash](docs/01-splash.png) ![Home](docs/02-home.png) ![Topics](docs/03-topics.png)

---

## TL;DR

A bilingual (Gujarati ↔ English, extensible to Hindi/Marathi/Tamil) on-device learning companion for primary school children (Std 1–5). Point the camera at any object and Gemma 4's multimodal vision identifies it in both languages. Speak a question in Gujarati and Gemma 4's native audio modality answers — no separate speech-to-text. A real **multi-tool agent** chains up to three on-device tools per turn against a local SQLite syllabus. A **Teacher Mode** with multi-student rosters, mastery matrices, and Bluetooth-shareable progress reports empowers the educator without ever touching the cloud.

Everything runs through `com.google.ai.edge.litertlm:litertlm-android` — the **LiteRT-LM runtime**, not the MediaPipe Tasks GenAI wrapper. We picked the runtime that matches the format Google ships Gemma 4 in on day one.

---

## The problem

In rural Gujarat, a Std-1 child enters school with two languages — Gujarati at home, English at school. They have a curriculum (the GSEB syllabus), a teacher who is often shared across grades, and **no internet**. The latest leap in education technology is generative-AI tutors. **None of them work for this child.**

- ChatGPT needs a connection.
- Khan Academy's Khanmigo needs a subscription.
- Even Google's own consumer assistants assume always-online.

We grew up watching cousins struggle with English vocabulary because nobody at home spoke it, and watching teachers stretched too thin to give every kid the patient repetition they need to learn pronunciation, build connections between concepts, and feel encouraged.

We wanted to build the AI tutor that **actually shows up** for that child — one that speaks their language, fits in their pocket, and never asks them to "check your connection."

**Gemma 4's E2B model finally makes that possible.** It runs on a 4 GB-RAM Android phone, supports Gujarati natively (140+ languages), takes audio and image input, and ships under Apache 2.0. We built around it.

---

## What it does

**Five core flows**, all in bilingual Gujarati + English (and extensible to Hindi, Marathi, Tamil):

### 1. 📷 Camera Scan
Point the phone at any object. Gemma 4's multimodal vision returns:
- The English name
- The Gujarati translation
- The IPA pronunciation
- A one-line story for the child to learn from

Real `getUserMedia` viewfinder, real frame capture, real Gemma 4 `addImage(Bitmap)` inference — no cloud round-trip.

### 2. 🎙 Ask
The child speaks a question (in Gujarati or English) or types it. Audio goes **directly to Gemma 4's native audio modality** (no separate speech-to-text). The model answers in both languages. The Android app **streams tokens live** so the kid sees the answer appearing as it's thought, then `TextToSpeech` reads it aloud.

### 3. 🧠 Multi-tool agent
When a question needs grounded curriculum data, Gemma 4 chooses from **eight on-device tools**:

| Tool | What it does |
|---|---|
| `query_syllabus(topic, name?)` | Bilingual lookups in the local GSEB curriculum |
| `get_pronunciation(word)` | IPA phonetic for English terms |
| `get_gk_fact(topic)` | One-line bilingual GK facts |
| `generate_quiz(topic, difficulty)` | Gemma 4 generates a custom 3-question quiz on demand |
| `explain_simpler(concept)` | Re-explains at younger-grade vocabulary (Std 1-2 / 3-4 / 5) |
| `find_weak_topics()` | Surfaces lowest-mastery topics from per-student data |
| `mark_homework(answer, expected)` | Graded with bilingual encouraging feedback |
| `draw_diagram(concept)` | Emoji/ASCII diagrams for water cycle, food chain, plants, solar system, digestion |

The runtime executes the tool against a local Room SQLite, injects the result, the model continues. Up to **3 chained tool calls per turn**.

**Tool calls are shown as inline yellow chips in the UI** so teachers can see how the agent reasoned and correct misconceptions in real time. Empowers, doesn't replace.

### 4. 🧩 Match Pairs
Drag/tap pairs (Gujarati ↔ English) sourced from the local syllabus. Earns stars; kids learn vocabulary by spotting visual associations.

### 5. 🏆 Quiz
10-question pool (national bird, capital of Gujarat, days of the week, …), 5 random per session. Streak tracking, perfect-score celebration with confetti. **Adaptive**: every answer updates per-topic mastery (0–10 score). The next session weights questions toward the weakest topics.

Plus a Dashboard with progress + 8 unlockable achievement badges, a Settings screen (language toggle, font size, sound, reset), and a friendly owl mascot named **Hooty** who's a constant presence in the UI.

![Animals](docs/04-animals.png) ![Quiz](docs/05-quiz.png) ![Ask](docs/06-ask.png) ![Match](docs/07-match.png)

---

## Real-world impact at scale

| Metric | Number | Source |
|---|---|---|
| Std 1-5 children speaking the 5 supported languages | **120 M+** | Census of India 2011 + state primary-enrolment registers |
| Rural Indians in connectivity-poor (≤2-bar 4G) zones | **250 M** | TRAI 2024 rural broadband report |
| Primary schools without persistent internet (Gujarat) | **~67%** | Gujarat State Education Dept. 2023 ICT survey |
| Cost per query (cloud LLM tutor) | $0.001–$0.005 | OpenAI / Anthropic standard pricing |
| **Cost per query (GyanSetu)** | **$0.000** | On-device. Forever. |
| One-time model download | ~2 GB | Wi-Fi at home, school, or community centre |
| Cold-start to ready (Pixel 7a) | ~3 s | Gemma 4 E2B int4 + LiteRT GPU delegate |

The cost difference **compounds**. A single rural school running 200 kids × 30 questions/day × 200 school days = 1.2 M queries/year. That's **$1,200/year/school on cloud LLM tutoring vs $0 on GyanSetu** — and GyanSetu works through monsoon-season blackouts that take cell towers offline for weeks.

### Research backing

- **UNESCO 2024 GEM report** on AI in education flags offline / low-resource deployment as the single largest gap between AI-tutoring research and field reality
- **NEP 2020 (India)** mandates "multilingual technology-supported instruction" for primary grades — federal alignment with what GyanSetu delivers
- **NCERT studies** on bilingual instruction show measurable improvement in L2 (English) vocabulary retention when L1 (mother tongue) and L2 are paired in the same lesson — the exact format GyanSetu uses

---

## How we built it

Two-track delivery, single repository.

### Track A — Web demo / PWA (fastest path to "playable today")

A single-file `index.html` (~134 KB) that opens directly in any browser:

- React via CDN, JSX via Babel-standalone
- All 9 screens, 60+ KB of bundled bilingual knowledge base
- **14 inline-SVG icons** (zero icon-CDN dependency)
- Web Speech for TTS + STT, `getUserMedia` for camera, `localStorage` for persistence, Web Audio API for procedural feedback chimes
- PWA manifest + service worker → installable on any Android phone, **fully offline after first load**

Object recognition in the web demo is intentionally a deterministic stub that cycles through the bundled KB — full Gemma multimodal recognition lives in the native track.

### Track B — Native Android with on-device Gemma 4

Kotlin + Jetpack Compose + Room SQLite + DataStore + CameraX + Google AI Edge LiteRT-LM (`com.google.ai.edge.litertlm:litertlm-android:0.10.2`). **22 Kotlin files, ~6,350 LOC.**

```
SplashScreen ──► LoadingScreen ──► HomeScreen ──► (5 feature screens)
                  │                                        │
                  └─ AppViewModel ──┬── EngineState flow ──┘
                                    ├── Settings (DataStore)
                                    ├── ScanState (camera result)
                                    └── AskResponse (streaming + tool steps)
                                                │
                                   GemmaInferenceEngine ──► LiteRT-LM
                                                │
                                                ├── addImage()  (camera scan)
                                                ├── addAudio()  (Gemma 4 native speech-in)
                                                └── generateResponseAsync (streaming)
                                                │
                                   ToolRegistry ──► Room SQLite (GSEB syllabus)
```

The on-device Gemma model (`gemma-4-E2B-it.litertlm`, ~2.5 GB) is downloaded once on first launch with progress shown in the LoadingScreen. After that, **the app runs fully offline**. The `.task` file in the same Hugging Face repo is the MediaPipe Web JS bundle and is intentionally not used on Android.

**Model:** [`litert-community/gemma-4-E2B-it-litert-lm`](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) — the Google AI Edge team's quantised on-device build of `google/gemma-4-E2B-it`. **Apache 2.0.**

---

## Why LiteRT-LM, not MediaPipe Tasks GenAI

This is the technical decision the LiteRT Prize is asking about, and it's where most on-device Gemma demos take the easier path. We didn't.

Every line of Gemma interaction in this submission flows through `com.google.ai.edge.litertlm:litertlm-android:0.10.2` — the **lower-level Google AI Edge runtime**, not the MediaPipe `tasks-genai` wrapper.

### The why

The Gemma 4 model files Google publishes (`litert-community/gemma-4-*-litert-lm` on Hugging Face) use the **LiteRT-LM proprietary format** (magic header `LITERTLM`), **not** the zip-bundle `.task` format that `tasks-genai` loads — the `.task` file in those repos is the **MediaPipe Web JS** packaging.

Most teams ship the `tasks-genai` route and run an older Gemma. We picked the runtime that matches the format Google ships on day one, so we get the latest Gemma 4 E2B/E4B weights without waiting for the wrapper to catch up. The `GemmaInferenceEngine.kt` header comment documents this trade-off in full so the next team won't waste a day on it.

### What we got for it

| Feature | What we did | Why it matters |
|---|---|---|
| **GPU/CPU smart selection** | `hasEnoughRam()` picks GPU backend on phones ≥6 GB RAM; automatic CPU fallback on init failure; backend exposed in live debug HUD (`lastBackend`) | Judges can re-run on their hardware and see LiteRT-LM making the decision live |
| **Single-conversation lifecycle done right** | LiteRT-LM allows only one active conversation per `Engine`. A second `createConversation` while one is alive throws `FAILED_PRECONDITION`. `GemmaInferenceEngine` enforces this with a `Mutex`-guarded conversation slot | The kind of detail that crashes naive integrations under load |
| **Multimodal end-to-end** | `addImage(Bitmap)` powers the camera-scan flow; `addAudio(16 kHz mono 16-bit PCM)` lets the kid ask in Gujarati audio with no separate STT | Real Gemma 4 native modalities, not pre-processed via a separate model |
| **Streaming tokens** | `Flow<String>` from `generateResponseAsync` straight to UI | Live answer appearance — kids stay engaged |

### Built-in reproducible benchmark

A dedicated `BenchmarkScreen` runs three Gemma 4 prompts and reports first-token latency, throughput (tok/s), peak heap usage, and the chosen backend. **Reproducible on any device** — judges can re-run on their own hardware.

**Measured on a 6 GB-RAM Android test device, GPU backend, `gemma-4-E2B-it.litertlm`:**

| Run | First token | Total | Tokens | tok/s | Heap delta |
|---|---|---|---|---|---|
| "Why is the sky blue?" | **1,555 ms** | 11,442 ms | 61 | **5.3** | 1 MB |
| "Write a 2-sentence story about a cow." | **772 ms** | 13,891 ms | 72 | **5.2** | 0 MB |
| "What is the capital of Gujarat?" | **905 ms** | 6,593 ms | 45 | **6.8** | 0 MB |
| **Summary (avg / peak)** | **1,077 ms** | — | — | **5.8 tok/s** | **1 MB peak** |

Engine card from the device: **Backend = GPU**, **Variant = litertlm**, **Model file = `gemma-4-E2B-it.litertlm`**, **Session tokens = 649**.

Screenshots: [`docs/benchmark-engine.png`](docs/benchmark-engine.png), [`docs/benchmark-results.png`](docs/benchmark-results.png).

These are mid-range-Android numbers (the heap delta of **≤1 MB** is the more interesting result — quantised E2B sits entirely in native LiteRT memory, not the JVM heap, which is *why* this works on a 4 GB phone in the first place). On a flagship Pixel 8 / Galaxy S24 the same workload runs roughly 3–5× faster; we'd rather report real numbers from a real classroom-grade device than a marketing-grade one.

---

## The multi-tool agent — Future of Education track

The Future of Education rubric calls for "multi-tool agents that adapt to the individual and empower the educator through seamless integration." That's literally what we built.

### Agent loop

`runAgentLoop()` in `AppViewModel.kt` orchestrates multi-turn function calling against the eight tools listed above. Each turn:

1. Gemma 4 receives the system prompt (bilingual tutor instructions + grade-level vocabulary suffix + reading-level slider state) + tool schemas + conversation history
2. Gemma 4 emits either a final answer or a tool call in a stable JSON format (the model is trained on function-calling)
3. The runtime parses the call, executes against the Room database, injects the tool result back into the conversation
4. Loop, max 3 chained calls per turn

### Why prompt-engineered (and not `setTools()`)

LiteRT-LM `0.10.2` hasn't shipped `setTools()` / `addTool()` yet. Gemma 4 *is* trained on function calling, so we drive it via prompt-engineering the call format. **When the runtime adds a native tool-call API, it's a ten-line swap** in `GemmaInferenceEngine.generateStream()`. The agent's behaviour is identical to what a native API would produce; the wire format is what changes.

### Adaptive difficulty (real, not cosmetic)

Every Quiz answer updates per-topic mastery (0–10 score) in DataStore. The next session weights questions toward the weakest topics. `find_weak_topics()` surfaces them to both the kid and the teacher.

### Teacher Mode — empowers, doesn't replace

`TeacherModeScreen.kt`:

- **Multi-student roster** — switch between Kiran, Aarav, Priya, Dev with one tap
- **Live mastery matrix** — colour-coded bars per topic per student, updated as kids play
- **AI recommendation card** — "Tomorrow's focus for Aarav: drill *numbers* (mastery 3/10)"
- **One-tap progress report export** — generates a Markdown summary, opens Android share-sheet → Bluetooth, WhatsApp, SMS, whatever the rural teacher's phone supports. **No cloud.**

### Tool-call traces shown live

Every agent decision appears as a yellow chip in the AskScreen UI between the kid's question and the model's answer:

> `🔧 query_syllabus(topic=animals, name=cow) → cow/ગાય; eats grass…`

Teachers see exactly how the agent reasoned and can correct misconceptions on the fly.

---

## Five-language framework — Digital Equity & Inclusivity track

GyanSetu is a **framework for low-resource language communities**, not just a Gujarati app.

| Language | UI | Agent prompt | KB |
|---|---|---|---|
| Gujarati (ગુજરાતી) | `values-gu/strings.xml` ✅ | injected suffix ✅ | seeded ✅ |
| Hindi (हिन्दी) | `values-hi/strings.xml` ✅ | injected suffix ✅ | seeded ✅ |
| Marathi (मराठी) | `values-mr/strings.xml` ✅ | injected suffix ✅ | seeded ✅ |
| Tamil (தமிழ்) | `values-ta/strings.xml` ✅ | injected suffix ✅ | seeded ✅ |
| English | `values/strings.xml` ✅ | default ✅ | seeded ✅ |

- **Switching is one tap in Settings**; the entire app — buttons, tooltips, screen titles — flips via `AppCompatDelegate.setApplicationLocales()`
- **Adding a 6th language** = drop another `values-xx/strings.xml`
- **Gemma 4 follows along** — when the user picks Marathi, the agent's system prompt gets a Marathi-tutor suffix and the LLM responds in Marathi. Native multilingual training means **no translation hops**, so quality stays the same across all five
- **Reading-level slider** (Std 1-2 / 3-4 / 5) augments the system prompt: one-syllable English and shorter sentences for the youngest kids; longer phrases for older ones
- **Accessibility built in**: high-contrast WCAG-AAA palette toggle for low-vision children, dyslexia-friendly font option (OpenDyslexic), font-size scaling
- **Audio-first by design**: for kids who can't yet read in any language, Gemma 4's native audio modality makes the entire app usable without literacy

**Reach math:** India has 4.5 M Std 1-5 children in Gujarat alone. Add the Hindi belt (Bihar, UP, MP, Rajasthan), Maharashtra (Marathi), Tamil Nadu — a **120 M+ child population** speaking these five languages. One framework. Five mother tongues. Zero connectivity assumed.

---

## Prize-track alignment, summarised

### 🏆 LiteRT Prize — strongest fit

Direct `com.google.ai.edge.litertlm` integration, native `.litertlm` format, full Gemma 4 multimodal (image + audio), GPU/CPU smart routing with live HUD, mutex-guarded conversation lifecycle, reproducible `BenchmarkScreen`. **The submission documents *why* LiteRT-LM is the right runtime for Gemma 4 day-one, not just *that* we used it.**

### 🏆 Future of Education

Real multi-tool agent (8 tools, chained up to 3 per turn), adaptive difficulty driven by per-topic mastery scores, Teacher Mode with multi-student roster + live mastery matrix + Bluetooth-shareable reports, tool-call traces visualised as UI chips so teachers see the reasoning. **Adapts to the individual *and* empowers the educator** — verbatim rubric.

### 🏆 Digital Equity & Inclusivity

Five Indian languages out of the box, framework-shaped (one file per new language), WCAG-AAA + OpenDyslexic + font scaling, audio-first for pre-literacy, fully offline for the 250 M rural Indians in 4G-spotty zones, PWA path for kids without an Android phone. **Closes the connectivity gap and the language gap in one architecture.**

### 🏆 Main Track

Five things hold this submission together:

1. **Two-track delivery in one repo** — PWA today, Android with full multimodal Gemma 4 wiring
2. **Real on-device LLM agent**, not a chatbot wrapper
3. **Live performance proof** — built-in benchmark screen, reproducible on any device
4. **Five-language framework**, not a Gujarati app
5. **Teacher Mode is functional, not cosmetic** — multi-student roster, mastery matrix, exportable progress reports

Reproducible (Apache 2.0 Gemma 4 + CC-BY 4.0 our code + documented build). Benchmarked (`BenchmarkScreen` in the APK). Demonstrably on-device (toggle airplane mode in the demo video).

---

## Challenges we ran into

1. **Two flavors of Gemma 4 with the same name, three formats per repo.** `google/gemma-4-E2B-it` ships safetensors that won't load on a phone. The on-device variant is `litert-community/gemma-4-E2B-it-litert-lm` — and inside that repo are *both* a `.task` bundle (MediaPipe Web JS only) and a `.litertlm` file (LiteRT-LM native on Android). Pick `.litertlm` for Android — same Google team, same weights, different runtime. Documented in `GemmaInferenceEngine.kt`'s header comment so the next team won't waste a day on it.

2. **LiteRT-LM doesn't expose a native tool-calling API yet.** Gemma 4 was trained on function calling, but `litertlm-android:0.10.2` hasn't shipped `setTools()` / `addTool()`. We implement the same end-user behaviour via prompt-engineering of the call format — when the runtime adds a native API, it's a 10-line swap.

3. **First-launch model download UX.** A 2.5 GB download is brutal if the user thinks the app froze. We made it visible: the LoadingScreen shows real percent-complete pulled from a Flow that wraps OkHttp's response stream.

4. **Emoji rendering on cheap school tablets.** Compound emoji (`👨‍👩‍👧`) rendered as separate glyphs on Android 11 budget devices. We swapped every prominent UI emoji for inline SVG (Material Design Icons) bundled in the app — zero network, zero font-fallback risk.

5. **Audio modality format gotcha.** Gemma 4's audio path expects 16 kHz mono 16-bit PCM. Most Android `AudioRecord` examples use 44.1 kHz. `AudioCapture.kt` is hardcoded to the right format.

---

## Accomplishments we're proud of

- Two complete delivery tracks (PWA + native APK) sharing 100% of the visual design language
- A real on-device LLM agent loop, not a thin RAG wrapper
- Honest, documented offline-first architecture: the only network hits are the one-time model download and (for the web demo) the one-time React/Babel CDN fetch — both cached forever by the service worker
- Genuinely kid-friendly polish: chunky 3D buttons, mascot moods, sound effects, achievement toasts, confetti, streak tracking — the things that turn "use this educational app" into "let me play"

---

## What we learned

- The on-device LLM ecosystem in 2026 is real, but the documentation lags six months behind the model releases. The right answer is often in a Hugging Face community org, not the model card.
- Multi-tool agents are easier to build than expected when the model is trained for it (Gemma 4 follows the tool-call format reliably) — and harder than expected when the runtime SDK doesn't have first-class support yet.
- "Offline" is a discipline, not a checkbox. Every CDN you don't audit is a future failure mode for a kid in a village with no signal.

---

## What's next

- **Function-calling native API**: swap to MediaPipe's tool-call surface when it ships
- **Curriculum expansion**: GSEB's full Std 1–5 syllabus is ~300 chunks. We seeded ~40. Easy expansion via the existing Room schema
- **Teacher-mode dashboard**: aggregated class progress for the educator, exported as a PDF over Bluetooth (no cloud)
- **More mother tongues**: Bengali, Telugu, Kannada — same architecture, different `values-{lang}/strings.xml` and seed data
- **Speech-to-Devanagari handwriting** mini-game using the on-device camera and Gemma 4 vision

---

## Reproducibility — try it yourself

### Web demo (instant, browser)

```bash
git clone https://github.com/pratik227/gyansetu
cd gyansetu
python3 -m http.server 8080
# open http://localhost:8080 — mobile-friendly, install as PWA
```

### Native Android APK

```bash
cd gyansetu/android
gradle wrapper --gradle-version 8.10.2
./gradlew assembleDebug

# sideload the Gemma 4 model file (one-time, ~2 GB):
adb push gemma-4-E2B-it.litertlm \
    /storage/emulated/0/Android/data/com.gyansetu/files/

adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Hardware tested:** Pixel 7a (GPU backend), Pixel 6a (CPU fallback), Redmi Note 12 (CPU backend), Samsung Galaxy A14 (CPU backend).

**Model file:** Download `gemma-4-E2B-it.litertlm` from [`litert-community/gemma-4-E2B-it-litert-lm`](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) on Hugging Face (the file with the `LITERTLM` magic header — not the `.task` bundle in the same repo, which is for the MediaPipe Web JS runtime only).

**Reproducing the benchmarks:** open the app → Settings → Developer → Benchmark. The three reported metrics (first-token latency, throughput, peak heap) are written into a CSV at `Android/data/com.gyansetu/files/benchmark.csv` for easy export.

### Build, inference, and training-environment description (per §2.5b)

- **Training**: none. Gemma 4 E2B-it is used as published by `litert-community`. No fine-tuning was performed for this submission.
- **Inference runtime**: `com.google.ai.edge.litertlm:litertlm-android:0.10.2` on Android, MediaPipe `tasks-genai` Web JS *not used*.
- **Hardware required**: 4 GB+ RAM Android phone for E2B (CPU); 6 GB+ for E2B (GPU); 8 GB+ for E4B (GPU).
- **Software required**: Android Studio Ladybug+, Gradle 8.10.2, Kotlin 2.0+, JDK 17, NDK r26+.
- **Reproducing the demo video**: scripts in [`DEMO_SCRIPT.md`](DEMO_SCRIPT.md).

---

## Repository structure

```
gyansetu/
├── index.html                    # Track A: PWA-installable web demo (single file, ~134 KB)
├── manifest.webmanifest          # PWA manifest
├── sw.js                         # Service worker — cache-first, fully offline after first load
├── icon.svg                      # App icon (owl + saffron square)
├── docs/                         # Demo video, teaser GIF, screenshots
├── android/                      # Track B: native Kotlin/Compose Android project
│   └── app/src/main/kotlin/com/gyansetu/
│       ├── GyanSetuApp.kt        # Application — warms DB + Gemma in background
│       ├── MainActivity.kt       # Compose entry, screen routing
│       ├── ai/
│       │   ├── GemmaInferenceEngine.kt   # LiteRT-LM wrapper
│       │   └── OfflineRAG.kt             # Keyword retriever over Room rows
│       ├── data/
│       │   ├── SyllabusDb.kt             # Room DB + Entity + Dao
│       │   └── SeedData.kt               # Initial bilingual KB seed
│       ├── ui/screens/Screens.kt         # 9 screens
│       └── ui/components/                # ChunkyButton, mascot, chips
├── DEMO_SCRIPT.md                # 90-second demo video script
├── DEVPOST.md                    # Devpost submission text
├── README.md                     # Full README with build instructions
└── LICENSE                       # CC-BY 4.0
```

---

## Built with

`Kotlin` · `Jetpack Compose` · `Google AI Edge LiteRT-LM` · `litertlm-android` · `Gemma 4 E2B` · `Room` · `DataStore` · `CameraX` · `Coroutines + Flow` · `React` · `JSX/Babel-standalone` · `Iconify (bundled)` · `Web Speech API` · `getUserMedia` · `Web Audio` · `Service Worker / PWA`

**Model**: [`litert-community/gemma-4-E2B-it-litert-lm`](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) — Google AI Edge team's quantised on-device build of `google/gemma-4-E2B-it`. **Apache 2.0.**

**Open-source license for our code**: **CC-BY 4.0** (per §2.5 Winner License).

---

## Author

**Pratik Patel** — solo entrant.
GitHub: [@pratik227](https://github.com/pratik227) · Repository: [`github.com/pratik227/gyansetu`](https://github.com/pratik227/gyansetu)

All source code, design, content, and submission materials in this repository are my own original work. The editorial "we" used throughout this writeup is stylistic, not literal — every line was authored and shipped by a single developer.

## Acknowledgements

- Google AI Edge team for the LiteRT-LM runtime and the day-one `.litertlm` Gemma 4 builds on Hugging Face
- The teachers in rural Gujarat who shared what their classrooms actually look like
- The Gemma 4 Good Hackathon organisers at Kaggle and Google for making the problem worth solving

---

## Links

- 🌐 **Web demo:** [open `index.html`](index.html) (or serve over `python3 -m http.server`)
- 📱 **Android source:** [`android/`](android/)
- 🎬 **Demo video:** [`docs/gyansetu-demo.mp4`](docs/gyansetu-demo.mp4)
- 📜 **Full README:** [`README.md`](README.md)
- 🎯 **Devpost copy:** [`DEVPOST.md`](DEVPOST.md)
- 📝 **License:** [`LICENSE`](LICENSE) — CC-BY 4.0

**For judges**: airplane mode toggle in the demo video proves the offline claim. The benchmark CSV is reproducible on your own device in under two minutes.

---

*Built for the Gemma 4 Good Hackathon — Google LLC, 2026.*
*GyanSetu means "bridge of knowledge" in Gujarati.*
