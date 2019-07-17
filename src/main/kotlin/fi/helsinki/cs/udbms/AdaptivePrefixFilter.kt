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
import fi.helsinki.cs.udbms.util.forEachParallelOrSequential
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class AdaptivePrefixFilter(private val threshold: Double, private val overlap: Int) {

    fun getCandidates(signature1: Map<SegmentedString, List<Pebble>>, index2: InvertedIndex)
            : List<SegmentedStringPair> {
        val candidatePairs = ConcurrentHashMap<SegmentedStringPair, Unit>()

        signature1.entries.forEachParallelOrSequential { (string1, keys1) ->
            val usedSegments = mutableMapOf<SegmentedString, MutableSet<Segment>>()
            val overlapCounts = mutableMapOf<SegmentedString, Int>()
            val candidate2 = mutableSetOf<SegmentedString>()

            keys1.forEach nextKey@{ key1 ->
                val segment1 = key1.segment
                val list2 = index2.getList(key1) ?: return@nextKey

                list2.forEach nextString2@{ segment2 ->
                    val string2 = segment2.segmentedString ?: return@nextString2

                    if (candidate2.contains(string2)) return@nextString2

                    // length filter
                    if (min(string1.minPartitionSize, string2.minPartitionSize)
                        < threshold * max(string1.minPartitionSize, string2.minPartitionSize)
                    ) return@nextString2

                    // check if either segment1 or segment2 is used by any other pebble pair
                    val used = usedSegments.getOrPut(string2, { mutableSetOf() })
                    if (used.contains(segment1) xor used.contains(segment2))
                        return@nextString2
                    // also check for conflict segments
                    if (used.intersect(segment1.conflictSegments).isNotEmpty() || used.intersect(segment2.conflictSegments).isNotEmpty())
                        return@nextString2

                    // mark segment1 and segment2 as used for string2
                    used.add(segment1); used.add(segment2)

                    overlapCounts[string2] = overlapCounts.getOrDefault(string2, 0) + 1

                    if (overlapCounts[string2] ?: 0 >= overlap) {
                        candidate2.add(string2)
                        candidatePairs[SegmentedStringPair(string1, string2)] = Unit
                    }
                }
            }
        }

        return candidatePairs.keys.toList()
    }
}
