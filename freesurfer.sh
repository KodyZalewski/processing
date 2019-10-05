#!/bin/bash

# This program provides a selection menu to the client for running one either one of the three successive freesurfer steps or allowing for editing in-between them. Creates an array with subject names and creates a new folder in the subject_directory/subject_name/subject_name_freesurfer (if it doesn't exist) where the data is stored and retrieved. 

# text for initial menu choices when opening this script
function menu() {
PS3='Which freesurfer step would you like to run?: '
options=("autorecon 1" "autorecon 1 edits" "autorecon 2" "autorecon 2 edits" "autorecon 3" "Open freeview" "QA measures" "exit")

## selection menu for choosing freesurfer options
select opt in "${options[@]}"
do
   case $opt in
      "autorecon 1")
   echo "performing autorecon 1"
   FREESURFER_MODE="-autorecon1"
   runFreesurfer;
      ;;
      "autorecon 1 edits")
   echo "performing autorecon 1 edits"
   talairachEdits;
      ;;
      "autorecon 2")
   echo "performing autorecon 2"
   FREESURFER_MODE="-autorecon2"
   runFreesurfer;
      ;;
      "autorecon 2 edits")
   echo "performing autorecon 2 edits"
   FREESURFER_MODE="-autorecon2-cp"	
   runFreesurfer;
      ;;
      "autorecon 3")
   echo "performing autorecon 3"
   FREESURFER_MODE="-autorecon-pial"
   runFreesurfer;
      ;;
      "QA measures")
   echo "Calculating QA measures"
   FREESURFER_MODE="QA measures"
   runFreesurfer;
      ;;
      "Open freeview")
   echo "Opening freeview"
   newFreeview;
      ;;		
      "exit")
   echo ""
   echo "Goodbye "$USER
   echo ""
   exit 0
      ;;
   *) echo invalid option;;		
   esac
done
}

## Add subjects to the array by repeatedly calling this function
function addSubjects() {
echo -n "Enter subject name here: "
read subjectName
arr+=($subjectName)
echo -n "Would you like to add more subjects?(Y/n): "
read addMore
if [ $addMore == 'Y' ]; then
   addSubjects;
elif [ $addMore == 'n' ]; then
   echo "Proceeding with $FREESURFER_MODE."
else
   echo "Please enter a yes(Y) or no(n) answer."
   addSubjects; 
fi
}

## Actually run freesurfer by calling this function, generates the array, reads in the user input for the study and desired subjects
function runFreesurfer() { 
echo -n "What study would you like to process? (e.g. LRMC, EVOLVE etc.): "
read study
arr=()
echo -n "Are there any subjects you would like to add?(Y/n): "
read option
if [ $option == 'Y' ]; then
   addSubjects;   
fi
# checks if we should use white matter edits before the program starts running
if [ "$FREESURFER_MODE" == "-autorecon2-cp" ]; then
   whiteMatterEdits;
fi
if [ "$FREESURFER_MODE" == "-autorecon2" ]; then
   bigventricles;
fi
for f in "${arr[@]}"; do
   #if [ "$study" == "ADNI" ]; then #exclusively for ADNI, could probably be changed for other studies too 
   #   export SUBJECTS_DIR=/evolveshare/$study/$f/F6 
   #else 
   export SUBJECTS_DIR=/evolveshare/$study/$f
   #fi;
   APPENDING="_FREESURFER" 	
   SUBJID=$f$APPENDING
   if [ ! -d $SUBJECTS_DIR ]; then
      warning;
      echo ""
      echo "Subject $SUBJID does not exist, this will be output"
      echo "to the log file, please make sure the subject name "
      echo "was spelled correctly and that they are located in "
      echo "the correct study."
      echo ""
      "The directory $SUBJID was not found, make sure it was spelled correctly and exists, then try again. " >> /evolveshare/EVOLVE/Freesurfer_Scripts/script_error.log
      exit 1
   # if autorecon1 is the desired choice
   elif [ "$FREESURFER_MODE" == "-autorecon1" ]; then
      autorecon1; 
   #if autorecon2 is the desired choice
   elif [ "$FREESURFER_MODE" == "-autorecon2-cp" ] || [ "$FREESURFER_MODE" == "-autorecon2" ] || [ "$FREESURFER_MODE" == "-autorecon2-wm -expert /evolveshare/EVOLVE/Freesurfer_Scripts/expert_options/expert.opts" ] || [ "$FREESURFER_MODE" == "-autorecon2 -bigventricles" ]; then
      autorecon2;		
   # if autorecon3 is the desired choice (runs -pial anyway since it doesn't take *that* much longer to do)			  
   elif [ "$FREESURFER_MODE" == "-autorecon-pial" ]; then
      autorecon3;
   elif [ "$FREESURFER_MODE" == "QA measures" ]; then
      CNR;
   # this shouldn't happen
   else
      echo "This statement should not be reached, something has gone wrong with this program"
      " A selection was made that should not be reached for $SUBJID" >> /evolveshare/EVOLVE/Freesurfer_Scripts/script_error.log
      exit 1; 
   fi
   [[ -e $f ]] || continue      
done
menu;
}

