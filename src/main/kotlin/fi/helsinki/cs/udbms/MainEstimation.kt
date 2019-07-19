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
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow
import kotlin.system.measureTimeMillis

fun main(args: Array<String>): Unit = mainBody {
    println("Arguments: ${args.joinToString(separator = " ")}")

    val params = EstimationParameters.initialise(args)
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

    val pebbles1 = list1.mapParallel { Pair(it, PebbleGenerator(syn, tax, params.gram).generate(it)) }.toMap()
    val pebbles2 = list2.mapParallel { Pair(it, PebbleGenerator(syn, tax, params.gram).generate(it)) }.toMap()

    val order = GlobalOrder()
    order.addAll(pebbles1.values.flatten())
    order.addAll(pebbles2.values.flatten())

    /*=================================================================*/

    print("Test driving... ")
    var verified = false
    val (filterTime, verifyTime) = params.overlapList.map {
        val result = testDrive(
            params, list1.shuffled().take(2000), list2.shuffled().take(2000),
            pebbles1, pebbles2, order, syn, tax, it, verified
        )
        verified = true
        return@map Pair(result.filterTime, result.verifyTime)
    }.reduceIndexed { i, acc, now ->
        Pair(
            acc.first + (now.first - acc.first) / (i + 1),
            if (now.second == 0.0) acc.second else acc.second + (acc.second - acc.second) / (i + 1)
        )
    }
    println("filtering time ${filterTime.format(4)} ms/pair, verification time ${verifyTime.format(4)} ms/candidate")

    /*=================================================================*/

    print("\t")
    print("i\t")
    params.overlapList.forEach { print("mean$it\t") }
    params.overlapList.forEach { print("error$it\t") }
    println()

    /*=================================================================*/

    val p1 = params.sampleSize.toDouble() / list1.size
    val p2 = params.sampleSize.toDouble() / list2.size

    var lastEstimations = emptyMap<Int, Estimation>() // Map[overlap, estimation]

    var iteration = 0
    while (++iteration <= params.iteration) {
        val sample1 = list1.getBernoulliSample(p1).toList()
        val sample2 = list2.getBernoulliSample(p2).toList()

        val estimations = params.overlapList.map { overlap ->
            val result = testDrive(params, sample1, sample2, pebbles1, pebbles2, order, syn, tax, overlap, true)

            val scaledCost =
                result.numberOfPairs / (p1 * p2) * filterTime + result.numberOfCandidates / (p1 * p2) * verifyTime
            val mean = movingMean(scaledCost, iteration, lastEstimations[overlap])
            val variance = movingVariance(scaledCost, iteration, lastEstimations[overlap])
            val error = params.quantile * (variance / iteration).pow(0.5)

            return@map Estimation(
                iteration, overlap, result.numberOfPairs, result.numberOfCandidates,
                scaledCost, mean, variance, error
            )
        }.associateBy { it.overlap }

        print("\t")
        print("$iteration\t")
        estimations.forEach { print("${it.value.meanOfScaledCost.format(0)}\t") }
        estimations.forEach { print("${it.value.errorOfScaledCost.format(0)}\t") }
        println()

        lastEstimations = estimations

        // should we stop here?
        if (shouldStop(iteration, estimations))
            break
    }

    print("Overlap parameters from the best to the worst: ")
    print(lastEstimations.values.sortedBy { it.meanOfScaledCost }.map { it.overlap }.joinToString())
    println()

    Dispatcher.shutdown()
    return@mainBody
}

private fun shouldStop(iteration: Int, estimations: Map<Int, Estimation>): Boolean {
    if (estimations.size == 1) return true // if only one candidate, stop anyway
    if (iteration < 5) return false // discard instability in early stages

    val estimationsSorted = estimations.values.sortedBy { it.scaledCost }

    return estimationsSorted.first().getMaxScaledCost() < estimationsSorted.drop(1).first().getMinScaledCost()
}

private fun <T> Iterable<T>.getBernoulliSample(p: Double): Iterable<T> {
    @Suppress("UNCHECKED_CAST")
    return this.mapParallel { if (ThreadLocalRandom.current().nextDouble(1.0) < p) it else Unit }.filterNot { it == Unit } as Iterable<T>
}

// region Estimation

