# Spec Delta

## MODIFIED Requirements

### Requirement: Манифест объявляет только используемые разрешения

Манифест MUST содержать только разрешения, реально используемые в коде: `VIBRATE` для
тактильного отклика. Разрешения без реализации в манифесте быть не должно, поскольку они
влияют на декларацию Data Safety в магазине.

#### Scenario: Вибрация при уроне
- **WHEN** персонаж получил урон
- **THEN** вибрация выполнена, и для этого достаточно `VIBRATE`

#### Scenario: Сетевого кода нет
- **WHEN** в приложении отсутствуют обращения к сети
- **THEN** разрешение `INTERNET` в манифесте отсутствует

#### Scenario: Список разрешений манифеста
- **WHEN** разобран `AndroidManifest.xml`
- **THEN** единственное `<uses-permission>` в нём — `android.permission.VIBRATE`

#### Scenario: Проверка собранного APK
- **WHEN** выполнен `aapt dump badging` на релизном APK
- **THEN** в выводе присутствует только `uses-permission: android.permission.VIBRATE`

#### Scenario:Lint-анализ
- **WHEN** запущен `lint` на модуле `app`
- **THEN** предупреждение `UnusedPermission` отсутствует
