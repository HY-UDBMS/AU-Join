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

package fi.helsinki.cs.udbms.script

import fi.helsinki.cs.udbms.util.IO
import java.util.*

fun main() {
    val needles = IO.readTaxonomy("data/mesh.taxonomy.txt").knowledge.keys.map { " $it " }

    val aho = AhoCorasick()
    aho.addNeedles(needles)
    aho.prepare()

    IO.readStringList("data/mesh.data.10k.1.txt").forEach {
        var haystack = it.unionSegments().replace(";", " ")
        haystack = " $haystack "

        val numMatches = aho.search(haystack)

        println(haystack)
        println(numMatches
            .asSequence()
            .withIndex()
            .filterNot { it.value == 0 }
            .associateBy({ needles[it.index] }, { it.value })
            .map { it.key + " - " + it.value }
            .joinToString(separator = ",\n")
        )
    }
}

class AhoCorasick {

    inner class Node(
        val parent: Node?,
        var charFromParent: Char? = null
    ) {
        var isLeaf: Boolean = false
        var failLink: Node? = null
        var children: HashMap<Char, Node> = hashMapOf()
        var outputs = mutableListOf<Int>()
        val isRoot: Boolean
            get() = this.parent == null
        var numMatches: Int = 0
    }

    private val root = Node(parent = null)
    private var needles = mutableListOf<String>()
    private var matches = mutableListOf<Int>()

    fun addNeedles(needles: Iterable<String>) {
        needles.forEach { needle ->
            if (this.needles.contains(needle)) return
            var currNode = root
            for (c in needle) {
                if (!currNode.children.containsKey(c))
                    currNode.children[c] = Node(parent = currNode, charFromParent = c)
                currNode = currNode.children[c] as Node
            }
            currNode.isLeaf = true
            this.needles.add(needle)
            currNode.outputs.add(this.needles.size - 1)
        }
    }

    private fun calcFailLink(node: Node) {
        if (node.isRoot || node.parent!!.isRoot) {
            node.failLink = root
        } else {
            var currBetterNode = node.parent.failLink as Node
            val chNode = node.charFromParent
            while (true) {
                if (currBetterNode.children.containsKey(chNode)) {
                    node.failLink = currBetterNode.children[chNode] as Node
                    node.outputs.addAll(node.failLink!!.outputs)
                    break
                }
                if (currBetterNode.isRoot) {
                    node.failLink = root
                    break
                }
                currBetterNode = currBetterNode.failLink as Node
            }
        }
    }

    fun prepare() {
        matches = MutableList(needles.size) { 0 }
        val queue: Queue<Node> = LinkedList<Node>()
        queue.add(root)
        while (queue.count() > 0) {
            val currNode = queue.remove()
            calcFailLink(currNode)
            for (key in currNode.children.keys) queue.add(currNode.children[key])
        }
    }

    private fun collectMatches(node: Node = root) {
        for (child in node.children.values) collectMatches(child) // depth first search
        if (node.isLeaf)
            node.outputs.forEach { matches[it] += node.numMatches }
    }

    fun search(text: String): List<Int> {
        var currState = root
        for (j in text.indices) {
            while (true) {
                if (currState.children.containsKey(text[j])) {
                    currState = currState.children[text[j]] as Node
                    break
                }
                if (currState.isRoot) break
                currState = currState.failLink as Node
            }
            if (currState.isLeaf) currState.numMatches++
        }
        collectMatches()
        val result = matches
        matches = MutableList(needles.size) { 0 }
        return result
    }
}