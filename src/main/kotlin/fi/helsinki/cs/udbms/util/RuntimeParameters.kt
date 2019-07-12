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

class RuntimeParameters(parser: ArgParser) {
    companion object {
        @JvmStatic
        private var parser: RuntimeParameters? = null

        @JvmStatic
        fun initialise(args: Array<String>): RuntimeParameters {
            parser = ArgParser(
                args,
                helpFormatter = DefaultHelpFormatter(
                    epilogue = """
                    Example: ./au-join --taxonomy tax.txt -j8 -o3 0.9 list1.txt list2.txt
                """.trimIndent()
                )
            ).parseInto(::RuntimeParameters)

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
        help = "gram length for Jaccard similarity (> 1)"
    ) { toInt() }.default(0)

    val taxonomy by parser.storing(
        "--taxonomy",
        help = "filename of taxonomy knowledge"
    ) { toString() }.default { "" }

    val synonym by parser.storing(
        "--synonym",
        help = "filename of synonym knowledge"
    ) { toString() }.default { "" }

    val threads by parser.storing(
        "-j", "--thread",
        help = "number of threads for filtering and verification (default: number of cores minus 2)"
    ) { toInt() }.default(maxOf(Runtime.getRuntime().availableProcessors() - 2, 1)).addValidator {
        if (value < 1) throw InvalidArgumentException("Number of threads must be at least 1")
    }

    val overlap by parser.storing(
        "-o", "--overlap",
        help = "number of overlaps (default: 1)"
    ) { toInt() }.default(1).addValidator {
        if (value < 1) throw InvalidArgumentException("Number of overlaps must be at least 1")
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
}