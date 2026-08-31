package dev.gaphunter.asyncapicompanion.detection

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnescapedChannelSlashHintTest {

    @Test
    fun `an unescaped slash matching a real channel name is flagged with the escaped form`() {
        val message = UnescapedChannelSlashHint.describe("/channels/user/signedup", setOf("user/signedup"))
        assertTrue(message!!.contains("user/signedup"))
        assertTrue(message.contains("#/channels/user~1signedup"))
    }

    @Test
    fun `a multi-segment pointer that does NOT match a real channel name is never flagged`() {
        // '#/channels/foo/subscribe/message' is a completely normal,
        // valid multi-segment ref deeper into a channel's own structure --
        // must never be second-guessed just because it contains slashes.
        assertNull(UnescapedChannelSlashHint.describe("/channels/foo/subscribe/message", setOf("foo")))
    }

    @Test
    fun `a pointer not under channels at all is never flagged`() {
        assertNull(UnescapedChannelSlashHint.describe("/components/messages/user/signedup", setOf("user/signedup")))
    }

    @Test
    fun `a single-segment channel name with no slash is never flagged`() {
        assertNull(UnescapedChannelSlashHint.describe("/channels/user-signedup", setOf("user-signedup")))
    }

    @Test
    fun `escaping also handles a literal tilde in the channel name`() {
        val message = UnescapedChannelSlashHint.describe("/channels/a/b~c", setOf("a/b~c"))
        assertTrue(message!!.contains("#/channels/a~1b~0c"))
    }
}
