# sugihara_record_resolver

## Required Directory Structure
WARNING! This project requires another directory where this repository should be cloned into.

```
.
├── (clone of this repository)
│   ├── data
│   │   ├── commitDifference
│   │   ├── dataset_spec
│   │   ├── project_specs
│   │   ├── rq1-1
│   │   ├── rq1-2
│   │   ├── rq2-1
│   │   └── rq2-2
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

## How to Prepare Dataset
### Before Running Our Scripts
Make sure your Java Development Kit (JDK) version is 17 or higher. 
No other special tools are required.

### Dataset OSS URLs
We provide dataset OSS urls in 'list' directory.
Note that some of them may not be available now because we collected these URLs by scraping on June 1, 2023.

```
.
├── README.md
├── ......
└── list
    └── repository_urls.txt
```

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
```

Run 'step2.sh' in the 'shell' directory to add a clone in each 'original' directory and its copy in each 'copied' directory.

```
.
├── (clone of this repository)
│   ├── ......
│   └── shell
│       ├── ......
│       ├── repository_urls.txt <- add this
│       └── step2.sh <- HERE
└── dataset (will be added later)
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
```