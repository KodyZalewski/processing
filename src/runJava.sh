#!/bin/bash


if [[ ! -z $1 && ! -z $2 && ! -z $3 && ! -z $4 ]]; then 
	study=$1
	subject=$2
	input=$3
	output=$4
else
	"ERROR!: Takes four arguments <study> -s <subject> <input scan> <output scan>"
	exit 1
fi

#path to subject to start exvivo processing, change as needed
#currentpath=$(pwd)
#path2subj=../../../${study}/${subject}
#cd $path2subj
#$(pwd) ./exvivo
#cd -

# .jar files need to be included for compiling and running for some reason despite already 
# being in the classpath, add these with -cp /directory/to/.jar for extra jars if needed

#$SOURCE=${PWD##*/}/src

# edit line below as needed
thispath="/home/ns-zalewk/Desktop/memorycube/00_Software/kz_scripts/exvivo"

javac -d $thispath/src/processExvivo.java

java $thispath/src/processExvivo $study $subjects $input $output

exit 0
