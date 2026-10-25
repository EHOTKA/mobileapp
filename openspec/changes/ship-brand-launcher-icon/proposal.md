# Proposal

## Why

Манифест объявляет иконку приложения как `@android:drawable/sym_def_app_icon` — системную
заглушку Android (`app/src/main/AndroidManifest.xml:11`). При этом в проекте уже есть
полный фирменный набор ресурсов, который игнорируется:

- `res/mipmap-anydpi-v26/ic_launcher.xml` и `ic_launcher_round.xml` — adaptive-icon с
  `@drawable/ic_launcher_background` и `@mipmap/ic_launcher_foreground`;
- растровые предки для `mdpi`/`hdpi`/`xhdpi`/`xxhdpi`/`xxxhdpi` (`ic_launcher.webp`,
  `ic_launcher_round.webp`, `ic_launcher_foreground.webp`);
- `src/main/ic_launcher-playstore.png` — пререндер для стора.

На устройстве и в списке приложений игра выглядит как безымянный системный значок, что
для публичного релиза неприемлемо: иконка участвует и в листинге стора, и в узнавании
установленного приложения пользователем.

## What Changes

- `android:icon` в манифесте переключается на `@mipmap/ic_launcher`.
- Добавляется `android:roundIcon="@mipmap/ic_launcher_round"` для лаунчеров, использующих
  круглую форму.
- Ссылки на `@android:drawable/...` как идентичность приложения в манифесте не остаются
  (системные drawable в проекте используются только как иконки кнопок UI — паузы и стрелки
  отправки на стартовом экране; они вне области действия этого изменения).

## Capabilities

### New Capabilities

_Нет — новая способность не вводится._

### Modified Capabilities

- `app-platform`: требование «Иконка запуска берётся из фирменных ресурсов проекта»
  дополняется сценариями про `roundIcon` и adaptive-icon на API 26+, чтобы приёмка была
  проверяемой, а не декларативной.

## Impact

- Код/ресурсы: `app/src/main/AndroidManifest.xml` — атрибуты `android:icon` и
  `android:roundIcon`. Сами ресурсные файлы менять не нужно, они уже на месте.
- Сборка: apk пересобирается; `res/mipmap-*` впервые попадают в итоговый ресурсный пул,
  размер APK вырастет на несколько килобайт (webp-набор уже лежит в репозитории и в
  `sourceset` входит в сборку независимо от ссылки в манифесте).
- Risks: на API < 26 используется растровый `ic_launcher.webp` из density-наборов —
  проверить, что он не пустой; `minSdk` 24, то есть этот путь реален. Themed icons
  опираются на `<monochrome>` — он уже объявлен в adaptive-icon.
- Область: не затрагивает `@android:drawable/ic_media_pause` и
  `@android:drawable/ic_menu_send` в `activity_main.xml` — это UI-глифы, не идентичность
  приложения.
