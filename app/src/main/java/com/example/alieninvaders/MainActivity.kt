package com.example.alieninvaders

import android.content.Context
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
        private var score = 0
        private var isFirstWave = true // Flag to skip the first wave
        private var waveCount = 0 // Track the number of waves spawned


        init {
            holder.addCallback(this)
            fixedRateTimer("gameLoop", initialDelay = 0, period = 16) {
                if (running) updateGame()
            }
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            Thread {
                while (running) {
                    val canvas = holder.lockCanvas()
                    if (canvas != null) {
                        synchronized(holder) {
                            drawGame(canvas)
                        }
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            }.start()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            running = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event?.action == MotionEvent.ACTION_DOWN) {
                player.x = event.x
                synchronized(projectiles) {
                    projectiles.add(Projectile(player.x, height.toFloat() - 200))
                }
            }
            return true
        }

        private fun resetGame() {
            synchronized(enemies) { enemies.clear() }
            synchronized(projectiles) { projectiles.clear() }
            score = 0
            isFirstWave = true // Reset the flag for the next game
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

                // Increment wave count after successfully spawning enemies
                waveCount += 1
            }
        }

        private fun updateGame() {
            synchronized(enemies) {
                synchronized(projectiles) {
                    // Move projectiles
                    projectiles.forEach { it.move() }

                    // Move enemies
                    enemies.forEach { it.move() }

                    // Remove off-screen projectiles
                    projectiles.removeAll { it.y < 0 }

                    // Automatically clear the first wave
                    if (waveCount == 1) {
                        enemies.clear() // Clear the first wave
                        waveCount += 1 // Increment to avoid repeating
                        return
                    }

                    // Check collisions
                    val enemiesToRemove = mutableListOf<Enemy>()
                    enemies.forEach { enemy ->
                        if (projectiles.any { it.collidesWith(enemy) }) {
                            projectiles.removeIf { it.collidesWith(enemy) }
                            enemiesToRemove.add(enemy)
                            score += 1
                        }
                    }

                    enemies.removeAll(enemiesToRemove)

                    // Respawn enemies if all are destroyed
                    if (enemies.isEmpty()) spawnEnemies()
                }
            }
        }


        private fun drawGame(canvas: Canvas) {
            synchronized(enemies) {
                synchronized(projectiles) {
                    canvas.drawColor(Color.BLACK)

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

        inner class Player(var x: Float = width / 2f)

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
                    randomizeDirection()
                }
            }

            private fun randomizeDirection() {
                speedX += Random.nextFloat() * 4 - 2
                speedY += Random.nextFloat() * 4 - 2

                if (speedX < 3 && speedX > -3) speedX += if (speedX > 0) 2 else -2
                if (speedY < 3 && speedY > -3) speedY += if (speedY > 0) 2 else -2
            }
        }
    }
}
