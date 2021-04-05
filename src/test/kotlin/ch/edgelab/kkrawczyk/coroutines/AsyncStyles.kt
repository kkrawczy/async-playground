import ch.edgelab.kkrawczyk.coroutines.common.doHttpCallAsync
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext


data class Token(val s: String)
data class Response(val s: String)
data class Input(val s: String)
data class Output(val s: String) {
    fun addValues(s: String) {}
}

object Blocking {
    //It's blocking so it's not good.
    fun handleRequest(input: Input): Output {
        val token = requestToken()
        val response = callHttpEndpoint(token, input)
        val output = prepareOutput(response)

        return output
    }

    private fun requestToken(): Token {
        //ask for token
        return Token("")
    }

    private fun callHttpEndpoint(token: Token, input: Input): Response {
        //call http endpoint
        return Response("")
    }

    private fun prepareOutput(response: Response): Output {
        //some business logic and output
        return Output("output")
    }
}

object Callbacks {
    //callback hell -
    //difficult to handle exceptions
    fun handleRequestListener(input: Input, output: Output) {
        requestToken { token ->
            callHttpEndpoint(token, input) { response ->
                prepareOutput(response, output)
            }
        }
    }

    private fun requestToken(cb: (Token) -> Any) {}

    private fun callHttpEndpoint(token: Any, input: Input, cb: (Response) -> Any) {}

    private fun prepareOutput(response: Response, output: Output) {
        output.addValues(response.s);
    }
}

object Futures {
    //composable
    //propagate exceptions
    //no {{{}}}
    //different style of programming (cannot do regular if's or for)
    fun handleRequest(input: Input): CompletableFuture<Output> {
        val output: CompletableFuture<Output> = requestToken()
            .thenCompose { token -> callHttpEndpoint(token, input) }
            .thenCompose { response -> prepareOutput(response) }

        return output
    }

    fun requestToken(): CompletableFuture<Token> {
        TODO("Not yet implemented")
    }

    private fun callHttpEndpoint(it: Token, input: Input): CompletableFuture<Response> {
        TODO("Not yet implemented")
    }

    private fun prepareOutput(response: Response): CompletableFuture<Output> {
        TODO("Not yet implemented")
    }
}

object Reactive {
    //type of Futures
    fun handleRequest(input: Input): Mono<Output> {
        return requestToken()
            .flatMap { token -> callHttpEndpoint(token, input) }
            .flatMap { response -> prepareOutput(response) }
    }

    private fun requestToken(): Mono<Token> = Mono.empty()
    private fun callHttpEndpoint(token: Token, input: Input): Mono<Response> = Mono.empty()
    private fun prepareOutput(response: Response): Mono<Output> = Mono.empty()
}

object Coroutines {
    //hides the complexity of async code
    //easy to do loops
    suspend fun handleRequest(input: Input): Output {
        //label 1
        val token: String = requestToken()
        //label 2
        val response: Response = callHttpEndpoint(token, input)
        //label 3
        val output: Output = prepareOutput(response)

        return output
    }

    suspend fun requestToken(): String {
        return doHttpCallAsync("token")
    }

    suspend fun callHttpEndpoint(token: String, input: Input): Response {
        return Response(doHttpCallAsync(input.s))
    }

    fun prepareOutput(response: Response): Output {
        //prepare output
        return Output(response.s)
    }
}

object TransformedCoroutines {

    fun handleRequest(input: Input, cont: Continuation<Any>) {
        //state machine
        class InnerContinuation(
            val outsideContinuation: Continuation<Any>
        ) : Continuation<Any> {
            var label: Int = 0
            lateinit var input: Input
            lateinit var token: Token
            lateinit var response: Response

            override val context: CoroutineContext
                get() = TODO("Not yet implemented")

            override fun resumeWith(result: Result<Any>) {
                if (result.getOrThrow() is Token) token = result.getOrThrow() as Token
                if (result.getOrThrow() is Response) response = result.getOrThrow() as Response
                handleRequest(input, this)
            }
        }

        val continuation = cont as? InnerContinuation ?: InnerContinuation(cont)


        when (continuation.label) {
            0 -> {
                continuation.input = input
                continuation.label = 1
                requestTokenTransformed(cont)
            }
            1 -> {
                val item = continuation.input
                val token = continuation.token
                continuation.label = 2
                doCallTransformed(token, item, cont)
            }
            2 -> {
                val response = continuation.response
                println(response)
                continuation.outsideContinuation.resumeWith(Result.success(""))
            }
        }
    }

    fun requestTokenTransformed(cont: Continuation<Any>) {
        //do some job, when ready go back to continuation

        val value = Token("token")
        cont.resumeWith(Result.success(value))
    }

    fun doCallTransformed(token: Token, input: Input, cont: Continuation<Any>) {
        //do some job, when ready go back to continuation


        val value = Response("response")
        cont.resumeWith(Result.success(value))
    }

    fun prepareOutput(response: Response): Output {
        return Output(response.s)
    }

}



