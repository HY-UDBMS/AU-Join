# AU-Join

A similarity join aims to find all similar pairs between two collections of records. Established algorithms utilise different similarity measures, either syntactic or semantic, to quantify the similarity between two records. However, when records are similar in forms of a mixture of syntactic and semantic relations, utilising a single measure becomes inadequate to disclose the real similarity between records, and hence unable to obtain high-quality join results.
    
In this implementation, we develop a unified framework to find similar records by combining multiple similarity measures.  We  develop a new similarity framework that unifies the existing three kinds of similarity measures simultaneously, including syntactic (typographic) similarity, synonym-based similarity, and taxonomy-based similarity. 

Please access the full PVLDB 2019 paper to find the technical details:

Towards a Unified Framework for String Similarity Joins. PVLDB 2019

https://www.cs.helsinki.fi/u/jilu/documents/P1131_Lu.pdf

## Environment

This software is developed by JAVA. You only need at least JRE version 8 to run this program.

But if you want to modfiy the codes of AU-Join, you should have at least JDK 8 installed, an IDE/editor with Kotlin language support, as well as Maven for restoring dependencies. (Shortcut: install JDK and IntelliJ 😉) Download the IntelliJ Community version here:
https://www.jetbrains.com/idea/download/#section=windows


## Usage

This program consists of two parts: `AU-Join` for similarity join and `AU-Esti` for estimating the best overlap constraint. The purpose of `AU-Join` is to perform the unified join and  `AU-Esti` implements the sampling alroithms to select the best overlapping thresholds.
 
To get help, run `./AU-Join --help` or `./AU-Esti --help`.

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

## Feedback

Pengfei Xu (pengfei.xu[at]helsinki[dot]fi) and Jiaheng Lu (jiahenglu[at]gmail[dot]com)

## License

MIT License
