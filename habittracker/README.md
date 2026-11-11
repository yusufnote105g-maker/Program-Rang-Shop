# HabitTracker (Android)

Локальный трекер привычек — учебный клон ядра Habitica. Kotlin + Jetpack Compose +
Room + Hilt. Без сервера, всё на устройстве.

Документы дизайна и разбора лежат в соседней папке `../replica/`:
`recon.md`, `architecture.md`, `schema.sql`, `design/`.

## Стек

| слой | выбор |
| --- | --- |
| язык | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| архитектура | MVVM + UDF, `StateFlow` |
| БД | Room (SQLite), offline-first |
| DI | Hilt |
| асинхронность | Coroutines + Flow |
| фоновые задачи | WorkManager (ежедневный сброс) |
| время | java.time (minSdk 26) |
| сборка | AGP 8.7, KSP |

## Структура

```
app/src/main/java/com/example/habittracker/
├── HabitApp.kt            @HiltAndroidApp
├── MainActivity.kt        точка входа, HabitTrackerTheme
├── data/
│   ├── local/
│   │   ├── entity/        UserEntity, TaskEntity, TagEntity, TaskTagEntity, TaskLogEntity
│   │   ├── dao/           UserDao, TaskDao, TagDao, TaskLogDao
│   │   ├── AppDatabase.kt
│   │   └── Mappers.kt     entity <-> domain
│   ├── repository/        TaskRepository (транзакции, награды, теги)
│   └── di/                DatabaseModule (Room, DAO, Clock)
├── domain/
│   ├── model/             TaskType, Difficulty, Task, Tag, RewardResult, UserStats, DailyResetReport
│   ├── cron/              CronEngine (календарь: due-дни, streak, граница дня)
│   ├── reward/            RewardEngine (математика наград)
│   └── tags/              TagRules (лимиты, trim, case-insensitive)
├── work/                  DailyResetWorker + DailyResetScheduler (WorkManager)
├── navigation/            AppNavHost, Routes
└── ui/
    ├── theme/             Color, Type, Shape, Dimens, Theme (из replica/design)
    ├── components/        TaskRow, CounterButton, CounterBadge, AvatarHeader,
    │                      EmptyState, TaskListSkeleton, TagDot, ConfirmDeleteDialog
    └── screens/
        ├── tasks/         S01 TasksScreen + TasksViewModel
        └── taskform/      S02/S06 TaskFormScreen + TaskFormViewModel
```

Тесты:

```
app/src/test/java/.../domain/            CronEngineTest, RewardEngineTest, TagRulesTest (JVM)
app/src/androidTest/java/.../data/       TaskRepositoryTest (Room in-memory)
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

## Что реализовано (этот срез)

- **S01** — список задач: табы «Привычки / Ежедневные / Разовые», шапка игрока
  (уровень/опыт/HP/золото), FAB, пустое состояние, snackbar с наградой, теги в строке.
- **S02** — создание: название (валидация), заметки, тип, сложность, теги;
  сохранение в Room.
- **S04** — строка привычки со счётчиками «+N / −M» за сегодня и меню «⋮».
- **S06** — редактирование: тап по строке или «⋮» → Edit; те же поля, что в S02,
  тип после создания read-only, удаление с подтверждением.
- **S08** — меню задачи: «⋮» → Edit / Delete; confirm-dialog с фокусом на «Отмена»,
  каскадное удаление `task_logs` и `task_tags`.
- **Теги** — чипы существующих тегов и создание новых на лету; лимит 8 тегов,
  длина 24 символа, сравнение без учёта регистра; цвет стабилен для имени.
- **F06** — ежедневный сброс: `DailyResetWorker` на границе дня пользователя, сброс серий
  за пропуски, штраф по HP, обнуление счётчиков; идемпотентно по `users.last_cron_day`
  и с догоном при каждом старте приложения.
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

## Тесты

- `app/src/test` — 20 JVM-тестов: `CronEngineTest` (due-дни, streak, monthly, every_x),
  `RewardEngineTest` (XP, уровни, клампинг HP) и `TagRulesTest` (trim, лимиты,
  case-insensitive, эмодзи).
- `app/src/androidTest` — 20 тестов на Room in-memory: создание задачи, «+»/«−»,
  guards для daily/todo, ежедневный cron, идемпотентность, откат часов назад,
  сохранность истории при повторной вставке, теги и каскадное удаление.
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

## Дальше

Duplicate/Move в меню S08, фильтр по тегам в S01, модал level-up, напоминания,
аналитика. См. `../replica/build-log.md` и `../replica/parity.md`.