# runs autorecon1, shuffles original niftis and mgz files around, makes sure the freesurfer won't complain about missing folders/files 
function autorecon1() {
if [ ! -d $SUBJECTS_DIR/$SUBJID ]; then
   mkdir $SUBJECTS_DIR/$SUBJID
fi
if [ ! -d $SUBJECTS_DIR/$SUBJID/mri ]; then
   mkdir $SUBJECTS_DIR/$SUBJID/mri
fi
if [ -d $SUBJECTS_DIR/*_MRICRON/*_MPRAGE ]; then
   cd $SUBJECTS_DIR/*_MRICRON/*_MPRAGE
elif [ -d $SUBJECTS_DIR/*_MRICRON/*_MPRAGE_FS ]; then # for alternative naming convention
   cd $SUBJECTS_DIR/*_MRICRON/*_MPRAGE_FS
else # for nifti files in the top directory of ADNI
   #THIS NOT FINISHED 
   cd $SUBJECTS_DIR
fi 
for nifti in *; do
   if [ -f $nifti ]; then
      echo ""
      echo "$nifti"
      if [[ ! "$nifti" =~ "co" ]] && [[ ! "$nifti" =~ "o" ]] && [[ ! "$nifti" =~ "std" ]] && [[ "$nifti" =~ ".nii" ]] && [[ "$study" != "LETBI" ]] && [[ "$study" != "ADNI" ]]; then
         echo ""
         echo "Copying $nifti to $f's freesurfer directory."
         echo ""
         cp $nifti $SUBJECTS_DIR/$SUBJID/mri
         cp $SUBJECTS_DIR/$SUBJID/mri/*nii $SUBJECTS_DIR/$SUBJID/mri/001
      elif [ "$study" == "LETBI" ]; then # for any study that needs to use the standardized nifti memprage instead, change as needed 
         if [[ "$nifti" =~ "std.nii" ]]; then
            echo ""
            echo "Copying $nifti to $f's freesurfer directory."
            echo ""
            cp $nifti $SUBJECTS_DIR/$SUBJID/mri
            mri_concat --rms --i $SUBJECTS_DIR/$SUBJID/mri/*std.nii --o $SUBJECTS_DIR/$SUBJID/mri/$f'_memprage'.mgz
            cp $SUBJECTS_DIR/$SUBJID/mri/$f'_memprage'.mgz $SUBJECTS_DIR/$SUBJID/mri/001
            mri_convert $SUBJECTS_DIR/$SUBJID/mri/$f'_memprage'.mgz $SUBJECTS_DIR/$SUBJID/mri/001.mgz
            cp $SUBJECTS_DIR/$SUBJID/mri/001.mgz $SUBJECTS_DIR/$SUBJID/mri/orig/001.mgz
         fi
      elif [ "$study" == "ADNI" ]; then
         #FIX THIS AT SOME POINT PERHAPS
         echo "roar"
      else
         echo ""
         echo "A file was found that was likely cropped/oriented or is a duplicate. Excluding this nifti from analysis."
         echo ""
      fi
   fi
done
cd $SUBJECTS_DIR/$SUBJID     	 	  
if [ ! -f $SUBJECTS_DIR/$SUBJID/mri/001 ]; then
   warning;
   echo "$SUBJID's mgz file does not exist. This is likely" 
   echo "because there were multiple acquisitions for this subject."
   echo "This error has been output to the error log."
   echo "$SUBJID does not have a vaild nifti file, or multiple files" >> /evolveshare/EVOLVE/Freesurfer_Scripts/script_error.log    
else
   if [ ! -f $SUBJECTS_DIR/$SUBJID/mri/orig/001.mgz ]; then	  	
      mri_convert $SUBJECTS_DIR/$SUBJID/mri/001 $SUBJECTS_DIR/$SUBJID/mri/001.mgz
   fi 
   # line below this actually runs freesurfer program
   recon-all $FREESURFER_MODE -subjid $SUBJID
   wait;
fi;
}

function autorecon2() {
if [ "$FREESURFER_MODE" != "-autorecon2" ]; then
   # this if statement will check if there is a tmp folder for placing control points, if not, one is created with previous control points
   if [ ! -d $SUBJECTS_DIR/$SUBJID/tmp ]; then
      mkdir $SUBJECTS_DIR/$SUBJID/tmp
   fi
   # if we don't have a current set of control points, but a previous one is in subjectID/mri, use those
   if [ -f $SUBJECTS_DIR/$SUBJID/mri/control.dat ] && [ ! -f $SUBJECTS_DIR/$SUBJID/tmp/control.dat ]; then
      cp $SUBJECTS_DIR/$SUBJID/mri/control.dat $SUBJECTS_DIR/$SUBJID/tmp/control.dat
   fi
   # this if statement will initiate if we are keeping voxels of 255 intensity during manual wm editing, original file needs to be removed to do so
   if [ -f $SUBJECTS_DIR/$SUBJID/scripts/expert-options ]; then
      rm $SUBJECTS_DIR/$SUBJID/scripts/expert-options
   fi
   # this if statement will intiate if autorecon3 has already been run on the subject, saving the original stats files in a sub-folder of stats
   if [ -f $SUBJECTS_DIR/$SUBJID/mri/aparc+aseg.mgz ] && [ -f $SUBJECTS_DIR/$SUBJID/stats/aseg.stats ]; then
      if [ ! -d $SUBJECTS_DIR/$SUBJID/stats/orig_stats ]; then
         mkdir $SUBJECTS_DIR/$SUBJID/stats/orig_stats
      fi
      cd $SUBJECTS_DIR/$SUBJID/stats
      for file in *stats
      do
         mv "${file}" "${file/stats/stats_old}"
      done
      if [ -f $SUBJECTS_DIR/$SUBJID/stats/CNR_$SUBJID.txt ]; then
         mv $SUBJECTS_DIR/$SUBJID/stats/CNR_$SUBJID.txt $SUBJECTS_DIR/$SUBJID/stats/orig_stats
      fi
      if [ -f $SUBJECTS_DIR/$SUBJID/stats/wmsnr.e3.dat ]; then
         mv $SUBJECTS_DIR/$SUBJID/stats/wmsnr.e3.dat $SUBJECTS_DIR/$SUBJID/stats/orig_stats
      fi
      mv * orig_stats_old      	
   fi     
fi
# line below actually runs the freesurfer program
recon-all $FREESURFER_MODE -subjid $SUBJID
wait
}

# chooses between running edits with control points exclusively or running it with white matter edits 
function whiteMatterEdits() {
echo -n "Did you want to include white matter edits alongside control points?(Y/n): "
read whiteMatterEdits;
# checks if white matter edits are going to be run alongside control points, if so, changes the command flag appropriately
if [[ $whiteMatterEdits =~ ^(yes|y|Y|Yes|YES)$ ]]; then
   FREESURFER_MODE="-autorecon2-wm -expert /evolveshare/EVOLVE/Freesurfer_Scripts/expert_options/expert.opts"
elif [ -n "$arr" ]; then 
   FREESURFER_MODE="-autorecon2-cp"
else
   echo "Please enter in a yes or no answer(Y/n)."
   whiteMatterEdits	
fi;
}

# runs autorecon3 alongside generating CNR/SNR statistics for the subject and moving the labels to the correct folder
function autorecon3() {
# this if statement checks to see if previous set of control points needs to be saved
if [ -f $SUBJECTS_DIR/$SUBJID/tmp/control.dat ]; then
   cp $SUBJECTS_DIR/$SUBJID/tmp/control.dat $SUBJECTS_DIR/$SUBJID/mri/control.dat
   rm $SUBJECTS_DIR/$SUBJID/tmp/control.dat
fi
# line below actually runs the freesurfer program
recon-all $FREESURFER_MODE -subjid $SUBJID
wait;
echo ""
echo "Generating CNR/SNR statistics for subject $SUBJID"
echo ""
CNR;
wait;
if [ ! -d $SUBJECTS_DIR/$SUBJID/original_labels ]; then
   mkdir $SUBJECTS_DIR/$SUBJID/original_labels
fi
mv $SUBJECTS_DIR/?h.EC* $SUBJECTS_DIR/$SUBJID/original_labels
mv $SUBJECTS_DIR/fsaverage $SUBJECTS_DIR/$SUBJID/original_labels
}

# changes freesurfer to run autorecon2 with a non-linear registration of ventricles first
function bigventricles() {
echo -n "(To handle large ventricles) Do you want to run a registration of the ventricles first for these subjects?(Y/n)): "
read ventricles
if [[ $ventricles =~ ^(yes|y|Y|Yes|YES)$ ]]; then
   FREESURFER_MODE="-autorecon2 -bigventricles"
else
   echo ""  
   echo "Continuing with autorecon 2."
   echo "" 
fi     
}

# GENERIC WARNING INTENDED TO INCITE POTENTIALLY NEEDLESS PANIC 
function warning() {
echo "*****		*****			*****"
echo "*****		*****			*****"
echo "*****           WARNING!!!		*****"
echo "*****		*****			*****"	
echo "*****		*****			*****"
}

# checks to see if control points exist, and if so, where (therefore you don't have to load them each time you open freeview)
function thisFreeview() {
if [ -f $SUBJECTS_DIR/$SUBJID/tmp/control.dat ]; then
   controlPoints="tmp"
   withControlPoints; 
elif [ -f $SUBJECTS_DIR/$SUBJID/mri/control.dat ]; then
   controlPoints="mri"
   withControlPoints;
else
   echo "No control points to display for subject $SUBJID"
   withoutControlPoints;
fi
}

# open freeview with control points
function withControlPoints() {
freeview -v $SUBJECTS_DIR/$SUBJID/mri/wm.mgz:colormap=heat:opacity=0.1 \
$SUBJECTS_DIR/$SUBJID/mri/brainmask.mgz \
-f $SUBJECTS_DIR/$SUBJID/surf/lh.white:edgecolor=yellow \
$SUBJECTS_DIR/$SUBJID/surf/lh.pial:edgecolor=cyan \
$SUBJECTS_DIR/$SUBJID/surf/rh.white:edgecolor=yellow \
$SUBJECTS_DIR/$SUBJID/surf/rh.pial:edgecolor=cyan \
-c $SUBJECTS_DIR/$SUBJID/$controlPoints/control.dat
}

# open freeview without control points
function withoutControlPoints() {
freeview -v $SUBJECTS_DIR/$SUBJID/mri/wm.mgz:colormap=heat:opacity=0.1 \
$SUBJECTS_DIR/$SUBJID/mri/brainmask.mgz \
-f $SUBJECTS_DIR/$SUBJID/surf/lh.white:edgecolor=yellow \
$SUBJECTS_DIR/$SUBJID/surf/lh.pial:edgecolor=cyan \
$SUBJECTS_DIR/$SUBJID/surf/rh.white:edgecolor=yellow \
$SUBJECTS_DIR/$SUBJID/surf/rh.pial:edgecolor=cyan
}

### If the subject directory and subject are not defined, this allows client input 
function newFreeview()
{
echo -n "What study is your subject in? (e.g. LRMC, EVOLVE etc.): "
read study
echo -n "What subject number would you like to take a look at?): "
read f
APPENDING="_FREESURFER" 	
SUBJID=$f$APPENDING
export SUBJECTS_DIR=/evolveshare/$study/$f
thisFreeview;
}

## Creates a text file containg all of the QA measures for the subject and places them in the subject_id/stats folder. 
# These include the SNR (signal-to-noise ratio) and CNR (contrast-to-noise ratio) for various measures of wm/gm/CSF. 
function CNR()
{
# checks to see if the orig_stats directory and the CNR stats already exist
# moves old file to the old stats folder 
if [ -f $SUBJECTS_DIR/$SUBJID/stats/CNR_$f.txt ]; then
   if [ ! -d $SUBJECTS_DIR/$SUBJID/stats/orig_stats ]; then
      mkdir $SUBJECTS_DIR/$SUBJID/stats/orig_stats
   fi
   #renames any files in the orig stats folder
   for file in $SUBJECTS_DIR/$SUBJID/stats/orig_stats/CNR*; do
      echo "Renaming file $file"
      mv $SUBJECTS_DIR/$SUBJID/stats/orig_stats/${file} $SUBJECTS_DIR/$SUBJID/stats/orig_stats/older_${file}
   done 
   mv $SUBJECTS_DIR/$SUBJID/stats/CNR_$f.txt $SUBJECTS_DIR/$SUBJID/stats/orig_stats
fi 
mri_cnr $SUBJECTS_DIR/$SUBJID/surf $SUBJECTS_DIR/$SUBJID/mri/norm.mgz >> $SUBJECTS_DIR/$SUBJID/stats/CNR_$f.txt
wait; 
mri_cnr $SUBJECTS_DIR/$SUBJID/surf $SUBJECTS_DIR/$SUBJID/mri/brain.mgz >> $SUBJECTS_DIR/$SUBJID/stats/CNR_$f.txt
wait;
cd $SUBJECTS_DIR
export QA_TOOLS=/usr/local/freesurfer/QAtools
wm-anat-snr --s  $SUBJID >> $SUBJECTS_DIR/$SUBJID/stats/CNR_$f.txt
wait;
cd $SUBJECTS_DIR/$SUBJID/stats
#if freesurfer v6.0 was used for processing you may get extra columns, different extraction is needed
#awk snr extraction needs to come from separate files declared each time or otherwise it treats them as single file 
oldfile=CNR_$f.txt
newfile=CNR_total_$f.txt 
if [[ ! -z $(grep 6.0 $SUBJECTS_DIR/$SUBJID/scripts/build-stamp.txt) ]]; then
   awk '{if (NR==1) print$2}' ORS=" " wmsnr.e3.dat >> $newfile
   awk '{if (NR==11) print$4}' ORS=" " $oldfile >> $newfile 	
   awk '{if (NR==14) print$4}' ORS=" " $oldfile >> $newfile	 
   awk '{if (NR==11) print$8}' ORS=" " $oldfile >> $newfile	
   awk '{if (NR==14) print$8}' ORS=" " $oldfile >> $newfile
   awk '{if (NR>58) print$11}' ORS=" " $SUBJECTS_DIR/$SUBJID/stats/lh*.pct.stats >> $newfile
   awk '{if (NR>58) print$11}' ORS=" " $SUBJECTS_DIR/$SUBJID/stats/rh*.pct.stats >> $newfile 
elif [[ ! -z $(grep 5.3 $SUBJECTS_DIR/$SUBJID/scripts/build-stamp.txt) ]]; then
   awk '{if (NR==88) print$2}' ORS=" " $oldfile >> $newfile
   awk '{if (NR==13) print$4}' ORS=" " $oldfile >> $newfile 	
   awk '{if (NR==16) print$4}' ORS=" " $oldfile >> $newfile	 
   awk '{if (NR==13) print$8}' ORS=" " $oldfile >> $newfile	
   awk '{if (NR==16) print$8}' ORS=" " $oldfile >> $newfile
   awk '{if (NR>54) print$11}' ORS=" " $SUBJECTS_DIR/$SUBJID/stats/lh*.pct.stats >> $newfile
   awk '{if (NR>54) print$11}' ORS=" " $SUBJECTS_DIR/$SUBJID/stats/rh*.pct.stats >> $newfile
else
   echo ""
   echo "Unknown version of freesurfer provided, please check before continuing with CNR extraction"
   echo ""
   exit 1	
fi
#oldfile=lh*.pct.stats
#awk '{if (NR>54) print$11}' ORS=" " $oldfile >> $newfile
#oldfile=rh*.pct.stats
#awk '{if (NR>54) print$11}' ORS=" " $SUBJECTS_DIR/$SUBJID/?h*pct.stats >> $newfile
awk '{gsub(",",""); print}' $newfile >> final_CNR_$f.txt
if [ -f $newfile ]; then
   rm $newfile
fi
#oldfile=CNR_$f.txt
if [ -f $oldfile ]; then
   rm $oldfile
fi
echo ""
echo "Finished processing $SUBJID for contrast-to-noise and signal-to-noise ratios."
echo ""
}

## brings up freeview/tkregister for autorecon1 manual editing
function braincheck()
{
echo "Check the brainmask. If it looks abnormally bright load brainmask.auto.mgz and rename it brainmask.mgz"
freeview -v $SUBJECTS_DIR/$SUBJID/mri/brainmask.mgz
wait;
echo "It is prudent for one to check the talairach transform."
tkregister2 --mgz --s $SUBJID --fstal
wait;
}

## re-runs part of autorecon1 needed for realigning talairach to the brainmask
function talairachregen()
{
echo " "
echo "Applying edits to the talairach transform."
echo " "
awk -f $FREESURFER_HOME/bin/extract_talairach_avi_QA.awk $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_avi.log
wait;
tal_QC_AZS $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_avi.log
wait; 
mri_nu_correct.mni --i $SUBJECTS_DIR/$SUBJID/mri/orig.mgz --o $SUBJECTS_DIR/$SUBJID/mri/nu.mgz --n 2 --uchar $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach.xfm 
wait; 
mri_normalize -g 1 $SUBJECTS_DIR/$SUBJID/mri/nu.mgz $SUBJECTS_DIR/$SUBJID/mri/T1.mgz
wait;
mri_em_register -skull nu.mgz $FREESURFER_HOME/average/RB_all_2008-03-26.gca $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_with_skull.lta
wait; 
mri_watershed -T1 -brain_atlas $FREESURFER_HOME/average/RB_all_2008-03-26.gca $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_with_skull.lta $SUBJECTS_DIR/$SUBJID/mri/T1.mgz $SUBJECTS_DIR/$SUBJID/mri/brainmask.auto.mgz
wait;
cp $SUBJECTS_DIR/$SUBJID/mri/brainmask.auto.mgz $SUBJECTS_DIR/$SUBJID/mri/brainmask.mgz
}

## performs watershed algorithm on brainmask if too much dura is removed/left adjacent to the brainmask
function watershed()
{
echo -n "Edit watershed algorithm, define new preflooding height in percent (default is 25%, smaller numbers denote a more aggressive algorithm), program will then re-run. Input here: "
read NEWHEIGHT 
mri_watershed -h $NEWHEIGHT -atlas /usr/local/freesurfer/average/RB_all_2008-03-26.gca $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_with_skull.lta $SUBJECTS_DIR/$SUBJID/mri/T1.mgz $SUBJECTS_DIR/$SUBJID/mri/brainmask.mgz
wait;
}

## This will only be invoked if gcut needs to be used 
function gcutshortcut()
{
echo "Performing gcut to remove extraneous dura";
#recon-all -skullstrip -gcut -subjid $SUBJECTFREESURFER
echo -n "Edit gcut algorithm with a new % intensity to white matter 'X' (0<X<1). Default is 40%, large values cut more tissue, but could also cut out pial surface. Input here: "
read GCUTVALUE
rm -f $SUBJECTS_DIR/$SUBJID/mri/brainmask.gcuts.mgz
mri_gcut -110 -mult $SUBJECTS_DIR/$SUBJID/mri/brainmask.auto.mgz -T $GCUTVALUE $SUBJECTS_DIR/$SUBJID/mri/brainmask.auto.mgz $SUBJECTS_DIR/$SUBJID/mri/brainmask.gcuts.mgz
wait;
echo "The removed surface can be viewed in tkmedit and will be labeled in red."
mri_binarize --i $SUBJECTS_DIR/$SUBJID/mri/brainmask.gcuts.mgz --o $SUBJECTS_DIR/$SUBJID/mri/brainmask.gcuts.mgz --binval 999 --min 1
wait;  
}

## This will only be invoked if the normalization is off (i.e. noteable hyper/hypointensities)
function normalize()
{
echo -n "Adjust normalization parameters to your specifications, default max intensity/mm gradient is 1. Input float value here: "
read textNormalize
echo -n "Perform gentler normalization?(Y/n): "
read textGentle
if [ $textGentle = "Y" ]; then
   mri_normalize -g $textNormalize $SUBJECTS_DIR/$SUBJID/mri/nu.mgz $SUBJECTS_DIR/$SUBJID/mri/T1.mgz
   wait;
   mri_em_register -skull nu.mgz $FREESURFER_HOME/average/RB_all_2008-03-26.gca $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_with_skull.lta
   wait; 
   mri_watershed -T1 -brain_atlas $FREESURFER_HOME/average/RB_all_2008-03-26.gca $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_with_skull.lta $SUBJECTS_DIR/$SUBJID/mri/T1.mgz $SUBJECTS_DIR/$SUBJID/mri/brainmask.auto.mgz
   wait;
   cp $SUBJECTS_DIR/$SUBJID/mri/brainmask.auto.mgz $SUBJECTS_DIR/$SUBJID/mri/brainmask.mgz	
elif [ $textGentle = "n" ]; then
   mri_normalize -g $textNormalize -gentle $SUBJECTS_DIR/$SUBJID/mri/nu.mgz $SUBJECTS_DIR/$SUBJID/mri/T1.mgz
   wait;
   mri_em_register -skull nu.mgz $FREESURFER_HOME/average/RB_all_2008-03-26.gca $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_with_skull.lta
   wait; 
   mri_watershed -T1 -brain_atlas $FREESURFER_HOME/average/RB_all_2008-03-26.gca $SUBJECTS_DIR/$SUBJID/mri/transforms/talairach_with_skull.lta $SUBJECTS_DIR/$SUBJID/mri/T1.mgz $SUBJECTS_DIR/$SUBJID/mri/brainmask.auto.mgz
   wait;
   cp $SUBJECTS_DIR/$SUBJID/mri/brainmask.auto.mgz $SUBJECTS_DIR/$SUBJID/mri/brainmask.mgz
else
   echo "Please enter a Y or n answer." 
   normalize;
fi;
}

## checks skullstripping
function skullstripif()
{
braincheck;
wait;
echo -n "Does the subject talairach transform look correct?(Y/n): " 
read text 
if [ $text = "Y" ]; then
  echo -n "Does cutting look correct?(Y/n) "
  read textA
  if [ $textA = "Y" ]; then
    echo -n "Does the subject brainmask look correct?(Y/n): "
    read textB
    if [ $textB = "Y" ]; then
      echo -n "Do normalization parameters differ greatly (i.e. hyper/hypointensities)?(Y/n): "
      read textC
      if [ $textC = "Y" ]; then
         normalize;
	 wait;
	 skullstripif;
      elif [ $textC = "n" ]; then
	 echo "Continuing with autorecon2 and white matter segmentation."	
      else
	 echo "Please enter a Y (yes) or n (no)";
         skullstripif;
      fi;      
    elif [ $textB = "n" ]; then       
      watershed; 
      wait;
      skullstripif; 
    else 
      echo "Please enter a Y (yes) or n (no)";
      skullstripif; 
    fi;
  elif [ $textA = "n" ]; then
    watershed;
    wait;
    gcutshortcut;
    wait; 
    skullstripif;
  else
    echo "Please enter a Y (yes) or n (no)";
    skullstripif;   
  fi;
elif [ $text = "n" ]; then
  echo "Applying changes made to the talairach in tkregister2."   
  talairachregen
  wait; 
  skullstripif; 
else
  echo "Please enter a Y (yes) or n (no)";
  skullstripif; 
fi;
}

## Series of commands for running Autorecon1 edits ### normalization doesn't work for some reason
function talairachEdits() {
echo -n "What study is your subject in? (e.g. LRMC, EVOLVE etc.): "
read study
echo -n "What subject number would you like to take a look at?): "
read f
APPENDING="_FREESURFER" 	
SUBJID=$f$APPENDING
# potentially remove the bottom line
if [ "$study" == "ADNI" ]; then #exclusively for ADNI, could probably be changed for other studies too 
   export SUBJECTS_DIR=/evolveshare/$study/$f/F6 
else 
   export SUBJECTS_DIR=/evolveshare/$study/$f
fi;
skullstripif;
}

# "This freesurfer editing manager was written by Kody J. Zalewski for the"
# "MacDonald lab. If you have questions about this, you can email him at:"
# "zalewk@u.washington.edu, or if this email no longer works you can"
# "email him at: zalew010@umn.edu"
echo ""
menu

exit 0			
