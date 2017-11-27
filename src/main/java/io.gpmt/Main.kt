package io.gpmt

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService
import rx.schedulers.Schedulers.computation
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    for (i in 0..0) {
        thread {
            ws()
        }
    }
    //val service = UnixIpcService("/tmp/docker2/jsonrpc.ipc")
    //func()
    val service = HttpService("http://35.189.68.234:8545")
    val web3 = Web3j.build(service)
//    val start = System.currentTimeMillis()
    val startBlock = 4632253L
//    val count = 16
//    val x = AtomicLong(0)
//    val length = AtomicLong(0)
//    Observable.range(startBlock, count).flatMap {
//        x.incrementAndGet()
//        val num = "0x" + BigInteger(it.toString()).toString(16)
//        println(num)
//        Request<Any, AnyResponse>("debug_traceBlockByNumber", mutableListOf<Any>(num), service, AnyResponse::class.java)
//                .observable()
//    }.subscribe({
//        println("x: "+ it.result)
//    }, {
//        it.printStackTrace()
//        println("error! " + it)
//    }, {
//        val end = System.currentTimeMillis()
//        println("Time: " + (end - start).toString() + " " + x.get().toString() + " " + length.get().toString())
//    })
//    Thread.sleep(60000)
//    web3.catchUpToLatestBlockObservable(DefaultBlockParameter.valueOf(BigInteger.valueOf(startBlock.toLong())), false)
//            //.doOnNext { println(it.block.transactions.size.toString()) }
//            .flatMapIterable { it.block.transactions }
//            .map { it.get() as String }
//            //.doOnNext { println(it) }
//            //.doOnNext { println(Thread.currentThread()) }
//            .flatMap {
//                //x.incrementAndGet()
//                Request<Any, AnyResponse>("debug_traceTransaction", mutableListOf<Any>(it), service, AnyResponse::class.java)
//                        .observable().subscribeOn(io())
//            }
////            .flatMap { web3.ethGetTransactionByHash(it).observable().subscribeOn(computation()) }
//            //.doOnNext { println("y: " + it.result) }
////            .doOnNext { length.addAndGet(it.result.input.length.toLong()) }
//            .subscribe({
//
//            }, {
//                it.printStackTrace()
//                println("error! " + it)
//            }, {
//                val end = System.currentTimeMillis()
//                //println("Time: " + (end - start).toString() + " " + x.get().toString() + " " + length.get().toString())
//            })
}

fun func() {
    val start = System.currentTimeMillis()
    val service = HttpService()
    Request<Any, AnyResponse>("trace_block", mutableListOf<Any>("0x2C77B4"), service, AnyResponse::class.java)
            .observable().subscribeOn(computation())
            .subscribe({
                println(it.result)
            }, {

            }, {
                val end = System.currentTimeMillis()
                println("Time: " + (end - start).toString())
            })
}

data class Json(
        val id: String?,
        val method: String?
) {
    val identifier: String get() = if (id != null) id else method!!
}

data class JsonBlock(
        val result: Result2
)

data class Result2(
        val transactions: List<String>
)

data class JsonSub(
        val params: Params
)

data class Params(
        val result: Result
)

data class Result(
        val number: String,
        val hash: String
)

fun ws() {
    val request = okhttp3.Request.Builder().url("ws://35.189.68.234:8546").build()
    val respCount = AtomicLong(0)
    val reqCount = AtomicLong(1)
    var webSocket1: WebSocket? = null
    var webSockets: MutableList<WebSocket> = mutableListOf()
    val listener: WebSocketListener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val resp = Gson().fromJson(text, Json::class.java)
            if (resp.identifier == "eth_getBlockByNumber") {
                val resp2 = Gson().fromJson(text, JsonBlock::class.java)
                resp2.result.transactions.take(40).forEachIndexed { i, tx ->
                    reqCount.addAndGet(1)
                    webSockets[i % webSockets.size].getTrace(tx)
                }
            } else if (resp.identifier == "eth_subscription") {
                val resp2 = Gson().fromJson(text, JsonSub::class.java)
                reqCount.incrementAndGet()
                webSocket.getBlock(resp2.params.result.number)
            } else if (resp.identifier == "debug_traceTransaction") {
                //println(text)
            }
            println(resp.identifier)
            if (resp.id != null) {
                respCount.incrementAndGet()
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            println("bytes: $bytes")
        }

        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            println("open")
        }

        override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
            println("close")
        }
    }
    webSocket1 = OkHttpClient().newWebSocket(request, listener)
    for (i in 0..3) {
        webSockets.add(OkHttpClient().newWebSocket(request, listener))
    }
    thread {
        while (true) {
            println("count: ${reqCount.get() - respCount.get()}")
            Thread.sleep(500)
        }
    }
//    while (true) {
//        webSocket.getTrace("0x125c10a4df211b5b0c0271c41421b5a5d12f6ed3d57cf43d78461d375f05810d")
//        reqCount.incrementAndGet()
//        Thread.sleep(2000)
//    }
    webSocket1.subscribe()
    //Thread.sleep(5000)
}

fun WebSocket.subscribe(): Boolean {
    return send("{\"jsonrpc\":\"2.0\", \"method\":\"eth_subscribe\", \"id\":\"1\", \"params\": [\"newHeads\", {\"includeTransactions\":true}]}")
}

fun WebSocket.getLatestBlock(): Boolean {
    return send("{\"jsonrpc\":\"2.0\", \"method\":\"eth_getLatestBlock\", \"id\":\"1\", \"params\": []}")
}

fun WebSocket.getBlock(blockNumber: String): Boolean {
    return send("{\"jsonrpc\":\"2.0\", \"method\":\"eth_getBlockByNumber\", \"id\":\"eth_getBlockByNumber\", \"params\": [\"$blockNumber\", false]}")
}

fun WebSocket.getReceipt(tx: String): Boolean {
    return send("{\"jsonrpc\":\"2.0\", \"method\":\"eth_getTransactionReceipt\", \"id\":\"eth_getTransactionReceipt\", \"params\": [\"$tx\"]}")
}

fun WebSocket.getTrace(tx: String) {
    send("{\"jsonrpc\":\"2.0\", \"method\":\"debug_traceTransaction\", \"id\":\"debug_traceTransaction\", \"params\": [\"$tx\"]}")
}

fun WebSocket.getTraceBlock(blockNumber: String) {
    send("{\"jsonrpc\":\"2.0\", \"method\":\"debug_traceBlockByNumber\", \"id\":\"1\", \"params\": [\"$blockNumber\"]}")
}

fun WebSocket.getTraceParity(tx: String) {
    send("{\"jsonrpc\":\"2.0\", \"method\":\"trace_replayTransaction\", \"id\":\"1\", \"params\": [\"$tx\", [\"trace\", \"vmTrace\", \"stateDiff\"]]}")
}

class AnyResponse : Response<Any>() {
}