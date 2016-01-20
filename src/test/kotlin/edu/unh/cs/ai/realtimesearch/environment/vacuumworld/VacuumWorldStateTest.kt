
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import org.junit.Test
import kotlin.test.assertTrue

class VacuumWorldStateTest {

    @Test
    fun locationAdditionTest()  {
        val l1 = Location(3, 5)
        val l2 = Location(0, 5)
        val l3 = Location(-1, 3)
        val l4 = Location(3, 10)
        val l5 = Location(100, 234522)

        assertTrue(true)
        assertTrue(l1 + l2 == Location(l1.x + l2.x, l1.y+l2.y))
        assertTrue(l2 + l2 == Location(l2.x + l2.x, l2.y+l2.y))
        assertTrue(l1 + l3 == Location(l1.x + l3.x, l1.y+l3.y))
        assertTrue(l4 + l5 == Location(l4.x + l5.x, l4.y+l5.y))
        assertTrue(l3 + l2 == Location(l3.x + l2.x, l3.y+l2.y))
        assertTrue(l3 + l1 == Location(l3.x + l1.x, l3.y+l1.y))
        assertTrue(l5 + l2 == Location(l5.x + l2.x, l5.y+l2.y))
        assertTrue(l2 + l5 == Location(l2.x + l5.x, l2.y+l5.y))
    }

}