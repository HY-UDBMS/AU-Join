//
// MIT License
//
// Copyright (c) 2019 pengxu
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

package fi.helsinki.cs.udbms

import fi.helsinki.cs.udbms.struct.GlobalOrder
import fi.helsinki.cs.udbms.util.IO
import fi.helsinki.cs.udbms.util.pmap
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) = runBlocking {

    println("Read strings ...")
    val list1 = IO.readStringList("data/mesh.segments.txt")
    val syn = IO.readSynonym("data/mesh.synonym.txt")
    val tax = IO.readTaxonomy("data/mesh.taxonomy.txt")

    println("Generating pebbles ...")

    val pg = PebbleGenerator(syn, tax, 3)

    val pebbles = list1.pmap { Pair(it, pg.generate(it)) }.toMap()


    val order = GlobalOrder()
    order.addAll(pebbles.values.flatten())

    println("Prefix reduction ...")

    val reducer = FastPebbleReducer(0.8, order)

    var time = measureTimeMillis {
        val signatures = list1.parallelStream().map { reducer.reduce(it, pebbles[it] ?: emptyList()) }

        val jb = signatures.toArray()
    }
    println(time)
    time = measureTimeMillis {
        val signatures = list1.pmap { reducer.reduce(it, pebbles[it] ?: emptyList()) }

        val d = signatures.size
    }
    println(time)

    println("test")
}

