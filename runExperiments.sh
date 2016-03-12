#!/bin/bash

SCRIPT=$(basename $0)
OPTIONS=":hf:c:d:m:a:n:t:p:e:i:Io:DgG:"
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
OUT_EXT=.json
IGNORE_ERR=false

usage() {
# lim:  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
  echo "usage:"
  echo "$SCRIPT [options]"
  echo "options:"
  echo "  h                  show this usage info"
  echo "  o <file>           specify an output file name"
  echo "  i <name>           specify an instance name for the configuration"
  echo "  I                  ignore errors in results when performing multiple runs"
  echo "  n <num>            specify the number of experiment runs"
  echo "file options: (overwrite separate options)"
  echo "  f <file>           specify configuration file"
  echo "  c <config>         specify configuration string"
  echo "separate options:"
  echo "  d <domain>         specify the domain to run against"
  echo "  a <name>           specify the algorithm to run"
  echo "  m <file>           specify a map input file"
  echo "  t <type>           specify the termination type"
  echo "  p <param>          specify the termination parameter to provide"
  echo "  e <key(type)=val>  specify key/value pair for extra parameters"
  echo "distribution options:"
  echo "  D                  run the experiments via installed distribution"
  echo "  g                  run experiments using 'gradle run'"
  echo "  G <args>           run experiments using 'gradle run' with custom arguments"
  echo "Results will be placed in separate files with the following directory structure:"
  echo "  results/algorithm/domain/params/[instance]/out"
  echo "If a parameter is not given then the directory name will be '$DEFAULT_DIR'."
  echo "The output file will be appended with 2 numbers with format 'out_XX_YY',"
  echo "  where XX is a unique digit and YY is the run number of that set of runs."
}

add_dir() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    NEW_DIR=$(echo $1 | sed -e 's/[^A-Za-z0-9._-\*]/_/g')
    DIR="$DIR/$NEW_DIR"
    if [ ! -d "$DIR" ]; then
      mkdir -p "$DIR"
    fi
  fi
}

add_arg() {
  if [ -z "$1" ] || [ -z "$2" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
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
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
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
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    CURRENT=_00
    COUNTER=0
    while [ -f "$1$CURRENT$OUT_EXT" ] || [ -f "$1${CURRENT}_$RUN_NUM$OUT_EXT" ] || [ -f "$1${CURRENT}_0$RUN_NUM$OUT_EXT" ]; do
      let COUNTER+=1
      CURRENT=$(get_file_num $COUNTER)
    done
    echo "$1$CURRENT"
  fi
}

# results/algorithm/domain/params/instance/out
get_dirs_from_config() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    CONFIG=$(echo $CONFIG | sed -re 's/\\/\\\\/g')
SUB_DIRS=$(python <(cat <<EOF
import json
config = json.loads('$CONFIG')
algorithm = config['algorithmName']
domain = config['domainName']
termType = config['terminationCheckerType']
termParam = config['terminationCheckerParameter']
print algorithm, '/', domain, '/', termType, '-', termParam
EOF
))
    add_dir "$SUB_DIRS"
  fi
}

check_error() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    # Read output file to check for errors
ERR=$(python <(cat <<EOF
import json
try:
  msg = json.loads(open('$NEW_OUT', 'r').read())['errorMessage']
  if msg != None:
    print msg
except IOError as e:
  print "I/O error({0}): {1}".format(e.errno, e.strerror)
EOF
))
    if [ -n "$ERR" ]; then
      echo "Detected error in file '$NEW_OUT': $ERR"
      if [ "$IGNORE_ERR" = false ]; then
        echo "Terminating..."
        exit 1
      fi
    fi
  fi
}

run_gradle() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    $GRADLE run $GRADLE_PARAMS -PappArgs="[$(add_arg "-o" "$1")]"
  fi
}

run_dest() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    eval $RUN_SCRIPT $(add_arg "-o" "$1")
  fi
}

run() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    EXP_SCRIPT="$1"
    if [ "$NUM_RUNS" -eq 1 ]; then
      NEW_OUT="$OUT_FILE$OUT_EXT"
      $EXP_SCRIPT "$NEW_OUT"
      check_error "$NEW_OUT"
    else
      for ((i=0; i < $NUM_RUNS; i++)); do
        RUN_NUM=$i
        NEW_OUT="$OUT_FILE$(get_file_num $i)$OUT_EXT"
        $EXP_SCRIPT "$NEW_OUT"
        check_error "$NEW_OUT"
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
    f)
      FILE_CONFIG="$OPTARG"
      ;;
    c)
      FILE_CONFIG="$OPTARG"
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
      EXPERIMENT_ARGS=$(add_arg "-m" "$MAP")
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
    e)
      EXPERIMENT_ARGS=$(add_arg "-e" "$OPTARG")
      ;;
    i)
      INSTANCE_NAME=$OPTARG
      ;;
    I)
      IGNORE_ERR=true
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

if [ -n "$FILE_CONFIG" ]; then
  EXPERIMENT_ARGS=""
  if [ -f "$FILE_CONFIG" ]; then
    CONFIG="$(cat $FILE_CONFIG)"
  else
    CONFIG="$FILE_CONFIG"
  fi
  get_dirs_from_config "$CONFIG"
  EXPERIMENT_ARGS=$(add_arg "-c" "$CONFIG")
else
  # Translate to experiment expected args and build directory structure
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
fi

if [ -n "$INSTANCE_NAME" ]; then
  add_dir "$INSTANCE_NAME"
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
