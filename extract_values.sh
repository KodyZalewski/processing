#!/bin/bash

#Written by Kody Zalewski 9.12.18

study_dir=${PWD##*/}
file_name=$study_dir"_volume_measures"
path=/home/ns-zalewk/Desktop/memorycube/ACT_Imaging/CLINICAL
for f in CLINICAL/*; do
	
	# get the subject name as a string from the file containing the subject data
	cd $f
		current_dir=${PWD##*/}
	cd ../..

	# stores all the variables needed from the whitespace delimited text files
	# also pipes them to xargs to remove the trailing whitespace
	scan_name=$(awk '{if (NR==7) print$1}' ORS=" " ${f}/ROIStatistics_WMH.txt | xargs)
	scan_name2=$(awk '{if (NR==7) print$1}' ORS=" " ${f}/ROIStatistics_TBV_VV.txt | xargs)
	wmhvol=$(awk '{if (NR==7) print$6}' ORS=" " ${f}/ROIStatistics_WMH.txt | xargs)
	tbvol=$(awk '{if (NR==7) print$6}' ORS=" " ${f}/ROIStatistics_TBV_VV.txt | xargs)

	vvol=$(awk '{if (NR==8) print$6}' ORS=" " ${f}/ROIStatistics_TBV_VV.txt | xargs)
	
	# sanity check to make sure the scans that the same scan was processed for both the
	# total brain volume/ventricle volume and white matter hyperintensity volume
	if [[ $scan_name == $scan_name2 ]]; then 
		echo "Using $scan_name as volumetric MRI scan"
	else
		echo "Scan $scan_name doesn't match other volumetric file $scan_name2"
		echo "Exiting ... "
		exit 1;
	fi;
	
	# reads the first field of the csv file, obviously, using a comma as a delimiter
	# declares a counter and stores the line (as int) containing pixel dimensions
	first_field=$(awk -F "\"*,\"*" ' { print$1 }' ${f}/${current_dir}_pxl_dims.csv | xargs)
	declare -i j=0
	declare -i number_line=0 
	for i in $first_field; do
		j=$((j+1))			
		if [[ $scan_name == $i ]]; then
			number_line=$j
		fi;			
	done

	# if the counter is 0 that means no scan value was found, otherwise stores the values
	# of the pixel dimensions for x, y, and z in their respective variables
	declare -f pxldimx && declare -f pxldimy && declare -f pxldimz
	if [[ $number_line -eq 0 ]]; then
		echo "Scan not found for subject $current_dir"
		echo ""
	else
		pxldimx=$(awk -v line=$number_line -F "\"*,\"*" ' { if (NR==line) { print$2 } } ' ${f}/${current_dir}_pxl_dims.csv | xargs)
		pxldimy=$(awk -v line=$number_line -F "\"*,\"*" ' { if (NR==line) { print$3 } } ' ${f}/${current_dir}_pxl_dims.csv | xargs)
		pxldimz=$(awk -v line=$number_line -F "\"*,\"*" ' { if (NR==line) { print$4 } } ' ${f}/${current_dir}_pxl_dims.csv | xargs)
	fi;

	# echo pixel dimensions as a sanity check, if none of them are empty (they shouldn't be)
	# calculates voxel dimensions as mm^3, puts the values into cm and outputs them to the $file_name variable
	if [[ -z $pxldimx || -z $pxldimy || -z $pxldimz || -z $wmhvol || -z $tbvol || -z $vvol ]]; then
		echo "Pixel dimensions and volumes must be declared."
		echo "No output for subject $current_dir"
	else
		voxeldim=$(echo "scale=3; $pxldimx * $pxldimy * $pxldimz" | bc -l )

		echo "Pixel dimensions are: $pxldimx x $pxldimy x $pxldimz"
		echo "Total voxel dimensions are: $voxeldim mm^3"
		
		wmhvol=$(echo "scale=2; $wmhvol * $voxeldim / 1000" | bc -l )
		tbvol=$(echo "scale=2; $tbvol * $voxeldim / 1000" | bc -l )
		vvol=$(echo "scale=2; $vvol * $voxeldim / 1000 " | bc -l )

		echo "" && echo ${current_dir} $wmhvol $tbvol $vvol >> $file_name.txt
	fi; 
done

exit 0;
