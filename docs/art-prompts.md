# Стилистика и промпты для новых уровней «Почтальон Драйв»

Документ для генерации ассетов нейросетью (Qwen-Image) с последующим удалением белого
фона. Промпты собраны полностью — копировать целиком, ничего доклеивать не нужно.

---

## 0. Как пользоваться

Каждый промпт собран по одной формуле, чтобы серия осталась стилистически единой:

```
ANCHOR + SUBJECT + CAMERA + TAIL
```

`ANCHOR` и `TAIL` во всех промптах **дословно одинаковые**. Это не косметика: у модели в
режиме text-to-image нет опоры на референс, и единообразие серии держится только на
неизменном якорном абзаце. Если захочешь изменить материал или свет — меняй `ANCHOR`
сразу во всех промптах, по одному за раз серия поплывёт.

Правила для Qwen-Image:

- Никаких `--ar`, `--style raw`, `--no` — это синтаксис Midjourney, тут он прочитается как
  ordinary текст и может отрисоваться в кадре. Всё ограничение сказано словами.
- Порядок внутри промпта фиксированный: стиль → объект → ракурс → фон и кадрирование.
- Отдельно запрещены надписи: модель необычно хороша в рендеринге текста внутри картинки и
  с удовольствием прилепит на дом вывеску, номер дома или «OPEN».
- Если в твоём эндпоинте есть отдельные параметры `negative_prompt` и `size` — положи в
  них блоки 2.3 и поставь `1024x1024`. Если это чистый чат — ничего не теряется, всё уже
  в тексте.
- Если есть режим редактирования с image reference — подай текущий `house.png` как
  референс стиля, это даст больше единообразия, чем любой текст.

---

## 1. Аудит того, что уже лежит в проекте

Проверено глазами (6 файлов из 14 в `assets/img`):

| файл | загружается кодом? | стиль | вердикт |
|---|---|---|---|
| `character.png` | да (`bicycle`) | матовая глина, вид сзади | **канон** |
| `house.png` | да (`house`) | матовая глина, изометрия 3/4 | **канон** |
| `obs_car.png` | да (`obs_car`) | фотореалистичная ржавчина, PBR-грязь | вне канона |
| `coin.png` | да (`coin`) | глянцевое полированное золото | вне канона |
| `obs_barrel.png` | **нет** | глянцевый металл + выдавленная буква «M» | вне канона, и надпись |
| `obs_hole.png` | **нет** | плоская 2D-иллюстрация, не 3D-рендер | вне канона, другой медиа |

Не смотрел (проверь сам на тот же канон): `obs_box.png`, `barier.png`, `paper.png`,
`heart.png`, `heart2.png`, `car.png`, `road.png`, `app-icon.png`.

Два замечания, важных для планирования:

1. `road.png` и `app-icon.png` в `loadBitmaps()` не участвуют — дорога и трава рисуются
   кодом сплошными прямоугольниками, так что `road.png` сейчас бесполезен.
2. Деревьев-спрайтов в проекте **нет вообще**: `drawTree()` рисует плоский тёмно-зелёный
   круг (`#14532d`, радиус 80). Все `tree_*.png` из этого документа требуют правки кода.

---

## 2. Общие блоки

### 2.1 ANCHOR (в начале каждого промпта)

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small.
```

### 2.2 TAIL (в конце каждого промпта)

```
The object is centered with about 10 percent empty margin on all sides, nothing is
cropped, square 1:1 composition. The object sits on a flat pure white background with hex
color FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no
reflection, no gradient, no vignette and no horizon. Single object only, no other objects,
no lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 2.3 NEGATIVE (если эндпоинт принимает отдельным параметром)

```
ground, floor, cast shadow, contact shadow, reflection, gradient background, vignette,
photorealistic, rust, grime, dirt, scratches, wear and tear, text, lettering, signage,
numbers, watermark, logo, multiple objects, cropped, frame, border, motion blur, noise
```

### 2.4 Ракурсы по ролям

Роль в игре определяет ракурс, потому что дорога и обочина читаются по-разному:

| роль | CAMERA |
|---|---|
| дом (цель доставки) | `Viewed from an isometric three-quarter angle from above at about 35 degrees, so that both the roof and the front facade are clearly visible.` |
| препятствие (статичное) | `Viewed straight from above in a top-down bird's-eye view at 90 degrees.` |
| машина (движущаяся) | `Viewed straight from above in a top-down bird's-eye view, the vehicle oriented vertically with its front pointing toward the bottom of the frame.` |
| пикап (бонус) | `Viewed flat from the front, icon-like composition, the object fills the center of the frame.` |
| декорация (дерево, реквизит) | `Viewed from a three-quarter angle from above at about 45 degrees.` |

