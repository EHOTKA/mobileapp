# save-persistence Specification

## Purpose

Фиксирует единственный слой персистентности игры — `SharedPreferences` с именем
`game_prefs` — и семантику его ключей. Реализация —
`MainActivity.saveHighScore/addCoins/updatePersistentUI/startGame/buyModifier`
(`app/src/main/java/com/example/game/MainActivity.kt`).

## Requirements

### Requirement: Все persisted-данные лежат в одном файле настроек

Приложение MUST хранить рекорд, баланс монет и флаги владения в `SharedPreferences`
`game_prefs` с modes `MODE_PRIVATE`, без резервных копий на устройство и без сети.

#### Scenario: чтение при старте
- **WHEN** активность создаётся
- **THEN** значения читаются из `game_prefs`; отсутствующий ключ трактуется как 0 для чисел
  и `false` для флагов

### Requirement: Рекорд обновляется только при превышении

Приложение MUST записывать новый рекорд, только если завершённый забег дал больше очков,
чем уже сохранённый.

#### Scenario: Новый максимум
- **WHEN** результат забега 120, сохранённый рекорд 90
- **THEN** `high_score` становится 120

#### Scenario: Результат хуже рекорда
- **WHEN** результат забега меньше сохранённого рекорда
- **THEN** `high_score` остаётся прежним

### Requirement: Ключи и их типы стабильны

Схема MUST использовать ключи `high_score` (Int), `total_coins` (Int) и `own_double`
(Boolean) и не менять их типы без миграции, иначе сохранённые прогрессы пользователей
будут потеряны.

#### Scenario: Покупка модификатора
- **WHEN** товар «Удвоение» куплен
- **THEN** записан `own_double = true` и уменьшен `total_coins`

### Requirement: Запись не блокирует UI-поток

Записи в настройки MUST выполняться через `apply()` (асинхронно), а не через синхронный
`commit()`, чтобы не тормозить кадр при начислении монет.

#### Scenario: Начисление монет в конце забега
- **WHEN** вызван `addCoins`
- **THEN** используется `edit().putInt(...).apply()`
