# [android-components](../../../README.md) > Feature > Takeout Importer

A feature that drives the Google Takeout export flow on `takeout.google.com`
via a built-in WebExtension, intercepts the resulting ZIP download, streams
the archive, and exposes the extracted entries as files on disk for callers
(e.g. the bookmark importer or password importer) to consume.

The WebExtension is responsible for navigating the Takeout web UI and
reporting each step over a native messaging port. The native `TakeoutImporterFeature`
observes downloads originating from `takeout.google.com`, hands the completed
file to `TakeoutZipExtractor`, and emits the resulting list of extracted file
URIs back to the caller.

## Usage

### Setting up the dependency

Use Gradle to download the library from [maven.mozilla.org](https://maven.mozilla.org/):

```Groovy
implementation "org.mozilla.components:feature-takeout-importer:{latest-version}"
```

### Integrating with a host app

```kotlin
val feature = TakeoutImporterFeature(
    context = context,
    engine = engine,
    store = browserStore,
    onStep = { step -> /* telemetry / progress UI */ },
    onCompleted = { result ->
        when (result) {
            is TakeoutImporterFeature.Result.Success ->
                // result.extractedFiles is a List<File> in cache
            is TakeoutImporterFeature.Result.Failure ->
                // result.reason describes what went wrong
        }
    },
)

feature.start()
```

The feature installs the WebExtension at start, observes any download whose
URL host matches `takeout.google.com`, and surfaces the extracted files via
`onCompleted`.

## License

```
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at http://mozilla.org/MPL/2.0/.
```
