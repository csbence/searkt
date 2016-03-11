#!/bin/bash

SCRIPT=$(basename $0)
OPTIONS=":hd:m:a:n:l:t:p:i:o:DgG:"
PROJECT_NAME="real-time-search"
GRADLE=./gradlew
BUILD_DIR=build
DEFAULT_DIR="unknown"
RESULTS_TOP_DIR="results"
DIR=$RESULTS_TOP_DIR
BIN="$BUILD_DIR/install/$PROJECT_NAME/bin"
RUN_SCRIPT="$BIN/$PROJECT_NAME"
NUM_RUNS=1
RUN_NUM=0

usage() {
# lim:  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
  echo "usage:"
  echo "$SCRIPT [options]"
  echo "options:"
  echo "  h            show this usage info"
  echo "  d <domain>   specify the domain to run against"
  echo "  a <name>     specify the algorithm to run"
  echo "  m <file>     specify a map input file"
  echo "  n <num>      specify the number of experiment runs"
  echo "  l <length>   specify the length of action durations"
  echo "  t <type>     specify the termination type"
  echo "  p <param>    specify the termination parameter to provide"
  echo "  i <name>     specify an instance name for the configuration"
  echo "  o <file>     specify an output file name"
  echo "  D            run the experiments via installed distribution"
  echo "  g            run gradle to install the distribution"
  echo "  G <args>     run gradle with custom arguments"
  echo "Results will be placed in separate files with the following directory structure:"
  echo "  results/algorithm/domain/params/[instance]/out"
  echo "If a parameter is not given then the directory name will be '$DEFAULT_DIR'."
  echo "The output file will be appended with 2 numbers with format 'out_XX_YY',"
  echo "  where XX is a unique digit and YY is the run number of that set of runs."
}

add_dir() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to add_dir"
  else
    NEW_DIR=$(echo $1 | sed -e 's/[^A-Za-z0-9._-\*]/_/g')
    DIR="$DIR/$NEW_DIR"
    if [ ! -d "$DIR" ]; then
      mkdir "$DIR"
    fi
  fi
}

add_arg() {
  if [ -z "$1" ] || [ -z "$2" ]; then
    >&2 echo "Internal script error: missing parameter to add_arg"
  else
    if [ -n "$DIST" ] && [ "$DIST" = true ]; then
      echo "$EXPERIMENT_ARGS $1 \"$2\""
    else
      echo "$EXPERIMENT_ARGS'$1','$2',"
    fi
  fi
}

get_file_num() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to get_file_num"
  else
    if [ $1 -lt 10 ]; then
      echo "_0$1"
    else
      echo "_$1"
    fi
  fi
}

get_unique_filename() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to get_file_num"
  else
    CURRENT=_00
    COUNTER=0
    while [ -f "$1$CURRENT" ] || [ -f "$1${CURRENT}_$RUN_NUM" ] || [ -f "$1${CURRENT}_0$RUN_NUM" ]; do
      let COUNTER+=1
      CURRENT=$(get_file_num $COUNTER)
    done
    echo "$1$CURRENT"
  fi
}

run_gradle() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to run_gradle"
  else
    $GRADLE run $GRADLE_PARAMS -PappArgs="[$(add_arg "-o" "$1")]"
  fi
}

run_dest() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to run_dest"
  else
    eval $RUN_SCRIPT $(add_arg "-o" "$1")
  fi
}

run() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to run"
  else
    EXP_SCRIPT="$1"
    if [ "$NUM_RUNS" -eq 1 ]; then
      $EXP_SCRIPT "$OUT_FILE"
    else
      for ((i=0; i < $NUM_RUNS; i++)); do
        RUN_NUM=$i
        NEW_OUT="$OUT_FILE$(get_file_num $i)"
        $EXP_SCRIPT "$NEW_OUT"
      done
    fi
  fi
}

while getopts $OPTIONS arg; do
  case $arg in
    h)
      usage
      exit 0
      ;;
    d)
      DOMAIN=$OPTARG
      ;;
    m)
      MAP=$OPTARG
      if [ ! -f $MAP ]; then
        >&2 echo "Map file $MAP does not exist"
        usage
        exit 1
      fi
      ;;
    a)
      ALG=$OPTARG
      ;;
    n)
      NUM_RUNS=$OPTARG
      ;;
    l)
      ACT_LEN=$OPTARG
      ;;
    t)
      TERM_TYPE=$OPTARG
      ;;
    p)
      TERM_PARAM=$OPTARG
      ;;
    i)
      INSTANCE_NAME=$OPTARG
      ;;
    o)
      OUT_FILE=$OPTARG
      ;;
    D)
      DIST=true
      ;;
    g)
      RUN_GRADLE=true
      ;;
    G)
      RUN_GRADLE=true
      GRADLE_PARAMS=$OPTARG
      ;;
    \?)
      >&2 echo "Invalid argument given: '$OPTARG'"
      usage
      exit 1
      ;;
    :)
      >&2 echo "Option '$OPTARG' requires a parameter"
      usage
      exit 1
      ;;
  esac
done

shift $((OPTIND-1))

# Experiment Directory Structure:
# results/algorithm/domain/params/instance/out
if [ ! -d "$RESULTS_TOP_DIR" ]; then
  mkdir $RESULTS_TOP_DIR
fi
if [ -z "$OUT_FILE" ]; then
  OUT_FILE="out"
fi

# Translate to experiment expected args and build directory structure
EXPERIMENT_ARGS=""
if [ -n "$ALG" ]; then
  EXPERIMENT_ARGS=$(add_arg "-a" "$ALG")
  add_dir "$ALG"
else
  add_dir "$DEFAULT_DIR"
fi

if [ -n "$DOMAIN" ]; then
  EXPERIMENT_ARGS=$(add_arg "-d" "$DOMAIN")
  add_dir "$DOMAIN"
else
  add_dir "$DEFAULT_DIR"
fi

if [ -n "$TERM_TYPE" ]; then
  EXPERIMENT_ARGS=$(add_arg "-t" "$TERM_TYPE")
  PARAM_DIR="$TERM_TYPE"
fi
if [ -n "$TERM_PARAM" ]; then
  EXPERIMENT_ARGS=$(add_arg "-p" "$TERM_PARAM")
  PARAM_DIR="$PARAM_DIR-$TERM_PARAM"
fi
add_dir "$PARAM_DIR"

if [ -n "$INSTANCE_NAME" ]; then
  add_dir "$INSTANCE_NAME"
fi
  
if [ -n "$MAP" ]; then
  EXPERIMENT_ARGS=$(add_arg "-m" "$MAP")
fi

if [ -n "$ACT_LEN" ]; then
  EXPERIMENT_ARGS=$(add_arg "-l" "$ACT_LEN")
fi

# Setup out file
OUT_FILE=$(get_unique_filename "$DIR/$OUT_FILE")

if [ -n "$DIST" ] && [ "$DIST" = true ]; then
  # Run it
  if [ -n "$RUN_GRADLE" ] && [ "$RUN_GRADLE" = true ]; then
    $GRADLE installDist $GRADLE_PARAMS
  fi

  if [ ! -d "$BIN" ]; then
    >&2 echo "'$BIN' directory does not exist; build first or run with gradle"
    usage
    exit 1
  fi

  run run_dest
else
  run run_gradle
fi
