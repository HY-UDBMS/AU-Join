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

import fi.helsinki.cs.udbms.struct.GlobalOrder
import fi.helsinki.cs.udbms.struct.Pebble
import fi.helsinki.cs.udbms.struct.SegmentedString
import kotlin.math.ceil
import kotlin.math.ln

abstract class PebbleReducer(val threshold: Double, val overlap: Int, val order: GlobalOrder) {
    abstract fun reduce(str: SegmentedString, pebbles: Iterable<Pebble>): List<Pebble>

    companion object {
        @JvmStatic
        fun getMinPartitionSize(str: SegmentedString): Int {
            val n = str.segments.map { it.numberOfWords }.max() ?: 1
            var parts = 0
            val usedWords = mutableSetOf<Int>()

            while (true) {
                val nextSeg = str.segments.maxBy { it.wordIds.subtract(usedWords).size }

                if (nextSeg != null) {
                    usedWords.addAll(nextSeg.wordIds)
                    parts++
                }

                if (usedWords.size == str.numberOfTokens)
                    return ceil(parts.toDouble() / (ln(n.toDouble()) + 1)).toInt()
            }
        }
    }
}