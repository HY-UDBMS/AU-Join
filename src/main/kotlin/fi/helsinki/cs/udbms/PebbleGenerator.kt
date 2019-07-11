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

class PebbleGenerator(
    private val synonymList: SynonymKnowledge?,
    private val taxonomyList: TaxonomyKnowledge?,
    private val gramSize: Int?
) {
    fun generate(str: SegmentedString) = listOf(
        *generateTaxonomy(str).toTypedArray(),
        *generateSynonym(str).toTypedArray(),
        *generateJaccard(str).toTypedArray()
    )

    fun generate(str: SegmentedString, type: KnowledgeType): List<Pebble> {
        return when (type) {
            KnowledgeType.Taxonomy -> generateTaxonomy(str)
            KnowledgeType.Synonym -> generateSynonym(str)
            KnowledgeType.Jaccard -> generateJaccard(str)
        }
    }

    private fun generateTaxonomy(str: SegmentedString): List<Pebble> {
        if (taxonomyList == null) return emptyList()

        val pebbles = mutableListOf<Pebble>()

        str.segments.forEach {
            val dewey = taxonomyList.getDewey(it.label) ?: return@forEach

            val w = 1.0 / dewey.size

            // add self
            pebbles.add(Pebble(dewey.label, KnowledgeType.Taxonomy, w, it))
            // add parents
            var parent = dewey.getParent()
            while (parent != null) {
                pebbles.add(Pebble(parent.label, KnowledgeType.Taxonomy, w, it))
                parent = parent.getParent()
            }
        }

        return pebbles.toList()
    }

    private fun generateSynonym(str: SegmentedString): List<Pebble> {
        if (synonymList == null) return emptyList()

        val pebbles = mutableListOf<Pebble>()

        str.segments.forEach {
            val lhs = synonymList.getLHS(it.label) ?: return@forEach

            pebbles.add(Pebble(lhs, KnowledgeType.Synonym, 1.0, it))
        }

        return pebbles.toList()
    }

    private fun generateJaccard(str: SegmentedString): List<Pebble> {
        if (gramSize == null) return emptyList()
        if (gramSize < 2) return emptyList()

        val pebbles = mutableListOf<Pebble>()

        str.segments.forEach {
            if (it.numberOfWords > 1) return@forEach

            // no gram for synonym and taxonomy segment. Trust them more than Jaccard
            if (synonymList?.getLHS(it.label) != null || taxonomyList?.getDewey(it.label) != null) return@forEach

            if (it.label.length < gramSize) {
                pebbles.add(Pebble(it.label, KnowledgeType.Jaccard, 1.0, it))
                return@forEach
            }

            val last = it.label.length - gramSize
            for (i in 0..last)
                pebbles.add(
                    Pebble(
                        it.label.substring(i, i + gramSize),
                        KnowledgeType.Jaccard,
                        1.0 / (last + 1),
                        it
                    )
                )
        }

        return pebbles.toList()
    }
}