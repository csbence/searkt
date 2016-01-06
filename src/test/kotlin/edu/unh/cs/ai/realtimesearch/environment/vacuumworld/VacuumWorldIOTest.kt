package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import org.junit.Test
import java.io.File
import java.io.FileInputStream

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class VacuumWorldIOTest {

    @Test
    fun testParserFromStream() {
        val file = File("input/vacuum/cups.vw")
        val vacuumWorldInstance = VacuumWorldIO.parserFromStream(FileInputStream(file))





    }
}