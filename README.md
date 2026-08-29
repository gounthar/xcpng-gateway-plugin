# XCP-ng for JetBrains Gateway (unofficial)

Open a JetBrains IDE against a virtual machine on an [XCP-ng](https://xcp-ng.org/) pool,
through [Xen Orchestra](https://xen-orchestra.com/), from the Gateway launcher.

**Unofficial, personal, and unaffiliated with Vates.** Nothing here is a product, mine or my
employer's.

## Status

Early. The Xen Orchestra client is carried over from a working sibling project and is
exercised by its own tests. The Gateway connector itself is not written yet: `plugin.xml`
loads and declares nothing, on purpose.

See `CONTEXT.md` in a working clone for what is next. It is not tracked.

## Why this exists separately from the sibling projects

Three surfaces, and confusing them is the mistake this repository was created to avoid.

| Repository | Lives in | Answers |
|---|---|---|
| `xcpng-toolbox-plugin` | the Toolbox App | pick a VM, open an IDE on it |
| `xcpng-gateway-plugin` (here) | the Gateway launcher | the same, for people not on Toolbox |
| `xcpng-intellij-plugin` | the IDE itself | see and drive the pool without leaving the IDE |

A connector, Gateway or Toolbox, hands the IDE a machine and an SSH endpoint and then stops.
Once the backend is running the IDE treats it as any other remote backend, so remote toolchain
detection and remote debugging are backend-side and cannot be added from here by anyone.
Anything that has to appear inside a running IDE belongs in `xcpng-intellij-plugin`.

Google's own plugins split along the same line, which is the clearest available evidence that
the line is real rather than an artifact of how this was built: `Cloud Code` is the in-IDE
integration, and `Cloud Workstations`, a separate plugin whose descriptor is literally named
`...jetbrains.connector`, is the Gateway half.

## Why Gateway, when Toolbox is the successor

Toolbox is where JetBrains is heading, and the sibling targets it. Gateway earns its own
repository for one reason: **Toolbox plugin publishing is restricted to pre-approved vendors
and Gateway's is not.** So Gateway is the surface a third party can actually ship on today.

Red Hat maintains both a `devspaces-gateway-plugin` and a `devspaces-toolbox-plugin`, so
carrying two is a normal thing to do rather than an admission of indecision.

The cost is real and worth stating: Gateway's connector API is documented as not final, and
JetBrains publishes a
[migration guide](https://www.jetbrains.com/help/toolbox-app/jetbrains-gateway-migrations-guide.html)
away from it.

## Building

```
./gradlew build          # compile, test, jar
./gradlew buildPlugin    # the distributable zip
./gradlew runIde         # launch Gateway with this plugin loaded
```

JDK 21. See `CONTRIBUTING.md`, including the one dependency-resolution trap worth knowing
before you hit it.

## Licence

MIT. See `LICENSE`.