### 2.5 Что остаётся универсальным

Монету **не** ретемим: `coin.png` одна на все уровни. Игрок считывает валюту по форме и
золотому цвету, а смена вида каждые 20 доставок только добавит шума. Тематическим делаем
только пикап жизни (`heart_*.png`) — там сюжетный предмет читается лучше красного сердца.

---

## 3. Соглашение об именах

Кладить в `app/src/main/assets/img/`, имя — `<роль>_<тема>.png`, строчные, `_` как
разделитель (так уже названы `obs_car.png`, `obs_box.png`).

| роль в имени | что это |
|---|---|
| `house_` | цель доставки, дом сбоку от дороги |
| `obs_` | статичное препятствие на дороге |
| `car_` | движущаяся опасность (едет быстрее дороги) |
| `heart_` | тематическая замена пикапа «+1 жизнь» |
| `tree_` | декоративное дерево на обочине |
| `prop_` | мелкий реквизит на обочине |

После вырезания фона: даунскейл до 512 px по длинной стороне. В `loadBitmaps()` стоит
`MAX_BITMAP_PX = 512` и `decodeSampled`, поэтому 2048 всё равно срежется при декоде, но
вес в APK и время старта уже испортит. 512 достаточно: самый крупный спрайт (дом) занимает
на экране 420 px.

---

## 4. L5 — «Снежная почта»

**Тема.** Тихий заснеженный посёлок. Дорога тёмная на светлом фоне, из труб дым, всё
округлое и пухлое от снега. Настроение — уют, а не холод: тёплые жёлтые окна и красные
акценты против бледно-синего поля. Темп ощущается медленнее, чем он есть, потому что
белый фон увеличивает контраст силуэтов.

**Палитра.** Трава `#d7e7f5`, акценты красный `#ef4444` и тёплый жёлтый окон, тёмная
сине-серая тень под снегом.

**Особая опасность.** Белое на белом: снег на белом фоне вырезается вместе с фоном. Во всех
зимних SUBJECT поэтому снег — не белый, а «off-white with a pale blue-grey tint», и у
каждой белой поверхности есть видимый тёплый серый контур.

### 4.1 `house_winter.png`
Дом-цель. Пухлая крыша под толстым слоем снега, сосульки, светящиеся окна, красная дверь.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A cozy
clay cottage with a thick rounded snow-covered roof in off-white with a pale blue-grey
tint, each snow surface separated by a visible warm grey ambient occlusion edge, small
blunt icicles hanging from the eaves, two glowing warm amber windows, a rounded red clay
front door, and a thick snow cap on the chimney. Viewed from an isometric three-quarter
angle from above at about 35 degrees, so that both the roof and the front facade are
clearly visible. The object is centered with about 10 percent empty margin on all sides,
nothing is cropped, square 1:1 composition. The object sits on a flat pure white
background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

### 4.2 `obs_snowbank.png`
Сугроб с торчащим камнем. Низкий силуэт — читается как «можно не врезаться, но не выйдет».

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A low
rounded clay snowbank in off-white with a pale blue-grey tint and a visible warm grey
ambient occlusion edge around its outline, with a single dark grey rounded rock poking out
of the top of it. Viewed straight from above in a top-down bird's-eye view at 90 degrees.
The object is centered with about 10 percent empty margin on all sides, nothing is
cropped, square 1:1 composition. The object sits on a flat pure white background with hex
color FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no
reflection, no gradient, no vignette and no horizon. Single object only, no other objects,
no lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 4.3 `obs_ice_patch.png`
Лужа, превратившаяся в лёд. Единственный «просвечивающий» объект в наборе — за счёт этого
отличается от сугроба на скорости.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A flat
irregularly shaped patch of glossy ice in pale cyan with a lighter frost rim around its
edge and two small blunt white highlights on its surface, shaped like a wide rounded blob
with soft lobes. Viewed straight from above in a top-down bird's-eye view at 90 degrees.
The object is centered with about 10 percent empty margin on all sides, nothing is
cropped, square 1:1 composition. The object sits on a flat pure white background with hex
color FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no
reflection, no gradient, no vignette and no horizon. Single object only, no other objects,
no lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 4.4 `obs_log_snow.png`
Упавшее бревно в снегу. Вытянутая форма даёт другой силуэт, чем круглые объекты.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A fallen
pine log lying horizontally, warm brown bark with rounded ends showing pale growth rings,
and a thick cap of off-white snow with a pale blue-grey tint running along the top of the
log with a visible warm grey ambient occlusion edge. Viewed straight from above in a
top-down bird's-eye view at 90 degrees. The object is centered with about 10 percent empty
margin on all sides, nothing is cropped, square 1:1 composition. The object sits on a flat
pure white background with hex color FFFFFF, with no ground plane, no floor, no cast
shadow, no contact shadow, no reflection, no gradient, no vignette and no horizon. Single
object only, no other objects, no lettering, no signs, no numbers, no text, no watermark,
no logo.
```

### 4.5 `car_snowplow.png`
Движущаяся опасность. передний отвал указывает вниз кадра — туда же, куда едет машина.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A small
clay snowplow truck with a rounded bright blue cab, a wide orange plow blade mounted at
the front, a roof rack on top, chunky dark grey tires and two pale amber headlights.
Viewed straight from above in a top-down bird's-eye view, the vehicle oriented vertically
with its front pointing toward the bottom of the frame. The object is centered with about
10 percent empty margin on all sides, nothing is cropped, square 1:1 composition. The
object sits on a flat pure white background with hex color FFFFFF, with no ground plane,
no floor, no cast shadow, no contact shadow, no reflection, no gradient, no vignette and
no horizon. Single object only, no other objects, no lettering, no signs, no numbers, no
text, no watermark, no logo.
```