private fun movingMean(estimate: Double, iteration: Int, last: Estimation?): Double {
    val lastSafe = last ?: Estimation()

    if (iteration == 1)
        return estimate

    return lastSafe.meanOfScaledCost + (estimate - lastSafe.meanOfScaledCost) / iteration
}

private fun movingVariance(estimate: Double, iteration: Int, last: Estimation?): Double {
    val lastSafe = last ?: Estimation()

    if (iteration == 1)
        return 0.0

    return (iteration - 2).toDouble() / (iteration - 1).toDouble() * lastSafe.varianceOfScaledCost +
            ((estimate - lastSafe.meanOfScaledCost).pow(2) / iteration)
}

private data class Estimation(
    val iteration: Int = 0,
    val overlap: Int = 0,
    val numberOfPairs: Int = 0,
    val numberOfCandidates: Int = 0,
    val scaledCost: Double = 0.0,
    val meanOfScaledCost: Double = 0.0,
    val varianceOfScaledCost: Double = 0.0,
    val errorOfScaledCost: Double = 0.0
) {
    fun getMinScaledCost() = scaledCost - errorOfScaledCost
    fun getMaxScaledCost() = scaledCost + errorOfScaledCost
}

// endregion

// region TestDrive

private fun testDrive(
    params: EstimationParameters,
    list1: List<SegmentedString>,
    list2: List<SegmentedString>,
    pebbles1: Map<SegmentedString, List<Pebble>>,
    pebbles2: Map<SegmentedString, List<Pebble>>,
    order: GlobalOrder,
    syn: SynonymKnowledge?,
    tax: TaxonomyKnowledge?,
    overlap: Int,
    skipVerify: Boolean
): TestDriveResult {
    System.gc(); System.runFinalization()

    /*=================================================================*/

    var signatures1: Map<SegmentedString, List<Pebble>> = emptyMap()
    var signatures2: Map<SegmentedString, List<Pebble>> = emptyMap()

    run {
        val reducer = when (params.filter) {
            "Fast" -> FastPebbleReducer(params.threshold, overlap, order)
            "DP" -> DynamicProgrammingPebbleReducer(params.threshold, overlap, order)
            else -> throw Exception("Invalid filtering method: ${params.filter}")
        }

        signatures1 = list1.mapParallel { Pair(it, reducer.reduce(it, pebbles1[it] ?: emptyList())) }.toMap()
        signatures2 = list2.mapParallel { Pair(it, reducer.reduce(it, pebbles2[it] ?: emptyList())) }.toMap()
    }.run { System.gc(); System.runFinalization(); }

    /*=================================================================*/

    val index2 = InvertedIndex()
    signatures2.map { str -> str.value.map { p -> index2.add(p, p.segment) } }

    /*=================================================================*/

    var candidates: List<SegmentedStringPair> = emptyList()
    var numberOfPairs = 0
    val filterTimeTotal = measureTimeMillis {
        val result = AdaptivePrefixFilter(params.threshold, overlap).getCandidates(signatures1, index2)
        candidates = result.first
        numberOfPairs = result.second
    }

    /*=================================================================*/

    var verifyTimeTotal = 0L
    if (!skipVerify) {
        val verifier = when (params.verify) {
            "Greedy" -> GreedySimilarityVerifier(params.threshold, syn, tax, params.gram)
            "SquareImp" -> SquareImpSimilarityVerifier(params.threshold, syn, tax, params.gram)
            "SquareImp-Improved" -> SquareImpSimilarityVerifier(params.threshold, syn, tax, params.gram, true)
            else -> throw Exception("Invalid verification method: ${params.verify}")
        }

        verifyTimeTotal = measureTimeMillis {
            candidates.mapParallelOrSequential { Pair(it, verifier.getSimilarity(it.first, it.second)) }
                .filter { it.second.start >= params.threshold }
                .toList()
        }
    }
    /*=================================================================*/

    System.gc(); System.runFinalization()

    return TestDriveResult(
        filterTimeTotal.toDouble() / numberOfPairs,
        verifyTimeTotal.toDouble() / candidates.size,
        numberOfPairs,
        candidates.size
    )
}

private data class TestDriveResult(
    val filterTime: Double,
    val verifyTime: Double,
    val numberOfPairs: Int,
    val numberOfCandidates: Int
)

// endregion
