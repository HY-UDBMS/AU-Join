# AU-Join
String similarity join incorporating multiple measures

## To-do list
* `SquareImp`-based verification algorithm
* DP prefix selection
* Sampling algorithm

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

  -o OUTPUT,            name of file for writing join results (default: to
  --output OUTPUT       stdout)


positional arguments:
  THRESHOLD             similarity threshold (0, 1]

  LIST_1                filename of the first segmented string list

  LIST_2                filename of the second segmented string list


Example: ./AU-Join --taxonomy tax.txt --synonym syn.txt --jaccard 3 -c3
-oresult.csv 0.9 list1.txt list2.txt
```
