# sugihara_record_resolver

## Required Directory Structure
WARNING! This project requires another directory where this repository should be cloned into.

.
├── (clone of this repository)
│   ├── data
│   │   ├── commitDifference
│   │   │   ├── (dataset repository name)
│   │   │   │   ├── repository-info.txt
│   │   │   │   ├── (yyyy-(m)m-(d)d:hh:mm:ss)
│   │   │   │   │   ├── commit-info.txt
│   │   │   │   │   ├── (fileX)
│   │   │   │   │   │   ├── before.java (optional)
│   │   │   │   │   │   └── after.java (optional)
│   │   │   │   │   └── ......
│   │   │   │   └── ......
│   │   │   └── ......
│   │   ├── dataset_spec
│   │   │   ├── Java16_repository_metrics.csv
│   │   │   ├── all_repository_metrics.csv
│   │   │   ├── record_repository_metrics.csv
│   │   │   └── repository_urls.txt
│   │   ├── project_specs
│   │   ├── rq1-1
│   │   ├── rq1-2
│   │   ├── rq2-1
│   │   ├── rq2-2
│   │   └── api17info.ser
│
└── dataset
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
    └── JavaAPI17
        ├── java.base
        ├── java.compiler
        ├── ......
        └── (jdk17 modules)