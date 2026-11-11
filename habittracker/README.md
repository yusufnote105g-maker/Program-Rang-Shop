# HabitTracker (Android)

Локальный трекер привычек — учебный клон ядра Habitica. Kotlin + Jetpack Compose +
Room + Hilt. Без сервера, всё на устройстве.

Документы дизайна и разбора лежат в соседней папке `../replica/`:
`recon.md`, `architecture.md`, `schema.sql`, `design/`, `features.csv`,
`parity.md`.

## Стек

| слой | выбор |
| --- | --- |
| язык | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| архитектура | MVVM + UDF, `StateFlow` |
| БД | Room (SQLite), offline-first |
| DI | Hilt |
| асинхронность | Coroutines + Flow |
| фоновые задачи | WorkManager (ежедневный сброс) + AlarmManager (напоминания) |
| время | java.time (minSdk 26) |
| сборка | AGP 8.7, KSP |

## Структура

```
app/src/main/java/com/example/habittracker/
├── HabitApp.kt            @HiltAndroidApp, канал уведомлений, восстановление будильников
├── MainActivity.kt        точка входа, HabitTrackerTheme
├── data/
│   ├── local/
│   │   ├── entity/        UserEntity, TaskEntity, TagEntity, TaskTagEntity, TaskLogEntity
│   │   ├── dao/           UserDao, TaskDao, TagDao, TaskLogDao, StatsRows
│   │   ├── AppDatabase.kt
│   │   └── Mappers.kt     entity <-> domain
│   ├── repository/        TaskRepository (транзакции, награды, теги, аналитика, виджет)
│   └── di/                DatabaseModule (Room, DAO, Clock), BackgroundEntryPoint
├── domain/
│   ├── model/             TaskType, Difficulty, Task, Tag, RewardResult, UserStats,
│   │                      DailyResetReport, AnalyticsData, WidgetSnapshot, ReminderFiring
│   ├── cron/              CronEngine (календарь: due-дни, streak, граница дня)
│   ├── reward/            RewardEngine (математика наград)
│   ├── tags/              TagRules (лимиты) + TaskFilters (фильтр/группировка)
│   ├── reminder/          ReminderTime (чистая арифметика времени)
│   └── analytics/         buildDailySeries (заполнение пропущенных дней)
├── work/                  DailyResetWorker + Scheduler; ReminderScheduler,
│                          ReminderReceiver, ReminderNotifications, BootReceiver
├── widget/                TodayWidgetProvider, WidgetUpdater
├── navigation/            AppNavHost, Routes
└── ui/
    ├── theme/             Color, Type, Shape, Dimens, Theme (из replica/design)
    ├── components/        TaskRow, CounterButton, CounterBadge, AvatarHeader,
    │                      EmptyState, TaskListSkeleton, TagDot, ConfirmDeleteDialog,
    │                      MoveTaskDialog
    └── screens/
        ├── tasks/         S01 TasksScreen + TasksViewModel
        ├── taskform/      S02/S06 TaskFormScreen + TaskFormViewModel
        └── analytics/     S09 AnalyticsScreen + AnalyticsViewModel
```

Тесты:

```
app/src/test/java/.../domain/            CronEngineTest, RewardEngineTest, TagRulesTest,
                                         ReminderTimeTest, TaskFiltersTest, AnalyticsSeriesTest (JVM)
app/src/androidTest/java/.../data/       TaskRepositoryTest (Room in-memory, 27 тестов)
```

## Сборка

В этом окружении JDK/Gradle/Android SDK нет, поэтому APK здесь не собирается.
Откройте папку `habittracker/` в Android Studio (Ladybug 2024.2.1 или новее,
JDK 17) или соберите из терминала:

```bash
cd habittracker
./gradlew :app:assembleDebug
```

Готовый APK: `app/build/outputs/apk/debug/app-debug.apk`.

Gradle wrapper 8.9 уже в комплекте (`gradlew`, `gradlew.bat`,
`gradle/wrapper/`), отдельно устанавливать Gradle не нужно — скрипт сам
скачает дистрибутив при первом запуске. Для release-сборки понадобится
свой keystore и `signingConfig`; debug-сборка работает без настроек.

### GitHub Actions

APK можно собрать без локального Android SDK:

1. Запушьте содержимое архива в GitHub-репозиторий.
2. В workflow установите JDK 17 (`actions/setup-java@v4`, Temurin) и
   Android SDK (`android-actions/setup-android@v3`, `platforms;android-35`,
   `build-tools;35.0.0`).
3. Запустите:
   ```bash
   cd habittracker
   ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon
   ```
4. Заберите `app/build/outputs/apk/debug/app-debug.apk` через
   `actions/upload-artifact@v4`.

Локальная сборка на телефоне не выполнялась — в этом окружении нет JDK и
Android SDK; это ограничение среды, а не проекта.

## Что реализовано

- **S01** — список задач: табы «Привычки / Ежедневные / Разовые», шапка игрока
  (уровень/опыт/HP/золото), FAB, пустое состояние, snackbar с наградой, теги в строке.
- **S01: фильтр и группировка** — multi-select чипы тегов над списком
  (OR-семантика), переключатель «По тегам». Задача с несколькими тегами видна
  в каждой группе, «Без тега» — последняя. Пустой результат фильтра объясняется
  и сбрасывается одной кнопкой.
- **S02** — создание: название (валидация), заметки, тип, сложность, теги,
  напоминание; сохранение в Room.
- **S04** — строка привычки со счётчиками «+N / −M» за сегодня и меню «⋮».
- **S06** — редактирование: тап по строке или «⋮» → Edit; те же поля, что в S02,
  тип после создания read-only, удаление с подтверждением.
