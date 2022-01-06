# exvivo
Manipulates exvivo brain imaging of both half and whole hemispheres as preprocessing for volumetric analysis. Freesurfer assumes a T1 weighted image surrounded by a skull, exvivo subjects are usually T2 weighted images without a skull. This program flips the intensity of the scan and removes voxels exterior of the brain assuming the lack of a skull. The end result is a nifti file available for volumetric analysis by Freesurfer.

./runExvivo.sh will run the program. Takes a study directory and subject number as arguments.     
