# Spec Delta

## MODIFIED Requirements

### Requirement: Иконка запуска берётся из фирменных ресурсов проекта

Приложение MUST использовать иконку launcher из собственных `mipmap`-ресурсов проекта, а
не системную заглушку, и MUST показывать узнаваемое изображение на лаунчере.

#### Scenario: Иконка на устройстве
- **WHEN** приложение установлено и открыт лаунчер
- **THEN** `android:icon` указывает на `@mipmap/ic_launcher` и отличается от системной заглушки

#### Scenario: Разбор манифеста
- **WHEN** прочитан `AndroidManifest.xml`
- **THEN** ни `android:icon`, ни `android:roundIcon` не ссылаются на `@android:drawable`

#### Scenario: Adaptive-icon на Android 8.0 и новее
- **WHEN** приложение запущено на устройстве с API ≥ 26
- **THEN** используется `mipmap-anydpi-v26/ic_launcher.xml` со слоем
  `@drawable/ic_launcher_background` и передним планом `@mipmap/ic_launcher_foreground`

#### Scenario: Круглая форма на лаунчере
- **WHEN** лаунчер запрашивает круглую иконку
- **THEN** `android:roundIcon` указывает на `@mipmap/ic_launcher_round`

#### Scenario: Растровый путь на Android 7.x
- **WHEN** приложение запущено на устройстве с API 24 или 25
- **THEN** берётся непустой `ic_launcher.webp` из density-набора, соответствующего экрану
