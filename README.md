# Down to Mark

An Android app for reading local markdown files with annotation support.

Open any `.md` file, read it with proper formatting, and build up a layer of highlights, bookmarks, comments, and tags on top of the source text. Notes are stored as plain JSON files alongside an index — no database, no cloud, fully inspectable.

## Features

- **Markdown rendering** — Headings, paragraphs, code blocks (with syntax font), blockquotes, ordered/unordered lists, horizontal rules, inline bold/italic/code/links. Parsed via CommonMark, rendered natively in Jetpack Compose.
- **Text highlighting** — Two-tap to select a range, pick a color, add an optional comment and tags. Highlights persist across sessions and render inline.
- **Bookmarks** — Tap the gutter beside any block to bookmark it. Quick access from the notes sheet.
- **Tag graph** — A force-directed graph showing how your tags relate across files. Nodes sized by usage, edges weighted by co-occurrence. Tap a node to see linked highlights and jump to source.
- **Three themes** — Everforest Dark, Everforest Light, and Newsprint (serif). Each provides a full Material 3 color scheme plus markdown-specific colors for code, blockquotes, links, and highlights.
- **JSON storage** — One index file, one notes file per document. Stored in app-internal storage under `notes/`. Human-readable, portable, diffable.

## Technical Details

- Kotlin, Jetpack Compose, Material 3
- Min SDK 31 (Android 12)
- CommonMark for parsing, custom Compose rendering
- Force-directed graph is hand-rolled Canvas + Fruchterman-Reingold physics
- No Room, no Retrofit, no network permissions, no heavyweight DI frameworks

## Building

Requires JDK 17+ and Android SDK with platform 35.

```bash
./gradlew assembleDebug
```

APK lands in `app/build/outputs/apk/debug/`.

## Status

Early. Core reading and annotation loop works. Rough edges remain — no image rendering, no table support, selection UX needs iteration. Contributions welcome but this is a personal tool first.

## License

MIT
