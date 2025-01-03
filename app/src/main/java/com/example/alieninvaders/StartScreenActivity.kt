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
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                drawScreen(canvas)
                holder.unlockCanvasAndPost(canvas)
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event?.action == MotionEvent.ACTION_DOWN) {
                // Handle touch event (e.g., start the game)
                performClick() // Notify accessibility services
                onGameStart()
                return true
            }
            return super.onTouchEvent(event)
        }

        override fun performClick(): Boolean {
            super.performClick()
            // Additional logic, if any
            return true
        }

        private fun onGameStart() {
            // Transition to MainActivity
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }

        private fun drawScreen(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)

            // Draw the title
            paint.color = Color.WHITE
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ALIEN INVADERS", (width / 2).toFloat(), (height / 4).toFloat(), paint)

            // Draw the subtitle
            paint.textSize = 50f
            canvas.drawText("by addison stuart", (width / 2).toFloat(), (height / 4 + 70).toFloat(), paint)

            // Draw the tap-to-start text
            paint.textSize = 60f
            canvas.drawText("-tap here to start-", (width / 2).toFloat(), (3 * height / 4).toFloat(), paint)

            // Draw enemy blocks at the top, centered horizontally
            paint.color = Color.RED
            val numEnemies = 5
            val enemyWidth = 100
            val totalEnemyWidth = numEnemies * enemyWidth
            val startX = (width - totalEnemyWidth) / 2

            for (i in 0 until numEnemies) {
                val left = startX + i * enemyWidth
                canvas.drawRect(
                    left.toFloat(),
                    50f,
                    (left + enemyWidth).toFloat(),
                    150f,
                    paint
                )
            }

            // Draw player block at the bottom
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
