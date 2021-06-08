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
class Server {
    val host = "localhost"
    val port = 9090
    private var readBuffer: ByteBuffer = ByteBuffer.allocate(8192)
    private val selector: Selector
    private val serverChannel: ServerSocketChannel
    private val dataToWrite: ByteBuffer = ByteBuffer.allocate(8192)

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

                //logger.info { "key $key" }
                when {
                    key.isAcceptable -> accept(key)
                    key.isReadable -> read(key)
                    key.isWritable -> write(key)
                }
            }
        }
    }

    private fun write(key: SelectionKey) {
        Thread.sleep(100)
//        val date: LocalDateTime? = (key.attachment() as LocalDateTime?)
//        if (date == null) {
//            key.attach(LocalDateTime.now())
//            return
//        } else {
//            if (Duration.between(date, LocalDateTime.now()).toMillis() < 10000)
//                return
//        }

        val socketChannel = key.channel() as SocketChannel
        val response = """
            HTTP/1.1 200 OK
            Date: Wed, 11 Apr 2012 21:29:04 GMT
            Server: Python/6.6.6 (custom)
            Content-Type: text/html

            <html><body>Hello World</body></html>\n
        """.trimIndent()

        dataToWrite.clear()
        dataToWrite.put(response.toByteArray())
        dataToWrite.flip()
        socketChannel.write(dataToWrite)
        socketChannel.finishConnect()
        socketChannel.close()
    }

    private fun read(key: SelectionKey) {
        val socketChannel = key.channel() as SocketChannel
        readBuffer.clear()
        val readCode = socketChannel.read(this.readBuffer)
        if (readCode == -1) {
            key.channel().close()
        }

        readBuffer.flip()

        val s = StandardCharsets.UTF_8.decode(readBuffer).toString()
        logger.info { "Received request:" }
        logger.info { s }
        socketChannel.register(selector, SelectionKey.OP_WRITE)

//        readBuffer.clear()
//        readBuffer.put(response.encodeToByteArray())
//        println("${readBuffer.position()} ${readBuffer.limit()}")
//        readBuffer.flip()
//        println("${readBuffer.position()} ${readBuffer.limit()}")
//        socketChannel.write(readBuffer)
//        key.channel().close()
    }


    private fun accept(key: SelectionKey) {
        logger.info { "accepting" }
        val socketChannel = (key.channel() as ServerSocketChannel).accept()
        socketChannel.configureBlocking(false)
        socketChannel.register(selector, SelectionKey.OP_READ)
    }
}

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "hre" }
    Server().run()
}
