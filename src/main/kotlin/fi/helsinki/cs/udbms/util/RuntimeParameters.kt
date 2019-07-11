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
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default

class RuntimeParameters(parser: ArgParser) {
    val threshold by parser.storing(
        "-t", "--threshold",
        help = "similarity threshold (0 - 1)"
    ) { toDouble() }.default { 0.9 }.addValidator {
        if (value <= 0 || value > 1) throw InvalidArgumentException("Threshold must be within (0, 1]")
    }

    val overlap by parser.storing(
        "-o", "--overlap",
        help = "number of overlaps (default: 1)"
    ) { toInt() }.default(1).addValidator {
        if (value < 1) throw InvalidArgumentException("Number of overlaps must be at least 1")
    }

    val gram by parser.storing(
        "--jac",
        help = "gram size for Jaccard similarity (default: 5)"
    ) { toInt() }.default(5).addValidator {
        if (value <= 1) throw InvalidArgumentException("Jaccard gram size must be at least 2")
    }

    val taxonomy by parser.storing(
        "--tax",
        help = "filename of taxonomy knowledge"
    ) { toString() }.default { "" }

    val synonym by parser.storing(
        "--sym",
        help = "filename of synonym knowledge"
    ) { toString() }.default { "" }

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