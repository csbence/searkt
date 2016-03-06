#!/bin/bash

SCRIPT=$(basename $0)
OPTIONS=":hd:m:a:n:t:p:vi:o:gG:"
PROJECT_NAME="real-time-search"
GRADLE=./gradlew
BUILD_DIR=build
DEFAULT_DIR="unknown"
RESULTS_TOP_DIR="results"
DIR=$RESULTS_TOP_DIR
BIN="$BUILD_DIR/install/$PROJECT_NAME/bin"
RUN_SCRIPT="$BIN/$PROJECT_NAME"

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
  echo "  t <type>     specify the termination type"
  echo "  p <param>    specify the termination parameter to provide"
  echo "  v            visualize the experiment"
  echo "  i <name>     specify an instance name for the configuration"
  echo "  o <file>     specify an output file name"
  echo "  g            run gradle to install the distribution"
  echo "  G <args>     run gradle with custom arguments"
  echo "Results will be placed in separate files with the following directory structure:"
  echo "  results/algorithm/domain/params/[instance]/out"
  echo "If a parameter is not given then the directory name will be '$DEFAULT_DIR'"
}

add_dir() {
  if [ -z "$1" ]; then
    echo "Internal script error: missing parameter to add_dir"
  else
    DIR="$DIR/$1"
    if [ ! -d "$DIR" ]; then
      mkdir "$DIR"
    fi
  fi
}

add_arg() {
  if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Internal script error: missing parameter to add_arg"
  else
      EXPERIMENT_ARGS="$EXPERIMENT_ARGS $1 $2"
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
        echo "Map file $MAP does not exist"
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
    t)
      TERM_TYPE=$OPTARG
      ;;
    p)
      TERM_PARAM=$OPTARG
      ;;
    v)
      VISUALIZE=true
      ;;
    i)
      INSTANCE_NAME=$OPTARG
      ;;
    o)
      OUT_FILE=$OPTARG
      ;;
    g)
      RUN_GRADLE=true
      ;;
    G)
      RUN_GRADLE=true
      GRADLE_PARAMS=$OPTARG
      ;;
    \?)
      echo "Invalid argument given: '$OPTARG'" >&2
      usage
      exit 1
      ;;
    :)
      echo "Option '$OPTARG' requires a parameter" >&2
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
  add_arg "-a" "$ALG"
  add_dir "$ALG"
else
  add_dir "$DEFAULT_DIR"
fi
if [ -n "$DOMAIN" ]; then
  add_arg "-d" "$DOMAIN"
  add_dir "$DOMAIN"
else
  add_dir "$DEFAULT_DIR"
fi
if [ -n "$TERM_PARAM" ]; then
  add_arg "-p" "$TERM_PARAM"
  add_dir "$TERM_PARAM"
else
  add_dir "$DEFAULT_DIR"
fi
if [ -n "$INSTANCE_NAME" ]; then
  add_dir "$INSTANCE_NAME"
fi
if [ -n "$MAP" ]; then
  add_arg "-m" "$MAP"
fi
if [ -n "$NUM_RUNS" ]; then
  add_arg "-n" "$NUM_RUNS"
fi
if [ -n "$TERM_TYPE" ]; then
  add_arg "-t" "$TERM_TYPE"
fi
if [ -n "$VISUALIZE" ]; then
  add_arg "-v" "$VISUALIZE"
fi

# Setup out file
OUT_FILE="$DIR/$OUT_FILE"
CURRENT=
COUNTER=0
while [ -f "$OUT_FILE$CURRENT" ]; do
  let COUNTER+=1
  if [ $COUNTER -lt 10 ]; then
    CURRENT="_0$COUNTER"
  else
    CURRENT="_$COUNTER"
  fi
done
add_arg "-o" "$OUT_FILE"

# Run it
if [ "$RUN_GRADLE" = true ]; then
  $GRADLE installDist $GRADLE_PARAMS
fi

if [ ! -d "$BIN" ]; then
  echo "'$BIN' directory does not exist; build first or run with gradle"
  usage
  exit 1
fi
$RUN_SCRIPT "$EXPERIMENT_ARGS"
