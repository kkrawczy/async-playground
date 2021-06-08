package ch.edgelab.kkrawczyk.javanio

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class EchoClient {
    private var client: SocketChannel
    private var buffer: ByteBuffer

    init {
        client = SocketChannel.open(InetSocketAddress("localhost", 8080))
        buffer = ByteBuffer.allocate(8192)
    }

    fun stop() {
        client.close()
        buffer.clear()
    }

    fun sendMessage(msg: String): String? {
        buffer.put(ByteBuffer.wrap(msg.toByteArray()))
        val response: String? = null
        try {
            buffer.flip()
            client.write(buffer)
            buffer.clear()
            println("writing done")
            buffer.clear()
            client.read(buffer)
            buffer.flip()

            println("Response: ")
            println(StandardCharsets.UTF_8.decode(buffer).toString())
            println("--------------------------------------------")

            buffer.clear()
            client.read(buffer)
            buffer.flip()

            println("Response: ")
            println(StandardCharsets.UTF_8.decode(buffer).toString())
            println("--------------------------------------------")

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return response
    }
}

fun main() {
    val client = EchoClient()

    val msg = """
            GET /token HTTP/1.1
            Host: localhost:8080
            User-Agent: Karol
            Accept: */*
        """.trimIndent()

    val m2 = msg + "\r\n" + "\r\n"

    println(m2)
    println("")
    client.sendMessage(
        m2
    )

}
