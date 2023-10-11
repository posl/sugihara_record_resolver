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
│       └── step1.sh 
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

