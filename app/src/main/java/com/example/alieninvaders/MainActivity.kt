package com.example.alieninvaders

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random
import android.media.SoundPool
import android.media.AudioAttributes
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.app.ActivityManager



class MainActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(GameView(this))
    }

    inner class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
        private val player = Player()
        private val projectiles = mutableListOf<Projectile>()
        private val enemies = mutableListOf<Enemy>()
        private val paint = Paint()
        private val originalPlayerBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.raw.player_ship)
        private val playerBitmap: Bitmap = Bitmap.createScaledBitmap(originalPlayerBitmap,
            originalPlayerBitmap.width / 30, originalPlayerBitmap.height / 30, true)
        private val originalEnemyBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.raw.enemy_ship)
        private val enemyBitmap: Bitmap = Bitmap.createScaledBitmap(originalEnemyBitmap,
            originalEnemyBitmap.width / 30, originalEnemyBitmap.height / 30, true)

        // Define tutorial image and scaling
        private val tutorialBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.tutorial)
        private val TUTORIAL_SCALE = 0.4f // 50% of original size

        // Scale the tutorial image based on the factor
        private val scaledTutorialBitmap: Bitmap = Bitmap.createScaledBitmap(
            tutorialBitmap,
            (tutorialBitmap.width * TUTORIAL_SCALE).toInt(),
            (tutorialBitmap.height * TUTORIAL_SCALE).toInt(),
            true
        )
        private var resetButtonRect = android.graphics.RectF()
        private var running = true
        private var gameOver = false
        private var score = 0
        private var waveCount = 0
        private var lastShotTime = 0L
        private var gameLoopThread: Thread? = null
        private var isPaused = false
        private var gameLoopTimer = fixedRateTimer("gameLoop", initialDelay = 0, period = 16) {}
        private var wasRunning = false // Track game state before background
        private val gameOverSoundId: Int
        private val soundPool: SoundPool
        private val enemyDestroyedSoundId: Int
        private val playerShotSoundIds = IntArray(10) // Array to hold player shot sound IDs
        private val starsBackground = BitmapFactory.decodeResource(resources, R.drawable.stars__very_dark)
        private val explosions = mutableListOf<Explosion>()
        private val explosionBitmaps = listOf(
            BitmapFactory.decodeResource(resources, R.raw.explosion),
            BitmapFactory.decodeResource(resources, R.raw.explosion_1),
            BitmapFactory.decodeResource(resources, R.raw.explosion_2)
        )

        private var tutorialActive = false
        private var initialPlayerX = -1f
        private var tutorialTimer: Thread? = null



    init {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(10) // Adjust based on how many simultaneous sounds you'd like
                .setAudioAttributes(audioAttributes)
                .build()

            // Load sound files
            enemyDestroyedSoundId = soundPool.load(context, R.raw.enemy_destroyed_sound, 1)
            gameOverSoundId = soundPool.load(context, R.raw.game_over_sound, 1) // Load the game over sound

            for (i in 1..10) {
                val resId = context.resources.getIdentifier("player_shot_$i", "raw", context.packageName)
                playerShotSoundIds[i - 1] = soundPool.load(context, resId, 1)
            }

            holder.addCallback(this)
            startGameLoop()
        }


        private fun saveScores() {
            val sharedPreferences = getSharedPreferences("AlienInvadersPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            val highScore = sharedPreferences.getInt("highScore", 0)
            if (score > highScore) {
                editor.putInt("highScore", score)
                editor.putString("highScoreDate", getCurrentDate())
            }

            editor.putInt("lastScore", score)
            editor.putString("lastScoreDate", getCurrentDate())
            editor.apply()
        }

        private fun getCurrentDate(): String {
            val current = java.util.Calendar.getInstance()
            return "${current.get(java.util.Calendar.MONTH) + 1}/${current.get(java.util.Calendar.DAY_OF_MONTH)}/${current.get(java.util.Calendar.YEAR)}"
        }
        private fun startGameLoop() {
            gameLoopTimer.cancel()
            gameLoopTimer = fixedRateTimer("gameLoop", initialDelay = 0, period = 16) {
                if (running) updateGame()
            }
        }

        private fun isAppReallyInBackground(): Boolean {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return true

            val packageName = packageName
            for (process in appProcesses) {
                if (process.processName == packageName) {
                    return process.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                }
            }
            return true
        }


        override fun surfaceCreated(holder: SurfaceHolder) {
            player.x = width / 2f // Ensure the player starts centered
            initialPlayerX = player.x // Save starting position

            // Start a delayed task to check for movement after 4 seconds
            postDelayed({
                if (Math.abs(player.x - initialPlayerX) < playerBitmap.width) {
                    tutorialActive = true // Show tutorial if not moved enough
                }
            }, 4000) // Delay of 4 seconds

            gameLoopThread = Thread {
                while (running) {
                    val canvas = holder.lockCanvas()
                    if (canvas != null) {
                        synchronized(holder) {
                            drawGame(canvas)
                        }
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            }.apply { start() }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            running = false
            gameLoopTimer.cancel()
            soundPool.release() // Release SoundPool resources
        }



        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event?.action == MotionEvent.ACTION_DOWN) {
                val touchX = event.x
                val touchY = event.y

                // Check if the pause/play button was tapped
                val buttonXStart = width - 120f
                val buttonXEnd = width - 20f
                val buttonYStart = 20f
                val buttonYEnd = 120f

                player.x = touchX.coerceIn(playerBitmap.width / 2f, width - playerBitmap.width / 2f)

// Check if player moved at least one ship width
                if (tutorialActive && Math.abs(player.x - initialPlayerX) >= playerBitmap.width) {
                    tutorialActive = false // Hide tutorial
                }


                if (touchX in buttonXStart..buttonXEnd && touchY in buttonYStart..buttonYEnd) {
                    isPaused = !isPaused
                    if (isPaused) {
                        gameLoopTimer.cancel() // Pause the game loop
                    } else {
                        startGameLoop() // Resume the game loop
                    }
                    return true // Exit touch handling here
                }

                if (gameOver) {
                    if (resetButtonRect.contains(touchX, touchY)) {
                        running = false
                        gameLoopTimer.cancel()
                        val intent = Intent(context, StartScreenActivity::class.java)
                        context.startActivity(intent)
                        (context as MainActivity).finish()
                    }
                    return true
                }

                // **Prevent shooting if the game is paused**
                if (isPaused) {
                    return true // Do nothing if paused
                }

                    player.x = touchX.coerceIn(0f + playerBitmap.width/2, width.toFloat() - playerBitmap.width/2) // Update player's x position *immediately*

                    val currentTime = System.currentTimeMillis()
                    // Firing Speed: Check if enough time (400ms) has passed since the last shot
                    if (currentTime - lastShotTime >= 400) {
                        synchronized(projectiles) {
                            projectiles.add(Projectile(player.x, height.toFloat() - 200))
                        }
                        lastShotTime = currentTime // Update the last shot time

                        // Play a random player shot sound
                        val randomSoundIndex = Random.nextInt(10) // Random number between 0 and 9
                        soundPool.play(playerShotSoundIds[randomSoundIndex], 1f, 1f, 0, 0, 1f)
                    }
                }
            return true
        }

        private fun resetGame() {
            synchronized(enemies) { enemies.clear() }
            synchronized(projectiles) { projectiles.clear() }
            score = 0
            gameOver = false
            player.x = width / 2f
        }

        private fun spawnEnemies() {
            synchronized(enemies) {
                enemies.clear()
                val numEnemies = 5 + (waveCount - 1) // Wave 1 starts with 5 enemies
                val enemyWidth = 100f
                val availableSpace = width - (numEnemies * enemyWidth)
                val spacing = availableSpace / (numEnemies - 1)

                for (i in 0 until numEnemies) {
                    val centerX = spacing * i + enemyWidth / 2 + i * enemyWidth
                    val y = Random.nextFloat() * (height / 3 - 150) + 75
                    val enemy = Enemy(centerX, y, 50f, width - 50f)
                    enemies.add(enemy)
                }

                waveCount += 1 // Increment wave count
                increaseEnemySpeed() // Apply the speed increase to the new wave
            }
        }



        private var lastScoreCheckpoint = 0 // Tracks the last score checkpoint for speed increases

        private fun updateGame() {
            if (gameOver) return

            synchronized(enemies) {
                synchronized(projectiles) {
                    projectiles.forEach { it.move() }
                    enemies.forEach { it.move() }
                    projectiles.removeAll { it.y < 0 }

                    if (waveCount == 1) {
                        enemies.clear()
                        waveCount += 1
                        return
                    }

                    val enemiesToRemove = mutableListOf<Enemy>()
                    enemies.forEach { enemy ->
                        if (projectiles.any { it.collidesWith(enemy) }) {
                            projectiles.removeIf { it.collidesWith(enemy) }
                            enemiesToRemove.add(enemy)
                            score += 1

                            // Spawn explosion at enemy's position
                            explosions.add(Explosion(enemy.x, enemy.y))

                            // Play the enemy destroyed sound
                            soundPool.play(enemyDestroyedSoundId, 1f, 1f, 0, 0, 1f)

                            // Increase enemy speed every 5 points
                            if (score % 5 == 0 && score > lastScoreCheckpoint) {
                                lastScoreCheckpoint = score
                                increaseEnemySpeed()
                            }
                        }

                        // Check for collision with the player
                        val playerLeft = player.x - 50
                        val playerRight = player.x + 50
                        val playerTop = height - 150f
                        val playerBottom = height - 100f

                        val enemyLeft = enemy.x - 50
                        val enemyRight = enemy.x + 50
                        val enemyTop = enemy.y - 50
                        val enemyBottom = enemy.y + 50

                        if (playerRight > enemyLeft && playerLeft < enemyRight &&
                            playerBottom > enemyTop && playerTop < enemyBottom
                        ) {
                            gameOver = true
                            soundPool.play(gameOverSoundId, 1f, 1f, 0, 0, 1f) // Play the game over sound
                        }
                    }

                    // Handle collisions between enemies
                    for (i in 0 until enemies.size) {
                        for (j in i + 1 until enemies.size) {
                            enemies[i].handleCollisionWith(enemies[j])
                        }
                    }

                    enemies.removeAll(enemiesToRemove)
                    if (enemies.isEmpty() && !gameOver) spawnEnemies()

                    if (gameOver) {
                        saveScores() // Save scores and timestamps when the game ends
                        return
                    }
                }
            }
        }

        // Increases the speed of all enemies by 10%
        private fun increaseEnemySpeed() {
            synchronized(enemies) {
                enemies.forEach { enemy ->
                    enemy.increaseSpeed(1.5f) // Pass the speed multiplier
                }
            }
        }



        private fun drawGame(canvas: Canvas) {

            // Draw the stars background - NO SCALING
            if (starsBackground != null) {
                canvas.drawBitmap(starsBackground, 0f, 0f, null) // Draw at top-left corner
            } else {
                canvas.drawColor(Color.BLACK) // Fallback
            }

            synchronized(enemies) {
                synchronized(projectiles) {

                    // Draw tutorial graphic if active

// Draw the scaled tutorial image
                    if (tutorialActive) {
                        val tutorialX = (player.x + width) / 2 - scaledTutorialBitmap.width / 2 - 400f
                        val tutorialY = height - 400f // Align with player
                        canvas.drawBitmap(scaledTutorialBitmap, tutorialX, tutorialY, null)
                    }



                    if (gameOver) {
                        // Load the game over splash image
                        val gameOverSplashBitmap = BitmapFactory.decodeResource(resources, R.drawable.game_over_splash);

                        // Define the scaling factor (e.g., 0.5 for 50%, 0.75 for 75%)
                        val scalingFactor = 0.45f // You can adjust this value as needed

                        // Calculate the scaled dimensions
                        val scaledWidth = gameOverSplashBitmap.width * scalingFactor;
                        val scaledHeight = gameOverSplashBitmap.height * scalingFactor;

                        // Create a scaled bitmap
                        val scaledBitmap = Bitmap.createScaledBitmap(gameOverSplashBitmap, scaledWidth.toInt(), scaledHeight.toInt(), true);

                        // Draw the scaled game over splash image centered on the screen
                        val canvasCenterX = width / 2f;
                        val canvasCenterY = height / 2f - 200
                        canvas.drawBitmap(scaledBitmap, canvasCenterX - scaledWidth / 2f, canvasCenterY - scaledHeight / 2f, null);


                        // Draw "Score: X" text
                        paint.color = Color.YELLOW
                        paint.textSize = 50f
                        val scoreText = "Score: $score"
                        val scoreWidth = paint.measureText(scoreText)
                        canvas.drawText(scoreText, (width - scoreWidth) / 2, height / 2f + 300, paint)

// Draw "-tap here to reset-" text with bounding box
                        paint.color = Color.WHITE
                        val resetText = "-tap here to reset-"
                        val resetWidth = paint.measureText(resetText)
                        val resetX = (width - resetWidth) / 2
                        val resetY = height / 2f + 400
                        canvas.drawText(resetText, resetX, resetY, paint)

// Define reset button bounding box
                        val resetPadding = 20f
                        resetButtonRect = android.graphics.RectF(
                            resetX - resetPadding,
                            resetY - paint.textSize - resetPadding,
                            resetX + resetWidth + resetPadding,
                            resetY + resetPadding
                        )

                        return
                    }

                    // Draw player
                    canvas.drawBitmap(playerBitmap, player.x - playerBitmap.width / 2, height - 150f, null)

                    paint.color = Color.YELLOW
                    projectiles.forEach {
                        canvas.drawRect(it.x - 10, it.y - 20, it.x + 10, it.y, paint)
                    }

                    // Draw enemies
                    enemies.forEach { enemy ->
                        canvas.drawBitmap(enemyBitmap, enemy.x - enemyBitmap.width / 2, enemy.y - enemyBitmap.height / 2, null)
                        // Draw and update explosions
                        val iterator = explosions.iterator()
                        while (iterator.hasNext()) {
                            val explosion = iterator.next()
                            explosion.draw(canvas)
                            if (explosion.update()) iterator.remove() // Remove explosion when done
                        }

                    }

                    // Draw the score
                    paint.color = Color.WHITE
                    paint.textSize = 60f
                    paint.typeface = Typeface.MONOSPACE
                    canvas.drawText("Score: $score", 20f, 70f, paint)

                    // Draw the pause/play button
                    val buttonPaint = Paint().apply { color = Color.WHITE }
                    val centerX = width - 70f
                    val centerY = 70f

                    if (isPaused) {
                        // Draw play symbol (triangle)
                        val path = android.graphics.Path().apply {
                            moveTo(centerX - 20f, centerY - 30f)
                            lineTo(centerX - 20f, centerY + 30f)
                            lineTo(centerX + 30f, centerY)
                            close()
                        }
                        canvas.drawPath(path, buttonPaint)
                    } else {
                        // Draw pause symbol (two bars)
                        canvas.drawRect(centerX - 30f, centerY - 30f, centerX - 10f, centerY + 30f, buttonPaint)
                        canvas.drawRect(centerX + 10f, centerY - 30f, centerX + 30f, centerY + 30f, buttonPaint)
                    }

                }
            }
        }



        inner class Player(var x: Float = 0f)

        inner class Projectile(var x: Float, var y: Float) {
            fun move() {
                y -= 20
            }

            fun collidesWith(enemy: Enemy): Boolean {
                return x > enemy.x - 50 && x < enemy.x + 50 && y > enemy.y - 50 && y < enemy.y + 50
            }
        }

        inner class Enemy(var x: Float, var y: Float, private val minX: Float, private val maxX: Float) {
            private var speedX = Random.nextFloat() * 10 - 5
            private var speedY = Random.nextFloat() * 10 - 5

            fun move() {
                x += speedX
                y += speedY

                if (x < minX) {
                    x = minX
                    speedX = -speedX
                } else if (x > maxX) {
                    x = maxX
                    speedX = -speedX
                }

                if (y < 50 || y > height - 150) {
                    speedY = -speedY
                }
            }

            fun increaseSpeed(multiplier: Float) {
                speedX *= multiplier
                speedY *= multiplier

                // Ensure speeds aren't too slow
                if (speedX in -1f..1f) speedX = if (speedX < 0) -1.1f else 3f
                if (speedY in -1f..1f) speedY = if (speedY < 0) -1.1f else 3f
            }

            fun handleCollisionWith(other: Enemy) {
                val dx = other.x - x
                val dy = other.y - y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                if (distance < 100) {
                    speedX = -speedX * 1.05f
                    speedY = -speedY * 1.05f
                    other.speedX = -other.speedX * 1.1f
                    other.speedY = -other.speedY * 1.1f

                    val overlap = 100 - distance
                    val angle = kotlin.math.atan2(dy, dx)
                    x -= overlap / 2 * kotlin.math.cos(angle)
                    y -= overlap / 2 * kotlin.math.sin(angle)
                    other.x += overlap / 2 * kotlin.math.cos(angle)
                    other.y += overlap / 2 * kotlin.math.sin(angle)
                }
            }
        }
        inner class Explosion(var x: Float, var y: Float) {
            private var startTime = System.currentTimeMillis()
            private var rotation = Random.nextInt(360) // Random rotation angle
            private var scale = 0.1f // Start small
            private var opacity = 255 // Fully visible

            // Randomly select an explosion bitmap from the preloaded ones
            private val explosionBitmap = explosionBitmaps[Random.nextInt(explosionBitmaps.size)]

            fun update(): Boolean {
                val elapsed = System.currentTimeMillis() - startTime

                return when {
                    elapsed < 300 -> { // Grow phase
                        scale = 0.1f + (elapsed / 300f) * 0.1f
                        opacity = 255
                        false
                    }
                    elapsed < 600 -> { // Shrink & fade phase
                        scale = 0.1f - ((elapsed - 300) / 300f) * 0.1f
                        opacity = (255 * (1 - (elapsed - 300) / 300f)).toInt()
                        false
                    }
                    else -> true // Remove after 600ms
                }
            }

            fun draw(canvas: Canvas) {
                val paint = Paint().apply { alpha = opacity }
                val explosionSize = explosionBitmap.width * scale

                val matrix = android.graphics.Matrix().apply {
                    postScale(scale, scale)
                    postRotate(rotation.toFloat(), explosionSize / 2, explosionSize / 2)
                    postTranslate(x - explosionSize / 2, y - explosionSize / 2)
                }

                canvas.drawBitmap(explosionBitmap, matrix, paint)
            }
        }



        fun collidesWithPlayer(player: Player): Boolean {
            val playerLeft = player.x - 50
            val playerRight = player.x + 50
            val playerTop = height - 150f
            val playerBottom = height - 100f

            val enemyLeft = x - 50
            val enemyRight = x + 50
            val enemyTop = y - 50
            val enemyBottom = y + 50

            return playerRight > enemyLeft && playerLeft < enemyRight &&
                    playerBottom > enemyTop && playerTop < enemyBottom
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if the activity was previously running before minimizing
        val sharedPreferences = getSharedPreferences("AlienInvadersPrefs", Context.MODE_PRIVATE)
        val wasMinimized = sharedPreferences.getBoolean("wasMinimized", false)

        if (wasMinimized) {
            // Reset the flag
            sharedPreferences.edit().putBoolean("wasMinimized", false).apply()

            // Redirect to StartScreenActivity
            val intent = Intent(this, StartScreenActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
    override fun onPause() {
        super.onPause()

        // Mark that the game was minimized
        val sharedPreferences = getSharedPreferences("AlienInvadersPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("wasMinimized", true).apply()
    }

}
