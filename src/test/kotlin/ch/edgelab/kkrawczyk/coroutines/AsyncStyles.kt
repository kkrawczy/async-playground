package ch.edgelab.kkrawczyk.coroutines.common

import ch.edgelab.kkrawczyk.coroutines.common.Classes.Data
import ch.edgelab.kkrawczyk.coroutines.common.Classes.Input
import ch.edgelab.kkrawczyk.coroutines.common.Classes.Output
import ch.edgelab.kkrawczyk.coroutines.common.Classes.Token
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture


/*
presenting different asynchronous programming styles
 */

object Classes {
    data class Token(val s: String)
    data class Data(val s: String)
    data class Input(val s: String)
    data class Output(val s: String) {
        fun addValues(s: String) {}
    }
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

    private fun callHttpEndpoint(token: Token, input: Input): Data {
        //call http endpoint
        return Data("")
    }

    private fun prepareOutput(data: Data): Output {
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

    private fun callHttpEndpoint(token: Any, input: Input, cb: (Data) -> Any) {}

    private fun prepareOutput(data: Data, output: Output) {
        output.addValues(data.s);
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

    private fun callHttpEndpoint(it: Token, input: Input): CompletableFuture<Data> {
        TODO("Not yet implemented")
    }

    private fun prepareOutput(data: Data): CompletableFuture<Output> {
        TODO("Not yet implemented")
    }
}

object Reactive {
    //type of ch.edgelab.kkrawczyk.coroutines.common.Futures
    fun handleRequest(input: Input): Mono<Output> {
        return requestToken()
            .flatMap { token -> callHttpEndpoint(token, input) }
            .flatMap { response -> prepareOutput(response) }
    }

    private fun requestToken(): Mono<Token> = Mono.empty()
    private fun callHttpEndpoint(token: Token, input: Input): Mono<Data> = Mono.empty()
    private fun prepareOutput(data: Data): Mono<Output> = Mono.empty()
}

object Coroutines {
    //hides the complexity of async code
    //easy to do loops
    suspend fun handleRequest(input: Input): Output {
        val token: String = requestToken()
        val data: Data = callHttpEndpoint(token, input)
        val output: Output = prepareOutput(data)

        return output
    }

    suspend fun requestToken(): String {
        return doHttpCallAsync("token")
    }

    suspend fun callHttpEndpoint(token: String, input: Input): Data {
        return Data(doHttpCallAsync(input.s))
    }

    fun prepareOutput(data: Data): Output {
        //prepare output
        return Output(data.s)
    }
}
