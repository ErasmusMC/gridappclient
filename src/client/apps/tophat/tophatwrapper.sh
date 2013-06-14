#!/bin/bash
#===================================================================================
#
#	AUTHOR: Bram Stoker
#	COMPANY: Erasmus MC, Bioinformatics Department
#	VERSION: 5.0
#	CREATED: 01.05.2013
#
#	TYPICAL USAGE:	./tophatwrapper [tophatoptions] prefix lfc_host lfc_home output_se output_file software genome reads [prerequisites]
#
#	PARAMETERS:	0)  prefix              Bowtie prefix
#                       1)  lfc_host            Address of the LFC host
#                       2)  lfc_home            LFC home directory
#                       3)  output_se           Address of SE to store results
#                       4)  output_file         Name of the outputfile
#                       5)  software            Relative path from $lfc_home to the binary archive
#                       6)  genome              Relative path from $lfc_home to the genome and indices
#                       7)  reads               Relative path from $lfc_home to the raw sequence
#                       8)  prerequisites       Relative path from $lfc_home to prerequisites
#
#	DESCRIPTION: Unpacks all required data from a SE. Adds TopHat 2 and 
#	dependencies to the PATH. Finally runs TopHat 2 and stores the results.
#
#	FILES:		tophatwrapper
#
#===================================================================================

program_name=$(basename "$0")
script_dir=$(cd $(dirname "$0") && pwd)
workdir="$(mktemp -d)"
verbose=false

trap 'dispose; exit 1' ERR SIGHUP SIGQUIT
cd $workdir

#=== FUNCTION ======================================================================
#
#	 NAME: error
# DESCRIPTION: Display an error message and exit
# PARAMETER 1: the error message to display
#===================================================================================
function error
{
  echo "${program_name}: ${1:-\"Unknown Error\"}" 1>&2
  exit 1
}

#=== FUNCTION ======================================================================
#
#	 NAME: log
# DESCRIPTION: Log date and a message
# PARAMETER 1: the message to log
#===================================================================================
function log
{
   $verbose && echo -e "[$(date +"%Y-%m-%d %T")] ${1:-}"
}

#=== FUNCTION ======================================================================
#
#	 NAME: version
# DESCRIPTION: Display version information
#===================================================================================
function version
{
   echo "${program_name} 5.0"
   echo "Copyright (C) 2013 Erasmus MC, Department of Bioinformatics"
   echo "This software is free to use."
   echo "There is NO WARRANTY, to the extent permitted by law."
   echo ""
   echo "Written by Bram Stoker."
}

#=== FUNCTION ======================================================================
#
#	 NAME: usage
# DESCRIPTION: Display the manual page
#===================================================================================
function usage
{
   echo "Usage: $program_name [tophatoptions] prefix lfc_host lfc_home output_se output_file software genome reads [prerequisites]"
   echo ""
   echo "Arguments:"
   echo "    prefix	  : Bowtie prefix"
   echo "    lfc_host	  : Logical File Catalog host address"
   echo "    lfc_home	  : Logical File Catalog home folder"
   echo "    output_se	  : Address of the Storage Element to store output"
   echo "    output_file   : Name of the file to copy output to"
   echo "    software	  : Name of the binary file to download"
   echo "    genome	  : Name of the genome file to download"
   echo "    reads	  : Name of the reads file to download"
   echo "    prerequisites : Name of the prerequisite file to download"
   echo ""
   echo "Options:"
   echo "    -h/--help	 : Display help and exit"
   echo "    --version    : Display version information and exit"
   echo "    -v/--verbose : Log output to stdout"
   echo ""
   echo "TopHat Options:"
   #tophat --help 2>&1 | tail -n +12 | sed '/-R\/--resume/d' | sed '/--transcriptome-index/d'
}

#=== FUNCTION ======================================================================
#
#	 NAME: dispose
# DESCRIPTION: Cleans up resources
#===================================================================================
function dispose
{
  rm "$script_dir/$(basename $0)" -r "$workdir"
}

#=== FUNCTION ======================================================================
#
#	 NAME: starttimer
# DESCRIPTION: Sets the current time
#===================================================================================
function starttimer
{
  begin=$(date +%s)
}

