# sugihara_record_resolver

## Recommended System Requirements
- Mac OS X
- Java SE 17 (or higher)
- Git environment

## Directory Structure
This project requires another directory where this repository should be cloned into.
However, the commands must be run in the top directory of this clone.
```
.
├── (clone of this repository) <- Please set this directory as your workspace.
│   ├── data
│   │   ├── commitDifference
│   │   ├── dataset_spec
│   │   ├── project_specs
│   │   ├── rq1-1
│   │   ├── rq1-2
│   │   ├── rq2-1
│   │   └── rq2-2
│   ├── jdk
│   ├── list
│   ├── out
│   ├── shell
│   └── src/main/java/rm4j
│       ├── compiler
│       ├── io
│       ├── test
│       │   └── Test.java (main)
│       ├── texts
│       └── util
└── dataset (will be added later)
    ├── repositories
    │   ├── rep1
    │   ├── rep2
    │   ├── ......
    │   ├── (repX)
    │   │   ├── original
    │   │   │   └── (a clone of a dataset repository)
    │   │   ├── copied
    │   │   │   └── (a copy of the clone above)
    │   │   └── commits.txt
    │   ├── ......
    │   └── rep2000
    └── jdk17modules
        ├── java.base
        ├── java.compiler
        ├── ......
        └── (jdk17 modules)

```

### Dataset OSS URLs
We provide dataset OSS urls in `list` directory.
(Note that some of the links may no longer be valid.)

```
.
├── README.md
├── ......
└── list
    └── repository_urls.txt
```

### Result Data
The result files are in `data` directory.

```
.
├── README.md
├── ......
└── data
    ├── commitDifference (<- commits which are relevant to records)
    ├── dataset_spec <- Dataset metrics
    ├── project_specs (<- .ser files which save commit traces of dataset)
    ├── rq1-1 <- RQ1-1 results
    ├── rq1-2 <- RQ1-2 results
    ├── rq2-1 <- RQ2-1 results
    └── rq2-2 <- RQ2-2 results
```

## How to Prepare Dataset

### Directory Structures
Run `step1.sh` in the `shell` directory to add `dataset` and its subdirectories.

```
.
├── (clone of this repository)
│   ├── ......
│   └── shell
│       ├── ......
│       └── step1.sh
└── dataset <-
    ├── repositories <-
    │   ├── rep1 <-
    │   ├── rep2 <-
    │   ├── ......
    │   ├── (repX) <-
    │   │   ├── original <-
    │   │   └── copied <-
    │   ├── ......
    │   └── rep2000 <-
    └── jdk17modules <-
```

### Cloning Repositories
Describe a list of GitHub repository URLs in `repository_urls.txt` (up to 2000) in the `shell` directory.
The program `url_collector.ipynb`, which is used to collect the url, is provided in the directory `list`.
The original list is also available in the `list` directory.

(repository_urls.txt) Like this, each URL must be separated by a new line:
```
https://github.com/elastic/elasticsearch
https://github.com/ReactiveX/RxJava
https://github.com/square/retrofit
......
https://github.com/godstale/retrowatch

```
NOTE: Without a new line after final url, the last repository will not be cloned.

Run `step2.sh` in the `shell` directory to add jdk17 modules in `jdk17modules` directory and a clone in each `original` directory and its copy in each `copied` directory.

```
.
├── (clone of this repository)
│   ├── ......
│   └── shell
│       ├── ......
│       ├── repository_urls.txt <- ADD THIS BY YOURSELF
│       └── step2.sh
└── dataset
    ├── repositories
    │   ├── rep1
    │   ├── rep2
    │   ├── ......
    │   ├── (repX)
    │   │   ├── original
    │   │   │   └── (a clone of a dataset repository) <-
    │   │   └── copied
    │   │       └── (a copy of the clone above) <-
    │   ├── ......
    │   └── rep2000
    └── jdk17modules
        ├── java.base <-
        ├── java.compiler <-
        ├── ......
        └── (jdk17 modules) <-
```

### Extracting the Number of Commits Grouped by Authors
Run `step3.sh` to add a `commits.txt` file for each repository.
This file contains a list of developers and the number of commits each developer has made.

