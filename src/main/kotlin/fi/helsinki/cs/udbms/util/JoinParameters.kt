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

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.DefaultHelpFormatter
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import kotlin.system.exitProcess

class JoinParameters(parser: ArgParser) {
    companion object {
        @JvmStatic
        private var parser: JoinParameters? = null

        @JvmStatic
        fun initialise(args: Array<String>): JoinParameters {
            parser = ArgParser(
                args,
                helpFormatter = DefaultHelpFormatter(
                    epilogue = """
                    example: ./AU-Join --taxonomy tax.txt --synonym syn.txt --jaccard 3 -c3 -oresult.csv 0.9 list1.txt list2.txt
                """.trimIndent()
                )
            ).parseInto(::JoinParameters)

            if (parser!!.synonym.isEmpty() && parser!!.taxonomy.isEmpty() && parser!!.gram == 0) {
                println("You must specify at least one of --jaccard, --taxonomy, or --synonym")
                exitProcess(1)
            }
            return parser!!
        }

        @JvmStatic
        fun getInstance() = parser
    }

    val gram by parser.storing(
        "--jaccard",
        help = "enable Jaccard similarity and set gram length (> 1)"
    ) { toInt() }.default(0)

    val taxonomy by parser.storing(
        "--taxonomy",
        help = "enable taxonomy similarity and specify the filename of taxonomy knowledge"
    ) { toString() }.default { "" }

    val synonym by parser.storing(
        "--synonym",
        help = "enable synonym similarity and specify the filename of synonym knowledge"
    ) { toString() }.default { "" }

    val overlap by parser.storing(
        "-c", "--common",
        help = "number of common signatures (default: 1)"
    ) { toInt() }.default(1).addValidator {
        if (value < 1) throw InvalidArgumentException("Number of common signatures must be at least 1")
    }

    val filter by parser.mapping(
        "--filter-fast" to "Fast",
        "--filter-dp" to "DP",
        help = "specify the filtering method: Fast (Heuristic) and DP (Dynamic Programming) (default: --filter-fast)"
    ).default { "Fast" }

    val verify by parser.mapping(
        "--verify-greedy" to "Greedy",
        "--verify-squareimp" to "SquareImp",
        "--verify-squareimp-improved" to "SquareImp-Improved",
        help = "specify the verification method: Greedy, SquareImp, or our improved SquareImp (default: --verify-greedy)"
    ).default { "Greedy" }

    val singleThread by parser.flagging(
        "--single",
        help = "perform filtering and verification on a single thread (default: on multiple threads)"
    )

    val output by parser.storing(
        "-o", "--output",
        help = "method for handling join results: null (no output), stdout (to standard output), or a filename (output as csv) (default: -o null)"
    ).default("null")

    val threshold by parser.positional(
        "THRESHOLD",
        help = "similarity threshold (0, 1]"
    ) { toDouble() }.default(0.0).addValidator {
        if (value <= 0 || value > 1) throw InvalidArgumentException("The similarity threshold must be within (0, 1].")
    }

    val list1 by parser.positional(
        "LIST_1",
        help = "filename of the first segmented string list"
    ).default("").addValidator {
        if (value.isEmpty()) throw InvalidArgumentException("You must specify two datasets")
    }

    val list2 by parser.positional(
        "LIST_2",
        help = "filename of the second segmented string list"
    ).default("").addValidator {
        if (value.isEmpty()) throw InvalidArgumentException("You must specify two datasets")
    }
}