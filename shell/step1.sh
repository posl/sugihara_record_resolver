#!/bin/bash

cd ../../
mkdir dataset

cd dataset

mkdir repositories
mkdir jdk17modules

cd repositories

for i in `seq 2000`
do
mkdir rep${i}
cd rep${i}
mkdir original
mkdir copied
cd ../
done