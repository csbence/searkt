#!/bin/bash

PLOTS_DIR=plots
DURATIONS=(20000000 40000000 80000000 160000000 320000000)
ALGORITHMS=""

add_algorithm() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    ALGORITHMS="$ALGORITHMS -a $1"
  fi
}

add_algorithm "LSS_LRTA_STAR"
add_algorithm "RTA_STAR"
add_algorithm "DYNAMIC_F_HAT"
add_algorithm "A_STAR"
add_algorithm "ARA_STAR"

make_graphs() {
  if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    DOMAIN="$1"
    INSTANCE="$2"
    FILE_HEADER="$3"
    MD=""

    if [ "$DOMAIN" == "SLIDING_TILE_PUZZLE_4" ]; then
      if [ "$FILE_HEADER" == "tiles_33" ]; then
        return
      fi
    fi

    FILE="${FILE_HEADER}_error.pdf"
    rtsMongoClient.py $ALGORITHMS -d "$DOMAIN" -i "$INSTANCE" -t "gatPerDuration" -s "$PLOTS_DIR/$FILE" -q $@
    MD="${MD}![$FILE_HEADER]($FILE)\n\n\\\\clearpage\n\n"

    for duration in ${DURATIONS[@]}; do
      # Skip instances with no results
      if [ "$DOMAIN" == "ACROBOT" ]; then
        if [ $duration -le 40000000 ]; then
          continue
        fi
        if [ $duration -le 80000000 ]; then
          if [ "$INSTANCE" == "0.07-0.07" ] || [ "$INSTANCE" == "0.09-0.09" ]; then
            continue
          fi
        fi
      fi

      FILE="${FILE_HEADER}_${duration}.pdf"
      rtsMongoClient.py $ALGORITHMS -d "$DOMAIN" -i "$INSTANCE" -c "$duration" -t "gatBoxPlot" -s "$PLOTS_DIR/$FILE" -q $@
      MD="${MD}![$FILE_HEADER - $duration]($FILE)\n\n\\\\clearpage\n\n"

      FILE="${FILE_HEADER}_${duration}_bars.pdf"
      rtsMongoClient.py $ALGORITHMS -d "$DOMAIN" -i "$INSTANCE" -c "$duration" -t "gatBars"    -s "$PLOTS_DIR/$FILE" -q $@
      MD="${MD}![$FILE_HEADER - $duration]($FILE)\n\n\\\\clearpage\n\n"

      FILE="${FILE_HEADER}_${duration}_violin.pdf"
      rtsMongoClient.py $ALGORITHMS -d "$DOMAIN" -i "$INSTANCE" -c "$duration" -t "gatViolin"  -s "$PLOTS_DIR/$FILE" -q $@
      MD="${MD}![$FILE_HEADER - $duration]($FILE)\n\n\\\\clearpage\n\n"
    done

    echo "$MD"
  fi
}

if [ ! -d "$PLOTS_DIR" ]; then
  mkdir "$PLOTS_DIR"
fi

# Grid World
GRID_WORLD_MD=""
GRID_WORLD_MD="${GRID_WORLD_MD}$(make_graphs "GRID_WORLD" "input/vacuum/dylan/slalom.vw" "dylan_slalom")"
GRID_WORLD_MD="${GRID_WORLD_MD}$(make_graphs "GRID_WORLD" "input/vacuum/dylan/uniform.vw" "dylan_uniform")"
GRID_WORLD_MD="${GRID_WORLD_MD}$(make_graphs "GRID_WORLD" "input/vacuum/dylan/cups.vw" "dylan_cups")"
GRID_WORLD_MD="${GRID_WORLD_MD}$(make_graphs "GRID_WORLD" "input/vacuum/dylan/wall.vw" "dylan_wall")"


# Sliding Tile Puzzle
PUZZLE_MD=""
for ((i=1; i <= 100; i++)); do
  PUZZLE_MD="${PUZZLE_MD}$(make_graphs "SLIDING_TILE_PUZZLE_4" "input/tiles/korf/4/all/$i" "tiles_${i}")"
done

# Point Robot
PR_MD=""
PR_MD="${PR_MD}$(make_graphs "POINT_ROBOT" "input/pointrobot/dylan/slalom.pr" "pr_dylan_slalom")"
PR_MD="${PR_MD}$(make_graphs "POINT_ROBOT" "input/pointrobot/dylan/uniform.pr" "pr_dylan_uniform")"
PR_MD="${PR_MD}$(make_graphs "POINT_ROBOT" "input/pointrobot/dylan/cups.pr" "pr_dylan_cups")"
PR_MD="${PR_MD}$(make_graphs "POINT_ROBOT" "input/pointrobot/dylan/wall.pr" "pr_dylan_wall")"

# Point Robot with Inertia
PRWI_MD=""
PRWI_MD="${PRWI_MD}$(make_graphs "POINT_ROBOT_WITH_INERTIA" "input/pointrobot/dylan/slalom.pr" "prwi_dylan_slalom")"
PRWI_MD="${PRWI_MD}$(make_graphs "POINT_ROBOT_WITH_INERTIA" "input/pointrobot/dylan/uniform.pr" "prwi_dylan_uniform")"
PRWI_MD="${PRWI_MD}$(make_graphs "POINT_ROBOT_WITH_INERTIA" "input/pointrobot/dylan/cups.pr" "prwi_dylan_cups")"
PRWI_MD="${PRWI_MD}$(make_graphs "POINT_ROBOT_WITH_INERTIA" "input/pointrobot/dylan/wall.pr" "prwi_dylan_wall")"

# Racetrack
RACETRACK_MD=""
RACETRACK_MD="${PRWI_MD}$(make_graphs "RACETRACK" "input/racetrack/barto-big.track" "rt_big")"
RACETRACK_MD="${PRWI_MD}$(make_graphs "RACETRACK" "input/racetrack/barto-small.track" "rt_small")"

# Acrobot
ACROBOT_MD=""
for i in 0.3 0.1 0.09 0.08 0.07; do
  echo "ACROBOT" "$i-$i" "acrobot_${i}-${i}"
  ACROBOT_MD="${ACROBOT_MD}$(make_graphs "ACROBOT" "$i-$i" "acrobot_${i}-${i}")"
done

# Write to Markdown files
echo -e "$GRID_WORLD_MD" > $PLOTS_DIR/grid_world_plots.md
echo -e "$PUZZLE_MD" > $PLOTS_DIR/sliding_tile_puzzle_plots.md
echo -e "$PR_MD" > $PLOTS_DIR/point_robot_plots.md
echo -e "$PRWI_MD" > $PLOTS_DIR/point_robot_with_inertia_plots.md
echo -e "$ACROBOT_MD" > $PLOTS_DIR/acrobot_plots.md
echo -e "$RACETRACK_MD" > $PLOTS_DIR/racetrack_plots.md

# Convert Markdown files to pdf
if command -v "pandoc" 2>/dev/null; then
  pushd "$PLOTS_DIR"
  pandoc -o grid_world_plots.pdf grid_world_plots.md
  pandoc -o sliding_tile_puzzle_plots.pdf sliding_tile_puzzle_plots.md
  pandoc -o point_robot_plots.pdf point_robot_plots.md
  pandoc -o point_robot_with_inertia_plots.pdf point_robot_with_inertia_plots.md
  pandoc -o acrobot_plots.pdf acrobot_plots.md
  pandoc -o racetrack_plots.pdf racetrack_plots.md
  popd
fi