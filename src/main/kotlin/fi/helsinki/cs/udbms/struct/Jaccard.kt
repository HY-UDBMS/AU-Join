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

object Jaccard {
    @JvmStatic
    fun getSimilarity(seg1: Segment, seg2: Segment, gramSize: Int): Double {
        val grams1 = generateGrams(seg1, gramSize)
        val grams2 = generateGrams(seg2, gramSize)

        var intersection = 0
        val h = mutableMapOf<String, Int>()
        grams1.forEach { h[it] = h.getOrDefault(it, 0) + 1 }

        for (g in grams2) {
            if (!h.containsKey(g)) continue

            val remaining = h[g]!! - 1
            if (remaining == 0) h.remove(g) else h[g] = remaining

            intersection++
        }

        return intersection.toDouble() / (grams1.size + grams2.size - intersection)
    }

    @JvmStatic
    fun generateGrams(seg: Segment, gramSize: Int): List<String> {
        if (gramSize < 2) return emptyList()

        val grams = mutableListOf<String>()

        if (seg.label.length < gramSize) {
            grams.add(seg.label)
        } else {
            val last = seg.label.length - gramSize
            for (i in 0..last) {
                grams.add(seg.label.substring(i, i + gramSize))
            }
        }

        return grams
    }
}