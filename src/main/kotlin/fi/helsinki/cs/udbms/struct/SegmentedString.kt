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

package fi.helsinki.cs.udbms.struct

import kotlin.math.ceil
import kotlin.math.ln

class SegmentedString(val datasetId: Int, val id: Int, val segments: List<Segment>) {
    val numberOfTokens = (segments.map { it.wordIds }.flatten().max() ?: -1) + 1
    val minPartitionSize = calculateMinPartitionSize()

    @Suppress("UNUSED_PARAMETER")
    constructor(datasetId: Int, id: Int, segments: List<String>, dummy: Unit)
            : this(datasetId, id, segments.map { Segment(it) }) {
        this.segments.forEach { it.segmentedString = this }
    }

    fun unionSegments(): String = segments.joinToString(separator = ";")

    private fun unionAllSegments(): String = segments.joinToString(separator = ";")

    override fun toString() = "$datasetId[$id]: ${unionAllSegments()}"

    private fun calculateMinPartitionSize(): Int {
        val n = this.segments.map { it.numberOfWords }.max() ?: 1
        var parts = 0
        val usedWords = mutableSetOf<Int>()

        while (true) {
            val nextSeg = this.segments.maxBy { it.wordIds.subtract(usedWords).size }

            if (nextSeg != null) {
                usedWords.addAll(nextSeg.wordIds)
                parts++
            }

            if (usedWords.size == this.numberOfTokens)
                return ceil(parts.toDouble() / (ln(n.toDouble()) + 1)).toInt()
        }
    }
}