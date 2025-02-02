//import android.content.Context
//import com.example.alieninvaders.MainActivity.GameView
//import org.junit.Assert.assertFalse
//import org.junit.Before
//import org.junit.Test
//import org.mockito
//
//class EnemySpawnTest {
//
//    private lateinit var gameView: GameView
//
//    @Before
//    fun setUp() {
//        // Create a mock Context
//        val mockContext = mock(Context::class.java)
//
//        // Initialize GameView with mock Context
//        gameView = GameView(mockContext)
//    }
//
//    @Test
//    fun testWaveOneEnemyClustering() {
//        // Run enemy spawning logic
//        gameView.spawnEnemies()
//
//        // Count how many enemies have the same X position
//        val xPositions = gameView.enemies.groupBy { it.x }
//        val hasClustering = xPositions.values.any { it.size >= 4 }
//
//        // The test fails if 4+ enemies spawn at the same X position
//        assertFalse("Enemies are clustering too much!", hasClustering)
//    }
//}


// This test doesn't work. Keep it around, and maybe try again later.