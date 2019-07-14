# AU-Join

A similarity join aims to find all similar pairs between two collections of records. Established algorithms utilise different similarity measures, either syntactic or semantic, to quantify the similarity between two records. However, when records are similar in forms of a mixture of syntactic and semantic relations, utilising a single measure becomes inadequate to disclose the real similarity between records, and hence unable to obtain high-quality join results.
    
In this implementation, we develop a unified framework to find similar records by combining multiple similarity measures.

## Environment

You will need at least JRE version 8 to run this program.

If you want to develop AU-Join, you should have at least JDK 8 installed, an IDE/editor with Kotlin language support, as well as Maven for restoring dependencies. (Shortcut: install JDK and IntelliJ 😉)

## Usage

To get help, run `./AU-Join --help`.

```
usage: [-h] [--jaccard JACCARD] [--taxonomy TAXONOMY] [--synonym SYNONYM]
       [-j THREAD] [-c COMMON] [-o OUTPUT] [THRESHOLD] [LIST_1] [LIST_2]

optional arguments:
  -h, --help            show this help message and exit

  --jaccard JACCARD     gram length for Jaccard similarity (> 1)

  --taxonomy TAXONOMY   filename of taxonomy knowledge

  --synonym SYNONYM     filename of synonym knowledge

  -j THREAD,            number of threads for filtering and verification
  --thread THREAD       (default: number of cores minus 2)

  -c COMMON,            number of common signatures (default: 1)
  --common COMMON

  -o OUTPUT,            name of a file for writing join results (default: to
  --output OUTPUT       stdout)


positional arguments:
  THRESHOLD             similarity threshold (0, 1]

  LIST_1                filename of the first segmented string list

  LIST_2                filename of the second segmented string list


Example: ./AU-Join --taxonomy tax.txt --synonym syn.txt --jaccard 3 -c3 -oresult.csv 0.9 list1.txt list2.txt
```

## Comments and feedback

Pengfei Xu (pengfei.xu@helsinki.fi) and Jiaheng Lu (jiahenglu@gmail.com)

## Next version will include
* `SquareImp`-based verification algorithm
* DP prefix selection
* Sampling algorithm
