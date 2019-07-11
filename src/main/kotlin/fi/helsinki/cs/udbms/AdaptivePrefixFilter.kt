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

import fi.helsinki.cs.udbms.struct.InvertedIndex
import fi.helsinki.cs.udbms.struct.Segment
import fi.helsinki.cs.udbms.struct.SegmentedStringPair
import kotlin.math.max
import kotlin.math.min

class AdaptivePrefixFilter(private val threshold: Double, private val overlap: Int) {
    fun getCandidates(index1: InvertedIndex, index2: InvertedIndex): List<SegmentedStringPair> {
        val usedSegments = mutableMapOf<SegmentedStringPair, MutableSet<Segment>>()
        val overlapCounts = mutableMapOf<SegmentedStringPair, Int>()
        val candidates = mutableSetOf<SegmentedStringPair>()

        index1.getAllKeys().intersect(index2.getAllKeys()).forEach nextKey@{ key ->
            val list1 = index1.getList(key)
            val list2 = index2.getList(key)
            if (list1 == null || list2 == null) return@nextKey

            list1.forEach nextSegment1@{ segment1 ->
                val string1 = segment1.segmentedString ?: return@nextSegment1

                list2.forEach nextSegment2@{ segment2 ->
                    val string2 = segment2.segmentedString ?: return@nextSegment2

                    val stringPair = SegmentedStringPair(string1, string2)

                    if (candidates.contains(stringPair)) return@nextSegment2

                    // length filter
                    if (min(string1.minPartitionSize, string2.minPartitionSize)
                        < threshold * max(string1.minPartitionSize, string2.minPartitionSize)
                    ) return@nextSegment2

                    // withCheck if either segment1 or segment2 is used for <string1, string2>
                    val used = usedSegments.getOrPut(stringPair, { mutableSetOf() })
                    if (used.contains(segment1) || used.contains(segment2))
                        return@nextSegment2

                    // mark segment1 and segment2 as used for <string1, string2>
                    used.add(segment1); used.add(segment2)

                    overlapCounts[stringPair] = overlapCounts.getOrDefault(stringPair, 0) + 1

                    if (overlapCounts[stringPair] ?: 0 >= overlap) {
                        candidates.add(stringPair)
                        usedSegments.remove(stringPair); overlapCounts.remove(stringPair)
                    }
                }
            }
        }

        return candidates.toList()
    }

}
