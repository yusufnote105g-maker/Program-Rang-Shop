# Architecture: свой трекер привычек (пересборка ядра Habitica)

Источник: `replica/recon.md`, `replica/features.csv`.
Тип приложения: **локальное Android-приложение** (offline-first, одна БД на устройстве, без сервера).

## Stack

| layer | choice | why |
| --- | --- | --- |
| язык | Kotlin | нативный Android, лучшая поддержка корутин и Compose |
| UI | Jetpack Compose + Material 3 | декларативный UI, темы и dark mode почти бесплатно |
| архитектура | MVVM + UDF (ViewModel → StateFlow → UI) | состояние живёт в БД, переживает пересоздание процесса |
| локальная БД | Room (SQLite) | официальный слой над SQLite, миграции, Flow-запросы |
| DI | Hilt | стандарт, аккуратно связывает ViewModel/Repository/Room |
| асинхронность | Coroutines + Flow | фоновые операции и реактивные списки |
| настройки | DataStore (Preferences) | тема, часовой пояс, граница дня |
| фоновые задачи | WorkManager (+ AlarmManager для точных напоминаний) | ежедневный сброс и напоминания надёжнее, чем таймеры в процессе |
| время | java.time (core library desugaring) | корректная работа с зонами и DST |
| ID | UUID (TEXT), генерируется в приложении | в SQLite нет `gen_random_uuid()` |
| тесты | JUnit, Room in-memory, Turbine, Compose UI test | проверяем и логику, и экраны |
| сборка | Gradle + KSP | Room/Hilt через KSP |

Отличия от дефолта набора (и почему):

- **Нет web/Next.js** — продукт целиком на устройстве.
- **Нет Postgres/Supabase** — база локальная, SQLite через Room. Одна БД.
- **Нет auth** — один пользователь, вход не нужен; защита = песочница приложения.
- **Нет payments/email/jobs-в-облаке** — вне среза; напоминания делает WorkManager локально.
- **SQLite, а не Postgres** — поэтому `id text` (UUID из кода), время `integer` (epoch millis UTC),
  а не `uuid`/`timestamptz`; RLS недоступен.

Опционально (не в этом срезе): экспорт/импорт JSON-бэкапа, чтобы данные не терялись при
удалении приложения. Облачная синхронизация — только если позже понадобится несколько устройств.

## Schema

Таблицы: **5** (`users`, `tasks`, `tags`, `task_tags`, `task_logs`).
Правила доступа: **RLS нет** (локальная БД). Доступ = один пользователь; в data-слое всё
скоупится по `user_id`, а БД лежит в приватном каталоге приложения. SQL см. `replica/schema.sql`.

Ключевые жёсткие ограничения (это проблемы БД, а не UI):

- **Идемпотентность ежедневного сброса** — `users.last_cron_day` (`YYYY-MM-DD` в локальной зоне).
  Сброс выполняется только если день сменился. Защищает от двойного сброса при ретраях WorkManager
  и множественных запусках приложения.
- **Атомарность отметки** — `counter_up`/`counter_down` меняются внутри одной транзакции вместе
  с записью в `task_logs` (`UPDATE ... SET counter_up = counter_up + 1`), чтобы быстрые тапы не теряли счёт.
- **Streak** — считается из `last_completed_day` и текущего локального дня, а не из таймера.
- **Сложность** ограничена `check`, `type` ограничен `check`, статусные поля — через `check`.
- Время везде — `integer` (epoch millis, UTC); «день» — `YYYY-MM-DD` в локальной зоне.

## API

HTTP-сервера нет. «API» здесь — операции репозитория/UseCase (контракт между ViewModel и данными).
Одна операция на поток из recon (F01–F06) плюс CRUD.

