package edu.unh.cs.ai.realtimesearch.visualizer

import javafx.animation.Animation

/**
 * @author Mike Bogochow
 * @since 5/6/16
 */

fun delayPlay(animation: Animation, delayTime: Long) {
    Thread({
        Thread.sleep(delayTime)
        animation.play()
    }).start()
}
