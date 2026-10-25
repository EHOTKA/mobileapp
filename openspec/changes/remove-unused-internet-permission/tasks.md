# Tasks

## 1. Убрать лишнее разрешение

- [ ] 1.1 В `app/src/main/AndroidManifest.xml` удалить
  `<uses-permission android:name="android.permission.INTERNET" />`
- [ ] 1.2 Оставить `android.permission.VIBRATE` без изменений

## 2. Проверить, что сеть действительно не используется

- [ ] 2.1 Поиск по `app/src/main/java` на `WebView|OkHttp|Retrofit|URL|Socket|http` не
  находит обращений к сети
- [ ] 2.2 Поиск по `app/src/main/res/layout` на `WebView` не находит view-элементов

## 3. Проверить сборку и вибрацию

- [ ] 3.1 `sh gradlew assembleDebug` — сборка проходит
- [ ] 3.2 `aapt dump badging <apk>` показывает только `uses-permission:
  android.permission.VIBRATE`
- [ ] 3.3 На устройстве с вибрацией: урон даёт вибрацию 300 мс, активация модификатора —
  100 мс