| operation | does | who | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| `tasks.create(...)` | создать привычку/задачу | локальный пользователь | TaskDraft | taskId | F01 |
| `tasks.observeByType(type)` | реактивный список | UI | type | Flow<List<Task>> | S01 |
| `tasks.tapPlus(id)` | положительная отметка | UI | taskId | RewardResult | F02 |
| `tasks.tapMinus(id)` | отрицательная отметка | UI | taskId | RewardResult | F03 |
| `tasks.completeTodo(id)` | закрыть to-do | UI | taskId | RewardResult | F02 |
| `tasks.edit(id, patch)` | изменить | UI | TaskPatch | Unit | F04 |
| `tasks.delete(id)` | удалить | UI | taskId | Unit | F05 |
| `tasks.reorder(ids)` | ручной порядок | UI | ordered ids | Unit | S01 |
| `tasks.setReminder(id, at)` | напоминание | UI | taskId, instant | Unit | S08 |
| `tags.create/rename/delete` | теги | UI | TagDraft | tagId | S02 |
| `tags.attach/detach(taskId, tagId)` | привязка тега | UI | ids | Unit | S02 |
| `cron.runDailyReset(now)` | пересчёт Dailies/streak/HP | система | now | ResetReport | F06 |
| `stats.snapshot()` | агрегаты для шапки/аналитики | UI | — | UserStats | S01 |
| `backup.export/import()` | JSON-бэкап (опция) | пользователь | uri | Unit | — |

Webhooks: нет (нет сервера).

Jobs (WorkManager):

| job | schedule | what it does |
| --- | --- | --- |
| `DailyResetWorker` | ежедневно на границе дня + догон при запуске приложения | идемпотентный сброс Dailies, streak, штрафы по HP |
| `ReminderWorker` | `OneTimeWorkRequest` под каждый `reminder_at` | локальное уведомление, перепланирование после срабатывания |
| `BackupWorker` (опция) | вручную/раз в неделю | экспорт JSON в хранилище |

Если позже появится синхронизация — эти же операции ложатся на REST
(`GET /tasks?type=`, `POST /tasks`, `POST /tasks/{id}/log`, `POST /cron/daily`).

## The parts that bite

- **Часовые пояса и DST:** «день» считается в зоне пользователя (`users.timezone`), а не в UTC.
  Храним UTC, группируем по локальному `YYYY-MM-DD`. Переход на летнее время меняет длину суток —
  границу дня задаём минутами после местной полуночи (`day_start_minutes`), а не фиксированным часом UTC.
- **Идемпотентность:** сброс дня и напоминания могут запускаться повторно (ретраи WorkManager,
  перезагрузка телефона). Защита — `last_cron_day` и уникальность «события на день».
- **Гонки:** быстрые тапы «+/−» и одновременный cron-сброс. Решение — транзакции Room и атомарные
  инкременты счётчиков; награда считается от снапшота `value` в момент действия.
- **Оффлайн:** приложение и так оффлайн, синка нет; риск — потеря данных при удалении. Смягчаем
  опциональным экспортом.
- **Уведомления (Android 13+):** нужно разрешение `POST_NOTIFICATIONS`; на Android 12+ точные
  будильники требуют `SCHEDULE_EXACT_ALARM`, а агрессивная экономия батареи может их глушить —
  поэтому напоминания best-effort + догон при открытии приложения.
- **Миграции Room:** схема будет меняться — версионируем и пишем `Migration`, иначе падение на старте.
- **Производительность списка:** при большом числе задач — `LazyColumn` + индексы; при необходимости Paging.

## Build order

1. **Вертикальный срез:** S01 + S02 + S04 + S05; таблицы `users`, `tasks`, `task_logs`;
   операции `create` / `observeByType` / `tapPlus` → увидеть награду. Доказывает связку
   Compose ↔ Room ↔ ViewModel.
2. **Must-haves:** `tapMinus`, `completeTodo`, streaks, `DailyResetWorker`, edit/delete, табы, оффлайн-хранение.
3. **Should-haves:** level-up, потеря HP, пустое состояние, теги, напоминания, контекстное меню.
4. **Could-haves:** виджет, аналитика/графики, темы.
5. **Фиксы от `replica-entrepreneur`** — после того как он отработает по отзывам.

## Next

`replica/schema.sql` — готовая DDL. Следующий шаг: `@replica-design` — токены, цвета, типографика
и спеки компонентов из списка в recon.
