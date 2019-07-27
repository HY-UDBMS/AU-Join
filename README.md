
# AU-Join

A similarity join aims to find all similar pairs between two collections of records. Established algorithms utilise different similarity measures, either syntactic or semantic, to quantify the similarity between two records. However, when records are similar in forms of a mixture of syntactic and semantic relations, utilising a single measure becomes inadequate to disclose the real similarity between records, and hence unable to obtain high-quality join results.
    
In this implementation, we develop a unified framework to find similar records by combining multiple similarity measures.  We  develop a new similarity framework that unifies the existing three kinds of similarity measures simultaneously, including syntactic (typographic) similarity, synonym-based similarity, and taxonomy-based similarity. 

Please access [the PVLDB 2019 paper](https://www.cs.helsinki.fi/u/jilu/documents/P1131_Lu.pdf) to find the technical details:

```
Pengfei Xu and Jiaheng Lu. Towards a Unified Framework for String Similarity Joins. PVLDB, 12(11): 1289–1302, 2019.

DOI: https://doi.org/10.14778/3342263.3342268
```



## Environment

This software is written in Kotlin and runs on JVM. You only need JRE ≥ 8 to run this software.

But, if you want to modify the code of AU-Join, you should have at least JDK 8 installed, an IDE/editor with Kotlin language support, as well as Maven for restoring dependencies. (Shortcut: install JDK and IntelliJ: https://www.jetbrains.com/idea/download/ 😉)


## Running

This program consists of two parts: `AU-Esti` for estimating the best overlap constraint and `AU-Join` for the actual similarity join. To test the software, one should first run `AU-Esti`

```
./AU-Esti --taxonomy tax.txt --synonym syn.txt --jaccard 3 0.9 list1.txt list2.txt 1 2 3 4 5
```

to get some output similar to the following:

```
Arguments: --taxonomy tax.txt --synonym syn.txt --jaccard 3 0.9 list1.txt list2.txt 1 2 3 4 5
Reading string... 2000 + 2000 strings loaded
Reading synonym... 200807 rules loaded
Reading taxonomy... 28470 nodes loaded
Test driving... filtering time 0.0046 ms/pair, verification time 0.0540 ms/candidate
	i	mean1	mean2	mean3	mean4	mean5	error1	error2	error3	error4	error5	
	1	15085	12293	12372	12930	13407	0	0	0	0
	...
	20	16444	13977	13907	14553	14798	425	354	345	313	316	
Overlap parameters from the best to the worst: 3, 2, 4, 5, 1
```

The last line indicates that the best overlap constraint is `3`. Now it is possible to run the actual join algorithm by specifying the number of common signatures, such as

```
./AU-Join --taxonomy tax.txt --synonym syn.txt --jaccard 3 -c3 -oresult.csv 0.9 list1.txt list2.txt
```
Note that the argument `-c3` specifies the overlap constraint for the join process.

#### Tuning the algorithm

The full list of arguments can be obtained by running `./AU-Esti --help` or `./AU-Join --help`. We attach them here for your convenience:

##### AU-Esti Usage

```
usage: [-h] [--jaccard JACCARD] [--taxonomy TAXONOMY] [--synonym SYNONYM]
       [--filter-fast] [--verify-greedy] [--single] [-s SAMPLE_SIZE]
       [-q QUANTILE] [-i ITERATION] [THRESHOLD] [LIST_1] [LIST_2] [OVERLAPS]...

optional arguments:
  -h, --help                    show this help message and exit

  --jaccard JACCARD             enable Jaccard similarity and set gram length
                                (> 1)

  --taxonomy TAXONOMY           enable taxonomy similarity and specify the
                                filename of taxonomy knowledge

  --synonym SYNONYM             enable synonym similarity and specify the
                                filename of synonym knowledge

  --filter-fast, --filter-dp    specify the filtering method: Fast (heuristic)
                                or DP (dynamic programming) (default:
                                --filter-fast)

  --verify-greedy,              specify the verification method: Greedy,
  --verify-squareimp,           SquareImp, or our improved SquareImp (default:
  --verify-squareimp-improved   --verify-greedy)

  --single                      perform filtering and verification on a single
                                thread (default: on multiple threads)

  -s SAMPLE_SIZE,               specify the expected sample size for
  --sample-size SAMPLE_SIZE     estimation (> 0, default: 100)

  -q QUANTILE,                  specify the quantile for Student
  --quantile QUANTILE           t-distribution (default: 0.842 for 60%
                                confidence levels on both sides)

  -i ITERATION,                 limit the number of iterations (> 0, default:
  --iteration ITERATION         20)

positional arguments:
  THRESHOLD                     similarity threshold (0, 1]

  LIST_1                        filename of the first segmented string list

  LIST_2                        filename of the second segmented string list

  OVERLAPS                      values of overlap to be tested

example: ./AU-Esti --taxonomy tax.txt --synonym syn.txt --jaccard 3 0.9 list1.txt list2.txt 1 2 3 4 5
```

##### AU-Join Usage

```
usage: [-h] [--jaccard JACCARD] [--taxonomy TAXONOMY] [--synonym SYNONYM]
       [-c COMMON] [--filter-fast] [--verify-greedy] [--single] [-o OUTPUT]
       [THRESHOLD] [LIST_1] [LIST_2]

optional arguments:
  -h, --help                    show this help message and exit

  --jaccard JACCARD             enable Jaccard similarity and set gram length
                                (> 1)

  --taxonomy TAXONOMY           enable taxonomy similarity and specify the
                                filename of taxonomy knowledge

  --synonym SYNONYM             enable synonym similarity and specify the
                                filename of synonym knowledge

  -c COMMON, --common COMMON    number of common signatures (default: 1)

  --filter-fast, --filter-dp    specify the filtering method: Fast (heuristic)
                                or DP (dynamic programming) (default:
                                --filter-fast)

  --verify-greedy,              specify the verification method: Greedy,
  --verify-squareimp,           SquareImp, or our improved SquareImp (default:
  --verify-squareimp-improved   --verify-greedy)

  --single                      perform filtering and verification on a single
                                thread (default: on multiple threads)

  -o OUTPUT, --output OUTPUT    method for handling join results: null (no
                                output), stdout (to standard output), or a
                                filename (output as csv) (default: -o null)

positional arguments:
  THRESHOLD                     similarity threshold (0, 1]

  LIST_1                        filename of the first segmented string list

  LIST_2                        filename of the second segmented string list

example: ./AU-Join --taxonomy tax.txt --synonym syn.txt --jaccard 3 -c3 -oresult.csv 0.9 list1.txt list2.txt
```

## Dataset

The taxonomy dataset is a text file in which each line is a [Dewey Decimal Class](https://en.wikipedia.org/wiki/Dewey_Decimal_Classification) representing a node and one label, separated by `↹` (ASCII Tab):

```
1.236.249	mammary glands, human
```

The synonym dataset is a text file in which each line is in the form of `LHS↹RHS1;RHS2;...`:

```
1-butanol	1 butanol;alcohol, butyl;alcohol, n-butyl
```

The above example shows three synonym rules: `1-butanol = 1 butanol`, `1-butanol = alcohol, butyl`, and `1-butanol = alcohol, n-butyl`.

The string dataset is a text file in which each line is a unique string id and all possible segments (separated by `↹`). Each token is assigned a id:

```
172	0:canadCa;1:drug;2:labeling;1:drug 2:labeling
```

The above example shows a string of three tokens: `canadCa drug labeling`. Four segments can be generated: `canadCa`, `drug`, `labeling`, and `drug labeling`. Segments are generated by using the [Aho–Corasick algorithm](https://en.wikipedia.org/wiki/Aho–Corasick_algorithm).


## Feedback

Pengfei Xu (pengfei.xu[at]helsinki[dot]fi) and Jiaheng Lu (jiahenglu[at]gmail[dot]com)

## License

MIT License
