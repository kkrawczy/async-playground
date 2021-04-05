package ch.edgelab.kkrawczyk.coroutines

import ch.edgelab.kkrawczyk.coroutines.common.doHttpCallAsync
import ch.edgelab.kkrawczyk.coroutines.common.stubHttpCallWithDelay
import com.github.jenspiegsa.wiremockextension.WireMockExtension
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext


data class Token(val s: String)
data class Data(val s: String)
data class Input(val s: String)
data class Output(val s: String)

private val logger = KotlinLogging.logger {}

/*
Example presenting how coroutines can handle async operations without using promises or callbacks directly in the code.
TLDR; suspended functions are transformed into something like state machines objects that control the execution of the method.
https://labs.pedrofelix.org/guides/kotlin/coroutines/coroutines-and-state-machines
https://www.youtube.com/watch?v=YrrUCSi72E8
 */

@ExtendWith(WireMockExtension::class)
class Coroutines {

    @Nested
    inner class SimpleCoroutinesExample {

        @Test
        fun execute() {
            stubHttpCallWithDelay(Duration.ofSeconds(1))

            GlobalScope.launch {
                logger.info { "Handling request" }
                val output = handleRequest(Input("input"))
                logger.info { "Request done. Output: $output" }
            }

            Thread.sleep(5000)
        }

        suspend fun handleRequest(input: Input): Output {
            val token: Token = requestToken()
            val data: Data = getData(token, input)
            val output: Output = prepareOutput(data)

            return output
        }

        suspend fun requestToken(): Token {
            return Token(doHttpCallAsync("token"))
        }

        suspend fun getData(token: Token, input: Input): Data {
            return Data(doHttpCallAsync(input.s))
        }

        fun prepareOutput(data: Data): Output {
            return Output(data.s)
        }

    }

    @Nested
    inner class DecompiledCoroutine {

        @Test
        fun execute() {
            val continuation = object : Continuation<Any> {
                override val context: CoroutineContext
                    get() = TODO("")

                lateinit var output: Output
                override fun resumeWith(result: Result<Any>) {
                    output = result.getOrThrow() as Output
                }
            }

            logger.info { "Handling request" }
            handleRequest(Input("input"), continuation)
            logger.info { "Request done. The output is: ${continuation.output}" }


        }

        //every suspended function receives additional parameter at compilation-time.
        //This parameter is of type Continuation and is used to store method specific
        //variables and the current execution state of the method.
        fun handleRequest(input: Input?, cont: Continuation<Any>) {
            class InnerContinuation(
                val outsideContinuation: Continuation<Any>,
                val input: Input
            ) : Continuation<Any> {
                var state: Int = 0
                lateinit var token: Token
                lateinit var data: Data

                override val context: CoroutineContext
                    get() = TODO("Not yet implemented")

                //method that would be called by down-stream async methods to give back the result when they are ready
                override fun resumeWith(result: Result<Any>) {
                    if (result.getOrThrow() is Token) token = result.getOrThrow() as Token
                    if (result.getOrThrow() is Data) data = result.getOrThrow() as Data
                    handleRequest(input, this)
                }
            }

            val continuation = cont as? InnerContinuation ?: InnerContinuation(cont, input!!)
            //method logic is split into parts, based on suspended functions present in method.
            //continuation.state decides which part of the function should be executed
            when (continuation.state) {
                0 -> {
                    continuation.state = 1
                    requestToken(continuation)
                }
                1 -> {
                    val item = continuation.input
                    val token = continuation.token
                    continuation.state = 2
                    getData(token, item, continuation)
                }
                2 -> {
                    val data = continuation.data
                    val output = prepareOutput(data)
                    continuation.outsideContinuation.resumeWith(Result.success(output))
                }
            }
        }

        fun requestToken(cont: Continuation<Any>) {
            //do some async job, when ready use callback from cont to go back:
            cont.resumeWith(Result.success(Token("token")))
        }

        fun getData(token: Token, input: Input, cont: Continuation<Any>) {
            //do some async job, when ready use callback from cont to go back:
            cont.resumeWith(Result.success(Data("response")))
        }

        fun prepareOutput(data: Data): Output {
            //this is sync step. Job do work and return.
            return Output(data.s)
        }

    }
}
