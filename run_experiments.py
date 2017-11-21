import datetime
import sys
import time
from subprocess import check_output
from tqdm import tqdm

if __name__ == '__main__':

    racetracks = ["input/racetrack/barto-big.track",
                  "input/racetrack/uniform.track",
                  "input/racetrack/hansen-bigger-quad.track"]

    # these are the action durations used in the plots
    durations = [50, 100, 150, 200, 250, 400, 800, 1600, 3200, 6400, 12800]

    # This is just used to record whether or not a run was successful, but it's not really needed now
    log = open("run_log_{}.txt".format(sys.argv[1]), "w")

    # As the first argument of this script is the name of the planner we are testing.
    # The planners are: "A_STAR", "SAFE_RTS", "LSS_LRTA_STAR", "S_ZERO", "SIMPLE_SAFE":
    # So to run the configurations for A_STAR, you do python3 run_tests.py A_STAR
    # I tend to run each as a separate process
    planner = sys.argv[1]

    # If you are running this for the Traffic domain, uncomment the line below
    # track = ""
    progress = tqdm(total=len(durations) * len(racetracks))

    # for seed_num in range(0, 99):
    # If you are running this for the Traffic domain, uncomment the line below and deindent the code block
    for track in racetracks:
        for duration in durations:
            command = ["./gradlew", "run",
                       "-PappArgs=['{}','{}','{}']".format(track, duration, planner)]
            finished = 1
            try:
                p = check_output(command, timeout=900)
                print(p.decode())
            except Exception:
                finished = 0
            progress.update()
            ts = time.time()
            st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
            log.write("({}, {}, {}): {}, {}\n".format(track, planner, duration, st, finished))

    log.close()
