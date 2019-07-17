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

import fi.helsinki.cs.udbms.struct.*

class DynamicProgrammingPebbleReducer(threshold: Double, overlap: Int, order: GlobalOrder) :
    PebbleReducer(threshold, overlap, order) {
    override fun reduce(str: SegmentedString, pebbles: Iterable<Pebble>): List<Pebble> {
        val bound = threshold * str.minPartitionSize
        val pebblesSorted = pebbles.sortedBy { order.getOrder(it) }

        val store = mutableMapOf<Segment, Array<Double>>()
        var accSim = 0.0

        var i = pebblesSorted.size - 1
        while (i > 0) {
            val dp = Array(str.segments.size + 1) { Array(overlap) { 0.0 } }
            val acc = Array(str.segments.size + 1) { Array(overlap) { 0.0 } }

            (1 until str.segments.size).forEach { p ->
                // fill V[p,_] by Equation 13
                (1 until overlap).map { c ->
                    acc[p][c] = getR(pebblesSorted, str.segments[p], i, c) - getR(pebblesSorted, str.segments[p], i, 0)
                }

                (1 until overlap).forEach { d ->
                    dp[p][d] = (0..d).map { c -> dp[p - 1][d - c] + acc[p][c] }.max() ?: 0.0
                    if (accSim + dp[p][d] >= bound)
                        return pebblesSorted.slice(0..i) // early termination
                }
            }

            // refresh accSim
            val it = pebblesSorted[i]
            store.putIfAbsent(it.segment, Array((KnowledgeType.values().map { it.id }.max() ?: 0) + 1) { 0.0 })
            accSim -= store[it.segment]?.max() ?: 0.0

            val old = store[it.segment]?.get(it.type.id) ?: 0.0
            store[it.segment]?.set(it.type.id, old + it.weight)

            accSim += store[it.segment]?.max() ?: 0.0

            i--
        }

        return pebblesSorted.take(1)
    }

    // Equation 14
    private fun getR(
        pebblesSorted: List<Pebble>,
        p: Segment,
        i: Int,
        c: Int
    ): Double {
        return KnowledgeType.values().map { f ->
            val r1 = pebblesSorted
                .slice(i until pebblesSorted.size)
                .asSequence()
                .filter { it.segment == p }
                .filter { it.type == f }
                .map { it.weight }
                .sum()

            if (c == 0) return@map r1

            val r2 = pebblesSorted
                .slice(0 until i)
                .asSequence()
                .filter { it.segment == p }
                .filter { it.type == f }
                .map { it.weight }
                .sortedDescending()
                .take(c)
                .sum()

            return@map r1 + r2
        }.max() ?: 0.0
    }
}