// 
// MIT License
// 
// Copyright (c) 2017 Holger Brandl
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
// 

package de.mpicbg.scicomp.kutils

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Parallel collection mimicking  scala's .par().
 */

/** A delegated tagging interface to allow for parallized extension functions */
class ParCol<T>(val it: Iterable<T>, val executorService: ExecutorService) : Iterable<T> by it

/** Convert a stream into a parallel collection. */
fun <T> Iterable<T>.par(
    numThreads: Int = maxOf(Runtime.getRuntime().availableProcessors() - 2, 1),
    executorService: ExecutorService = Executors.newFixedThreadPool(numThreads)
): ParCol<T> {
    return ParCol(this, executorService)
}

/** De-parallelize a collection. Undos <code>par</code> */
fun <T> ParCol<T>.unpar(): Iterable<T> {
    this.executorService.shutdown()
    return this.it
}

fun <T, R> ParCol<T>.map(transform: (T) -> R): ParCol<R> {
    val destination = ConcurrentLinkedQueue<R>()

    // http://stackoverflow.com/questions/3269445/executorservice-how-to-wait-for-all-tasks-to-finish
    // use futures here to allow for recycling of the executorservice
    val futures = this.asIterable().map { executorService.submit { destination.add(transform(it)) } }
    futures.map { it.get() } // this will block until all are done

    //    executorService.shutdown()
    //    executorService.awaitTermination(1, TimeUnit.DAYS)

    return ParCol(destination, executorService)
}

/** A parallelizable version of kotlin.collections.map
 * @param numThreads The number of parallel threads. Defaults to number of cores minus 2
 * @param exec The executor. By exposing this as a parameter, application can share an executor among different parallel
 *             mapping taks.
 */
fun <T, R> Iterable<T>.parmap(
    numThreads: Int = maxOf(Runtime.getRuntime().availableProcessors() - 2, 1),
    exec: ExecutorService = Executors.newFixedThreadPool(numThreads),
    transform: (T) -> R
): Iterable<R> {
    return this.par(executorService = exec).map(transform).unpar()
}

internal fun main(args: Array<String>) {
    val totalLength =
        listOf("test", "sdf", "foo").par().map { it.length }.map { it + 2 }.fold(0, { sum, el -> sum + el })
    println(totalLength)

    //    listOf("test", "sdf", "foo").with {}
}