```
524 Harry Potter
308 Ronald Weasley
186 Hermione Granger
......
```

```
.
├── (clone of this repository)
│   ├── ......
│   └── shell
│       ├── ......
│       └── step3.sh
└── dataset
    ├── repositories
    │   ├── rep1
    │   ├── rep2
    │   ├── ......
    │   ├── (repX)
    │   │   ├── original
    │   │   ├── copied
    │   │   └── commits.txt <-
    │   ├── ......
    │   └── rep2000
    └── jdk17modules
        ├── java.base
        ├── java.compiler
        ├── ......
        └── (jdk17 modules)
```

## Running Scripts
First, please build this project. The `main` method is in `Test.java` file, which is shown below.

```
.
├── (clone of this repository)
│   ├── data
│   ├── jdk
│   ├── list
│   ├── out <- All output goes here.
│   ├── shell
│   └── src/main/java/rm4j
│       ├── compiler
│       ├── io
│       ├── test
│       │   └── Test.java (main)
│       ├── texts
│       └── util
└── dataset
```

Our script take several arguments. (Without arguments, our script does nothing.)
In the first argument, please specify the step you want to run e.g. `step1`, `step7`.
In the second argument, please specify the size of the dataset (the number of cloned repositories) e.g. `100`. If you do not specify anything, the default is `2000`.

### Correspondence Between Execution Steps and Files

```
.
├── (clone of this repository)
│   ├── ......
│   └── out
│       ├── dataset_spec
│       │   ├── all_repository_metrics.csv <- step1
│       │   ├── Java16_repository_metrics.csv <- step1
│       │   └── record_reposioty_metrics.csv <- step1
│       ├── project_specs (for script performance only)
│       │   ├── rep1.ser
│       │   ├── rep2.ser
│       │   ├── ......
│       │   └── (repX.ser)
│       ├── rq1-1
│       │   ├── expressions.csv <- step9
│       │   ├── record_histroy.csv <- step5
│       │   ├── type_declarations_A <- step2
│       │   ├── type_declarations_B <- step2
│       │   └── type_declarations_C <- step2
│       ├── rq1-2 
│       │   ├── classes
│       │   │   ├── field_type.csv <- step3
│       │   │   ├── interface_type.csv <- step3
│       │   │   ├── num_of_fields <- step3
│       │   │   ├── num_of_interfaces.csv <- step3
│       │   │   └── num_of_methods.csv <- step3
│       │   └── records
│       │       ├── field_type.csv <- step3
│       │       ├── interface_type.csv <- step3
│       │       ├── num_of_fields.csv <- step3
│       │       ├── num_of_interfaces.csv <- step3
│       │       └── num_of_methods.csv <- step3
│       ├── rq2-1
│       │   ├── constructor_override_cases <- step6
│       │   │   ├── case0.java
│       │   │   ├── case1.java
│       │   │   ├── ......
│       │   │   └── (caseX.java)
│       │   ├── diffs <- step4
│       │   │   ├── ......
│       │   │   └── (dataset_repository_name)
│       │   │       ├── ......
│       │   │       ├── (yyyy-mm-dd:hh:mm:ss) (commit timestamp)
│       │   │       │   ├── file1
│       │   │       │   ├── file2
│       │   │       │   ├── ......
│       │   │       │   ├── (fileX)
│       │   │       │   │   ├── after.java (optional)
│       │   │       │   │   └── before.java (optional)
│       │   │       │   └── commit-info.txt
│       │   │       └── repository-info.csv
│       │   └── conversion_data.csv <- step4
│       ├── rq2-2
│       │   ├── to_class_conversions <- step8
│       │   │   ├── ......
│       │   │   └── (commit id:fileX)
│       │   │       ├── after.java
│       │   │       ├── before.java
│       │   │       └── converted_records.txt
│       │   ├── accessors.csv <- step10
│       │   ├── class_data.csv <- step7
│       │   └── converted_record_data.csv <- step8
│       └── api17info.ser (for script performance only)
└── dataset
```

