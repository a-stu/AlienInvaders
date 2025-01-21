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

// ... (rest of the MainActivity code remains the same)

        private fun spawnEnemies() {
            synchronized(enemies) {
                enemies.clear()
                val numEnemies = 5
                val spacing = (width - (numEnemies * 100f)) / (numEnemies - 1) // Calculate spacing considering enemy width

                for (i in 0 until numEnemies) {
                    enemies.add(Enemy(50f + i * spacing, 200f)) // Start from 50f to avoid immediate edge collision
                }
            }
        }

// ... (rest of the MainActivity code)

        private fun updateGame() {
            if (isFirstWave) {
                spawnEnemies()
                isFirstWave = false
            }

            synchronized(enemies) {
                synchronized(projectiles) {
                    // Move projectiles
                    projectiles.forEach { it.move() }

                    // Move enemies
                    enemies.forEach { it.move() }

                    // Remove off-screen projectiles
                    projectiles.removeAll { it.y < 0 }

                    // Check collisions
                    val enemiesToRemove = mutableListOf<Enemy>()
                    enemies.forEach { enemy ->
                        if (projectiles.any { it.collidesWith(enemy) }) {
                            projectiles.removeIf { it.collidesWith(enemy) }
                            enemiesToRemove.add(enemy)
                            score += 1 // Increment score
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

                    // Draw player
                    paint.color = Color.GREEN
                    canvas.drawRect(player.x - 50, height - 150f, player.x + 50, height - 100f, paint)

                    // Draw projectiles
                    paint.color = Color.YELLOW
                    projectiles.forEach {
                        canvas.drawRect(it.x - 10, it.y - 20, it.x + 10, it.y, paint)
                    }

                    // Draw enemies
                    paint.color = Color.RED
                    enemies.forEach {
                        canvas.drawRect(it.x - 50, it.y - 50, it.x + 50, it.y + 50, paint)
                    }

                    // Draw score
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

        inner class Enemy(var x: Float, var y: Float) {
            private var direction = 5

            fun move() {
                x += direction
                if (x < 50) direction = 5
                else if (x > width - 50) direction = -5
            }
        }
    }
}