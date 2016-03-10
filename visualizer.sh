#!/bin/bash

SCRIPT=$(basename $0)
OPTIONS=":hd:m:a:n:t:p:vi:o:DgG:"
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
  echo "$SCRIPT filename"
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
    $GRADLE run $GRADLE_PARAMS -PmainClass="edu.unh.cs.ai.realtimesearch.visualizer.Visualizer" -PappArgs="['$1',]"
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
      EXP_SCRIPT=$1
      $EXP_SCRIPT $2
  fi
}

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
  run run_gradle $1
fi
