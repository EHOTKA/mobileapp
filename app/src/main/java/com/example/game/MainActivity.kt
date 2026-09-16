package com.example.game

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var startScreen: View
    private lateinit var shopScreen: View
    private lateinit var gameOverScreen: View
    private lateinit var pauseScreen: View
    private lateinit var statsPanel: View
    private lateinit var btnPause: ImageButton
    private lateinit var lifeNotification: View
    private lateinit var ivNotifIcon: ImageView
    private lateinit var tvNotifText: TextView
    private lateinit var speedUpNotif: TextView
    private lateinit var modifierContainer: View
    private lateinit var pbModifier: ProgressBar
    
    private lateinit var tvScore: TextView
    private lateinit var tvLives: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvCoins: TextView
    private lateinit var tvFinalScore: TextView
    private lateinit var tvHighScore: TextView
    private lateinit var tvTotalCoins: TextView
    private lateinit var tvShopCoins: TextView
    private lateinit var tvCombo: TextView
    private lateinit var btnBuyDouble: Button

    private var mediaPlayer: MediaPlayer? = null
    private var currentMusicTrack: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val musicTracks = listOf("sounds/fone_music.mp3", "sounds/fone_music-2.mp3", "sounds/fone_music-3.mp3")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI
        startScreen = findViewById(R.id.startScreen)
        shopScreen = findViewById(R.id.shopScreen)
        gameOverScreen = findViewById(R.id.gameOverScreen)
        pauseScreen = findViewById(R.id.pauseScreen)
        statsPanel = findViewById(R.id.statsPanel)
        btnPause = findViewById(R.id.btnPause)
        lifeNotification = findViewById(R.id.lifeNotification)
        ivNotifIcon = findViewById(R.id.ivNotifIcon)
        tvNotifText = findViewById(R.id.tvNotifText)
        speedUpNotif = findViewById(R.id.speedUpNotif)
        modifierContainer = findViewById(R.id.modifierContainer)
        pbModifier = findViewById(R.id.pbModifier)
        
        tvScore = findViewById(R.id.tvScore)
        tvLives = findViewById(R.id.tvLives)
        tvProgress = findViewById(R.id.tvProgress)
        tvCoins = findViewById(R.id.tvCoins)
        tvFinalScore = findViewById(R.id.tvFinalScore)
        tvHighScore = findViewById(R.id.tvHighScore)
        tvTotalCoins = findViewById(R.id.tvTotalCoins)
        tvShopCoins = findViewById(R.id.tvShopCoins)
        tvCombo = findViewById(R.id.tvCombo)
        btnBuyDouble = findViewById(R.id.btnBuyDouble)

        val gameContainer = findViewById<FrameLayout>(R.id.gameContainer)
        gameView = GameView(this)
        gameContainer.addView(gameView, 0)

        updatePersistentUI()

        // Слушатели
        gameView.onUpdateListener = {
            runOnUiThread {
                tvScore.text = gameView.score.toString()
                tvLives.text = gameView.lives.toString()
                tvCoins.text = gameView.sessionCoins.toString()
                val progress = gameView.deliveredCount % 10
                tvProgress.text = "$progress/10"
                pbModifier.progress = gameView.modifierCharge.toInt()
            }
        }

        gameView.onGameOverListener = { finalScore ->
            saveHighScore(finalScore)
            addCoins(gameView.sessionCoins)
            stopMusic()
            runOnUiThread {
                gameOverScreen.visibility = View.VISIBLE
                statsPanel.visibility = View.GONE
                btnPause.visibility = View.GONE
                modifierContainer.visibility = View.GONE
                tvFinalScore.text = finalScore.toString()
                updatePersistentUI()
            }
        }

        gameView.onBonusListener = { type ->
            runOnUiThread {
                when(type) {
                    "life" -> showNotification(R.drawable.heart2, "+1 Жизнь!", 0xFFEF4444.toInt())
                    "coin" -> showNotification(R.drawable.coin, "+50 Монет!", 0xFFFACC15.toInt())
                }
            }
        }

        gameView.onComboListener = { combo ->
            runOnUiThread {
                if (combo >= 3) {
                    tvCombo.visibility = View.VISIBLE
                    tvCombo.text = "COMBO X${if (combo > 5) 3 else 2}!"
                } else {
                    tvCombo.visibility = View.GONE
                }
            }
        }

        gameView.onSpeedUpListener = { runOnUiThread { showSpeedUpNotification() } }

        gameView.onModifierActivated = {
            runOnUiThread {
                showNotification(R.drawable.paper, "УДВОЕНИЕ АКТИВИРОВАНО!", 0xFFFACC15.toInt())
            }
        }

        // Кнопки
        findViewById<Button>(R.id.btnStart).setOnClickListener { startGame() }
        findViewById<Button>(R.id.btnRestart).setOnClickListener { startGame() }
        findViewById<Button>(R.id.btnShop).setOnClickListener { openShop() }
        findViewById<Button>(R.id.btnCloseShop).setOnClickListener { closeShop() }
        btnBuyDouble.setOnClickListener { buyModifier("double", 10) }
        
        btnPause.setOnClickListener { pauseGame() }
        findViewById<Button>(R.id.btnResume).setOnClickListener { resumeGame() }
        findViewById<Button>(R.id.btnToMenu).setOnClickListener { goToMenu() }

        setFullScreen()
    }

    private fun playRandomMusic() {
        try {
            mediaPlayer?.release()
            val otherTracks = musicTracks.filter { it != currentMusicTrack }
            val nextTrack = if (otherTracks.isNotEmpty()) otherTracks.random() else musicTracks.random()
            currentMusicTrack = nextTrack
            
            val afd = assets.openFd(nextTrack)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = false // Убираем зацикливание одного трека
                setVolume(0.5f, 0.5f)
                setOnCompletionListener { playRandomMusic() } // По завершению играем следующий
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMusic() { mediaPlayer?.stop() }

    private fun startGame() {
        startScreen.visibility = View.GONE
        gameOverScreen.visibility = View.GONE
        pauseScreen.visibility = View.GONE
        statsPanel.visibility = View.VISIBLE
        btnPause.visibility = View.VISIBLE
        
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("own_double", false)) {
            gameView.activeModifier = "double"
            modifierContainer.visibility = View.VISIBLE
        } else {
            gameView.activeModifier = null
            modifierContainer.visibility = View.GONE
        }
        
        gameView.startGame()
        playRandomMusic()
    }

    private fun openShop() {
        startScreen.visibility = View.GONE
        shopScreen.visibility = View.VISIBLE
        updatePersistentUI()
    }

    private fun closeShop() {
        shopScreen.visibility = View.GONE
        startScreen.visibility = View.VISIBLE
    }

    private fun buyModifier(id: String, price: Int) {
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val coins = prefs.getInt("total_coins", 0)
        val isOwned = prefs.getBoolean("own_$id", false)

        if (isOwned) {
            showNotification(R.drawable.coin, "УЖЕ КУПЛЕНО", 0xFF666666.toInt())
            return
        }

        if (coins >= price) {
            prefs.edit().apply {
                putInt("total_coins", coins - price)
                putBoolean("own_$id", true)
                apply()
            }
            updatePersistentUI()
            showNotification(R.drawable.coin, "УСПЕШНО КУПЛЕНО!", 0xFF10B981.toInt())
        } else {
            showNotification(R.drawable.coin, "НЕДОСТАТОЧНО МОНЕТ", 0xFFEF4444.toInt())
        }
    }

    private fun pauseGame() { gameView.pauseGame(); pauseScreen.visibility = View.VISIBLE; mediaPlayer?.pause() }
    private fun resumeGame() { pauseScreen.visibility = View.GONE; gameView.resumeGame(); mediaPlayer?.start() }
    private fun goToMenu() {
        gameView.gameActive = false
        addCoins(gameView.sessionCoins)
        stopMusic()
        pauseScreen.visibility = View.GONE
        statsPanel.visibility = View.GONE
        btnPause.visibility = View.GONE
        modifierContainer.visibility = View.GONE
        startScreen.visibility = View.VISIBLE
        updatePersistentUI()
    }

    private fun showNotification(iconRes: Int, text: String, color: Int) {
        ivNotifIcon.setImageResource(iconRes)
        tvNotifText.text = text
        tvNotifText.setTextColor(color)
        lifeNotification.visibility = View.VISIBLE
        lifeNotification.alpha = 0f
        lifeNotification.animate().alpha(1f).setDuration(300).start()
        handler.postDelayed({
            lifeNotification.animate().alpha(0f).setDuration(300).withEndAction { lifeNotification.visibility = View.GONE }.start()
        }, 2000)
    }

    private fun showSpeedUpNotification() {
        speedUpNotif.visibility = View.VISIBLE
        speedUpNotif.alpha = 0f; speedUpNotif.scaleX = 0.5f; speedUpNotif.scaleY = 0.5f
        speedUpNotif.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(400).withEndAction {
            speedUpNotif.animate().alpha(0f).scaleX(1.5f).scaleY(1.5f).setDuration(600).setStartDelay(500).withEndAction {
                speedUpNotif.visibility = View.GONE
            }.start()
        }.start()
    }

    private fun saveHighScore(score: Int) {
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        if (score > prefs.getInt("high_score", 0)) prefs.edit().putInt("high_score", score).apply()
    }

    private fun addCoins(count: Int) {
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("total_coins", prefs.getInt("total_coins", 0) + count).apply()
    }

    private fun updatePersistentUI() {
        val prefs = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        tvHighScore.text = "РЕКОРД: ${prefs.getInt("high_score", 0)}"
        val totalCoins = prefs.getInt("total_coins", 0)
        tvTotalCoins.text = "МОНЕТЫ: $totalCoins"
        tvShopCoins.text = "МОНЕТЫ: $totalCoins"
        
        if (prefs.getBoolean("own_double", false)) {
            btnBuyDouble.text = "КУПЛЕНО"
            btnBuyDouble.isEnabled = false
            btnBuyDouble.alpha = 0.5f
        }
    }

    private fun setFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    override fun onResume() { super.onResume(); setFullScreen(); if (statsPanel.visibility == View.VISIBLE && pauseScreen.visibility == View.GONE) mediaPlayer?.start() }
    override fun onPause() { super.onPause(); mediaPlayer?.pause() }
    override fun onDestroy() { super.onDestroy(); gameView.release(); mediaPlayer?.release(); mediaPlayer = null }
}
