#!/bin/bash

cd `dirname $0`

cd ../../dataset/repositories

for i in `seq 2000`
do
cd rep${i}/original
cp -rf ./ ../copied
cd ../../
done