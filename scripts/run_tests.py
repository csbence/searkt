import sys
import time
import datetime
from subprocess import Popen


if __name__ == '__main__':
    racetracks = ["input/racetrack/hansen-bigger-quad.track", "input/racetrack/barto-big.track", "input/racetrack/uniform.track", "input/racetrack/barto-small.track"]
    # durations = [50, 100, 150, 200, 250, 400, 800, 1600, 3200, 6400, 12800]
    durations = [50]
    log = open("run_log.txt", "w")

    for seed_num in range(1):
            for track in racetracks:
                for duration in durations:
                    p = Popen(["./gradlew", "run", "-PappArgs=['{}','{}','{}']".format(seed_num, track, duration)])
                    p.communicate()
                    ts = time.time()
                    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
                    log.write("({}, {}, {}): {}\n".format(seed_num, track, duration, st))

    log.close()
