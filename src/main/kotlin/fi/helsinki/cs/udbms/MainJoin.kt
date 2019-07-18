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
import fi.helsinki.cs.udbms.struct.*
import fi.helsinki.cs.udbms.util.*
import java.io.BufferedWriter
import java.io.File
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) = mainBody {
    val params = JoinParameters.initialise(args)
    Dispatcher.initialise(params.singleThread)

    /*=================================================================*/

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
        tax = IO.readTaxonomy(params.taxonomy)
        println("${tax.knowledge.size} nodes loaded")
    }

    /*=================================================================*/

    var signatures1: Map<SegmentedString, List<Pebble>> = emptyMap()
    var signatures2: Map<SegmentedString, List<Pebble>> = emptyMap()

    run {
        print("Generating pebbles... ")
        val pebbles1 = list1.mapParallel { Pair(it, PebbleGenerator(syn, tax, params.gram).generate(it)) }.toMap()
        val pebbles2 = list2.mapParallel { Pair(it, PebbleGenerator(syn, tax, params.gram).generate(it)) }.toMap()
        println("${pebbles1.values.sumBy { it.size }} + ${pebbles2.values.sumBy { it.size }} pebbles generated")

        println("Initialising global ordering... ")
        val order = GlobalOrder()
        order.addAll(pebbles1.values.flatten())
        order.addAll(pebbles2.values.flatten())

        /*=================================================================*/

        val reducer = when (params.filter) {
            "Fast" -> FastPebbleReducer(params.threshold, params.overlap, order)
            "DP" -> DynamicProgrammingPebbleReducer(params.threshold, params.overlap, order)
            else -> throw Exception("Invalid filtering method: ${params.filter}")
        }

        print("Selecting prefixes... ")
        val time = measureTimeMillis {
            signatures1 = list1.mapParallel { Pair(it, reducer.reduce(it, pebbles1[it] ?: emptyList())) }.toMap()
            signatures2 = list2.mapParallel { Pair(it, reducer.reduce(it, pebbles2[it] ?: emptyList())) }.toMap()
        }
        println("${signatures1.values.sumBy { it.size }} + ${signatures2.values.sumBy { it.size }} pebbles as signatures in $time ms")
    }.run { println("Cleansing up... "); System.gc(); System.runFinalization(); }

    /*=================================================================*/

    println("Building inverted list... ")
    val index2 = InvertedIndex()
    signatures2.map { str -> str.value.map { p -> index2.add(p, p.segment) } }

    /*=================================================================*/

    print("Filtering using ${params.filter} on ${if (params.singleThread) "a single thread" else "multiple threads"}... ")
    var candidates: List<SegmentedStringPair> = emptyList()
    var time = measureTimeMillis {
        candidates = AdaptivePrefixFilter(params.threshold, params.overlap).getCandidates(signatures1, index2).first
    }
    println("${candidates.size} candidates obtained in $time ms")

    /*=================================================================*/

    val verifier = when (params.verify) {
        "Greedy" -> GreedySimilarityVerifier(params.threshold, syn, tax, params.gram)
        "SquareImp" -> SquareImpSimilarityVerifier(params.threshold, syn, tax, params.gram)
        "SquareImp-Improved" -> SquareImpSimilarityVerifier(params.threshold, syn, tax, params.gram, true)
        else -> throw Exception("Invalid verification method: ${params.verify}")
    }

    print("Verifying using ${params.verify} on ${if (params.singleThread) "a single thread" else "multiple threads"}... ")
    var results: List<Pair<SegmentedStringPair, ClosedRange<Double>>> = emptyList()
    time = measureTimeMillis {
        results =
            candidates.mapParallelOrSequential { Pair(it, verifier.getSimilarity(it.first, it.second)) }
                .filter { it.second.start >= params.threshold }
                .toList()
    }
    println("${results.size} results obtained in $time ms")

    /*=================================================================*/

    val bw: BufferedWriter? = when (params.output) {
        "null" -> null
        "stdout" -> null
        else -> {
            print("Writing results to ${params.output}... ")
            val w = File(params.output).bufferedWriter()
            w.write("string_1,string_2,sim_min,sim_max")
            w.newLine()
            w
        }
    }
    println()

    results.sortedBy { it.first.second.id }.sortedBy { it.first.first.id }.withIndex().forEach {
        val str = it.value.first
        val sim = it.value.second
        when (params.output) {
            "null" -> {
            }
            "stdout" -> println(
                "  ${it.index}: "
                        + "(${str.first.id}, ${str.second.id}) has similarity "
                        + "[${sim.start.format(3)}, ${sim.endInclusive.format(3)}]"
            )
            else -> {
                bw!!.write("${str.first.id},${str.second.id},${sim.start},${sim.endInclusive}")
                bw.newLine()
            }
        }
    }

    bw?.close()

    /*=================================================================*/

    Dispatcher.shutdown()
    return@mainBody
}
