# Tasks

## 1. Переключить манифест на фирменную иконку

- [x] 1.1 В `app/src/main/AndroidManifest.xml` заменить `android:icon` на
  `@mipmap/ic_launcher`
- [x] 1.2 Добавить `android:roundIcon="@mipmap/ic_launcher_round"`
- [x] 1.3 Убедиться, что в манифесте не осталось ссылок `@android:drawable/...` на
  идентичность приложения

## 2. Проверить результат

- [x] 2.1 `sh gradlew assembleDebug` — сборка проходит (`BUILD SUCCESSFUL in 1s`)
- [x] 2.2 На устройстве/эмуляторе API ≥ 26 иконка на лаунчере — фирменная (adaptive), а не
  системная заглушка (проверено на эмуляторе API 37, `sdk_gphone64_arm64`)
- [ ] 2.3 На устройстве/эмуляторе API 24–25 иконка берётся из растрового `mipmap`-набора и
  не пустая — **не проверено**: под рукой только эмулятор API 37. Косвенно подтверждено
  размерами ресурсов (`mipmap-mdpi/ic_launcher.webp` = 48×48, 3392 Б;
  `mipmap-xxxhdpi/ic_launcher.webp` = 192×192, 31834 Б), но растровый путь на живом
  API < 26 не наблюдался
- [x] 2.4 `aapt dump badging` показывает ожидаемый `package`/`application-icon`, без
  `sym_def_app_icon` (`application: icon='res/mipmap-anydpi-v26/ic_launcher.xml'`,
  `application-icon-120/160/240/320/480/640` → тот же adaptive-ресурс)
