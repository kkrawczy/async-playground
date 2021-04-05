package ch.edgelab.kkrawczyk.coroutines

import ch.edgelab.kkrawczyk.coroutines.common.doHttpCallAsync
import ch.edgelab.kkrawczyk.coroutines.common.doHttpCallBlocking
import ch.edgelab.kkrawczyk.coroutines.common.doHttpCallFake
import ch.edgelab.kkrawczyk.coroutines.common.stubHttpCallWithDelay
import com.github.jenspiegsa.wiremockextension.WireMockExtension
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.util.concurrent.Executors

val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

@ExtendWith(WireMockExtension::class)
class TestsWithHttpClients {
    private val _logger = KotlinLogging.logger {}

    @Nested
    inner class WhenCoroutinesExecutedOnOneThread {

        @BeforeEach
        fun before() {
            stubHttpCallWithDelay(Duration.ofSeconds(1))
        }

        @Test
        fun `fake http client works concurrently`() {
            run(::doHttpCallFake)
        }

        @Test
        fun `async http client works concurrently`() {
            run(::doHttpCallAsync)
        }

        @Test
        fun `blocking http client blocks the thread`() {
            run(::doHttpCallBlocking)
        }

        private fun run(doHttpCall: suspend (String) -> String) {
            log("Before coroutine")
            GlobalScope.launch(singleDispatcher) {
                log("Before http calls")
                launch { doHttpCall.invoke("1") }
                launch { doHttpCall.invoke("2") }
                log("After http calls")
                repeat((1..10).count()) {
                    log("Doing something else")
                    delay(100)
                }
            }

            log("After coroutine")
            Thread.sleep(3000)
        }
    }


    @Nested
    inner class WithManyThreads {

        @BeforeEach
        fun before() {
            stubHttpCallWithDelay(Duration.ofSeconds(100))
        }

        @Test
        fun `blocking client blocks all the threads from the pool`() {
            val job = GlobalScope.launch {
                (1..50).forEach {
                    launch { doHttpCallBlocking(it.toString()) }
                }
            }

            Thread.sleep(2000)
            job.cancel()
        }

        @Test
        fun `async client runs concurrently 50 calls`() {
            val job = GlobalScope.launch {
                (1..50).forEach {
                    launch { doHttpCallAsync(it.toString()) }
                }
            }

            Thread.sleep(2000)
            job.cancel()
        }
    }

    private fun log(s: String) {
        _logger.info { s }
    }
}

//https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ScheduledExecutorService.html
