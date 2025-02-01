package com.example.alieninvaders

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity


class StartScreenActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(StartScreenView(this))

        mediaPlayer = MediaPlayer.create(this, R.raw.start_screen_music_loop)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    inner class StartScreenView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

        private val paint = Paint()
        private var highScore = 0
        private var highScoreDate = ""
        private var lastScore = 0
        private var lastScoreDate = ""
        private val titleSplash = BitmapFactory.decodeResource(resources, R.drawable.title_splash)
        private val starsBackground = BitmapFactory.decodeResource(resources, R.drawable.stars__very_dark) // Load background


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
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            // Clear wasMinimized flag when starting a new game
            val sharedPreferences = getSharedPreferences("AlienInvadersPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("wasMinimized", false).apply()

            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
            (context as StartScreenActivity).finish()
        }


        private fun drawScreen(canvas: Canvas) {

            // Draw the stars background - NO SCALING
            if (starsBackground != null) {
                canvas.drawBitmap(starsBackground, 0f, 0f, null) // Draw at top-left corner
            } else {
                canvas.drawColor(Color.BLACK) // Fallback
            }

            // Draw the title splash image (scaled by 50%)
            if (titleSplash != null) {
                val scaleFactor = 0.45f // Scale the image by 50%
                val scaledWidth = titleSplash.width * scaleFactor
                val scaledHeight = titleSplash.height * scaleFactor

                val centerX = width / 2f
                val centerY = height / 4f
                val imageX = centerX - scaledWidth / 2f
                val imageY = centerY - scaledHeight / 2f

                val matrix = Matrix()
                matrix.setScale(scaleFactor, scaleFactor)

                val scaledBitmap = Bitmap.createBitmap(titleSplash, 0, 0, titleSplash.width, titleSplash.height, matrix, true)

                canvas.drawBitmap(scaledBitmap, imageX, imageY, null)


            }

            // Subtitle - Moved Closer
            paint.color = Color.WHITE
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER

            // Calculate the position dynamically, adjust the - 20f value for spacing
            val subtitleY = height / 3.5f + titleSplash.height * 0.45f / 2f + 20f  // Dynamic position
            canvas.drawText("by addison stuart", width / 2f, subtitleY, paint)

            // Scores (Dynamically positioned)
            paint.textSize = 40f
            var nextLineY = subtitleY + 200f // Start below the subtitle (adjust spacing as needed)

            if (highScore > 0 && highScoreDate != "N/A") {
                canvas.drawText("Top Score: $highScore ($highScoreDate)", width / 2f, nextLineY, paint)
                nextLineY += 50f // Increment for the next line
            }

            if (lastScore > 0 && lastScoreDate != "N/A") {
                canvas.drawText("Last Score: $lastScore ($lastScoreDate)", width / 2f, nextLineY, paint)
                nextLineY += 50f // Increment for the next line if you have more to add
            }

            paint.textSize = 60f
            canvas.drawText("-tap here to start-", (width / 2).toFloat(), (3 * height / 4).toFloat(), paint)

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