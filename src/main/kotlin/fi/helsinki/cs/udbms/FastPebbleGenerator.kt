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

class FastPebbleGenerator(
    threshold: Double,
    synonyms: SynonymKnowledge,
    taxonomies: TaxonomyKnowledge
) : IPebbleGenerator(threshold, synonyms, taxonomies) {
    override fun generate(str: SegmentedString, type: KnowledgeType): List<Pebble> {
        return when (type) {
            KnowledgeType.Taxonomy -> generateTaxonomy(str)
            KnowledgeType.Synonym -> generateSynonym(str)
            KnowledgeType.Jaccard -> generateJaccard(str)
        }
    }

    private fun generateTaxonomy(str: SegmentedString): List<Pebble> {
        val pebbles = mutableListOf<Pebble>()

        str.segments.forEach {
            var dewey = taxonomies.getDewey(it.label) ?: return@forEach

            val w = 1.toDouble() / dewey.size

            // add self
            pebbles.add(Pebble(dewey.label, KnowledgeType.Taxonomy, w, it))

            var parent = dewey.getParent()
            while (parent != null) {
                pebbles.add(Pebble(parent.label, KnowledgeType.Taxonomy, w, it))
                parent = parent.getParent()
            }
        }

        return pebbles.toList()
    }

    private fun generateSynonym(str: SegmentedString): List<Pebble> {
        val pebbles = mutableListOf<Pebble>()

        str.segments.forEach {
            val dewey = taxonomies.getDewey(it.label) ?: return@forEach

            pebbles.add(Pebble(dewey.label, KnowledgeType.Taxonomy, 1.toDouble() / dewey.path.size, it))
        }

        return pebbles.toList()
    }

    private fun generateJaccard(str: SegmentedString): List<Pebble> {
        val pebbles = mutableListOf<Pebble>()

        str.segments.forEach {
            val dewey = taxonomies.getDewey(it.label) ?: return@forEach

            pebbles.add(Pebble(dewey.label, KnowledgeType.Taxonomy, 1.toDouble() / dewey.path.size, it))
        }

        return pebbles.toList()
    }
}