### 4.6 `heart_scarf.png`
Пикап «+1 жизнь». Красный шарф вместо сердца: «спасение от холода» считывается сразу.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
red knitted winter scarf coiled into a loose circle, with thick blunt fringe tassels at
the two ends and a soft chunky knit texture across the surface. Viewed flat from the
front, icon-like composition, the object fills the center of the frame. The object is
centered with about 20 percent empty margin on all sides, nothing is cropped, square 1:1
composition. The object sits on a flat pure white background with hex color FFFFFF, with
no ground plane, no floor, no cast shadow, no contact shadow, no reflection, no gradient,
no vignette and no horizon. Single object only, no other objects, no lettering, no signs,
no numbers, no text, no watermark, no logo.
```

### 4.7 `tree_fir_snow.png`
Ёлка в снегу. **Требует правки кода** — сейчас дерево это плоский круг.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
fir tree with three stacked rounded cone tiers of deep pine green, each tier topped with a
soft cap of off-white snow with a pale blue-grey tint and a visible warm grey ambient
occlusion edge, on a short stubby brown trunk. Viewed from a three-quarter angle from
above at about 45 degrees. The object is centered with about 10 percent empty margin on
all sides, nothing is cropped, square 1:1 composition. The object sits on a flat pure
white background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

### 4.8 `prop_snowman.png`
Снеговик на обочине. Чистая декорация, награды не даёт.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A small
round clay snowman made of two stacked snowball spheres in off-white with a pale blue-grey
tint and a visible warm grey ambient occlusion edge, wearing a tipped-over red bucket as a
hat and a short teal knitted scarf, with two small dark twig arms. Viewed from a
three-quarter angle from above at about 45 degrees. The object is centered with about 10
percent empty margin on all sides, nothing is cropped, square 1:1 composition. The object
sits on a flat pure white background with hex color FFFFFF, with no ground plane, no
floor, no cast shadow, no contact shadow, no reflection, no gradient, no vignette and no
horizon. Single object only, no other objects, no lettering, no signs, no numbers, no
text, no watermark, no logo.
```

### 4.9 Погода (без спрайтов)
Снег делается правкой `updateWeather()`: тот же список капель, но скорость падения
`1500f * dt` заменить примерно на `250f * dt` и рисовать кружками вместо линий. Отдельные
ассеты не нужны.

---

## 5. L6 — «Пыльная жара»

**Тема.** Южный городок в полдень. Песочное поле, выгоревшие пастельные стены, терракота и
бирюза. Самый «тёплый» и контрастный уровень: тёмные объекты на светлой траве читаются
лучше всего, поэтому он идёт первым в списке на пилот.

**Палитра.** Трава `#e8c987`, акценты терракота `#c2410c` и бирюза `#2dd4bf`.

**Опасности нет** — белого на белом тут нет, это самый безопасный уровень для проверки
канона.

