#!/bin/bash

cd `dirname $0`

cd ../../dataset/repositories

i=0
cat ../../`dirname $0`/repository_urls.txt | while read line
do
((i++))
cd rep${i}/original
git clone ${line}
cp -r ./ ../copied
cd ../../
done