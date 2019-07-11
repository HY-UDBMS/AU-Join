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

import de.mpicbg.scicomp.kutils.parmap
import fi.helsinki.cs.udbms.struct.GlobalOrder
import fi.helsinki.cs.udbms.struct.InvertedIndex
import fi.helsinki.cs.udbms.util.IO

fun main(args: Array<String>) {

    print("Reading string: ")
    val list1 = IO.readStringList("data/mesh.segments.5k.1.txt")
    println("${list1.size} strings loaded")

    print("Reading synonym: ")
    val syn = IO.readSynonym("data/mesh.synonym.txt")
    println("${syn.knowledge.size} rules loaded")

    print("Reading taxonomy: ")
    val tax = IO.readTaxonomy("data/mesh.taxonomy.txt")
    println("${tax.knowledge.size} nodes loaded")

    print("Generating pebbles: ")
    val pebbles = list1.parmap { Pair(it, PebbleGenerator(syn, tax, 4).generate(it)) }.toMap()
    println("${pebbles.values.sumBy { it.size }} pebbles generated")

    println("Initialising global order...")
    val order = GlobalOrder()
    order.addAll(pebbles.values.flatten())

    print("Selecting prefixes: ")
    val reducer = FastPebbleReducer(0.8, 1, order)
    val signatures = list1.map { Pair(it, reducer.reduce(it, pebbles[it] ?: emptyList())) }.toMap()
    println("${signatures.values.sumBy { it.size }} pebbles in prefix")

    val index = InvertedIndex()
    signatures.map { str -> str.value.map { p -> index.add(p, p.segment) } }

    val raw = index.index.toList().sortedBy { it.first.label }
}