#=== FUNCTION ======================================================================
#
#	 NAME: stoptimer
# DESCRIPTION: Prints the elasped time (in seconds) between the start and current time
#===================================================================================
function stoptimer
{
  elapsed=$(($(date +%s) - $begin))
  #prevent division by zero, always return 1 as minimum
  [ ! -z $begin ] && [ $elapsed -gt 0 ] && echo $elapsed || echo 1
}

#===================================================================================
# 	Commandline Arguments Processor
#===================================================================================
declare -a args
while [ "$1" != "" ]; do
  case "$1" in
    (-h|--help)		usage
			exit
			;;
    (--version)		version
			exit
			;;
    (-v|--verbose)	verbose=true
			;;
    (--bowtie1)		error "Error: Bowtie1 option is not supported!"
			;;
    (-R|--resume)	shift
			error "Error: Resume option is not supported!"
			;;
    (--transcriptome-index)
			shift
			echo "Warning: Transcriptome-index option is not supported!"
			;;
    (-o|--output-dir)	shift
			echo "Warning: TopHats output-dir option is not supported!"
			;;
    (--tmp-dir)		shift
			echo "Warning: TopHats tmp-dir option is not supported!"
			;;
    #flags
    (--solexa-quals|--solexa1.3-quals|--report-secondary-alignments|--no-discordant|--no-mixed|--coverage-search|--no-coverage-search|--microexon-search|--bowtie-n|--keep-fasta-order|--no-sort-bam|--no-convert-bam|--fusion-search|--fusion-ignore-chromosomes|--no-novel-juncs|--transcriptome-only|--prefilter-multihits|--no-novel-indels)
                        tophat_options=(${tophat_options[@]} "$1")
                        ;;
    #options
    (-*)		tophat_options=(${tophat_options[@]} "$1")
			shift
			tophat_options=(${tophat_options[@]} "$1")
			;;
    #parameters
    (*)			args=(${args[@]} "$1")
  esac
  shift
done

if [ ${#args[@]} -lt 8 ]; then
  echo -e "Error: Invalid number of arguments!\n"
  usage
  exit 1
fi

#===================================================================================
# 	Set Environment & Variables
#===================================================================================
export LFC_HOST="${args[1]}"
export LFC_HOME="${args[2]}"
export LCG_GFAL_INFOSYS=bdii.grid.sara.nl:2170
export LCG_CATALOG_TYPE=lfc
export VO_LSGRID_DEFAULT_SE="${args[3]}"
export PATH=$workdir:$PATH

#===================================================================================
# 	Copy and Extract Required Data from a Storage Element (SE) 
#	onto this Worker Node (WN)
#===================================================================================
for file in ${args[*]:5}; do

log "Copying $file from SE" && starttimer
lcg-cp --verbose --srm-timeout=3600 --connect-timeout=300 --sendreceive-timeout=3600 "lfn:$file" $file
log "Done copying $(ls -lah $file | awk '{ print $5}') ($(($(stat -c%s $file) / $(stoptimer))) bytes/s)\n"
log "Unpacking $file" && starttimer
tar -zxf $file
log "Done unpacking ($(($(stat -c%s $file) / $(stoptimer))) bytes/s)\n"

done

#===================================================================================
#	Run TopHat 2
#===================================================================================
reads=($(find $workdir -type f -regex ".*${args[7]}\(_[12]\)?\.\(fastq\|fq\|fasta\|fa\)\$" | sort))

tophat ${tophat_options[@]} "${args[0]}" "${reads[@]}" 2>&1

#===================================================================================
#	Archive & Copy Results back to a Storage Element (SE)
#===================================================================================
echo -e "\n"
log "Packing results" && starttimer
$(cd "tophat_out" && tar -cf "${args[4]}" *)
log "Done packing ($(($(stat -c%s tophat_out/${args[4]}) / $(stoptimer))) bytes/s)\n"
log "Copying results to SE $result_se" && starttimer
lcg-cr --verbose --srm-timeout=3600 --connect-timeout=300 --sendreceive-timeout=3600 -l "${args[4]}" "tophat_out/${args[4]}" 
log "Done copying $(ls -lah tophat_out/${args[4]} | awk '{ print $5}') ($(($(stat -c%s file:tophat_out/${args[4]}) / $(stoptimer))) bytes/s)\n"

#===================================================================================
#	Dispose Resources and Exit
#===================================================================================
dispose; exit
