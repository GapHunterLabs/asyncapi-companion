# AsyncAPI Companion

IntelliJ-family plugin. Reliable go-to-definition for `$ref` values in
AsyncAPI documents (JSON and YAML) — 100% local, no network calls, and
it never breaks when a new JetBrains IDE version ships.

## Why it exists

Born from real evidence in JetBrains Marketplace reviews, not
assumptions: the leading AsyncAPI plugin in this space (FREEMIUM,
actively developed) has 55% of its reviews at 3 stars or fewer —
higher than the negative-review rate that anchored several other
plugins in this catalog. Paying and free users alike report:

- *"Suddenly no longer works in PyCharm. Says the java plugin is
  required."* — breaks on a routine update, no code change on the
  user's end.
- *"Does not work with IntelliJ 2024.2.4. The install does not
  complete and just reverts back to not being installed without any
  errors or error messages."*
- *"I'm getting error when I try to load the preview. I'm getting
  blank white screen with this: Error: The asyncapi field is missing.
  I have a valid asyncapi.yaml file."* — a real bug, not user error.

## Why built this way

- **Local-only reference resolution, no HTTP client anywhere in this
  plugin.** A `$ref` pointing at `http(s)://` is shown as unresolved,
  never fetched. This is the direct fix for the "suddenly no longer
  works" class of complaint: there's no external dependency (a bundled
  language server, an account/login step, a remote fetch) that can
  break independently of the code you're actually editing.
- **RFC 6901 JSON Pointer resolution against real PSI**, both JSON
  ([com.intellij.json.psi]) and YAML ([org.jetbrains.yaml.psi]) — two
  independent backends, not one lenient parser guessing at both
  formats. Ported from this catalog's own `openapi-companion` (which
  ported it from `json-schema-companion`) — same RFC, same resolution
  rules, AsyncAPI's `$ref` mechanic is identical to OpenAPI's.
- **Content-based detection, never by file extension.** A `.yaml` file
  is meaningless on its own; this plugin only activates on a document
  that actually declares `asyncapi: "2.x"` or `asyncapi: "3.x"` at the
  top level.
- **Suppresses the bundled JSON Schema go-to-declaration handler** on
  recognized AsyncAPI files via the platform's own
  `json.jsonSchemaGotoDeclarationSuppressor` extension point (same
  mechanism `openapi-companion` already uses for the identical
  problem: the platform's generic JSON Schema handler intercepts
  Ctrl+B before this plugin's own, more accurate resolution gets a
  chance to run).
- **v1 scope cuts, deliberate:** same-format `$ref` resolution only
  (no JSON file referencing into a YAML file or vice versa); no
  AsyncAPI-version-aware validation of the document itself beyond
  detecting it. Pure reference resolution, done reliably, is the whole
  product.
- **One specific, real gotcha explained, not just flagged as broken:**
  AsyncAPI channel names are commonly topic-like paths
  (`user/signedup`, idiomatic for MQTT/Kafka-style channels). A `$ref`
  into one written as `#/channels/user/signedup` instead of the
  RFC-6901-escaped `#/channels/user~1signedup` silently fails to
  resolve — when the unescaped literal genuinely matches a real
  declared channel name, the warning names the fix directly instead of
  a generic "cannot resolve".

## Usage

Open any `.yaml`/`.yml`/`.json` file that declares `asyncapi: "2.x"`
or `asyncapi: "3.x"` at the top level. Ctrl+Click / Ctrl+B on any
`$ref` value jumps straight to its target — same-file or another file
in your project. A `$ref` that can't be resolved is flagged with a
warning instead of failing silently.

## Enterprise / Team Licensing

Need enterprise features, custom validation rules, or team licensing?
Contact us at **gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