### 5.1 `house_adobe.png`
Побеленный глинобитный дом с плоской крышей и бирюзовой дверью.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A cozy
clay adobe house with cream stucco walls, a flat roof edged with a row of rounded
terracotta clay tiles, a turquoise wooden arched door, two small arched windows with white
frames, a short square chimney, and a terracotta pot with a small round cactus beside the
door. Viewed from an isometric three-quarter angle from above at about 35 degrees, so that
both the roof and the front facade are clearly visible. The object is centered with about
10 percent empty margin on all sides, nothing is cropped, square 1:1 composition. The
object sits on a flat pure white background with hex color FFFFFF, with no ground plane,
no floor, no cast shadow, no contact shadow, no reflection, no gradient, no vignette and
no horizon. Single object only, no other objects, no lettering, no signs, no numbers, no
text, no watermark, no logo.
```

### 5.2 `obs_cactus_cluster.png`
Три кактуса. Круглые колючки вместо острых — иначе сольётся в шум на 220 px.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A cluster
of three rounded clay cacti of different heights in soft sage green, each with short blunt
upward arms and two tiny pink flower buds on the very top. Viewed straight from above in a
top-down bird's-eye view at 90 degrees. The object is centered with about 10 percent empty
margin on all sides, nothing is cropped, square 1:1 composition. The object sits on a flat
pure white background with hex color FFFFFF, with no ground plane, no floor, no cast
shadow, no contact shadow, no reflection, no gradient, no vignette and no horizon. Single
object only, no other objects, no lettering, no signs, no numbers, no text, no watermark,
no logo.
```

### 5.3 `obs_suitcases.png`
Стопка чемоданов — намёк на «не доставленный багаж», вписывается в почтовую тему.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A stack
of two vintage clay suitcases, the lower one sun-faded warm brown and the upper one faded
teal, both with rounded corners, two pale straps and small brass-colored buckles, with a
short carrying handle on the very top. Viewed straight from above in a top-down bird's-eye
view at 90 degrees. The object is centered with about 10 percent empty margin on all
sides, nothing is cropped, square 1:1 composition. The object sits on a flat pure white
background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

### 5.4 `obs_road_cone.png`
Дорожный конус. Самый читаемый силуэт из всех препятствий — полезен как «честная»
препятствие-ловушка.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
roadwork cone in saturated orange with a single wide off-white stripe around its middle, a
rounded blunt tip and a thick square base. Viewed straight from above in a top-down bird's
eye view at 90 degrees. The object is centered with about 10 percent empty margin on all
sides, nothing is cropped, square 1:1 composition. The object sits on a flat pure white
background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

### 5.5 `car_desert_truck.png`
Движущаяся опасность: пикап с багажником.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A
slightly battered but cute clay pickup truck with faded turquoise bodywork, a rounded cab
with a pale windshield, a small wooden cargo rack over the bed, chunky dark grey tires and
two blunt amber indicators at the front. Viewed straight from above in a top-down bird's
eye view, the vehicle oriented vertically with its front pointing toward the bottom of the
frame. The object is centered with about 10 percent empty margin on all sides, nothing is
cropped, square 1:1 composition. The object sits on a flat pure white background with hex
color FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no
reflection, no gradient, no vignette and no horizon. Single object only, no other objects,
no lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 5.6 `heart_icecream.png`
Пикап «+1 жизнь»: мороженое. Спасение от жары.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
ice cream cone with a waffle-textured tan cone and a soft rounded vanilla scoop leaning
slightly to one side on top, with a small strawberry-pink drip running down one side of
the scoop. Viewed flat from the front, icon-like composition, the object fills the center
of the frame. The object is centered with about 20 percent empty margin on all sides,
nothing is cropped, square 1:1 composition. The object sits on a flat pure white
background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

### 5.7 `tree_cactus.png`
Высокий кактус-сагуаро. Требует правки кода (см. 4.7).

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A tall
clay saguaro cactus in soft sage green with one thick rounded trunk and two blunt upward
arms at different heights, a small pink bud on the very top, and a shallow ring of warm
brown stones at its base. Viewed from a three-quarter angle from above at about 45
degrees. The object is centered with about 10 percent empty margin on all sides, nothing
is cropped, square 1:1 composition. The object sits on a flat pure white background with
hex color FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no
reflection, no gradient, no vignette and no horizon. Single object only, no other objects,
no lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 5.8 `prop_well.png`
Адобовый колодец с навесом.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A low
clay adobe water well with a round cream stone rim, two short brown wooden posts and a
small pitched terracotta tiled roof between them, with a pale bucket hanging on a short
rope. Viewed from a three-quarter angle from above at about 45 degrees. The object is
centered with about 10 percent empty margin on all sides, nothing is cropped, square 1:1
composition. The object sits on a flat pure white background with hex color FFFFFF, with
no ground plane, no floor, no cast shadow, no contact shadow, no reflection, no gradient,
no vignette and no horizon. Single object only, no other objects, no lettering, no signs,
no numbers, no text, no watermark, no logo.
```

### 5.9 Погода
Пыльная марево-дельта: частицы `#e8c987`, дрейф вбок вместо падения. Спрайты не нужны.

