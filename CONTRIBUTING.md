# Contributing

Unofficial, personal, and unaffiliated with Vates. Issues and pull requests are welcome.

## The two rules that are enforced mechanically

### Nothing tracked here names the tools used to write it

Not in code, not in comments, not in commit messages, not in pull request titles or bodies.

Enforced in three places: `.githooks/prepare-commit-msg` strips attribution trailers before
you see them, `.githooks/commit-msg` refuses a message that still names one, and the
`tooling-references` job in CI checks tracked content, the commit range, and the pull request
text. The last of those is the one that cannot be skipped.

The patterns live base64-encoded in `.github/tooling-patterns.b64` so the checker does not
trip the rule it enforces. That is the only reason for the encoding; `base64 -d` reads it.

Turn the hooks on once per clone:

```
git config core.hooksPath .githooks
```

`./gradlew build` does this for you, and says so when it overrides a global hooks directory.

### No em dashes in anything tracked

`NoEmDashesTest` scans everything `git ls-files` reports, by extension. Use the punctuation
the clause wants: a full stop between two sentences, a colon before an explanation, a comma
or parentheses around an aside.

The test asserts a floor on how many files it scanned, because a scan that silently covered
nothing would pass and a tool reporting absence is a claim that needs its own control.

## Building

```
./gradlew build          # compile, test, jar
./gradlew buildPlugin    # the distributable zip
./gradlew runIde         # launch Gateway with this plugin loaded
```

JDK 21, pinned in `gradle/gradle-daemon-jvm.properties`.

**If dependency resolution fails on `com.jetbrains.gateway:JetBrainsGateway`, check the
repositories block before doubting the version.** Gateway is published only to the JetBrains
*snapshots* repository; the releases repository answers 404. `build.gradle.kts` adds
`snapshots()` for exactly that reason, and without it the error names a version that visibly
exists on JetBrains' website.

## The thing CI structurally cannot check

**A change can compile, pass every test, and be wrong on screen.** A connector is a user
interface inside a launcher, and nothing in CI launches one. So if your change touches
anything Gateway renders, run it and look at it. Screenshots in the pull request are worth
more than a green tick.

If the evidence for your change is a dialog, transcribe the wording **while it is on screen**
rather than from memory afterwards.

## Version numbers that are ceilings, not floors

`kotlin`, `coroutines` and `serialization` in `gradle/libs.versions.toml` are provided by the
IntelliJ Platform at runtime. Compiling above what the platform loads is a `NoSuchMethodError`
inside the IDE that **CI cannot see, because CI only compiles**.

Worse here than in the sibling toolbox project: over there the pins were measured against an
installed app. Here they have not been. Do not raise one because a bot suggested it. Raising
one means establishing what the platform actually ships first.

## Pull requests

Conventional commits. One logical change per commit. Say what you ran, not what should work.

If a claim in your description is not something you ran or read, mark it as a guess. A hedge
does not transfer between sentences: each mechanism claim needs its own evidence.

## Filing an issue

Include the Gateway build, the plugin version, and what Xen Orchestra reported. Redact the
token and any address you would not put in a public message.
