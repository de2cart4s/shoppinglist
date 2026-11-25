# Список покупок (Shopping List)

Android-приложение для ведения списка покупок с контролем расходов: товары группируются по категориям, отмечаются галочкой «куплено», а внизу экрана всегда видны суммы «В корзине» и «Всего».

Проект написан на **Kotlin + Jetpack Compose (Material 3)** по архитектуре **MVVM**, данные хранятся локально в **SQLite**. Без интернета, без сторонних библиотек кроме AndroidX.

---

## Содержание

- [Возможности](#возможности)
- [Стек технологий](#стек-технологий)
- [Архитектура](#архитектура)
- [Структура проекта](#структура-проекта)
- [База данных](#база-данных)
  - [Где находится файл БД](#где-находится-файл-бд)
  - [Как посмотреть содержимое](#как-посмотреть-содержимое)
  - [Схема таблиц](#схема-таблиц)
  - [Версии и миграции](#версии-и-миграции)
  - [API класса ShoppingDatabaseHelper](#api-класса-shoppingdatabasehelper)
- [Хранилище настроек](#хранилище-настроек)
- [Как это работает](#как-это-работает)
- [Детали реализации](#детали-реализации)
- [Сборка и запуск](#сборка-и-запуск)
- [Известные ограничения](#известные-ограничения)
- [Планы по развитию](#планы-по-развитию)
- [Лицензия](#лицензия)

---

## Возможности

| Функция | Описание |
|---|---|
| Добавление товара | Bottom sheet: название, категория, единица измерения, количество (степпер −/+), цена |
| Редактирование | Тап по карточке товара открывает тот же sheet в режиме редактирования |
| Отметка «куплено» | Чекбокс зачёркивает название и переносит сумму в строку «В корзине» |
| Категории | Предустановленные (Овощи, Молочное, Мясо, Бытовое) + создание своих категорий |
| Фильтр | Горизонтальная лента чипов: «Все» или конкретная категория |
| Итоги | Нижняя панель: сумма отмеченных товаров и общая сумма списка |
| Удаление с отменой | Snackbar «Товар удалён» + кнопка **Отменить** (одноуровневый Undo) |
| Очистка купленного | Одна SQL-команда удаляет все отмеченные товары (с диалогом подтверждения) |
| Поделиться | Готовый текст списка: копирование в буфер обмена или отправка в мессенджер |
| Тёмная тема | Ручное переключение, выбор сохраняется между запусками |
| Empty state | Отдельный экран-приглашение, когда список пуст |
| Форматирование | Рубли с разделением тысяч (`1 234 567 ₽`) и правильное склонение («1 товар / 2 товара / 5 товаров») |

---

## Стек технологий

| Компонент | Технология |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Архитектура | MVVM (ViewModel + единый UiState) |
| Асинхронность | Kotlin Coroutines, `viewModelScope`, `Dispatchers.IO` |
| Реактивное состояние | `StateFlow` / `MutableStateFlow` + `collectAsState()` |
| Хранение данных | SQLite через `SQLiteOpenHelper` (без Room) |
| Настройки | `SharedPreferences` |
| Зависимости | AndroidX Activity Compose, Lifecycle ViewModel Compose, Material 3, Material Icons Extended |

> Точные версии SDK и библиотек — в `app/build.gradle.kts` и `gradle/libs.versions.toml`.

---

## Архитектура

Однонаправленный поток данных: **UI только читает состояние и вызывает методы ViewModel**, ViewModel пишет в БД и перечитывает её, обновляя `StateFlow`.

```
┌──────────────────────────────────────────────┐
│  UI — MainActivity.kt (Compose)              │
│  Scaffold · LazyColumn · BottomSheets        │
└───────────────┬──────────────────────────────┘
                │ вызовы: addItem(), toggleChecked()…
                ▼
┌──────────────────────────────────────────────┐
│  ShoppingViewModel (AndroidViewModel)        │
│  ShoppingUiState ← единый источник состояния │
└───────────────┬──────────────────────────────┘
                │ CRUD на Dispatchers.IO
                ▼
┌──────────────────────────────────────────────┐
│  ShoppingDatabaseHelper (SQLiteOpenHelper)   │
│  shopping_list.db → items, categories        │
└──────────────────────────────────────────────┘

Обратно: StateFlow → collectAsState() → рекомпозиция UI
```

**Правила, которых придерживается проект:**

1. UI не знает о существовании БД — только `ShoppingUiState` и лямбды-колбэки.
2. После **любой** записи вызывается `loadData()` — БД всегда «источник правды», рассинхрон невозможен.
3. Все дисковые операции — только на `Dispatchers.IO` (никакого ANR).
4. Модели иммутабельны (`data class` + `copy()`), что корректно работает с Compose.

---

## Структура проекта

```
app/src/main/java/com/example/shoppinglist/
├── MainActivity.kt              # Весь UI: тема, Scaffold, список, шиты, диалоги
├── ShoppingViewModel.kt         # ShoppingUiState + бизнес-логика + связь с БД
├── ShoppingDatabaseHelper.kt    # SQLite: создание таблиц, миграции, CRUD
└── ShoppingModel.kt             # ShoppingItem, ShoppingDefaults, ShoppingCalculator
```

| Файл | Ответственность |
|---|---|
| `ShoppingModel.kt` | Модель `ShoppingItem`, справочники категорий/единиц, расчёт сумм, форматирование рублей, склонение, генерация текста для «Поделиться» |
| `ShoppingDatabaseHelper.kt` | Создание и апгрейд схемы, чтение/запись товаров и категорий |
| `ShoppingViewModel.kt` | `ShoppingUiState`, `StateFlow`, CRUD-обёртки в корутинах, Undo, фильтр, тема |
| `MainActivity.kt` | Палитры Light/Dark, `ShoppingApp`, `CategoryFilterRow`, `ShoppingListContent`, `ShoppingItemCard`, `SummaryBottomBar`, `EmptyShoppingState`, `ItemBottomSheet`, `ShareBottomSheet`, `AddCategoryDialog` |

---

## База данных

Используется **встроенная в Android SQLite** через `SQLiteOpenHelper` — без Room и без сторонних ORM.

```kotlin
class ShoppingDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "shopping_list.db", null, 2)
//                            ↑ имя файла        ↑ factory  ↑ версия схемы
```

### Где находится файл БД

Файл создаётся в **приватном каталоге приложения** на устройстве:

```
/data/data/com.example.shoppinglist/databases/shopping_list.db
```

Эквивалентный путь на современных версиях Android:

```
/data/user/0/com.example.shoppinglist/databases/shopping_list.db
```

Рядом могут лежать служебные файлы SQLite: `shopping_list.db-journal`, а при включённом WAL — `shopping_list.db-wal` и `shopping_list.db-shm`.

**Важно знать:**

- БД **не** создаётся при установке APK. Она появляется лениво — при первом обращении к `readableDatabase` / `writableDatabase`, то есть когда `ShoppingViewModel.init` вызывает `loadData()`.
- Каталог приватный: без root или debug-сборки посторонние приложения (и файловые менеджеры) туда не попадут.
- Файл удаляется при удалении приложения или через *Настройки → Приложения → Хранилище → Очистить данные*.
- БД одна на всё приложение; `SQLiteOpenHelper` кэширует соединение, закрывать его вручную не требуется.
- Товары **не** синхронизируются между устройствами — всё локально.

### Как посмотреть содержимое

**Способ 1 — Database Inspector (проще всего)**

Android Studio → **View → Tool Windows → App Inspection** (в старых версиях — *Database Inspector*) → выбрать запущенный процесс → раскрыть `shopping_list.db`. Позволяет смотреть таблицы и выполнять SQL «живьём». Требования: debug-сборка и **API 26+**.

**Способ 2 — Device Explorer**

**View → Tool Windows → Device Explorer** → путь `/data/data/com.example.shoppinglist/databases/`. Полный доступ есть на эмуляторе (не Google Play-образ) и на rooted-устройстве; на обычном релизном устройстве каталог закрыт.

**Способ 3 — adb + `run-as` (для debug-сборки, работает и на реальном устройстве)**

```bash
# список файлов БД
adb shell run-as com.example.shoppinglist ls -l databases

# вытащить файл на компьютер
adb exec-out run-as com.example.shoppinglist cat databases/shopping_list.db > shopping_list.db

# запрос прямо на устройстве (если в системе есть sqlite3)
adb shell run-as com.example.shoppinglist \
  sqlite3 databases/shopping_list.db "SELECT id, name, price, is_checked FROM items;"
```

Вытянутый файл открывается в **DB Browser for SQLite**, **DBeaver** или консольным `sqlite3`.

### Схема таблиц

#### `items` — товары списка

```sql
CREATE TABLE items (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL,
    category   TEXT    NOT NULL,
    quantity   INTEGER NOT NULL,
    unit       TEXT    NOT NULL,
    price      INTEGER NOT NULL,
    is_checked INTEGER NOT NULL
);
```

| Колонка | Тип | Kotlin-поле | Назначение |
|---|---|---|---|
| `id` | INTEGER PK AI | `id: Long` | Идентификатор; `0` у ещё не сохранённого товара |
| `name` | TEXT | `name: String` | Название («Молоко 2,5%») |
| `category` | TEXT | `category: String` | Название категории **строкой**, без внешнего ключа |
| `quantity` | INTEGER | `quantity: Int` | Количество (минимум 1) |
| `unit` | TEXT | `unit: String` | Единица измерения (`шт.`, `кг`, `г`, `л`, `уп.`) |
| `price` | INTEGER | `price: Int` | Цена в **целых рублях**, без копеек |
| `is_checked` | INTEGER | `isChecked: Boolean` | Флаг «куплено»: `0` / `1` (в SQLite нет BOOLEAN) |

#### `categories` — справочник категорий

```sql
CREATE TABLE IF NOT EXISTS categories (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL
);
```

`UNIQUE` защищает от дублей, а вставка выполняется с `CONFLICT_IGNORE` — повторное добавление существующей категории молча игнорируется, без исключения.

Стартовый набор засевается в `createCategoriesTable()`: **Овощи, Молочное, Мясо, Бытовое**.

#### Особенности схемы

- Порядок выборки — `ORDER BY id ASC`, то есть порядок добавления (новые товары в конце).
- Внешних ключей нет: `items.category` — обычный текст. Переименование или удаление категории **не** затрагивает товары.
- Индексов, кроме PK и UNIQUE, нет — для десятков записей это не проблема.
- `getAllCategories()` возвращает `ShoppingDefaults.categories`, если таблица пуста (страховка).

### Версии и миграции

| Версия | Что содержит | Миграция |
|---|---|---|
| 1 | только `items` | — |
| 2 | `items` + `categories` (со стартовым набором) | `onUpgrade`: при `oldVersion < 2` вызывается `createCategoriesTable(db)` |

```kotlin
override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) createCategoriesTable(db)
}
```

Пользовательские товары при обновлении **сохраняются**: миграция только досоздаёт новую таблицу (`CREATE TABLE IF NOT EXISTS`), старые данные не пересоздаются и не удаляются.

**Внимание:** понижение версии БД не поддерживается (`onDowngrade` не переопределён) — установка старой версии поверх новой приведёт к исключению. Для отката нужно очистить данные приложения.

### API класса ShoppingDatabaseHelper

| Метод | Действие | Возвращает |
|---|---|---|
| `getAllItems()` | `SELECT * FROM items ORDER BY id ASC` | `List<ShoppingItem>` |
| `insertItem(item)` | `INSERT INTO items` | `Long` — новый `id` (или `-1` при ошибке) |
| `updateItem(item)` | `UPDATE items … WHERE id = ?` | — |
| `updateChecked(id, isChecked)` | точечный `UPDATE` только флага | — |
| `deleteItem(id)` | `DELETE FROM items WHERE id = ?` | — |
| `deleteCheckedItems()` | `DELETE FROM items WHERE is_checked = 1` | — |
| `getAllCategories()` | `SELECT name FROM categories ORDER BY id ASC` | `List<String>` |
| `insertCategory(name)` | `INSERT … CONFLICT_IGNORE`, имя тримится | — |

Все параметры передаются через `ContentValues` и `selectionArgs` (`?`) — **SQL-инъекции невозможны**. Курсоры закрываются автоматически благодаря `.use { }`.

---

## Хранилище настроек

Тема хранится отдельно от БД — в `SharedPreferences`:

```
/data/data/com.example.shoppinglist/shared_prefs/shopping_prefs.xml
```

| Файл | Ключ | Тип | Значение по умолчанию |
|---|---|---|---|
| `shopping_prefs` | `dark_theme` | Boolean | `false` (светлая тема) |

Запись через `apply()` — асинхронно, без блокировки UI. Флаг читается один раз при создании ViewModel, поэтому тема переживает перезапуск приложения. Системной теме приложение **не** следует — переключение только вручную кнопкой в топбаре.

---

## Как это работает

### Добавление товара

1. Тап по **FAB** → `editingItem = null`, `showItemSheet = true` → открывается `ItemBottomSheet` в режиме «Новый товар».
2. Ввод данных, валидация: название не пустое, цена — только цифры и `>= 0`.
3. `onSave(...)` → `viewModel.addItem(...)`.
4. ViewModel: `viewModelScope.launch` → `withContext(Dispatchers.IO) { db.insertItem(item) }` → `loadData()`.
5. `loadData()` читает товары и категории → `_uiState.update { it.copy(...) }`.
6. `StateFlow` эмитит новое состояние → `collectAsState()` → Compose перерисовывает список, счётчики и суммы.

### Удаление с отменой

```kotlin
viewModel.deleteItem(item)                 // товар запоминается в lastDeletedItem
scope.launch {
    snackbarHostState.currentSnackbarData?.dismiss()
    val res = snackbarHostState.showSnackbar("Товар удалён", actionLabel = "Отменить", …)
    if (res == SnackbarResult.ActionPerformed) viewModel.undoDelete()
}
```

`showSnackbar` — **suspend-функция**: корутина ждёт, пока snackbar исчезнет или будет нажата «Отменить». Undo одноуровневый: запоминается только последний удалённый товар.

### Фильтр по категории

`selectCategory()` меняет только состояние — **БД не трогается**. Список пересчитывается вычисляемым свойством:

```kotlin
val filteredItems: List<ShoppingItem>
    get() = if (selectedCategory == null) items else items.filter { it.category == selectedCategory }
```

### Поделиться списком

`ShoppingCalculator.buildShareText(items)` собирает текст по **всему** списку (не по фильтру), с группировкой по категориям, отметками `[x]` / `[ ]` и итогом. Далее — `LocalClipboardManager.setText(...)` либо `Intent.ACTION_SEND` + `createChooser`.

---

## Детали реализации

**Единое состояние экрана.** Весь экран описан одним `ShoppingUiState`; суммы и счётчики — вычисляемые свойства, а не отдельные поля. Дублировать данные и синхронизировать их не нужно.

**Инкапсуляция потока.**

```kotlin
private val _uiState = MutableStateFlow(...)          // изменяемый, приватный
val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()  // read-only для UI
```

**Стабильные ключи в `LazyColumn`** — `key = { it.id }` для товаров и `"header_$category"` для заголовков. Compose корректно переиспользует элементы при удалении/вставке, а не пересоздаёт весь список.

**Форматирование рублей без `String.format`** — группировка по три цифры справа через двойной разворот строки:

```kotlin
str.reversed().chunked(3).joinToString(" ").reversed()
// 1234567 → "7654321" → ["765","432","1"] → "765 432 1" → "1 234 567 ₽"
```

**Русское склонение** с учётом исключения 11–19:

```kotlin
mod100 in 11..19 -> "товаров"
mod10 == 1       -> "товар"
mod10 in 2..4    -> "товара"
else             -> "товаров"
```

**Один sheet на два сценария.** `editingItem == null` → «Новый товар», иначе «Редактировать товар»; сохранение через `current.copy(...)`.

**Почему `AndroidViewModel`.** Нужен `Context` для создания БД и `SharedPreferences`; используется `Application`-контекст, поэтому утечек Activity при поворотах нет.

---

## Сборка и запуск

### Требования

- Android Studio (актуальная стабильная версия)
- JDK 17
- Android SDK — версии `compileSdk` / `minSdk` / `targetSdk` указаны в `app/build.gradle.kts`
- Устройство или эмулятор

### Шаги

```bash
# 1. Клонировать репозиторий
git clone <URL_репозитория>
cd ShoppingList

# 2. Открыть проект в Android Studio → дождаться Gradle Sync

# 3. Запустить: кнопка Run (Shift+F10)
```

Сборка из консоли:

```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/
./gradlew installDebug           # собрать и установить на подключённое устройство
./gradlew clean build            # полная пересборка
```

### Сброс данных

Если нужно начать с чистого списка (или откатить версию БД):

```bash
adb shell pm clear com.example.shoppinglist
```

Это удалит и `shopping_list.db`, и настройки темы.

---

## Известные ограничения

| # | Проблема | Где | Как исправить |
|---|---|---|---|
| 1 | **Пробелы в конце строк** справочников (`"шт. "`, `"Овощи "`) не совпадают с дефолтом модели `"шт."` → чип единицы не подсвечивается; возможны дубли категорий («Овощи» vs «Овощи ») | `ShoppingDefaults` | Убрать пробелы из строк |
| 2 | Итог считается как `sumOf { it.price }` — **количество не умножается на цену** | `ShoppingCalculator.totalAmount` | Либо `price * quantity`, либо переименовать поле в UI в «Стоимость, ₽» |
| 3 | Undo вставляет товар **без прежнего `id`** → он уезжает в конец списка | `undoDelete()` / `insertItem()` | Сохранять `id` при вставке или добавить колонку порядка |
| 4 | `remember` вместо `rememberSaveable` → при повороте экрана шиты/диалоги закрываются, введённый текст теряется | `MainActivity` | Заменить на `rememberSaveable` / вынести состояние в ViewModel |
| 5 | Возможные гонки: каждая операция — отдельная корутина + полный `loadData()`; `!item.isChecked` берётся из UI-состояния | `ShoppingViewModel` | Переключать флаг в SQL (`is_checked = 1 - is_checked`) или сериализовать операции |
| 6 | Чтение курсора по **жёстким индексам** колонок (`getLong(0)`, `getString(1)`…) | `getAllItems()` | `getColumnIndexOrThrow("name")` |
| 7 | Нет обработки ошибок БД (`insert` может вернуть `-1`) | `ShoppingDatabaseHelper` | Проверять результат и показывать ошибку в UI |
| 8 | Категории нельзя переименовать/удалить; связь с товарами — просто текст | схема БД | Внешний ключ `category_id` + каскадные операции |
| 9 | Цена только в целых рублях, `formatRubles` некорректен для отрицательных чисел | `ShoppingCalculator` | Хранить копейки (`Long`) и/или использовать `BigDecimal` |
| 10 | Полный перезагруз списка после каждого изменения | `loadData()` | При росте данных — Room + `Flow`, точечные обновления состояния |

---

## Планы по развитию

- [ ] Исправить пробелы в `ShoppingDefaults` и семантику «цена × количество»
- [ ] `rememberSaveable` для состояния шитов и диалогов
- [ ] Миграция на **Room** + `Flow` (реактивная БД, миграции, типобезопасные запросы)
- [ ] Переименование и удаление категорий, drag & drop порядка товаров
- [ ] Поиск по названию и сортировка (по цене / по алфавиту / купленные вниз)
- [ ] Несколько списков («Продукты», «Аптека», «Хозтовары»)
- [ ] Цена за единицу + подытог по позиции, поддержка копеек
- [ ] Экспорт/импорт списка (JSON, файл), синхронизация
- [ ] Unit-тесты `ShoppingCalculator` и UI-тесты Compose
- [ ] Следование системной теме + Material You (dynamic color)

---

## Лицензия

Проект распространяется под лицензией **MIT** — см. файл [LICENSE](LICENSE).

```
MIT License — свободное использование, копирование, изменение и распространение
при сохранении уведомления об авторских правах.
```