---

## 6. L7 — «Морской бриз»

**Тема.** Набережная посёлка: светлая аква-трава, дощатые настилы, полосатые тенты. Самый
«игровой» уровень — краб вместо машины и спасательный буй вместо сердца.

**Палитра.** Трава `#a5f3fc`, акценты navy `#1e3a8a` и коралл `#f87171`.

**Опасность.** Белые полосы буйка и пена — снова белый на белом. У буя полосы сделаны
«pale cream», чтобы выжили при вырезании.

### 6.1 `house_beach.png`
Пляжный домик с полосатым тентом и террасой.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A cozy
clay beach cottage with pale cream wooden walls, a rounded powder-blue shingle roof, a
porch awning striped in navy and pale cream, two small square windows with white frames,
and a short front deck with three stubby steps. Viewed from an isometric three-quarter
angle from above at about 35 degrees, so that both the roof and the front facade are
clearly visible. The object is centered with about 10 percent empty margin on all sides,
nothing is cropped, square 1:1 composition. The object sits on a flat pure white
background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

### 6.2 `obs_crates_rope.png`
Стопка ящиков с верёвкой.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A stack
of two weathered clay cargo crates in warm tan with darker plank lines and rounded corners,
with a coil of pale cream rope resting on top of the upper crate. Viewed straight from
above in a top-down bird's-eye view at 90 degrees. The object is centered with about 10
percent empty margin on all sides, nothing is cropped, square 1:1 composition. The object
sits on a flat pure white background with hex color FFFFFF, with no ground plane, no
floor, no cast shadow, no contact shadow, no reflection, no gradient, no vignette and no
horizon. Single object only, no other objects, no lettering, no signs, no numbers, no
text, no watermark, no logo.
```

### 6.3 `obs_cooler.png`
Холодильный ящик. Прямоугольный силуэт контрастирует с круглыми объектами.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
cooler box with a rounded coral-red body, a pale cream lid with a short handle in the
middle, two small teal latches on the front edge and a subtle drain cap on one corner.
Viewed straight from above in a top-down bird's-eye view at 90 degrees. The object is
centered with about 10 percent empty margin on all sides, nothing is cropped, square 1:1
composition. The object sits on a flat pure white background with hex color FFFFFF, with
no ground plane, no floor, no cast shadow, no contact shadow, no reflection, no gradient,
no vignette and no horizon. Single object only, no other objects, no lettering, no signs,
no numbers, no text, no watermark, no logo.
```

### 6.4 `obs_beach_ball.png`
Мяч на дороге. Единственный объект, который «хочется» сделать катящимся.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A round
clay beach ball with soft wedge panels in coral red, pale cream, teal and warm yellow,
with a small dark seam line curving across it. Viewed straight from above in a top-down
bird's-eye view at 90 degrees. The object is centered with about 10 percent empty margin
on all sides, nothing is cropped, square 1:1 composition. The object sits on a flat pure
white background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

### 6.5 `car_crab.png`
Движущаяся опасность — краб. claws указывают вниз кадра, как фронт у машины.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A round
clay crab with a coral-red shell, eight short blunt legs spread out sideways and two large
rounded claws pointing forward, with two small dark stalked eyes. Viewed straight from
above in a top-down bird's-eye view, the creature oriented vertically with its claws
pointing toward the bottom of the frame. The object is centered with about 10 percent
empty margin on all sides, nothing is cropped, square 1:1 composition. The object sits on
a flat pure white background with hex color FFFFFF, with no ground plane, no floor, no
cast shadow, no contact shadow, no reflection, no gradient, no vignette and no horizon.
Single object only, no other objects, no lettering, no signs, no numbers, no text, no
watermark, no logo.
```

### 6.6 `heart_buoy.png`
Пикап «+1 жизнь»: спасательный круг. Лучшая семантическая замена сердца во всём наборе —
«спасение» читается без обучения игрока.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
lifebuoy ring with a slightly puffy rounded torus shape, alternating coral-red and pale
cream segments around the ring, a thin pale rope threaded along its outer edge and a small
dark hole in the middle. Viewed flat from the front, icon-like composition, the object
fills the center of the frame. The object is centered with about 20 percent empty margin
on all sides, nothing is cropped, square 1:1 composition. The object sits on a flat pure
white background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

### 6.7 `tree_palm.png`
Пальма. Требует правки кода.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A small
clay palm tree with a slightly curved warm tan trunk made of stacked rounded segments,
five thick blunt sage-green fronds spreading from the top and two small brown coconuts at
the crown. Viewed from a three-quarter angle from above at about 45 degrees. The object is
centered with about 10 percent empty margin on all sides, nothing is cropped, square 1:1
composition. The object sits on a flat pure white background with hex color FFFFFF, with
no ground plane, no floor, no cast shadow, no contact shadow, no reflection, no gradient,
no vignette and no horizon. Single object only, no other objects, no lettering, no signs,
no numbers, no text, no watermark, no logo.
```

