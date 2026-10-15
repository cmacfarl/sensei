# Sensei

A personal Japanese tutor for Android. Type a sentence:

- **English →** natural Japanese in both **polite** and **casual** registers, with a grammar
  breakdown by phrase.
- **Japanese →** a teacher's verdict (*correct*, *grammatical but unnatural*, or *needs fixing*),
  what to fix and why, a minimal correction, and how a native speaker would say it.

Then ask follow-up questions in the same thread. **New sentence** (toolbar) starts a fresh thread.

The rules can't be forgotten, because the app sends them with every request:

- **No romaji, ever.** The app enforces this as well as asking for it (see below).
- **Furigana above the kanji**, never inline. Toggle it with the eye icon. Long-press a word to
  mark its kanji as known; furigana is then hidden on words whose kanji you all know.
- Explanations are always in English.

## Setup

1. Open the project in Android Studio (or run `./gradlew assembleDebug`).
2. Get an API key:
   - Gemini: create one at [Google AI Studio](https://aistudio.google.com/). The free tier works.
   - Claude: create one in the Claude Console.
3. Add your keys to `local.properties` in the project root. That file is git-ignored, and
   Android Studio already created it with `sdk.dir`.
   ```
   gemini.api.key=AIza...
   claude.api.key=sk-ant-...
   ```
   Debug builds (Android Studio's Run or Debug button) build these in, so nothing needs typing
   on the phone. Release builds never include them.
4. Run the app, then open **Settings** to choose a provider and optionally change the model.
   Defaults: `gemini-3.8-flash`, `claude-opus-5-5`. Cheaper Claude options: `claude-sonnet-5-5`,
   `claude-haiku-5-5`.

A key typed into Settings overrides the built-in one. Typed keys live only in the app's private
preferences, and backups are disabled. Anyone holding a debug APK can extract its built-in keys,
so don't share that APK. Setting a spend limit on each key is a good idea.

**Offline demo:** in a debug build, enter `demo` as the API key to get canned replies, so you
can try the UI without network access or cost. The first demo translation deliberately contains
romaji so you can watch the rewrite step fix it.

## How the no-romaji rule is enforced

1. **The system prompt** ([Prompts.java](app/src/main/java/com/cmacfarl/sensei/tutor/Prompts.java))
   forbids romaji, including in grammar-term names (て-form, not te-form), and goes out with every
   request.
2. **Structured output.** Replies must match a JSON schema
   ([Schemas.java](app/src/main/java/com/cmacfarl/sensei/tutor/Schemas.java)), so Japanese goes in
   its own fields. Furigana is written as `食[た]べる`, and the app renders it as ruby text.
3. **Validation.** [RomajiDetector](app/src/main/java/com/cmacfarl/sensei/text/RomajiDetector.java)
   scans every field. It catches:
   - romanized grammar terms
   - romanized particles and words
   - romanized verb endings
   - macrons
   - romaji next to the Japanese it spells, e.g. `食べる (taberu)`
   - Latin letters inside furigana brackets

   It's tuned not to flag ordinary English ("Use と to connect nouns").

   If anything is found, the app asks the model to rewrite the reply, up to 2 times. You only
   ever see a clean reply. In the rare case romaji survives both rewrites, the reply is shown
   with the romaji highlighted and a warning.

## Project layout

```
app/src/main/java/com/cmacfarl/sensei/
  text/    FuriganaParser, RomajiDetector, KanaRomanizer (detection only), JapaneseChars
  tutor/   Tutor (request + rewrite loop), Prompts, Schemas, Responses, Conversation/Turn
  llm/     LlmClient interface; GeminiClient (REST/OkHttp), ClaudeClient (Anthropic Java SDK), DemoClient
  data/    Settings, KnownKanji, ConversationStore (in-memory today)
  ui/      MainActivity, ChatAdapter, RubyTextView/RubySpan/RubyRenderer, Settings, KnownKanji screens
```

- **Rules:** edit `Prompts.SYSTEM` and redeploy.
- **History:** threads are plain objects, serializable with Gson. Implement `ConversationStore`
  with Room or files and swap it in for `InMemoryConversationStore`.
- **Providers:** a thread keeps the provider and model it started with. Settings changes apply
  from the next new sentence.

## Notes

- **Claude requests:**
  - Effort is set to medium.
  - The system prompt is prompt-cached.
  - On models that support it, server-side refusal fallback (`fallbacks: "default"`) is turned on.
    If Claude's safety classifier declines a request, the API retries it on a suitable model
    instead of failing.
- **Gemini requests:** they use `generateContent` with `responseJsonSchema`. Model replies are
  replayed exactly, thought signatures included.
- **Starter kanji list:** `res/raw/starter_kanji.txt` holds about 265 N5/N4 kanji, based on
  common study lists (there is no official list). Edit it, or use the Known kanji screen.
- **Minify:** off for release builds. The Anthropic SDK relies on Jackson reflection, so R8 would
  need keep rules first.

## Tests

```bash
./gradlew testDebugUnitTest
```

JVM unit tests cover:
- furigana parsing
- the romaji detector, both catching leaks and passing normal English
- the rewrite loop
- thread history
- Gemini request and response handling
