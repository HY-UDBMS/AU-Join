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

import fi.helsinki.cs.udbms.struct.SynonymKnowledge
import fi.helsinki.cs.udbms.struct.TaxonomyKnowledge
import java.util.*
import kotlin.math.pow

class SquareImpSimilarityVerifier(
    threshold: Double,
    synonymList: SynonymKnowledge?,
    taxonomyList: TaxonomyKnowledge?,
    gramSize: Int?
) : SimilarityVerifier(threshold, synonymList, taxonomyList, gramSize) {
    override fun solveMIS(graph: Graph): Solution {
        val claws = enumClaws(graph)

        var selectedVertices = emptySet<Vertex>()

        var weight = 0.0
        do {
            var improved = false

            claws.forEach { claw ->
                val newSelectedVertices =
                    if (claw.talons.isEmpty())
                        selectedVertices.plus(claw.centre).subtract(claw.centre.neighbours)
                    else
                        selectedVertices.union(claw.talons).subtract(claw.talons.flatMap { it.neighbours })

                val newWeight = newSelectedVertices.sumByDouble { it.weight }

                if (newWeight.pow(2) > weight.pow(2)) {
                    selectedVertices = newSelectedVertices; weight = newWeight
                    improved = true
                }
            }
        } while (improved)

        return Solution(selectedVertices, 2.0 / graph.clawFree)
    }

    private fun enumClaws(graph: Graph): Set<Claw> {
        // based on https://git.facom.ufms.br/diego/ffdcj-sim/blob/master/src/claw.cpp
        // NOTE THAT the above source code is GPL-licensed.
        // Therefore, this enumClaws() function you are now looking at has violated the GPL term.
        // Ask a lawyer before using this function in a commercial environment.

        val claws = mutableSetOf<Claw>()
        val stack = Stack<Triple<Claw, Set<Vertex>, Int>>()

        val vertexIds = graph.vertices.mapIndexed { index, vertex -> Pair(vertex, index) }.toMap()

        graph.vertices.forEach { centre ->
            claws.add(Claw(centre, emptySet())) // 1-claw

            centre.neighbours.forEach { neighbour ->
                val talons = setOf(neighbour)
                val remaining = centre.neighbours.subtract(neighbour.neighbours).minus(neighbour)

                // Note: do not add this claw to final result. 'd' in 'd-claw' means d talons.
                // 1-claw means zero talon with one centre node; 2-claw means two talons with one centre node.
                // One talon with one centre node is not a claw.
                stack.push(Triple(Claw(centre, talons), remaining, vertexIds[centre] ?: -1))
            }

            while (stack.isNotEmpty()) {
                val (claw, remaining, lastId) = stack.pop()

                remaining.forEach { nextVertex ->
                    if (vertexIds[nextVertex] ?: -1 < lastId) return@forEach

                    val nextClaw = Claw(claw.centre, claw.talons.plus(nextVertex))
                    val nextRemaining = remaining.subtract(nextVertex.neighbours).minus(nextVertex)

                    if (nextRemaining.isNotEmpty()) stack.push(
                        Triple(
                            nextClaw,
                            nextRemaining,
                            vertexIds[nextVertex] ?: -1
                        )
                    )

                    claws.add(nextClaw)
                }
            }
        }

        return claws
    }

    private class Claw(val centre: Vertex, val talons: Set<Vertex>) {
        override fun hashCode(): Int = centre.hashCode() * 34 + talons.map { it.hashCode() }.sum()

        override fun equals(other: Any?): Boolean {
            return when (other) {
                !is Claw -> false
                centre != other.centre -> false
                talons.intersect(other.talons).size != talons.size -> false
                else -> true
            }
        }

        override fun toString(): String =
            "${centre.relation.seg1.label} & ${centre.relation.seg2.label}: ${talons.size} talons"
    }
}