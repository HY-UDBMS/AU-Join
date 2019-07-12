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

import com.xenomachina.argparser.mainBody
import de.mpicbg.scicomp.kutils.parmap
import fi.helsinki.cs.udbms.struct.*
import fi.helsinki.cs.udbms.util.IO
import fi.helsinki.cs.udbms.util.RuntimeParameters
import fi.helsinki.cs.udbms.util.format
import java.io.BufferedWriter
import java.io.File
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) = mainBody {
    val params = RuntimeParameters.initialise(args)

    print("Reading string... ")
    val list1 = IO.readSegmentedStrings(params.list1)
    val list2 = IO.readSegmentedStrings(params.list2)
    println("${list1.size} + ${list2.size} strings loaded")

    var syn: SynonymKnowledge? = null
    if (params.synonym.isNotEmpty()) {
        print("Reading synonym... ")
        syn = IO.readSynonym(params.synonym)
        println("${syn.knowledge.size} rules loaded")
    }
    var tax: TaxonomyKnowledge? = null
    if (params.taxonomy.isNotEmpty()) {
        print("Reading taxonomy... ")
        tax = IO.readTaxonomy("data/mesh.taxonomy.txt")
        println("${tax.knowledge.size} nodes loaded")
    }

    var signatures1: Map<SegmentedString, List<Pebble>> = emptyMap()
    var signatures2: Map<SegmentedString, List<Pebble>> = emptyMap()

    run {
        print("Generating pebbles... ")
        val pebbles1 = list1.parmap { Pair(it, PebbleGenerator(syn, tax, params.gram).generate(it)) }.toMap()
        val pebbles2 = list2.parmap { Pair(it, PebbleGenerator(syn, tax, params.gram).generate(it)) }.toMap()
        println("${pebbles1.values.sumBy { it.size }} + ${pebbles2.values.sumBy { it.size }} pebbles generated")

        println("Initialising global ordering... ")
        val order = GlobalOrder()
        order.addAll(pebbles1.values.flatten())
        order.addAll(pebbles2.values.flatten())

        print("Selecting prefixes... ")
        val time = measureTimeMillis {
            val reducer = FastPebbleReducer(params.threshold, params.overlap, order)
            signatures1 = list1.parmap { Pair(it, reducer.reduce(it, pebbles1[it] ?: emptyList())) }.toMap()
            signatures2 = list2.parmap { Pair(it, reducer.reduce(it, pebbles2[it] ?: emptyList())) }.toMap()
        }
        println("${signatures1.values.sumBy { it.size }} + ${signatures2.values.sumBy { it.size }} pebbles as signatures in $time ms")
    }.run { println("Cleansing up... "); System.gc(); System.runFinalization(); }

    println("Building inverted list... ")
    //val index1 = InvertedIndex()
    //signatures1.map { str -> str.value.map { p -> index1.add(p, p.segment) } }

    val index2 = InvertedIndex()
    signatures2.map { str -> str.value.map { p -> index2.add(p, p.segment) } }

    print("Filtering on ${params.threads} threads... ")
    var candidates: List<SegmentedStringPair> = emptyList()
    var time = measureTimeMillis {
        candidates = AdaptivePrefixFilter(params.threshold, params.overlap).getCandidates(signatures1, index2)
    }
    println("${candidates.size} candidates obtained in $time ms")

    print("Verifying on ${params.threads} threads... ")
    var results: List<Pair<SegmentedStringPair, ClosedRange<Double>>> = emptyList()
    time = measureTimeMillis {
        val verifier = GreedySimilarityVerifier(params.threshold, syn, tax, params.gram)
        results =
            candidates.parmap(
                numThreads = params.threads,
                transform = { Pair(it, verifier.getSimilarity(it.first, it.second)) })
                .filter { it.second.endInclusive >= params.threshold }
                .toList()
    }
    println("${results.size} results obtained in $time ms")

    val bw: BufferedWriter? = if (params.output.isNotEmpty()) File(params.output).bufferedWriter() else null
    if (bw != null) println("Writing results to ${params.output}... ") else println()

    results.sortedBy { it.first.second.id }.sortedBy { it.first.first.id }.withIndex().forEach {
        val str = it.value.first
        val sim = it.value.second
        if (bw == null) {
            println(
                "  ${it.index}: "
                        + "(${str.first.id}, ${str.second.id}) has similarity "
                        + "[${sim.start.format(3)}, ${sim.endInclusive.format(3)}]"
            )
        } else {
            bw.write("${str.first.id},${str.second.id},${sim.start},${sim.endInclusive}")
            bw.newLine()
        }
    }

    bw?.close()

    return@mainBody
}
