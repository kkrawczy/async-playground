package ch.edgelab.kkrawczyk.javanio

import java.io.IOException

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import java.nio.channels.SocketChannel

class EchoClient {
    private var client: SocketChannel
    private var buffer: ByteBuffer

    init {
        client = SocketChannel.open(InetSocketAddress("localhost", 9090))
        buffer = ByteBuffer.allocate(256)
    }

    fun stop() {
        client.close()
        buffer.clear()
    }

    fun sendMessage(msg: String): String? {
        buffer = ByteBuffer.wrap(msg.toByteArray())
        var response: String? = null
        try {
            client.write(buffer)
            buffer.clear()
            client.read(buffer)
            response = String(buffer.array())
            println("response=$response")
            buffer.clear()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return response
    }
}

fun main() {
    val client = EchoClient()

    client.sendMessage("haha");

}
