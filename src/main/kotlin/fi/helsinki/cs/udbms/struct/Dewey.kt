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

import kotlin.math.min

class Dewey(val label: String) {
    companion object {
        @JvmStatic
        fun getLCP(n1: Dewey, n2: Dewey): Dewey {
            return Dewey(n1.path.take(getLCPLength(n1, n2)))
        }

        @JvmStatic
        fun getLCPLength(n1: Dewey, n2: Dewey): Int {
            for (i in 0..min(n1.size, n2.size)) {
                if (n1.path[i] != n2.path[i])
                    return i
            }
            return min(n1.size, n2.size)
        }
    }

    val path: List<Int> = label.split('.').map { it.toInt() }
    val size: Int = path.size

    constructor(path: List<Int>) : this(path.joinToString(separator = "."))

    fun getParent(): Dewey? = if (size == 1) null else Dewey(path.take(size - 1))

    override fun toString() = label
}