### 6.8 `prop_deck_chair.png`
Лежак с полотенцем.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A small
clay deck chair with a pale cream wooden frame, a reclined seat made of navy and pale
cream stripes, and a short coral towel folded over one armrest. Viewed from a
three-quarter angle from above at about 45 degrees. The object is centered with about 10
percent empty margin on all sides, nothing is cropped, square 1:1 composition. The object
sits on a flat pure white background with hex color FFFFFF, with no ground plane, no
floor, no cast shadow, no contact shadow, no reflection, no gradient, no vignette and no
horizon. Single object only, no other objects, no lettering, no signs, no numbers, no
text, no watermark, no logo.
```

### 6.9 Погода
Чайки: 2–3 маленьких спрайта, летящих через экран по горизонтали. Если делать не хочется —
уровень работает и без погоды.

---

## 7. L8 — «Ночной карнавал»

**Тема.** Праздник в деревне после заката: глубокий индиго-поле, гирлянды, палатки. Самый
насыщенный уровень и самый рискованный — его надо делать последним.

**Палитра.** Трава `#312e81` (отличается от существующей ночной `#1e1b4b`), акценты фуксия
`#d946ef`, циан `#22d3ee`, золото.

**Опасность — glow.** Светящиеся части на белом фоне теряются и при вырезании, и при
наложении на тёмную траву. Поэтому во всех промптах уровня лампы и огоньки — **плотные
непрозрачные цветные шарики**, а bloom домешивается кодом, если вообще понадобится.

### 7.1 `house_caravan.png`
Карнавальный фургон-палатка.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
circus caravan wagon with a rounded fuchsia body, a cream and coral striped awning along
the roof edge, a small round window with a pale cream frame, a short door step, two chunky
dark wheels and a row of small solid opaque round bulbs in warm yellow along the awning
edge. Viewed from an isometric three-quarter angle from above at about 35 degrees, so that
both the roof and the front facade are clearly visible. The object is centered with about
10 percent empty margin on all sides, nothing is cropped, square 1:1 composition. The
object sits on a flat pure white background with hex color FFFFFF, with no ground plane,
no floor, no cast shadow, no contact shadow, no reflection, no gradient, no vignette and
no horizon. Single object only, no other objects, no lettering, no signs, no numbers, no
text, no watermark, no logo.
```

### 7.2 `obs_gift_boxes.png`
Стопка подарков с бантами.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A tower
of three wrapped clay gift boxes of decreasing size, the bottom box teal with a cream
ribbon, the middle box fuchsia with a gold ribbon and the top box warm yellow with a coral
ribbon, each ribbon tied into two soft rounded loops. Viewed straight from above in a
top-down bird's-eye view at 90 degrees. The object is centered with about 10 percent empty
margin on all sides, nothing is cropped, square 1:1 composition. The object sits on a flat
pure white background with hex color FFFFFF, with no ground plane, no floor, no cast
shadow, no contact shadow, no reflection, no gradient, no vignette and no horizon. Single
object only, no other objects, no lettering, no signs, no numbers, no text, no watermark,
no logo.
```

