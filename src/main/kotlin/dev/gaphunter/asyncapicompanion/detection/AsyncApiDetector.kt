package dev.gaphunter.asyncapicompanion.detection

/**
 * Detects an AsyncAPI spec by real content, never by file extension --
 * a `.json`/`.yaml`/`.yml` file is meaningless on its own, same
 * principle as openapi-companion's `OpenApiDetector`. A real AsyncAPI
 * document always declares its version at the top level:
 * `asyncapi: "2.x.x"` or `asyncapi: "3.x.x"`. Pure text scan -- callers
 * pass in the already-loaded file text, no I/O here.
 */
object AsyncApiDetector {
    // YAML's top-level keys are conventionally unindented at column 0, so a
    // line-start anchor is a real, format-specific signal there. JSON has no
    // such convention (a compact, single-line document is completely valid),
    // so its check only requires the exact quoted key with nothing between
    // the quotes -- same dual-pattern split as OpenApiDetector.
    private val YAML_ASYNCAPI = Regex("""(?m)^asyncapi:\s*["']?[23]\.""")
    private val JSON_ASYNCAPI = Regex(""""asyncapi"\s*:\s*"[23]\.""")

    fun isAsyncApiSpec(text: String): Boolean =
        YAML_ASYNCAPI.containsMatchIn(text) || JSON_ASYNCAPI.containsMatchIn(text)
}
