# game-loop Specification

## Purpose

Задаёт детерминированный игровой цикл на `Choreographer`: как кадры обновляются по
delta-time, где цикл останавливается на паузе и при game over, и с каким состоянием
игра стартует заново. Реализация — `GameView.doFrame/update/startGame`
(`app/src/main/java/com/example/game/GameView.kt`).

## Requirements

### Requirement: Кадры двигаются по Choreographer с delta-time

Игровой цикл MUST использовать `Choreographer.FrameCallback` как источник кадров и
рассчитывать смещение сущностей из delta-time между кадрами, а не из фиксированного
шага. Каждый кадр завершается повторной подпиской `postFrameCallback`, пока игра активна.

#### Scenario: Первый кадр после startGame
- **WHEN** цикл получает первый кадр и `lastFrameTimeNanos` ещё не установлен
- **THEN** delta-time принимается за 1/120 секунды, а не за время с момента запуска процесса

#### Scenario: Смещение за кадр
- **WHEN** кадр получен с delta-time `dt` и текущей скоростью `currentSpeed`
- **THEN** все объекты дороги смещаются вниз на `currentSpeed * dt` пикселей

### Requirement: Delta-time ограничен сверху

Цикл MUST обрезать delta-time до 0.05 секунды, чтобы длительная блокировка потока или
сворачивание приложения не телепортировали сущности через всю дорогу и не пропускали
столкновения.

#### Scenario: Длинный простой
- **WHEN** между кадрами прошло больше 0.05 секунды
- **THEN** для расчётов этого кадра используется значение 0.05 секунды

### Requirement: Цикл останавливается на паузе и при game over

Цикл MUST прекратить обновление и пере-подписку кадров, когда `gameActive` равен `false`
или `isPaused` равен `true`. После возобновления цикл MUST продолжить работу без скачка
времени.

#### Scenario: Пауза
- **WHEN** `pauseGame()` устанавливает `isPaused` в `true`
- **THEN** `doFrame` выходит сразу, не вызывая `update`, `invalidate` или `postFrameCallback`

#### Scenario: Возобновление
- **WHEN** вызван `resumeGame()`
- **THEN** `lastFrameTimeNanos` сбрасывается в 0, цикл подписывается на новый кадр, и
  первый кадр после возобновления использует базовые 1/120 секунды

#### Scenario: Смерть игрока
- **WHEN** число жизней становится 0 или меньше
- **THEN** `gameActive` становится `false` и цикл больше не переподписывается

### Requirement: Старт забега сбрасывает состояние

`startGame()` MUST полностью пересобирать состояние забега, чтобы повторный запуск не
наследовал объекты, очки или таймеры прошлого.

#### Scenario: Повторный запуск
- **WHEN** игрок запускает игру второй раз
- **THEN** score, deliveredCount, sessionCoins, comboCount, modifierCharge и
  modifierTimeRemaining равны 0, lives равно 3, ammo равно 999, все списки сущностей
  пусты, скорость равна базовой, а цвет травы — первый из палитры уровней

### Requirement: Каждый кадр обновляет HUD через слушателя

`GameView` MUST вызывать `onUpdateListener` после отрисовки активного (не поставленного)
кадра, чтобы активность могла обновить счётчики, не обращаясь к внутренностям движка.

#### Scenario: Активный кадр
- **WHEN** кадр отрисован при `gameActive == true` и `isPaused == false`
- **THEN** `onUpdateListener` вызывается один раз за кадр

#### Scenario: Кадр на паузе
- **WHEN** игра на паузе
- **THEN** `onUpdateListener` не вызывается, и значения в HUD не обновляются
