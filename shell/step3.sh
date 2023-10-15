#!/bin/bash

cd `dirname $0`

cd ../../dataset/repositories

for i in `seq 10`
do
cd rep${i}/original
REP=`ls`
(cd ${REP} && git shortlog -ns --until="2023-06-01" > ../../commits.txt)
cd ../../

done