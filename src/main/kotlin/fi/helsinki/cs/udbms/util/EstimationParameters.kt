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

class EstimationParameters(parser: ArgParser) {
    companion object {
        @JvmStatic
        private var parser: EstimationParameters? = null

        @JvmStatic
        fun initialise(args: Array<String>): EstimationParameters {
            parser = ArgParser(
                args,
                helpFormatter = DefaultHelpFormatter(
                    epilogue = """
                    example: ./AU-Esti --taxonomy tax.txt --synonym syn.txt --jaccard 3 0.9 list1.txt list2.txt 1 2 3 4 5
                """.trimIndent()
                )
            ).parseInto(::EstimationParameters)

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

    val sampleSize by parser.storing(
        "-s", "--sample-size",
        help = "specify the expected sample size for estimation (> 0, default: 100)"
    ) { toInt() }.default { 100 }.addValidator {
        if (value <= 0) throw InvalidArgumentException("the expected sample size must be at least 1")
    }

    val quantile by parser.storing(
        "-q", "--quantile",
        help = "specify the quantile for Student t-distribution (default: 0.842 for 60% confidence levels on both sides)"
    ) { toDouble() }.default { 0.842 }

    val iteration by parser.storing(
        "-i", "--iteration",
        help = "limit the number of iterations (> 0, default: 20)"
    ) { toInt() }.default { 20 }.addValidator {
        if (value <= 0) throw InvalidArgumentException("the expected sample size must be at least 1")
    }

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

    val overlapList by parser.positionalList(
        "OVERLAPS",
        "values of overlap to be tested",
        1..Int.MAX_VALUE
    ) { toInt() }.default { emptyList() }.addValidator {
        if (value.isEmpty()) throw InvalidArgumentException("You muse specify at least one overlap value")
    }
}