package com.example.alieninvaders

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity

class StartScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(StartScreenView(this))
    }

    inner class StartScreenView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

        private val paint = Paint()
        private var highScore = 0
        private var highScoreDate = ""
        private var lastScore = 0
        private var lastScoreDate = ""

        init {
            holder.addCallback(this)
            loadScores()
        }

        private fun loadScores() {
            val sharedPreferences = context.getSharedPreferences("AlienInvadersPrefs", Context.MODE_PRIVATE)
            highScore = sharedPreferences.getInt("highScore", 0)
            highScoreDate = sharedPreferences.getString("highScoreDate", "N/A") ?: "N/A"
            lastScore = sharedPreferences.getInt("lastScore", 0)
            lastScoreDate = sharedPreferences.getString("lastScoreDate", "N/A") ?: "N/A"
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            Thread {
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        drawScreen(canvas)
                    }
                    holder.unlockCanvasAndPost(canvas)
                }
            }.start()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event?.action == MotionEvent.ACTION_DOWN) {
                performClick()
                onGameStart()
                return true
            }
            return super.onTouchEvent(event)
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        private fun onGameStart() {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }

        private fun drawScreen(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)

            // Game title
            paint.color = Color.WHITE
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ALIEN INVADERS", (width / 2).toFloat(), (height / 4).toFloat(), paint)

            // Subtitle
            paint.textSize = 50f
            canvas.drawText("by addison stuart", (width / 2).toFloat(), (height / 4 + 70).toFloat(), paint)

            // Display Top Score if valid
            var nextLineY = height / 2 - 50f // Start drawing scores
            paint.textSize = 40f
            if (highScore > 0 && highScoreDate != "N/A") {
                canvas.drawText(
                    "Top Score: $highScore ($highScoreDate)",
                    (width / 2).toFloat(),
                    nextLineY,
                    paint
                )
                nextLineY += 50f // Move to the next line if top score is drawn
            }

            // Display Last Score if valid
            if (lastScore > 0 && lastScoreDate != "N/A") {
                canvas.drawText(
                    "Last Score: $lastScore ($lastScoreDate)",
                    (width / 2).toFloat(),
                    nextLineY,
                    paint
                )
            }

            // "Tap to Start" text
            paint.textSize = 60f
            canvas.drawText("-tap here to start-", (width / 2).toFloat(), (3 * height / 4).toFloat(), paint)

            // Enemy blocks
            paint.color = Color.RED
            val numEnemies = 5
            val enemyWidth = 100
            val spacing = 20
            val totalWidth = numEnemies * (enemyWidth + spacing) - spacing
            val startX = (width - totalWidth) / 2
            for (i in 0 until numEnemies) {
                val left = startX + i * (enemyWidth + spacing)
                canvas.drawRect(left.toFloat(), 50f, (left + enemyWidth).toFloat(), 150f, paint)
            }

            // Player block
            paint.color = Color.GREEN
            val playerWidth = 200
            canvas.drawRect(
                (width / 2 - playerWidth / 2).toFloat(),
                (height - 150).toFloat(),
                (width / 2 + playerWidth / 2).toFloat(),
                (height - 100).toFloat(),
                paint
            )
        }
    }
}
