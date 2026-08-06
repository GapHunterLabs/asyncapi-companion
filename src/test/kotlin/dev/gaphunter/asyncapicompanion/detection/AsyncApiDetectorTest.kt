package dev.gaphunter.asyncapicompanion.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AsyncApiDetectorTest {
    @Test
    fun `detects an AsyncAPI 2 x document by its top-level key`() {
        assertTrue(AsyncApiDetector.isAsyncApiSpec("asyncapi: 2.6.0\ninfo:\n  title: Order Events"))
        assertTrue(AsyncApiDetector.isAsyncApiSpec("""{"asyncapi": "2.6.0", "info": {}}"""))
    }

    @Test
    fun `detects an AsyncAPI 3 x document by its top-level key`() {
        assertTrue(AsyncApiDetector.isAsyncApiSpec("asyncapi: 3.0.0\ninfo:\n  title: Order Events"))
    }

    @Test
    fun `a plain JSON or YAML file with no version key is not recognized`() {
        assertFalse(AsyncApiDetector.isAsyncApiSpec("""{"name": "Acme", "version": "1.0"}"""))
        assertFalse(AsyncApiDetector.isAsyncApiSpec("name: Acme\nversion: '1.0'"))
    }

    @Test
    fun `a file mentioning asyncapi in an unrelated field is not recognized`() {
        // Real false-positive risk, same class as OpenApiDetectorTest's
        // package.json case: a project could depend on an "asyncapi"-named
        // tool without the file itself being a spec.
        assertFalse(AsyncApiDetector.isAsyncApiSpec("""{"dependencies": {"asyncapi-cli": "^1.0"}}"""))
    }
}
