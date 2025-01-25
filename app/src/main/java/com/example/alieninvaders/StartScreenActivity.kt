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

        init {
            holder.addCallback(this)
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            // Ensure drawing the start screen on creation
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
                performClick() // Notify accessibility services
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
            // Transition to MainActivity when the screen is tapped
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }

        private fun drawScreen(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)

            // Draw the game title
            paint.color = Color.WHITE
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ALIEN INVADERS", (width / 2).toFloat(), (height / 4).toFloat(), paint)

            // Draw the subtitle
            paint.textSize = 50f
            canvas.drawText("by addison stuart", (width / 2).toFloat(), (height / 4 + 70).toFloat(), paint)

            // Draw "Tap to Start" text
            paint.textSize = 60f
            canvas.drawText("-tap here to start-", (width / 2).toFloat(), (3 * height / 4).toFloat(), paint)

            // Draw enemy blocks at the top
            paint.color = Color.RED
            val numEnemies = 5
            val enemyWidth = 100
            val spacing = 20 // Spacing between enemy blocks
            val totalWidth = numEnemies * (enemyWidth + spacing) - spacing
            val startX = (width - totalWidth) / 2

            for (i in 0 until numEnemies) {
                val left = startX + i * (enemyWidth + spacing)
                canvas.drawRect(
                    left.toFloat(),
                    50f,
                    (left + enemyWidth).toFloat(),
                    150f,
                    paint
                )
            }

            // Draw the player block at the bottom
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
