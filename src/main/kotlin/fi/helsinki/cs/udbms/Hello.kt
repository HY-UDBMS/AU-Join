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

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import de.mpicbg.scicomp.kutils.parmap
import fi.helsinki.cs.udbms.struct.*
import fi.helsinki.cs.udbms.util.IO
import fi.helsinki.cs.udbms.util.RuntimeParameters
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) = mainBody {
    val params = ArgParser(args).parseInto(::RuntimeParameters)

    print("Reading string......... ")
    val list1 = IO.readSegmentedStrings(params.list1)
    val list2 = IO.readSegmentedStrings(params.list2)
    println("${list1.size} + ${list2.size} strings loaded")

    var syn: SynonymKnowledge? = null
    if (params.synonym.isNotEmpty()) {
        print("Reading synonym......... ")
        syn = IO.readSynonym(params.synonym)
        println("${syn.knowledge.size} rules loaded")
    }

    var tax: TaxonomyKnowledge? = null
    if (params.taxonomy.isNotEmpty()) {
        print("Reading taxonomy......... ")
        tax = IO.readTaxonomy("data/mesh.taxonomy.txt")
        println("${tax.knowledge.size} nodes loaded")
    }

    print("Generating pebbles......... ")
    val pebbles1 = list1.parmap { Pair(it, PebbleGenerator(syn, tax, params.gram).generate(it)) }.toMap()
    val pebbles2 = list2.parmap { Pair(it, PebbleGenerator(syn, tax, params.gram).generate(it)) }.toMap()
    println("${pebbles1.values.sumBy { it.size }} + ${pebbles2.values.sumBy { it.size }} pebbles generated")

    println("Initialising global order")
    val order = GlobalOrder()
    order.addAll(pebbles1.values.flatten())
    order.addAll(pebbles2.values.flatten())

    print("Selecting prefixes in parallel......... ")
    var signatures1: Map<SegmentedString, List<Pebble>> = emptyMap()
    var signatures2: Map<SegmentedString, List<Pebble>> = emptyMap()
    var time = measureTimeMillis {
        val reducer = FastPebbleReducer(params.threshold, params.overlap, order)
        signatures1 = list1.parmap { Pair(it, reducer.reduce(it, pebbles1[it] ?: emptyList())) }.toMap()
        signatures2 = list2.parmap { Pair(it, reducer.reduce(it, pebbles2[it] ?: emptyList())) }.toMap()
    }
    println("${signatures1.values.sumBy { it.size }} + ${signatures2.values.sumBy { it.size }} pebbles as signatures; time cost $time ms")

    val index1 = InvertedIndex()
    signatures1.map { str -> str.value.map { p -> index1.add(p, p.segment) } }

    val index2 = InvertedIndex()
    signatures2.map { str -> str.value.map { p -> index2.add(p, p.segment) } }

    print("Filtering......... ")
    var candidates: List<SegmentedStringPair> = emptyList()
    time = measureTimeMillis {
        candidates = AdaptivePrefixFilter(params.threshold, params.overlap).getCandidates(index1, index2)
    }
    println("${candidates.size} candidates obtained; time cost $time ms")
}
