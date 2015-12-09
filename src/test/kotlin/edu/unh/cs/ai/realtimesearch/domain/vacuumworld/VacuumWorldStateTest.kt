import edu.unh.cs.ai.realtimesearch.domain.vacuumworld.VacuumWorldState
import org.junit.Test

class VacuumWorldStateTest {

    @Test
    fun locationAdditionTest()  {
        val l1 = VacuumWorldState.Location(3, 5)
        val l2 = VacuumWorldState.Location(0, 5)
        val l3 = VacuumWorldState.Location(-1, 3)
        val l4 = VacuumWorldState.Location(3, 10)
        val l5 = VacuumWorldState.Location(100, 234522)

        assert(true)
        assert(l1 + l2 == VacuumWorldState.Location(l1.x + l2.x, l1.y+l2.y))
        assert(l2 + l2 == VacuumWorldState.Location(l2.x + l2.x, l2.y+l2.y))
        assert(l1 + l3 == VacuumWorldState.Location(l1.x + l3.x, l1.y+l3.y))
        assert(l4 + l5 == VacuumWorldState.Location(l4.x + l5.x, l4.y+l5.y))
        assert(l3 + l2 == VacuumWorldState.Location(l3.x + l2.x, l3.y+l2.y))
        assert(l3 + l1 == VacuumWorldState.Location(l3.x + l1.x, l3.y+l1.y))
        assert(l5 + l2 == VacuumWorldState.Location(l5.x + l2.x, l5.y+l2.y))
        assert(l2 + l5 == VacuumWorldState.Location(l2.x + l5.x, l2.y+l5.y))
    }

}