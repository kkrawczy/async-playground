package ch.edgelab.kkrawczyk.javanio

import mu.KotlinLogging
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import java.nio.charset.StandardCharsets

//http://rox-xmlrpc.sourceforge.net/niotut/
//https://www.baeldung.com/java-nio-selector
//http://tutorials.jenkov.com/java-nio/non-blocking-server.html
//https://github.com/netty/netty/issues/2515
//https://jvns.ca/blog/2017/06/03/async-io-on-linux--select--poll--and-epoll/
//https://news.ycombinator.com/item?id=8526264
//https://www.oreilly.com/library/view/java-nio/0596002882/ch01.html
class Server(host: String, port: Int) {
    private var buffer: ByteBuffer = ByteBuffer.allocate(8192)
    private val selector: Selector
    private val serverChannel: ServerSocketChannel

    init {
        selector = SelectorProvider.provider().openSelector()
        serverChannel = ServerSocketChannel.open()
        serverChannel.configureBlocking(false)
        serverChannel.socket().bind(InetSocketAddress(host, port))
        serverChannel.register(selector, SelectionKey.OP_ACCEPT)
    }

    fun run() {
        while (true) {
            selector.select()
            val selectedKeys = selector.selectedKeys().iterator()

            while (selectedKeys.hasNext()) {
                val key = selectedKeys.next()
                selectedKeys.remove()

                when {
                    key.isAcceptable -> accept(key)
                    key.isReadable -> read(key)
                    key.isWritable -> write(key)
                }
            }
        }
    }

    private fun accept(key: SelectionKey) {
        //maybe some filtering based on the caller IP
        val socketChannel = (key.channel() as ServerSocketChannel).accept()
        socketChannel.configureBlocking(false)
        socketChannel.register(selector, SelectionKey.OP_READ)
    }

    private fun read(key: SelectionKey) {
        val socketChannel = key.channel() as SocketChannel
        buffer.clear()
        socketChannel.read(buffer)
        buffer.flip()
        val request = StandardCharsets.UTF_8.decode(buffer).toString()
        if (request.isNotFinished()) return
        val response = processRequest(request)
        //scheduler.submit(new Handler(request, socketChannel));

        socketChannel.register(selector, SelectionKey.OP_WRITE, response)
    }

    private fun write(key: SelectionKey) {
        val socketChannel = key.channel() as SocketChannel

        val response = key.attachment() as String

        buffer.clear()
        buffer.put(response.toByteArray())
        buffer.flip()
        socketChannel.write(buffer)
        socketChannel.finishConnect()
        socketChannel.close()
    }

    private fun processRequest(request: String): String {
        logger.info { "Received request: \n$request" }
        //request processing and response
        return """
                    HTTP/1.1 200 OK
                    Date: Wed, 11 Apr 2012 21:29:04 GMT
                    Server: Karol/1.1.1 (custom)
                    Content-Type: text/html
        
                    <html><body>Hello World</body></html>
                """.trimIndent()
    }
}

private fun String.isNotFinished() = false

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting server" }
    Server("localhost", 9090).run()
}
