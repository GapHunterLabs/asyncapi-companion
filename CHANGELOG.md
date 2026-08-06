<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# AsyncAPI Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Go-to-definition for `$ref` values in AsyncAPI documents (JSON and
  YAML, both same-file and cross-file), resolved entirely locally.
- Content-based AsyncAPI document recognition (`asyncapi: "2.x"` /
  `"3.x"` top-level key, not file extension).
- Warning annotation for `$ref` values that fail to resolve.
- Suppresses the bundled JSON Schema go-to-declaration handler on
  recognized AsyncAPI files so this plugin's own resolution wins.

[Unreleased]: https://github.com/GapHunterLabs/asyncapi-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/asyncapi-companion/commits/0.1.0
