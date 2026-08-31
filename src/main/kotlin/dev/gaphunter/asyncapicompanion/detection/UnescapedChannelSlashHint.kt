package dev.gaphunter.asyncapicompanion.detection

/**
 * Flags the specific, real AsyncAPI gotcha where a channel name is
 * itself a topic-like path (`user/signedup`, extremely common with
 * MQTT/Kafka-style channel naming) and a `$ref` into it was written
 * with the raw, unescaped name instead of RFC 6901's required `~1` for
 * a literal `/` inside a single path segment
 * (`#/channels/user/signedup` splits into three pointer segments
 * instead of the two intended -- `channels` then the whole channel
 * name -- so it silently fails to resolve). Only fires when the
 * unescaped literal genuinely matches a real declared channel name, so
 * a normal, valid multi-segment ref deeper into a channel's own
 * structure (`#/channels/foo/subscribe/message`) is never
 * second-guessed.
 */
object UnescapedChannelSlashHint {

    private const val CHANNELS_PREFIX = "/channels/"

    /**
     * [pointer] is the fragment after `#`, as written (not yet
     * RFC 6901-unescaped). [channelNames] are the real, literal keys
     * declared directly under the document's top-level `channels:`
     * object. Returns a specific explanation, or null when this
     * particular gotcha doesn't apply.
     */
    fun describe(pointer: String, channelNames: Set<String>): String? {
        if (!pointer.startsWith(CHANNELS_PREFIX)) return null
        val remainder = pointer.removePrefix(CHANNELS_PREFIX)
        if (!remainder.contains("/")) return null
        if (remainder !in channelNames) return null

        val escaped = remainder.replace("~", "~0").replace("/", "~1")
        return "the channel name '$remainder' contains '/', which must be escaped as '~1' in a JSON Pointer -- try '#/channels/$escaped'"
    }
}
