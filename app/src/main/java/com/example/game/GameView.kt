package com.example.game

import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.media.SoundPool
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.*

class GameView(context: Context) : View(context), Choreographer.FrameCallback {

    // Состояния игры
    var gameActive = false
    var isPaused = false
    var score = 0
    var ammo = 999
    var lives = 3
    var deliveredCount = 0
    var sessionCoins = 0
    
    private var baseSpeedPxPerSec = 0f
    private var currentSpeed = 0f
    private var pointValue = 10
    private var nextSpeedScore = 75

    // Параметры дороги
    private var roadWidth = 0f
    private var roadX = 0f
    private var laneWidth = 0f
    private var roadOffset = 0f

    // Delta Time
    private var lastFrameTimeNanos: Long = 0
    private var deltaTime: Float = 0f

    // Модификаторы
    var activeModifier: String? = null
    var modifierCharge = 0f
    private var modifierTimeRemaining = 0f
    private val MODIFIER_CHARGE_RATE = 10f
    private val MODIFIER_DURATION = 30f

    // Сущности
    private val player = Player()
    private val newspapers = mutableListOf<Newspaper>()
    private val houses = mutableListOf<House>()
    private val obstacles = mutableListOf<Obstacle>()
    private val cars = mutableListOf<Car>()
    private val pickups = mutableListOf<Pickup>()
    private val trees = mutableListOf<Tree>()
    private val particles = mutableListOf<Particle>()

    // Контроль спавна
    private val lastSpawnY = FloatArray(3) { -1000f }
    private val MIN_VERTICAL_GAP = 450f // Уменьшил (было 600) для плотности
    private var globalSpawnTimer = 0f
    private val SPAWN_COOLDOWN = 0.5f // Уменьшил (было 0.8) для динамики

