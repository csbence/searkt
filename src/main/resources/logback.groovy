import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender

// define the USER_HOME variable setting its value
// to that of the "user.home" system property
def HOSTNAME=hostname
def USER_HOME = System.getProperty("user.home")
appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %5level %logger{0} - %msg%n"
    }
}

appender("FILE", FileAppender) {
    // make use of the USER_HOME variable
    println "Setting [file] property to [/home/aifs2/group/data/real_time_search_log/rts_client_${hostname}.log]"
    file =  "/home/aifs2/group/data/real_time_search_log/rts_client_${HOSTNAME}.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %5level %logger{0} - %msg%n"
    }
}

root(INFO, ["STDOUT"])

