package ch.edgelab.kkrawczyk.javanio

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import java.time.Duration


fun stubHttpCallWithDelay(duration: Duration = Duration.ofSeconds(1)) {
    WireMock.stubFor(
        WireMock.get("/token")
            .willReturn(
                WireMock.aResponse()
                    .withFixedDelay(duration.toMillis().toInt())
                    .withStatus(200)
                    .withBody("token1234")
            )
    )
}

fun main() {
    // Simple case
    val options = WireMockConfiguration.options()
        .port(8080)
    val server = WireMockServer(options)
    server.start()

    stubHttpCallWithDelay()
    Thread.sleep(10000000)

    server.stop()
}
