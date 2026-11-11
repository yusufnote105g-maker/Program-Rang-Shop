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
│   │   ├── dao/           UserDao, TaskDao, TaskLogDao
│   │   ├── AppDatabase.kt
│   │   └── Mappers.kt     entity <-> domain
│   ├── repository/        TaskRepository (транзакции, награды)
│   └── di/                DatabaseModule (Room, DAO, Clock)
├── domain/
│   ├── model/             TaskType, Difficulty, Task, RewardResult, UserStats, DailyResetReport
│   ├── cron/              CronEngine (календарь: due-дни, streak, граница дня)
│   └── reward/            RewardEngine (математика наград)
├── work/                  DailyResetWorker + DailyResetScheduler (WorkManager)
├── navigation/            AppNavHost, Routes
└── ui/
    ├── theme/             Color, Type, Shape, Dimens, Theme (из replica/design)
    ├── components/        TaskRow, CounterButton, CounterBadge, AvatarHeader, EmptyState
    └── screens/
        ├── tasks/         S01 TasksScreen + TasksViewModel
        └── addhabit/      S02 AddHabitScreen + AddHabitViewModel
```

## Сборка

В этом окружении JDK/Gradle/Android SDK нет, поэтому APK здесь не собирается.
Откройте папку `habittracker/` в Android Studio (Ladybug или новее) или:

```bash
./gradlew :app:assembleDebug
```

Gradle wrapper (`gradlew`) не сгенерирован — Android Studio создаст его при первом
открытии, либо выполните `gradle wrapper --gradle-version 8.9`.

## Что реализовано (этот срез)

- **S01** — список задач: табы «Привычки / Ежедневные / Разовые», шапка игрока
  (уровень/опыт/HP/золото), FAB, пустое состояние, snackbar с наградой.
- **S02** — создание: название (валидация), заметки, тип, сложность; сохранение в Room.
- **S04** — строка привычки со счётчиками «+N / −M» за сегодня.
- **F06** — ежедневный сброс: `DailyResetWorker` на границе дня пользователя, сброс серий
  за пропуски, штраф по HP, обнуление счётчиков; идемпотентно по `users.last_cron_day`
  и с догоном при каждом старте приложения.
- **Room** — 5 таблиц из `replica/schema.sql`; транзакционные отметки, награды, журнал.
- **Дизайн-токены** — светлая/тёмная темы M3, отступы 4dp, формы M3.

## Как работает день и серия

- «Сегодня» считается в зоне пользователя и с учётом `day_start_minutes` (граница дня).
- Отметка ежедневной задачи продлевает серию, только если предыдущий **нужный** день был
  закрыт; иначе серия начинается заново с 1 (`CronEngine.streakAfterCompletion`).
- Пропущенная нужная задача на сбросе: серия → 0, списание HP по сложности.
- `DailyResetWorker` запускается раз в сутки со сдвигом на границу дня; WorkManager не
  гарантирует точность, поэтому сброс идемпотентен и догоняется при запуске приложения.

## Дальше

Редактирование/удаление (S06/S08), теги, модал level-up, напоминания.
См. `../replica/build-log.md`.
