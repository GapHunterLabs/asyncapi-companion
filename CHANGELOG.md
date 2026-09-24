<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# AsyncAPI Companion Changelog

## [Unreleased]

## [0.3.0]

### Added

- Cross-format `$ref` resolution: a JSON-formatted AsyncAPI document
  can now reference a YAML-formatted shared component file, and vice
  versa -- previously a deliberate v1 scope cut. The target file's
  own real format decides which resolver applies, not the format of
  the file containing the `$ref`.

## [0.2.0]

### Added

- An unresolved `$ref` into `#/channels/...` now explains the specific,
  common gotcha when it applies: a channel name that's itself a
  topic-like path (`user/signedup`) needs its `/` escaped as `~1` per
  RFC 6901 -- when the unescaped literal genuinely matches a real
  declared channel name, the warning names the exact fix.

## [0.1.2]

### Added

- Review/star CTA: after 10 distinct real unresolved `$ref` findings, a
  one-time notification asks whether to rate the plugin on Marketplace,
  with a permanent "Don't ask again" option. Standard mechanism used
  catalog-wide since 2026-08-24, rolled out to
  this plugin now.

## [0.1.1]

### Fixed

- Ctrl+Click/Ctrl+B on a `$ref` value showed "No usages found" instead
  of navigating, in every real IDE session -- confirmed via live
  logging that the bundled-handler suppressor and this plugin's own
  reference resolution both worked correctly in isolation, but the
  platform never fell back to the generic reference after suppressing
  the bundled handler. Fixed by registering a real
  `GotoDeclarationHandler` (the same extension point the bundled
  handler itself uses), sidestepping that broken hand-off entirely.
  Also fixes a caret-on-the-key-token case (clicking the literal
  `$ref` text, not the value after it) that a naive first version of
  the handler still missed.

## [0.1.0]

### Added

- Go-to-definition for `$ref` values in AsyncAPI documents (JSON and
  YAML, both same-file and cross-file), resolved entirely locally.
- Content-based AsyncAPI document recognition (`asyncapi: "2.x"` /
  `"3.x"` top-level key, not file extension).
- Warning annotation for `$ref` values that fail to resolve.
- Suppresses the bundled JSON Schema go-to-declaration handler on
  recognized AsyncAPI files so this plugin's own resolution wins.

[Unreleased]: https://github.com/GapHunterLabs/asyncapi-companion/compare/0.3.0...HEAD
[0.3.0]: https://github.com/GapHunterLabs/asyncapi-companion/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/GapHunterLabs/asyncapi-companion/compare/0.1.2...0.2.0
[0.1.2]: https://github.com/GapHunterLabs/asyncapi-companion/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/GapHunterLabs/asyncapi-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/asyncapi-companion/commits/0.1.0
