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

package fi.helsinki.cs.udbms.util

import de.mpicbg.scicomp.kutils.map
import de.mpicbg.scicomp.kutils.par
import de.mpicbg.scicomp.kutils.unpar
import fi.helsinki.cs.udbms.struct.SegmentedString
import fi.helsinki.cs.udbms.struct.SynonymKnowledge
import fi.helsinki.cs.udbms.struct.TaxonomyKnowledge
import java.io.File
import java.util.zip.GZIPInputStream

object IO {
    @JvmStatic
    fun readSynonym(file: String): SynonymKnowledge =
        SynonymKnowledge(
            readLines(file)
                .par()
                .map { it.split('\t') }
                .map { Pair(it[0], ("${it[0]};${it[1]}").split(';')) } // add LHS itself to hash
                .unpar()
                .flatMap { kv -> kv.second.map { Pair(it, kv.first) } }
                .associate { it }
        )

    @JvmStatic
    fun readTaxonomy(file: String): TaxonomyKnowledge =
        TaxonomyKnowledge(
            readLines(file)
                .par()
                .map { it.split('\t') }
                .unpar()
                .associate { Pair(it[1], it[0]) }
        )

    @JvmStatic
    fun readSegmentedStrings(file: String): List<SegmentedString> =
        readLines(file)
            .par()
            .map { it.split('\t') }
            .map { SegmentedString(file.hashCode(), it[0].toInt(), it[1].split(';'), Unit) }
            .unpar()
            .toList()

    @JvmStatic
    private fun readLines(file: String): List<String> =
        (if (file.endsWith(".gz")) GZIPInputStream(File(file).inputStream()) else File(file).inputStream())
            .bufferedReader()
            .readLines()
            .filterNot { it.isBlank() }
            .filterNot { it.contentEquals("null") }

}
