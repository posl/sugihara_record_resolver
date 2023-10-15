# sugihara_record_resolver

## Recommended System Requirements
Mac OS X
Java SE 17 (or higher)
Git environment

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
We provide dataset OSS urls in 'list' directory.
(Note that some of the links may no longer be valid.)

```
.
├── README.md
├── ......
└── list
    └── repository_urls.txt
```

### Result Data
The result files are in 'data' directory.

```
.
├── README.md
├── ......
└── data
    ├── commitDifference (<- commits which are relevant to records)
    ├── dataset_spec <- Dataset metrics
    ├── project_specs (<- .ser files which save commit traces of dataset)
    ├── rq1-1 <- RQ1-1
    ├── rq1-2 <- RQ1-2
    ├── rq2-1 <- RQ2-1
    └── rq2-2 <- RQ2-2
```

## How to Prepare Dataset

### Directory Structures
Run 'step1.sh' in the 'shell' directory to add 'dataset' and its subdirectories.

```
.
├── (clone of this repository)
│   ├── ......
│   └── shell
│       ├── ......
│       └── step1.sh <- HERE
└── dataset
    ├── repositories
    │   ├── rep1
    │   ├── rep2
    │   ├── ......
    │   ├── (repX)
    │   │   ├── original
    │   │   └── copied
    │   ├── ......
    │   └── rep2000
    └── jdk17modules
```

### Cloning Repositories
Describe a list of GitHub repository URLs in 'repository_urls.txt' (up to 2,000) in the 'shell' directory.
The original list is also available in the 'list' directory.

(repository_urls.txt) Like this, each URL must be separated by a new line:
```
https://github.com/elastic/elasticsearch
https://github.com/ReactiveX/RxJava
https://github.com/square/retrofit
......
https://github.com/godstale/retrowatch

```
NOTE: Without a new line after final url, the last repository will not be cloned.

Run 'step2.sh' in the 'shell' directory to add jdk17 modules in 'jdk17modules' directory and a clone in each 'original' directory and its copy in each 'copied' directory.

```
.
├── (clone of this repository)
│   ├── ......
│   └── shell
│       ├── ......
│       ├── repository_urls.txt <- ADD THIS
│       └── step2.sh
└── dataset
    ├── repositories
    │   ├── rep1
    │   ├── rep2
    │   ├── ......
    │   ├── (repX)
    │   │   ├── original
    │   │   │   └── (a clone of a dataset repository)
    │   │   └── copied
    │   │       └── (a copy of the clone above)
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

Our scripts take several arguments.
In the first argument, please specify the step you want to run e.g. `step1`, `step7`.
In the second argument, please specify the size of the dataset (the number of cloned repositories) e.g. `100`. If you do not specify anything, the default is `2000`.

### Step 1
dataset spec

### Step 2
rq1-1

### Step 3
rq1-2

### Step 4
rq2-1

### Step 5
rq1-1 record_history.csv

### Step 6
rq2-1 constructor_override_cases

### Step 7
rq2-2 class_data.csv

### Step8
rq2-2 to_class_conversions, converted_record_data

### Step9
rq1-1 expressions.csv

### Step10
rq2-2 accessors.csv