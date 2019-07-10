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

class FastPebbleReducer(threshold: Double, order: GlobalOrder) : PebbleReducer(threshold, order) {
    override fun reduce(str: SegmentedString, pebbles: Iterable<Pebble>): List<Pebble> {
        val bound = threshold * getMinPartitionSize(str)
        val pebblesSorted = pebbles.sortedByDescending { order.getOrder(it) }.toMutableList()

        val sims = mutableMapOf<Segment, Array<Double>>()
        var removed = 0.0

        while (pebblesSorted.size > 1) {
            val it = pebblesSorted.first()
            pebblesSorted.removeAt(0) // delete the first pebble

            sims.putIfAbsent(it.segment, Array(KnowledgeType.values().size) { 0.0 })

            removed -= sims[it.segment]?.max() ?: 0.0

            val old = sims[it.segment]?.get(it.type.id) ?: 0.0
            sims[it.segment]?.set(it.type.id, old + it.weight)

            removed += sims[it.segment]?.max() ?: 0.0

            if (removed >= bound) { // if too much, add it back and stop
                pebblesSorted.add(0, it)
                break
            }
        }

        return pebblesSorted.toList()
    }
}