package com.example.androidautoytstreamer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class YouTubeStreamResolverTest {

    @Test
    fun `resolving valid or invalid video returns result without crashing`() = runBlocking {
        YouTubeStreamResolver.clearCache()
        val result = YouTubeStreamResolver.resolveAudioStreamUrl("CpzMGwNZkx4")
        assertNotNull(result)
    }
}
