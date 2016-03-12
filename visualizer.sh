#!/bin/bash

SCRIPT=$(basename $0)
OPTIONS=":hG:"
GRADLE_ARGS=
MAIN_CLASS="edu.unh.cs.ai.realtimesearch.visualizer.VisualizerApplication"

usage() {
  echo "usage:"
  echo "$SCRIPT [options] resultFile [visualizerOptions]"
  echo "options:"
  echo "  h           show this usage info"
  echo "  G <params>  specify gradle parameters"
}

add_single_arg() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to add_single_arg"
  else
    if [ -n "$DIST" ] && [ "$DIST" = true ]; then
      echo "$ARGS \"$1\""
    else
      echo "$ARGS'$1',"
    fi
  fi
}

add_arg() {
  if [ -z "$1" ] || [ -z "$2" ]; then
    >&2 echo "Internal script error: missing parameter to add_arg"
  else
    if [ -n "$DIST" ] && [ "$DIST" = true ]; then
      echo "$ARGS $1 \"$2\""
    else
      echo "$ARGS'$1','$2',"
    fi
  fi
}

while getopts $OPTIONS arg; do
  case $arg in
    h)
      usage
      exit 0
      ;;
    G)
      GRADLE_ARGS="$OPTARG"
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
# Add remaining arguments to ARGS to forward to application
for arg in "$@"; do
  ARGS=$(add_single_arg "$arg")
done

echo "Running gradle with '$GRADLE_ARGS'"
echo "Running with args [$ARGS]"
./gradlew run $GRADLE_ARGS -PappArgs="[$ARGS]" -PmainClass="$MAIN_CLASS"
