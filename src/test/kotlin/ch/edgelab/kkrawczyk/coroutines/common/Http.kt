package ch.edgelab.kkrawczyk.coroutines.common

import com.github.tomakehurst.wiremock.client.WireMock
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger {}

val asyncClient = HttpClient(CIO)

fun stubHttpCallWithDelay(duration: Duration = Duration.ofSeconds(1)) {
    WireMock.stubFor(
        WireMock.get("/token").willReturn(
            WireMock.aResponse()
                .withFixedDelay(duration.toMillis().toInt())
                .withStatus(200)
                .withBody("token1234")
        )
    )
}

suspend fun doHttpCallFake(id: String): String {
    logger.info { "Doing fake http call: $id" }
    delay(1000)
    logger.info { "Done - fake http call $id. Response: 1234" }
    return "1234"
}

fun doHttpCallBlocking(id: String): String {
    logger.info { "Doing blocking http call: $id" }
    val response = khttp.get("http://localhost:8080/token").text
    logger.info { "Done - blocking http call $id, $response" }
    return response
}

suspend fun doHttpCallAsync(id: String): String {
    logger.info { "Doing async http call: $id" }
    val response = asyncClient.get<HttpResponse>("http://localhost:8080/token").readText()
    logger.info { "Done - blocking http call $id, $response" }
    return response
}