### 7.3 `obs_candy_floss.png`
Прилавок с сахарной ватой.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A small
clay candy floss cart with a rounded cream body, a wide shallow pink basin on top and a
big soft fluffy cloud of pink spun sugar rising above it, with a short handle sticking out
to one side. Viewed straight from above in a top-down bird's-eye view at 90 degrees. The
object is centered with about 10 percent empty margin on all sides, nothing is cropped,
square 1:1 composition. The object sits on a flat pure white background with hex color
FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no reflection,
no gradient, no vignette and no horizon. Single object only, no other objects, no
lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 7.4 `obs_banana_peel.png`
Банановая кожура — классическая ловушка, единственный объект уровня без «карнавального»
антуража, поэтому хорошо разбавляет пёстрое поле.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
banana peel lying open on the ground with three thick blunt strips splaying outward from a
short central stub, warm yellow on the outside and pale cream on the inside, with a small
brown tip on one strip. Viewed straight from above in a top-down bird's-eye view at 90
degrees. The object is centered with about 10 percent empty margin on all sides, nothing
is cropped, square 1:1 composition. The object sits on a flat pure white background with
hex color FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no
reflection, no gradient, no vignette and no horizon. Single object only, no other objects,
no lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 7.5 `car_drone.png`
Движущаяся опасность: дрон. Лучший кандидат на «летает быстрее машин».

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A small
clay quadcopter drone with a rounded dark indigo body, four short blunt arms ending in
flat rounded rotor discs, a small camera ball under the body and four solid opaque round
cyan lights, one on each arm tip. Viewed straight from above in a top-down bird's-eye view
at 90 degrees, the drone oriented vertically with its front pointing toward the bottom of
the frame. The object is centered with about 10 percent empty margin on all sides, nothing
is cropped, square 1:1 composition. The object sits on a flat pure white background with
hex color FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no
reflection, no gradient, no vignette and no horizon. Single object only, no other objects,
no lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 7.6 `heart_balloons.png`
Пикап «+1 жизнь»: связка шаров. Работает и как «летишь выше», и как награда за праздник.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A bunch
of five round clay balloons in fuchsia, cyan, warm yellow, coral and mint, clustered side
by side and tied together with a single pale cream ribbon gathered at the bottom. Viewed
flat from the front, icon-like composition, the object fills the center of the frame. The
object is centered with about 20 percent empty margin on all sides, nothing is cropped,
square 1:1 composition. The object sits on a flat pure white background with hex color
FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no reflection,
no gradient, no vignette and no horizon. Single object only, no other objects, no
lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 7.7 `tree_bulb_pole.png`
Столб с гирляндой. Требует правки кода.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A clay
festival pole of dark indigo wood with two short crossbars near the top, from which a
draped string of five solid opaque round bulbs hangs in warm yellow, coral, fuchsia, cyan
and mint. Viewed from a three-quarter angle from above at about 45 degrees. The object is
centered with about 10 percent empty margin on all sides, nothing is cropped, square 1:1
composition. The object sits on a flat pure white background with hex color FFFFFF, with
no ground plane, no floor, no cast shadow, no contact shadow, no reflection, no gradient,
no vignette and no horizon. Single object only, no other objects, no lettering, no signs,
no numbers, no text, no watermark, no logo.
```

### 7.8 `prop_popcorn_cart.png`
Попкорн-тележка.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A small
clay popcorn cart with a rounded red and cream striped body, a glass-front kettle on top
filled with pale yellow popcorn, a short push handle at the back and two small dark wheels.
Viewed from a three-quarter angle from above at about 45 degrees. The object is centered
with about 10 percent empty margin on all sides, nothing is cropped, square 1:1
composition. The object sits on a flat pure white background with hex color FFFFFF, with
no ground plane, no floor, no cast shadow, no contact shadow, no reflection, no gradient,
no vignette and no horizon. Single object only, no other objects, no lettering, no signs,
no numbers, no text, no watermark, no logo.
```

### 7.9 Погода
Конфетти: переиспользовать `rainDrops`, но рисовать короткие прямоугольники случайного
цвета из палитры уровня и медленным падением.

---

## 8. Вне уровней: привести старые ассеты к канону

Эти четыре — не новые уровни, а исправление уже существующего визуала. Сделай их до
остального, иначе новые уровни будут стоять рядом с фоторжавчиной и глянцевым золотом.

### 8.1 `obs_car_rusty.png` (замена `obs_car.png`)
Сейчас — фотореалистичная ржавая машина с треснувшим лобовым и паутиной. Нужна та же
машина, но из глины.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A small
vintage clay car with a rounded beetle-like body in muted brick red, a pale curved
windshield, two big round cream headlights, a short horizontal grille and chunky dark grey
tires, with a couple of soft lighter scuff patches suggesting it is old but never dirty.
Viewed straight from above in a top-down bird's-eye view, the vehicle oriented vertically
with its front pointing toward the bottom of the frame. The object is centered with about
10 percent empty margin on all sides, nothing is cropped, square 1:1 composition. The
object sits on a flat pure white background with hex color FFFFFF, with no ground plane,
no floor, no cast shadow, no contact shadow, no reflection, no gradient, no vignette and
no horizon. Single object only, no other objects, no lettering, no signs, no numbers, no
text, no watermark, no logo.
```

### 8.2 `coin_clay.png` (замена `coin.png`)
Сейчас — полированное золото с бликами. Нужна матовая глиняная монета.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A round
clay coin in warm matte gold-yellow with a soft rounded raised rim, a slightly pressed
plain circle in the middle and a small embossed five-pointed star in the center, its
surface showing gentle finger-pressed clay texture rather than shine. Viewed flat from the
front, icon-like composition, the object fills the center of the frame. The object is
centered with about 20 percent empty margin on all sides, nothing is cropped, square 1:1
composition. The object sits on a flat pure white background with hex color FFFFFF, with
no ground plane, no floor, no cast shadow, no contact shadow, no reflection, no gradient,
no vignette and no horizon. Single object only, no other objects, no lettering, no signs,
no numbers, no text, no watermark, no logo.
```

