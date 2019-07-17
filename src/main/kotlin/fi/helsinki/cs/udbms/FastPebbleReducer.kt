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

class FastPebbleReducer(threshold: Double, overlap: Int, order: GlobalOrder) :
    PebbleReducer(threshold, overlap, order) {
    override fun reduce(str: SegmentedString, pebbles: Iterable<Pebble>): List<Pebble> {
        val bound = threshold * str.minPartitionSize
        val pebbleRemaining = pebbles.sortedByDescending { order.getOrder(it) }.toMutableList()

        val store = mutableMapOf<Segment, Array<Double>>()
        var accSim = 0.0

        while (pebbleRemaining.size > 1) {
            val it = pebbleRemaining.first()
            pebbleRemaining.removeAt(0) // delete the first pebble

            store.putIfAbsent(it.segment, Array((KnowledgeType.values().map { it.id }.max() ?: 0) + 1) { 0.0 })

            accSim -= store[it.segment]?.max() ?: 0.0

            val old = store[it.segment]?.get(it.type.id) ?: 0.0
            store[it.segment]?.set(it.type.id, old + it.weight)

            accSim += store[it.segment]?.max() ?: 0.0

            // [overlap-1] heaviest pebbles
            val futureSim = pebbleRemaining.map { it.weight }.sortedDescending().take(overlap - 1).sum()

            if (accSim + futureSim >= bound) { // if too much, add it back and stop
                pebbleRemaining.add(0, it)
                break
            }
            if (pebbleRemaining.isEmpty()) return listOf(it)
        }

        return pebbleRemaining.toList()
    }
}