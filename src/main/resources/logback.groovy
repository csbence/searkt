import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender

// define the USER_HOME variable setting its value
// to that of the "user.home" system property
def HOSTNAME = hostname

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %5level %logger{0} - %msg%n"
    }
}

appender("FILE", FileAppender) {
    // make use of the USER_HOME variable
    String logFile = "rts_client_${HOSTNAME}.log"
    String path = "/home/aifs2/group/data/real_time_search_log/$logFile"
//    println "Setting [file] property to [$path]"

    if (new File(path).exists()) {
        file = path
    } else {
//        println("File '$path' does not exist; using working directory")
        file = logFile
    }

    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %5level %logger{0} - %msg%n"
    }
}

root(INFO, ["STDOUT", "FILE"])

