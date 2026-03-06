# Text Utilities

String manipulation, logging, and date generation utilities.

## Overview

**File:** `app/src/main/kotlin/com/looker/droidify/utility/common/Text.kt`

## String Utilities

### Null If Empty

```kotlin
fun <T : CharSequence> T.nullIfEmpty(): T? {
    return if (isNullOrBlank()) null else this
}
```

Returns `null` if the string is null, empty, or contains only whitespace.

**Usage:**
```kotlin
val name = input.nullIfEmpty() ?: "Default"
val query = searchText.nullIfEmpty()  // null if blank
```

## Logging

### Generic Logger

```kotlin
fun Any.log(
    message: Any?,
    tag: String = this::class.java.simpleName + ".DEBUG",
    type: Int = Log.DEBUG,
) {
    Log.println(type, tag, message.toString())
}
```

**Usage:**
```kotlin
class MyClass {
    fun doSomething() {
        log("Processing started")  // Tag: MyClass.DEBUG
        log("Error occurred", type = Log.ERROR)
        log("Custom tag", tag = "NETWORK")
    }
}
```

## Date Utilities

### Monthly File Names Generator

Generates monthly file names from a given timestamp to current month.

```kotlin
@OptIn(ExperimentalTime::class)
fun generateMonthlyFileNames(since: Long?): List<String> = buildList {
    val current = Clock.System.now()
        .toLocalDateTime(TimeZone.UTC)
        .date.yearMonth

    var ym = if (since == null) {
        YearMonth(2024, 12)  // Download stats project start
    } else {
        Instant.fromEpochMilliseconds(since)
            .toLocalDateTime(TimeZone.UTC).date.yearMonth
    }

    while (ym <= current) {
        add("$ym.json")
        ym = ym.plusMonth()
    }
}
```

**Usage:**
```kotlin
// Generate from Dec 2024 to now
val files = generateMonthlyFileNames(null)
// Result: ["2024-12.json", "2025-01.json", "2025-02.json", ...]

// Generate from specific date
val files = generateMonthlyFileNames(1704067200000L)  // Jan 1, 2024
// Result: ["2024-01.json", "2024-02.json", ..., "current-month.json"]
```

Used for downloading monthly statistics files.

## HTML Formatting

**File:** `app/src/main/kotlin/com/looker/droidify/utility/text/HtmlFormatter.kt`

### Format HTML to SpannableStringBuilder

```kotlin
fun Html.format(
    onUrlClick: ((String) -> Unit)? = null,
): SpannableStringBuilder
```

### Behaviors

1. **HTML Parsing** - Parses HTML with `HtmlCompat.FROM_HTML_MODE_LEGACY`
2. **Newline Cleanup** - Trims excessive newlines, collapses 3+ to 2
3. **Link Detection** - Linkifies web and email addresses
4. **Click Handler** - Optional URL click callback
5. **Bullet Formatting** - Replaces `BulletSpan` with "• " character

### Implementation

```kotlin
fun Html.format(onUrlClick: ((String) -> Unit)?): SpannableStringBuilder {
    if (isPlainText) return SpannableStringBuilder(toString())

    val builder = run {
        val builder = SpannableStringBuilder(toSpanned())
        // Trim trailing newlines
        val last = builder.indexOfLast { it != '\n' }
        if (last >= 0) builder.delete(last + 1, builder.length)
        // Trim leading newlines
        val first = builder.indexOfFirst { it != '\n' }
        if (first in 1 until last) builder.delete(0, first - 1)
        // Collapse multiple newlines
        generateSequence(builder) {
            val index = it.indexOf("\n\n\n")
            if (index >= 0) it.delete(index, index + 1) else null
        }.last()
    }

    // Add links
    LinkifyCompat.addLinks(builder, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)

    // Replace URL spans with clickable spans
    if (onUrlClick != null) {
        builder.getSpans(0, builder.length, URLSpan::class.java)?.forEach { span ->
            val start = builder.getSpanStart(span)
            val end = builder.getSpanEnd(span)
            val flags = builder.getSpanFlags(span)
            val url = span.url
            builder.removeSpan(span)
            builder.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = onUrlClick(url)
            }, start, end, flags)
        }
    }

    // Replace bullet spans
    builder.getSpans(0, builder.length, BulletSpan::class.java)
        ?.asSequence()
        ?.map { it to builder.getSpanStart(it) }
        ?.sortedByDescending { it.second }
        ?.forEach { (span, start) ->
            builder.removeSpan(span)
            builder.insert(start, "\u2022 ")  // • bullet
        }

    return builder
}
```

### Convenience Function

```kotlin
fun formatHtml(html: String, onUrlClick: ((String) -> Unit)?): SpannableStringBuilder =
    Html(html).format(onUrlClick)
```

### Usage

```kotlin
// In legacy View-based UI
textView.text = formatHtml(description) { url ->
    openUrl(url)
}

// Using Html model
val html = Html("<p>Hello <b>World</b></p>")
val formatted = html.format { url -> handleLink(url) }
```

## Annotated String (Compose)

**File:** `app/src/main/kotlin/com/looker/droidify/utility/text/AnnotatedString.kt`

For Compose-based HTML rendering. See [Compose Screens](../ui/compose-screens.md) for details.

## Constants

**File:** `app/src/main/kotlin/com/looker/droidify/utility/common/Constants.kt`

```kotlin
object Constants {
    // Notification channels
    const val NOTIFICATION_CHANNEL_SYNCING = "syncing"
    const val NOTIFICATION_CHANNEL_UPDATES = "updates"
    const val NOTIFICATION_CHANNEL_DOWNLOADING = "downloading"
    const val NOTIFICATION_CHANNEL_INSTALL = "install"

    // Notification IDs
    const val NOTIFICATION_ID_SYNCING = 1
    const val NOTIFICATION_ID_UPDATES = 2
    const val NOTIFICATION_ID_DOWNLOADING = 3
    const val NOTIFICATION_ID_INSTALL = 4
    const val NOTIFICATION_ID_RB_DOWNLOAD = 5
    const val NOTIFICATION_ID_STATS_DOWNLOAD = 6
    const val NOTIFICATION_ID_INDEX_DOWNLOAD = 7

    // Job IDs
    const val JOB_ID_SYNC = 1
}
```

## Best Practices

1. Use `nullIfEmpty()` for optional string parameters
2. Use the `log()` extension for consistent logging
3. Use `Html.format()` for rich text rendering
4. Reference `Constants` for notification/job IDs
