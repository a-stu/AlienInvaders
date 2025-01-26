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
        private var running = true
        private var gameOver = false
        private var score = 0
        private var waveCount = 0
        private var lastShotTime = 0L
        private var gameLoopThread: Thread? = null
        private var gameLoopTimer = fixedRateTimer("gameLoop", initialDelay = 0, period = 16) {}

        init {
            holder.addCallback(this)
            startGameLoop()
        }

        private fun startGameLoop() {
            gameLoopTimer.cancel()
            gameLoopTimer = fixedRateTimer("gameLoop", initialDelay = 0, period = 16) {
                if (running) updateGame()
            }
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            player.x = width / 2f
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
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event?.action == MotionEvent.ACTION_DOWN) {
                if (gameOver) {
                    running = false
                    gameLoopTimer.cancel()
                    val intent = Intent(context, StartScreenActivity::class.java)
                    context.startActivity(intent)
                    (context as MainActivity).finish()
                } else {
                    player.x = event.x
                    val currentTime = System.currentTimeMillis()

                    // Firing Speed: Check if enough time (500ms) has passed since the last shot
                    if (currentTime - lastShotTime >= 500) {
                        synchronized(projectiles) {
                            projectiles.add(Projectile(player.x, height.toFloat() - 200))
                        }
                        lastShotTime = currentTime // Update the last shot time
                    }
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
                val numEnemies = 5
                val enemyWidth = 100f
                val availableSpace = width - (numEnemies * enemyWidth)
                val spacing = availableSpace / (numEnemies - 1)

                for (i in 0 until numEnemies) {
                    val centerX = spacing * i + enemyWidth / 2 + i * enemyWidth
                    val y = Random.nextFloat() * (height / 3 - 150) + 75
                    val enemy = Enemy(centerX, y, 50f, width - 50f)
                    enemies.add(enemy)
                }

                waveCount += 1
            }
        }

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
                        }
                        if (enemy.collidesWithPlayer(player)) {
                            gameOver = true
                        }
                    }

                    enemies.removeAll(enemiesToRemove)
                    if (enemies.isEmpty() && !gameOver) spawnEnemies()
                }
            }
        }

        private fun drawGame(canvas: Canvas) {
            synchronized(enemies) {
                synchronized(projectiles) {
                    canvas.drawColor(Color.BLACK)

                    if (gameOver) {
                        // Draw "GAME OVER" text
                        paint.color = Color.RED
                        paint.textSize = 100f
                        paint.typeface = Typeface.DEFAULT_BOLD
                        val gameOverText = "GAME OVER"
                        val gameOverWidth = paint.measureText(gameOverText)
                        canvas.drawText(gameOverText, (width - gameOverWidth) / 2, height / 2f, paint)

                        // Draw "Score: X" text
                        paint.color = Color.YELLOW
                        paint.textSize = 50f
                        val scoreText = "Score: $score"
                        val scoreWidth = paint.measureText(scoreText)
                        canvas.drawText(scoreText, (width - scoreWidth) / 2, height / 2f + 100, paint)

                        // Draw "-tap here to reset-" text
                        paint.color = Color.WHITE
                        val resetText = "-tap here to reset-"
                        val resetWidth = paint.measureText(resetText)
                        canvas.drawText(resetText, (width - resetWidth) / 2, height / 2f + 200, paint)

                        return
                    }

                    // Normal game drawing
                    paint.color = Color.GREEN
                    canvas.drawRect(
                        player.x - 50,
                        height - 150f,
                        player.x + 50,
                        height - 100f,
                        paint
                    )

                    paint.color = Color.YELLOW
                    projectiles.forEach {
                        canvas.drawRect(it.x - 10, it.y - 20, it.x + 10, it.y, paint)
                    }

                    paint.color = Color.RED
                    enemies.forEach {
                        canvas.drawRect(it.x - 50, it.y - 50, it.x + 50, it.y + 50, paint)
                    }

                    paint.color = Color.WHITE
                    paint.textSize = 60f
                    paint.typeface = Typeface.MONOSPACE
                    canvas.drawText("Score: $score", 20f, 70f, paint)
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
    }
}
