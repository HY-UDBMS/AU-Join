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
import kotlin.math.max
import kotlin.math.min

abstract class SimilarityVerifier(
    val threshold: Double,
    private val synonymList: SynonymKnowledge?,
    private val taxonomyList: TaxonomyKnowledge?,
    private val gramSize: Int?
) {
    fun getSimilarity(str1: SegmentedString, str2: SegmentedString): ClosedRange<Double> {
        val solution = solveMIS(buildGraph(getRelations(str1, str2)))

        // Do not merge unselected tokens
        // val hasUnusedToken =
        //     str1.numberOfTokens != solution.selected.flatMap { it.relation.seg1.wordIds }.distinct().count()
        //             || str2.numberOfTokens != solution.selected.flatMap { it.relation.seg2.wordIds }.distinct().count()
        val unusedMax = max(
            (0 until str1.numberOfTokens).subtract(solution.selected.flatMap { it.relation.seg1.wordIds }.distinct()).count(),
            (0 until str2.numberOfTokens).subtract(solution.selected.flatMap { it.relation.seg2.wordIds }.distinct()).count()
        )

        val partitionSize = solution.selected.size + unusedMax // (if (hasUnusedToken) 1 else 0)

        val sim = solution.selected.map { it.weight }.sum() / partitionSize
        return sim..(min(1.0, sim / solution.approximationRatio))
    }

    //region MIS

    protected class Solution(val selected: Set<Vertex>, val approximationRatio: Double)

    protected abstract fun solveMIS(graph: Graph): Solution

    //endregion

    //region Finding Relations

    protected class Relation(val seg1: Segment, val seg2: Segment, val type: KnowledgeType, val weight: Double)

    protected fun getRelations(str1: SegmentedString, str2: SegmentedString) = listOf(
        *getTaxonomyRelations(str1, str2).toTypedArray(),
        *getSynonymRelations(str1, str2).toTypedArray(),
        *getJaccardRelations(str1, str2).toTypedArray()
    )

    protected fun getRelations(str1: SegmentedString, str2: SegmentedString, type: KnowledgeType): List<Relation> {
        return when (type) {
            KnowledgeType.Taxonomy -> getTaxonomyRelations(str1, str2)
            KnowledgeType.Synonym -> getSynonymRelations(str1, str2)
            KnowledgeType.Jaccard -> getJaccardRelations(str1, str2)
        }
    }

    private fun getTaxonomyRelations(str1: SegmentedString, str2: SegmentedString): List<Relation> {
        if (taxonomyList == null) return emptyList()
        val relations = mutableListOf<Relation>()

        str1.segments.forEach nextSegment1@{ seg1 ->
            val dewey1 = taxonomyList.getDewey(seg1.label) ?: return@nextSegment1

            str2.segments.forEach nextSegment2@{ seg2 ->
                val dewey2 = taxonomyList.getDewey(seg2.label) ?: return@nextSegment2

                val sim = Dewey.getLCPLength(dewey1, dewey2).toDouble() / max(dewey1.length, dewey2.length)

                if (sim > 0)
                    relations.add(Relation(seg1, seg2, KnowledgeType.Taxonomy, sim))
            }
        }
        return relations
    }

    private fun getSynonymRelations(str1: SegmentedString, str2: SegmentedString): List<Relation> {
        if (synonymList == null) return emptyList()
        val relations = mutableListOf<Relation>()

        str1.segments.forEach nextSegment1@{ seg1 ->
            val lhs1 = synonymList.getLHS(seg1.label) ?: return@nextSegment1

            str2.segments.forEach nextSegment2@{ seg2 ->
                val lhs2 = synonymList.getLHS(seg2.label) ?: return@nextSegment2

                if (!lhs1.contentEquals(lhs2)) return@nextSegment2

                relations.add(Relation(seg1, seg2, KnowledgeType.Synonym, 1.0))
            }
        }
        return relations
    }

    private fun getJaccardRelations(str1: SegmentedString, str2: SegmentedString): List<Relation> {
        if (gramSize == null) return emptyList()
        val relations = mutableListOf<Relation>()

        str1.segments.forEach nextSegment1@{ seg1 ->
            if (seg1.numberOfWords > 1) return@nextSegment1
            // no gram for synonym and taxonomy segment. Trust them more than Jaccard
            if (synonymList?.getLHS(seg1.label) != null || taxonomyList?.getDewey(seg1.label) != null) return@nextSegment1

            str2.segments.forEach nextSegment2@{ seg2 ->
                if (seg2.numberOfWords > 1) return@nextSegment2
                // no gram for synonym and taxonomy segment. Trust them more than Jaccard
                if (synonymList?.getLHS(seg2.label) != null || taxonomyList?.getDewey(seg2.label) != null) return@nextSegment2

                val sim = Jaccard.getSimilarity(seg1, seg2, gramSize)

                if (sim > 0)
                    relations.add(Relation(seg1, seg2, KnowledgeType.Jaccard, sim))
            }
        }
        return relations
    }

    //endregion

    //region Graphs

    protected class Vertex(val relation: Relation, val neighbours: MutableSet<Vertex>, val weight: Double)

    protected class Graph(val vertices: Set<Vertex>, val clawFree: Int)

    private fun buildGraph(relations: List<Relation>): Graph {
        val vertices = relations.map { Vertex(it, mutableSetOf(), it.weight) }.toMutableSet()
        var k = 1 // for convenience, we define 1-claw to be a singleton set C with centre = C
        vertices.forEach { centre ->
            val conflicts = vertices
                .filterNot { it == centre }
                .filter {
                    it.relation.seg1.wordIds.intersect(centre.relation.seg1.wordIds).isNotEmpty()
                            || it.relation.seg2.wordIds.intersect(centre.relation.seg2.wordIds).isNotEmpty()
                }
            centre.neighbours.addAll(conflicts)
            k = max(k, conflicts.count())
        }

        return Graph(vertices, k + 1)
    }

    //endregion
}