### 8.3 `obs_barrel_clay.png` (замена `obs_barrel.png`)
Сейчас — глянцевый металлический кег с выдавленной буквой «M» (и это ещё и чужая
торговая маркировка). Нужен бочонок без надписей.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A short
round clay barrel lying on its side, warm tan with two soft grey hoops around its body,
three short plank lines across the top and a rounded flat end cap, with a completely blank
top surface. Viewed straight from above in a top-down bird's-eye view at 90 degrees. The
object is centered with about 10 percent empty margin on all sides, nothing is cropped,
square 1:1 composition. The object sits on a flat pure white background with hex color
FFFFFF, with no ground plane, no floor, no cast shadow, no contact shadow, no reflection,
no gradient, no vignette and no horizon. Single object only, no other objects, no
lettering, no signs, no numbers, no text, no watermark, no logo.
```

### 8.4 `obs_hole_clay.png` (замена `obs_hole.png`)
Сейчас — плоская 2D-иллюстрация, единственный ассет в проекте не-3D. Нужна та же яма, но
объектом из глины.

```
3D render of a single isolated game asset. The object is sculpted from matte polymer clay
with a soft plasticine surface, rounded chunky toy-like proportions, gentle ambient
occlusion in the crevices, and smooth diffuse studio lighting coming from the upper left.
Cozy handcrafted children's game art direction, pastel base colors with one saturated
accent color, crisp readable silhouette that still reads when shrunk very small. A round
clay pothole seen from above, with a thick raised broken rim in warm grey and a deep
shaded hollow in the center fading from slate grey to dark charcoal, three small blunt
chips of clay lying beside the rim. Viewed straight from above in a top-down bird's-eye
view at 90 degrees. The object is centered with about 10 percent empty margin on all
sides, nothing is cropped, square 1:1 composition. The object sits on a flat pure white
background with hex color FFFFFF, with no ground plane, no floor, no cast shadow, no
contact shadow, no reflection, no gradient, no vignette and no horizon. Single object
only, no other objects, no lettering, no signs, no numbers, no text, no watermark, no
logo.
```

---

## 9. Как это потом подключается (код сейчас не тронут)

Порядок, в котором это вообще начнёт работать:

1. `levelColors` в `GameView.kt` — добавить цвета тем. Сейчас там 4 значения, и
   `changeLevel()` берёт `(deliveredCount / 20) % levelColors.size`, так что новые темы
   подхватятся автоматически.
2. `loadBitmaps()` — добавить ключи `house_winter`, `obs_snowbank`, `car_snowplow` и т.д.
   Сейчас карта жёстко заполнена 9 ключами.
3. Погода завязана на `if (grassColor == levelColors[2])` — то есть дождь привязан к
   **индексу** через сравнение цвета. Это хрупко: если вставить тему перед ночной, дождь
   переедет на неё. Правильно — хранить текущий индекс уровня в поле и сравнивать его.
4. Тип препятствия выбирается жёстко: `if (Random().nextBoolean()) "obs_car" else
   "obs_box"`. Чтобы на уровне был свой набор, нужен список типов темы и выбор из него.
5. `drawTree()` рисует круг кодом — чтобы появились `tree_*.png`, нужен `drawEntity` для
   дерева вместо `drawCircle`.
6. Пикап жизни рисуются под ключом `heart` независимо от темы — для `heart_scarf` и
   остальных нужен выбор ключа по текущей теме.

Пункты 3–6 — это уже задача на change-предложение в OpenSpec, а не на промпты.

---

## 10. Чек-лист приёмки каждого файла

- [ ] Белый фон вырезан, на прозрачном шахматном фоне **нет** съевшихся светлых областей
      (критично для зимы и морского буя).
- [ ] Под объектом **нет** тени из ассета — движок рисует свою через `drawShadow`, иначе
      будет двойная неподвижная тень.
- [ ] Направление света совпадает с остальными: `ANCHOR` везде один и тот же, свет слева
      сверху.
- [ ] Силуэт читается на 100×100 (пикапы рисуются ровно в 100 px; проверь уменьшением
      превью, а не «на глаз в оригинале»).
- [ ] Внутри объекта нет букв, цифр и логотипов.
- [ ] Даунскейл до 512 px по длинной стороне перед укладкой в `assets/img/`.
- [ ] Имя файла точно по таблице имён, иначе `loadBitmaps` его не найдёт.