- **S08** — меню задачи: «⋮» → Edit / Duplicate / Move / Delete.
  Duplicate копирует теги и напоминание, но сбрасывает прогресс; Move меняет тип,
  сбрасывает тип-специфичные поля и сохраняет историю `task_logs`.
- **S09** — аналитика: 30 дней из `task_logs`, график за 14 дней, сводка
  отметок/пропусков/XP/золота, топ серий и распределение по типам.
- **Напоминания** — чекбокс и выбор времени в S02/S06. `AlarmManager` +
  `ReminderReceiver` + `BootReceiver`: habits/dailies перепланируются на
  следующий день, todo срабатывает один раз. На Android 13+ запрашивается
  `POST_NOTIFICATIONS`.
- **Виджет** — `TodayWidgetProvider` на `RemoteViews`: уровень/XP/золото,
  сводка «привычки / ежедневные / разовые», лучшая серия. Обновляется по тапу
  и после изменений в приложении.
- **Теги** — чипы существующих тегов и создание новых на лету; лимит 8 тегов,
  длина 24 символа, сравнение без учёта регистра; цвет стабилен для имени.
- **F06** — ежедневный сброс: `DailyResetWorker` на границе дня пользователя,
  сброс серий за пропуски, штраф по HP, обнуление счётчиков; идемпотентно по
  `users.last_cron_day` и с догоном при каждом старте приложения.
- **Room** — 5 таблиц из `replica/schema.sql`; транзакционные отметки, награды,
  журнал, теги.
- **Дизайн-токены** — светлая/тёмная темы M3, отступы 4dp, формы M3.

## Как работает день и серия

- «Сегодня» считается в зоне пользователя и с учётом `day_start_minutes` (граница дня).
- Отметка ежедневной задачи продлевает серию, только если предыдущий **нужный** день был
  закрыт; иначе серия начинается заново с 1 (`CronEngine.streakAfterCompletion`).
- Пропущенная нужная задача на сбросе: серия → 0, списание HP по сложности.
- `DailyResetWorker` запускается раз в сутки со сдвигом на границу дня; WorkManager не
  гарантирует точность, поэтому сброс идемпотентен и догоняется при запуске приложения.

## Напоминания и виджет

- Время напоминания хранится в `tasks.reminder_at` (epoch millis), включение —
  в `tasks.reminder_enabled`. Схема не менялась.
- `ReminderScheduler` использует точный будильник, если система разрешает
  `SCHEDULE_EXACT_ALARM`; иначе — `setAndAllowWhileIdle` (срабатывание может
  сдвинуться). `ReminderReceiver` показывает уведомление и планирует следующий
  запуск, `BootReceiver` восстанавливает будильники после перезагрузки.
- Без разрешения на уведомления напоминание сохраняется, но не показывается;
  форма честно предупреждает об этом.
- Виджет перечитывает Room по broadcast от `WidgetUpdater` после создания,
  редактирования, удаления, дублирования, переноса и отметок.

## Аналитика

- Источник — `task_logs`: `direction` (`up`/`complete`/`down`/`miss`), `delta_xp`,
  `delta_gold`, `logged_day`.
- Экран показывает 30 локальных дней; график — последние 14. Пустые дни
  добавляются нулями, чтобы график не «прыгал».
- История удалённой задачи удаляется каскадом вместе с `task_logs`, поэтому
  аналитика не хранит данные удалённых задач. Это осознанное ограничение.

## Тесты

- `app/src/test` — **33 JVM-теста**: `CronEngineTest` (due-дни, streak, monthly,
  every_x), `RewardEngineTest` (XP, уровни, клампинг HP), `TagRulesTest` (trim,
  лимиты, case-insensitive, эмодзи), `ReminderTimeTest` (сегодня/завтра, DST,
  формат), `TaskFiltersTest` (OR-фильтр, группировка, «Без тега»),
  `AnalyticsSeriesTest` (заполнение пропущенных дней).
- `app/src/androidTest` — **27 тестов** на Room in-memory: создание задачи,
  «+»/«−», guards для daily/todo, ежедневный cron, идемпотентность, откат часов
  назад, сохранность истории при повторной вставке, теги и каскадное удаление,
  duplicate/move, напоминания, аналитика и снимок для виджета.
- Запуск:
  `./gradlew :app:testDebugUnitTest` и `./gradlew :app:connectedDebugAndroidTest`.
- В этом окружении тесты не запускались: нет JDK, Android SDK и эмулятора.
  Что реально прогнано (SQLite, Python-модель логики, статика) — в
  `../replica/test-plan.md`, найденные баги — в `../replica/bugs.md`.

## Что исправлено тестовым прогоном

- cron больше не обнуляет серию, заработанную позже закрываемого дня (S1).
- `@Upsert` вместо `REPLACE`: история `task_logs` не удаляется каскадом (S1).
- проверки типа и направления действия в репозитории, `dueToday`, тикер дня,
  синхронизация часового пояса при старте.
- обработка ошибок БД и отмена корутин: snackbar вместо краша, ограниченные ретраи.
- i18n и a11y: строки в ресурсы, числовые подписи XP/HP, подписи чекбоксов,
  тёмная тема системных баров.
- форма создания переживает убийство процесса; длинные имя и заметки ограничены.

## Ограничения

- Layout-diff не посчитан: нет скриншотов оригинала/клона 390x844 и эмулятора.
- APK в этом окружении не собран, instrumented-тесты не прогнаны.
- На Android 12+ точные будильники могут быть запрещены пользователем; тогда
  используется неточный fallback.
- История удалённых задач не сохраняется в аналитике (каскад `task_logs`).

## Дальше

Сборка APK, прогон тестов на устройстве, скриншоты для layout-diff, проверка
напоминаний после перезагрузки и модал level-up вместо snackbar. См.
`../replica/build-log.md` и `../replica/parity.md`.
