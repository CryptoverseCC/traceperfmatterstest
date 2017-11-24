package io.gpmt

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.infura.InfuraHttpService
import org.web3j.protocol.ipc.UnixIpcService
import rx.exceptions.CompositeException
import rx.schedulers.Schedulers.computation
import rx.schedulers.Schedulers.io
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicLong

fun main(args: Array<String>) {
    //val service = UnixIpcService("/tmp/docker2/jsonrpc.ipc")
    //func()
    val service = HttpService()
    val web3 = Web3j.build(service)
    val start = System.currentTimeMillis()
    val startBlock = 3339593L
    val count = 10
    val x = AtomicLong(0)
    val length = AtomicLong(0)
    web3.replayBlocksObservable(DefaultBlockParameter.valueOf(BigInteger.valueOf(startBlock)), DefaultBlockParameter.valueOf(BigInteger.valueOf(startBlock + count - 1)), false)
            //.doOnNext { println(it.block.transactions.size.toString()) }
            .flatMapIterable { it.block.transactions }
            .map { it.get() as String }
            //.doOnNext { println(it) }
            //.doOnNext { println(Thread.currentThread()) }
            .flatMap {
                x.incrementAndGet()
                Request<Any, AnyResponse>("trace_replayTransaction", mutableListOf<Any>(it, listOf("vmTrace", "trace2", "stateDiff")), service, AnyResponse::class.java)
                        .observable().subscribeOn(io())
            }
            .flatMap { web3.ethGetTransactionByHash(it).observable().subscribeOn(computation()) }
            .doOnNext { println(it.result) }
//            .doOnNext { length.addAndGet(it.result.input.length.toLong()) }
            .subscribe({

            }, {
                it.printStackTrace()
                println("error! " + it)
            }, {
                val end = System.currentTimeMillis()
                println("Time: " + (end - start).toString() + " " + x.get().toString() + " " + length.get().toString())
            })
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

class AnyResponse : Response<Any>() {
}