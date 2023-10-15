#!/bin/bash

cd `dirname $0`

unzip -d ../../dataset/jdk17modules ../jdk/src.zip
cd ../../dataset/repositories

i=0
cat ../../sugihara_record_resolver/shell/repository_urls.txt | while read line
do
((i++))
cd rep${i}/original
git clone ${line}
cp -r ./ ../copied
cd ../../
done