    // Графика
    private val bitmaps = mutableMapOf<String, Bitmap>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 60
    }
    
    // Окружение
    private var grassColor = Color.parseColor("#34d399")
    private val levelColors = listOf(
        Color.parseColor("#34d399"), Color.parseColor("#fb923c"),
        Color.parseColor("#1e1b4b"), Color.parseColor("#4ade80")
    )

    // Визуальные эффекты
    private var cameraShakeTime = 0f
    private var comboCount = 0
    private val rainDrops = mutableListOf<RainDrop>()
    private val speedLines = mutableListOf<SpeedLine>()

    // Анимация игрока
    private var playerTilt = 0f
    private var targetTilt = 0f
    private var animationTimer = 0f

    // Управление
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var lastTapTime = 0L
    private val swipeThreshold = 100f

    // Звуки и Вибрация
    private lateinit var soundPool: SoundPool
    private var soundIds = mutableMapOf<String, Int>()
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Интерфейс для связи с UI
    var onUpdateListener: (() -> Unit)? = null
    var onGameOverListener: ((Int) -> Unit)? = null
    var onBonusListener: ((String) -> Unit)? = null
    var onComboListener: ((Int) -> Unit)? = null
    var onSpeedUpListener: (() -> Unit)? = null
    var onModifierActivated: (() -> Unit)? = null

    init {
        loadBitmaps()
        setupTrees()
        initSounds()
    }

    private fun loadBitmaps() {
        val assets = context.assets
        try {
            bitmaps["house"] = BitmapFactory.decodeStream(assets.open("img/house.png"))
            bitmaps["paper"] = BitmapFactory.decodeStream(assets.open("img/paper.png"))
            bitmaps["barrier"] = BitmapFactory.decodeStream(assets.open("img/barier.png"))
            bitmaps["car"] = BitmapFactory.decodeStream(assets.open("img/car.png"))
            bitmaps["bicycle"] = BitmapFactory.decodeStream(assets.open("img/character.png"))
            bitmaps["heart"] = BitmapFactory.decodeStream(assets.open("img/heart2.png"))
            bitmaps["coin"] = BitmapFactory.decodeStream(assets.open("img/coin.png"))
            bitmaps["obs_car"] = BitmapFactory.decodeStream(assets.open("img/obs_car.png"))
            bitmaps["obs_box"] = BitmapFactory.decodeStream(assets.open("img/obs_box.png"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSounds() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        try {
            soundIds["throw"] = soundPool.load(context.assets.openFd("sounds/throw.mp3"), 1)
            soundIds["success"] = soundPool.load(context.assets.openFd("sounds/success.mp3"), 1)
            soundIds["fail"] = soundPool.load(context.assets.openFd("sounds/fail.mp3"), 1)
            soundIds["pickup"] = soundPool.load(context.assets.openFd("sounds/pickup.mp3"), 1)
            soundIds["levelup"] = soundIds["pickup"] ?: 0 
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSound(key: String) {
        soundIds[key]?.let { soundPool.play(it, 1f, 1f, 0, 0, 1f) }
    }

    private fun vibrate(duration: Long) {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            } else {
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect, attributes)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun setupTrees() {
        trees.clear()
        repeat(12) {
            trees.add(Tree(Random().nextFloat(), Random().nextFloat() * 2000f))
        }
    }

    fun startGame() {
        score = 0
        ammo = 999
        lives = 3
        deliveredCount = 0
        sessionCoins = 0
        comboCount = 0
        modifierCharge = 0f
        modifierTimeRemaining = 0f
        nextSpeedScore = 75
        currentSpeed = baseSpeedPxPerSec
        isPaused = false
        animationTimer = 0f
        lastFrameTimeNanos = 0
        cameraShakeTime = 0f
        globalSpawnTimer = 0f
        newspapers.clear()
        houses.clear()
        obstacles.clear()
        cars.clear()
        pickups.clear()
        particles.clear()
        rainDrops.clear()
        speedLines.clear()
        grassColor = levelColors[0]
        gameActive = true
        lastSpawnY.fill(-1000f)
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun pauseGame() { isPaused = true }
    fun resumeGame() {
        isPaused = false
        lastFrameTimeNanos = 0
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!gameActive || isPaused) return
        if (lastFrameTimeNanos > 0) {
            deltaTime = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
        } else {
            deltaTime = 1f / 120f
        }
        lastFrameTimeNanos = frameTimeNanos
        if (deltaTime > 0.05f) deltaTime = 0.05f

        update(deltaTime)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        roadWidth = w * 0.85f 
        roadX = (w - roadWidth) / 2
        laneWidth = roadWidth / 3
        player.y = h * 0.8f
        updatePlayerPos()
        player.x = player.targetX
        baseSpeedPxPerSec = h.toFloat() / 3.8f // Вернул скорость (было 4.5)
        if (currentSpeed == 0f) currentSpeed = baseSpeedPxPerSec
    }

    private fun updatePlayerPos() {
        player.targetX = roadX + (player.lane * laneWidth) + (laneWidth / 2)
    }

    override fun onDraw(canvas: Canvas) {
        if (cameraShakeTime > 0) {
            canvas.translate((Random().nextFloat() - 0.5f) * 30f, (Random().nextFloat() - 0.5f) * 30f)
        }
        drawGame(canvas)
        if (gameActive && !isPaused) onUpdateListener?.invoke()
    }

    private fun update(dt: Float) {
        if (player.invuln > 0) player.invuln--
        if (cameraShakeTime > 0) cameraShakeTime -= dt
        
        roadOffset += currentSpeed * dt
        animationTimer += 10f * dt
        globalSpawnTimer -= dt

        if (modifierTimeRemaining > 0) {
            modifierTimeRemaining -= dt
        } else {
            if (modifierCharge < 100f) {
                modifierCharge += MODIFIER_CHARGE_RATE * dt
                if (modifierCharge > 100f) modifierCharge = 100f
            }
        }

        score += (currentSpeed * dt / height * 10f).toInt()

        val dx = player.targetX - player.x
        player.x += dx * (1.0f - Math.pow(0.001, dt.toDouble()).toFloat())
        targetTilt = (dx / 40f).coerceIn(-20f, 20f)
        playerTilt += (targetTilt - playerTilt) * (1.0f - Math.pow(0.001, dt.toDouble()).toFloat())

        spawnObjects(dt)

        val frameMovement = currentSpeed * dt
        updateList(houses, frameMovement)
        updateList(obstacles, frameMovement)
        updateList(pickups, frameMovement)
        updateList(trees, frameMovement)
        
        for (i in 0..2) lastSpawnY[i] += frameMovement

        val carMovement = currentSpeed * 1.6f * dt
        val carIt = cars.iterator()
        while (carIt.hasNext()) {
            val c = carIt.next()
            c.y += carMovement
            // Машины тоже блокируют полосу в логике спавна
            if (c.y < 400f) lastSpawnY[c.lane] = c.y
            if (c.y > height + 500) carIt.remove()
        }

        updateParticles(dt)
        updateWeather(dt)
        updateSpeedLines(dt)

        val it = newspapers.iterator()
        while (it.hasNext()) {
            val n = it.next()
            n.x += n.vx * (dt * 60f); n.y += n.vy * (dt * 60f)
            houses.forEach { h ->
                if (!h.hit && n.x > h.x - h.w/2 && n.x < h.x + h.w/2 && n.y > h.y - h.h/2 && n.y < h.y + h.h/2) {
                    h.hit = true; n.delivered = true; comboCount++
                    val comboBonus = if (comboCount >= 3) 2 else 1
                    score += pointValue * comboBonus
                    if (comboCount >= 3) onComboListener?.invoke(comboCount)
                    deliveredCount++; playSound("success"); spawnExplosion(h.x, h.y, Color.WHITE, 20, 500f)
                    if (deliveredCount % 10 == 0) { sessionCoins += 50; onBonusListener?.invoke("coin"); playSound("levelup") }
                    if (deliveredCount % 20 == 0) changeLevel()
                }
            }
            if (!n.delivered && (n.y < -100 || n.x < 0 || n.x > width)) {
                comboCount = 0; onComboListener?.invoke(0); it.remove()
            } else if (n.delivered) it.remove()
        }
        if (score >= nextSpeedScore) {
            currentSpeed += baseSpeedPxPerSec * 0.12f 
            nextSpeedScore += 75 
            onSpeedUpListener?.invoke()
        }
        checkPlayerCollisions()
    }

    private fun changeLevel() {
        val nextIndex = (deliveredCount / 20) % levelColors.size
        grassColor = levelColors[nextIndex]
    }

    private fun spawnExplosion(x: Float, y: Float, color: Int, count: Int, baseVel: Float, onlyUp: Boolean = false) {
        repeat(count) {
            val vx = (Random().nextFloat() - 0.5f) * baseVel * 2f
            val vy = if (onlyUp) -Random().nextFloat() * baseVel * 2f else (Random().nextFloat() - 0.5f) * baseVel * 2f
            particles.add(Particle(x, y, color, vx, vy))
        }
    }

    private fun updateParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next(); p.update(dt)
            if (p.life <= 0) it.remove()
        }
    }

    private fun updateWeather(dt: Float) {
        if (grassColor == levelColors[2]) {
            if (Random().nextFloat() < 0.3f) rainDrops.add(RainDrop(Random().nextFloat() * width, -10f))
        }
        val it = rainDrops.iterator()
        while (it.hasNext()) { val d = it.next(); d.y += 1500f * dt; if (d.y > height) it.remove() }
    }

    private fun updateSpeedLines(dt: Float) {
        if (currentSpeed > baseSpeedPxPerSec * 1.5f) {
            if (Random().nextFloat() < 0.2f) {
                val side = if (Random().nextBoolean()) 0f else width.toFloat()
                speedLines.add(SpeedLine(side + (Random().nextFloat() - 0.5f) * 100f, Random().nextFloat() * height))
            }
        }
        val it = speedLines.iterator()
        while (it.hasNext()) { val l = it.next(); l.life -= dt * 2f; if (l.life <= 0) it.remove() }
    }

    private fun updateList(list: MutableList<out Entity>, movement: Float) {
        val it = list.iterator()
        while (it.hasNext()) { val e = it.next(); e.y += movement; if (e.y > height + 420) it.remove() }
    }

    private fun spawnObjects(dt: Float) {
        // Спавн домов (вне дороги) - реже
        if (Random().nextFloat() < 0.015f * (deltaTime * 120f)) {
            val side = if (Random().nextBoolean()) -1 else 1
            val x = if (side == -1) roadX - 180f else roadX + roadWidth + 180f
            if (houses.none { it.y < 420 }) houses.add(House(x, -420f))
        }

        // Кулдаун между спавнами на дороге
        if (globalSpawnTimer > 0) return

        // Выбираем полосу
        val lane = Random().nextInt(3)
        val x = roadX + (lane * laneWidth) + (laneWidth / 2)
        
        // ПРАВИЛО: Не спавним, если на полосе кто-то уже есть близко к верху
        if (lastSpawnY[lane] < -MIN_VERTICAL_GAP) return

        // ПРАВИЛО: Не закрываем всю дорогу
        // Если две другие полосы УЖЕ заняты чем-то опасным в верхней зоне
        val otherLanesDanger = (0..2).filter { it != lane }.count { lastSpawnY[it] < 300f }
        val isBlocked = otherLanesDanger >= 2

        val rand = Random().nextFloat()
        val isDouble = activeModifier == "double" && modifierTimeRemaining > 0
        
        var spawned = false
        when {
            // Машины (опасность) - увеличил шанс
            rand < 0.18f && !isBlocked -> {
                cars.add(Car(x, -450f, lane))
                lastSpawnY[lane] = -450f
                spawned = true
            }
            // Сердце (редко)
            rand < 0.20f -> {
                pickups.add(Pickup(x, -100f, "heart"))
                if (isDouble) pickups.add(Pickup(x + 20, -120f, "heart"))
                lastSpawnY[lane] = -120f
                spawned = true
            }
            // Монета - увеличил шанс
            rand < 0.45f -> {
                pickups.add(Pickup(x, -100f, "coin"))
                if (isDouble) pickups.add(Pickup(x + 20, -120f, "coin"))
                lastSpawnY[lane] = -120f
                spawned = true
            }
            // Препятствие (статичное) - увеличил шанс
            rand < 0.70f && !isBlocked -> {
                val obsType = if (Random().nextBoolean()) "obs_car" else "obs_box"
                obstacles.add(Obstacle(x, -300f, obsType))
                lastSpawnY[lane] = -300f
                spawned = true
            }
        }
        
        if (spawned) globalSpawnTimer = SPAWN_COOLDOWN
    }

    private fun checkPlayerCollisions() {
        val carIt = cars.iterator()
        while (carIt.hasNext()) {
            val c = carIt.next()
            if (player.invuln == 0 && dist(player.x, player.y, c.x, c.y) < 110) {
                handleDamage(0.6f); carIt.remove()
                if (lives <= 0) { gameActive = false; onGameOverListener?.invoke(score) }
            }
        }
        val obsIt = obstacles.iterator()
        while (obsIt.hasNext()) {
            val o = obsIt.next()
            if (player.invuln == 0 && dist(player.x, player.y, o.x, o.y) < 100) {
                handleDamage(0.4f); obsIt.remove()
                if (lives <= 0) { gameActive = false; onGameOverListener?.invoke(score) }
            }
        }
        val pickIt = pickups.iterator()
        while (pickIt.hasNext()) {
            val p = pickIt.next()
            if (dist(player.x, player.y, p.x, p.y) < 80) {
                val gain = if (activeModifier == "double" && modifierTimeRemaining > 0) 2 else 1
                when(p.type) {
                    "coin" -> { sessionCoins += 10 * gain; playSound("pickup") }
                    "heart" -> { lives += gain; playSound("levelup"); onBonusListener?.invoke("life") }
                }
                spawnExplosion(player.x, player.y - 20f, Color.YELLOW, 15, 400f, true); pickIt.remove()
            }
        }
    }

    private fun handleDamage(shake: Float) {
        lives--; player.invuln = 60; cameraShakeTime = shake; comboCount = 0; onComboListener?.invoke(0)
        playSound("fail"); vibrate(300); spawnExplosion(player.x, player.y, Color.GRAY, 15, 300f)
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(grassColor)
        paint.color = Color.parseColor("#4b5563"); canvas.drawRect(roadX, 0f, roadX + roadWidth, height.toFloat(), paint)
        paint.color = Color.WHITE; paint.strokeWidth = 10f; paint.alpha = 150
        canvas.drawLine(roadX, 0f, roadX, height.toFloat(), paint)
        canvas.drawLine(roadX + roadWidth, 0f, roadX + roadWidth, height.toFloat(), paint)
        paint.alpha = 80; val totalCycle = 180f
        var startY = (roadOffset % totalCycle) - totalCycle
        while (startY < height) {
            canvas.drawLine(roadX + laneWidth, startY, roadX + laneWidth, startY + 100f, paint)
            canvas.drawLine(roadX + laneWidth * 2, startY, roadX + laneWidth * 2, startY + 100f, paint)
            startY += totalCycle
        }
        trees.forEach { drawTree(canvas, it) }
        houses.forEach { drawShadow(canvas, it.x, it.y, it.w * 0.8f, it.h * 0.3f); drawEntity(canvas, it, "house") }
        obstacles.forEach { drawShadow(canvas, it.x, it.y, it.w * 0.8f, it.h * 0.3f); drawEntity(canvas, it, it.type) }
        cars.forEach { 
            drawShadow(canvas, it.x, it.y, laneWidth * 0.9f, it.h * 0.3f)
            bitmaps["car"]?.let { bmp ->
                val rect = RectF(it.x - laneWidth/2 + 5, it.y - it.h/2, it.x + laneWidth/2 - 5, it.y + it.h/2)
                canvas.drawBitmap(bmp, null, rect, null)
            }
        }
        pickups.forEach { drawEntity(canvas, it, it.type) }
        newspapers.forEach { drawEntity(canvas, it, "paper") }
        particles.forEach { it.draw(canvas) }
        drawWeather(canvas); drawSpeedLines(canvas)
        if (player.invuln % 10 < 5) {
            bitmaps["bicycle"]?.let {
                drawShadow(canvas, player.x, player.y + 40f, 100f, 40f)
                canvas.save()
                val bob = sin(animationTimer.toDouble()).toFloat()
                canvas.translate(player.x, player.y + bob * 8f)
                canvas.rotate(playerTilt); canvas.scale(1f + bob * 0.03f, 1f + bob * 0.03f)
                canvas.drawBitmap(it, null, RectF(-90f, -90f, 90f, 90f), null)
                canvas.restore()
            }
        }
        if (modifierTimeRemaining > 0) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 10f; paint.color = Color.YELLOW
            paint.alpha = (sin(animationTimer.toDouble() * 2) * 100 + 155).toInt()
            canvas.drawCircle(player.x, player.y, 110f, paint); paint.style = Paint.Style.FILL
        }
    }

    private fun drawShadow(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.drawOval(RectF(x - w/2, y + h*0.5f, x + w/2, y + h*0.9f), shadowPaint)
    }

    private fun drawWeather(canvas: Canvas) {
        paint.color = Color.WHITE; paint.alpha = 100; paint.strokeWidth = 2f
        rainDrops.forEach { canvas.drawLine(it.x, it.y, it.x, it.y + 40f, paint) }
    }

    private fun drawSpeedLines(canvas: Canvas) {
        paint.color = Color.WHITE
        speedLines.forEach { paint.alpha = (it.life * 100).toInt(); canvas.drawLine(it.x, it.y, it.x, it.y + 200f, paint) }
    }

    private fun drawTree(canvas: Canvas, t: Tree) {
        val tx = if (t.treeLane < 0.5) roadX - 200f else roadX + roadWidth + 200f
        drawShadow(canvas, tx, t.y, 120f, 30f); paint.color = Color.parseColor("#14532d"); paint.alpha = 255
        canvas.drawCircle(tx, t.y, 80f, paint)
    }

    private fun drawEntity(canvas: Canvas, e: Entity, key: String) {
        bitmaps[key]?.let {
            val rect = RectF(e.x - e.w / 2, e.y - e.h / 2, e.x + e.w / 2, e.y + e.h / 2)
            paint.alpha = if (e is House && e.hit) 150 else 255
            canvas.drawBitmap(it, null, rect, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!gameActive || isPaused) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x; touchStartY = event.y
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 300) { if (dist(event.x, event.y, player.x, player.y) < 150f) activateModifier() }
                lastTapTime = now
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchStartX; val dy = event.y - touchStartY
                if (abs(dx) > swipeThreshold && abs(dx) > abs(dy)) {
                    if (dx > 0 && player.lane < 2) player.lane++
                    else if (dx < 0 && player.lane > 0) player.lane--
                    updatePlayerPos()
                } else if (abs(dy) < swipeThreshold) {
                    playSound("throw")
                    newspapers.add(Newspaper(player.x, player.y, (if (event.x < width / 2) -1 else 1) * 30f, -8f))
                }
                performClick()
            }
        }
        return true
    }

    private fun activateModifier() {
        if (modifierCharge >= 100f && activeModifier != null) {
            modifierCharge = 0f; modifierTimeRemaining = MODIFIER_DURATION
            onModifierActivated?.invoke(); vibrate(100)
        }
    }

    override fun performClick(): Boolean { super.performClick(); return true }
    class Particle(val startX: Float, val startY: Float, val color: Int, val vx: Float, val vy: Float) {
        private var curX = startX; private var curY = startY; var life = 1.0f 
        fun update(dt: Float) { curX += vx * dt; curY += vy * dt; life -= dt * 2.0f }
        fun draw(canvas: Canvas) {
            val p = Paint().apply { color = this@Particle.color; alpha = (life * 255).toInt().coerceIn(0, 255) }
            canvas.drawCircle(curX, curY, Random().nextFloat() * 12f + 4f, p)
        }
    }
    class RainDrop(var x: Float, var y: Float)
    class SpeedLine(var x: Float, var y: Float) { var life = 1.0f }
    open class Entity(var x: Float, var y: Float, var w: Float = 100f, var h: Float = 100f)
    class Player : Entity(0f, 0f) { var lane = 1; var targetX = 0f; var invuln = 0 }
    class House(x: Float, y: Float) : Entity(x, y, 420f, 420f) { var hit = false }
    class Obstacle(x: Float, y: Float, val type: String = "barrier") : Entity(x, y, 220f, 200f)
    class Car(x: Float, y: Float, val lane: Int) : Entity(x, y, 220f, 280f)
    class Pickup(x: Float, y: Float, val type: String = "paper") : Entity(x, y, 100f, 100f)
    class Newspaper(x: Float, y: Float, var vx: Float, var vy: Float) : Entity(x, y, 60f, 60f) { var delivered = false }
    class Tree(val treeLane: Float, y: Float) : Entity(0f, y)
    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float) = sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)))
    fun release() { soundPool.release() }
}
