# Bugs: HabitTracker, тестовый прогон F01–F06

Build: `9f6db71` → `fd0887b`
Date: 2026-11-11
Env: локально. JDK/Gradle/Android SDK/эмулятор отсутствуют; проверки — статика,
Python-модель логики и настоящий SQLite по `replica/schema.sql`.

| severity | сколько | что значит |
| --- | --- | --- |
| S1 | 2 | потеря данных или заблокирован основной сценарий |
| S2 | 10 | сценарий сломан, обходного пути нет |
| S3 | 14 | работает, но заметно неправильно или есть обход |
| S4 | 4 | косметика, i18n, документация |

Все 30 багов исправлены в `fd0887b`. Ниже — по одному блоку на баг.

---

### BUG-001: cron обнуляет серию, заработанную позже закрываемого дня

- Severity: S1
- Flow / case: F06 / F06-E5
- Screen: S01
- Build: 9f6db71, JVM/SQLite-модель (эмулятора нет)

Steps
1. Ежедневная задача закрыта сегодня: `last_completed_day = 2026-11-10`.
2. `last_cron_day = 2026-11-08`, запускаем `runDailyCron()`.

Expected: дни 11-08 и 11-09 считаются пропущенными, HP списывается, но серия,
заработанная 11-10, остаётся 1.
Actual: id задачи попадал в `missedIds` за каждый пропущенный день, затем
`resetStreaks()` обнулял серию — вместе с только что заработанной.
Evidence: `TaskRepositoryTest.cron_keepsStreak_whenTaskWasCompletedLater`;
Python-модель, кейс «BUG-001».
Suspected cause: список на сброс строился по всем пропущенным дням без проверки
`lastCompletedDay > day`.
Status: fixed in fd0887b

---

### BUG-002: чекбокс ежедневной активен в день, когда она не нужна

- Severity: S2
- Flow / case: F02 / F02-E1
- Screen: S04
- Build: 9f6db71

Steps
1. Создать weekly-ежедневную на понедельник.
2. Открыть список во вторник и нажать чекбокс.

Expected: чекбокс disabled, под названием «Не сегодня».
Actual: чекбокс активен, нажатие принимается, признака выходного дня нет.
Evidence: старый `TaskRow.kt` (`Checkbox(enabled = ...)` отсутствовал);
в `strings.xml` не было `not_due_today`.
Suspected cause: в доменной модели не было `dueToday`, UI не знал расписание.
Status: fixed in fd0887b

---

### BUG-003: двойной тап Save создаёт две задачи

- Severity: S2
- Flow / case: F01 / F01-E5
- Screen: S02
- Build: 9f6db71

Steps
1. Ввести название.
2. Дважды быстро нажать «Сохранить» до перерисовки кнопки.

Expected: одна задача.
Actual: два вызова `save()` запускали две корутины и два `createTask`.
Evidence: старый `AddHabitViewModel.save()` без проверки `saving`.
Suspected cause: защита от повторного входа отсутствовала.
Status: fixed in fd0887b

---

### BUG-004: строки в шапке игрока захардкожены

- Severity: S4
- Flow / case: F01 / X2
- Screen: S01
- Build: 9f6db71

Steps
1. Открыть S01.
2. Сменить язык системы или проверить ресурсы.

Expected: все строки берутся из `strings.xml`.
Actual: `"Ур. ${stats.level}"` и `"${stats.gold} з."` были в коде.
Evidence: старый `AvatarHeader.kt`.
Suspected cause: строки писались до выноса в ресурсы.
Status: fixed in fd0887b

---

### BUG-005: на всех вкладках одинаковое пустое состояние

- Severity: S3
- Flow / case: F01 / F01-H2
- Screen: S07
- Build: 9f6db71

Steps
1. Открыть вкладку «Ежедневные» без задач.
2. Посмотреть текст и CTA.

Expected: «Пока нет ежедневных» и кнопка «Добавить ежедневную».
Actual: «Пока нет привычек» и «Добавить привычку» на всех вкладках.
Evidence: старый `TasksScreen.kt` использовал `empty_habits_*` и `empty_cta`.
Suspected cause: пустое состояние не параметризовали по типу.
Status: fixed in fd0887b

---

### BUG-006: полосы XP и HP без числовых подписей

- Severity: S3
- Flow / case: F02 / X2
- Screen: S01
- Build: 9f6db71

Steps
1. Открыть S01.
2. Включить TalkBack или посмотреть на прогресс.

Expected: «Опыт X / Y» и «Здоровье X / Y».
Actual: только цветные полосы; прогресс передавался цветом и длиной.
Evidence: старый `AvatarHeader.kt` без `xp_label`/`hp_label`.
Suspected cause: прогресс считался визуальным, а не текстовым.
Status: fixed in fd0887b

---

### BUG-007: в тёмной теме иконки системных баров не видны

- Severity: S3
- Flow / case: F01 / X3
- Screen: S01
- Build: 9f6db71

Steps
1. Включить тёмную тему.
2. Посмотреть на статус-бар и навигационную панель.

Expected: светлые иконки на тёмном фоне.
Actual: `windowLightStatusBar = true` из `values/themes.xml` делал иконки тёмными.
Evidence: старый `values/themes.xml`; отсутствовал `values-night/themes.xml`.
Suspected cause: ночной вариант темы не был создан.
Status: fixed in fd0887b

---

### BUG-008: клавиатура закрывает нижние поля формы S02

- Severity: S2
- Flow / case: F01 / F01-E10
- Screen: S02
- Build: 9f6db71

Steps
1. Открыть S02.
2. Сфокусировать поле заметок и открыть клавиатуру.

Expected: форма прокручивается, тип и сложность остаются доступны.
Actual: нижние поля уходили под IME.
Evidence: старый `AddHabitScreen.kt` без `imePadding()`.
Suspected cause: `adjustResize` в манифесте не учитывался Compose-разметкой.
Status: fixed in fd0887b

---

### BUG-009: при первой загрузке список пустой, без скелетона

- Severity: S3
- Flow / case: F01 / F01-H1
- Screen: S01
- Build: 9f6db71

Steps
1. Холодный старт на медленном устройстве.
2. Посмотреть на область списка до прихода данных.

Expected: скелетон или прогресс.
Actual: пустая область, экран «прыгает» после загрузки.
Evidence: старый `TasksScreen.kt`: `loading` нигде не отображался.
Suspected cause: состояние загрузки было, но UI его игнорировал.
Status: fixed in fd0887b

---

### BUG-010: английское название приложения в русском UI

- Severity: S4
- Flow / case: F01 / X2
- Screen: S01
- Build: 9f6db71

Steps
1. Посмотреть на иконку в лаунчере и заголовок S01.

Expected: «Трекер привычек».
Actual: «Habit Tracker».
Evidence: старый `strings.xml`, `app_name`.
Suspected cause: название не локализовали.
Status: fixed in fd0887b

---

### BUG-011: воркер сброса ретраится бесконечно

- Severity: S3
- Flow / case: F06 / F06-N1
- Screen: F06
- Build: 9f6db71

Steps
1. Смоделировать постоянную ошибку БД в `DailyResetWorker`.
2. Дать WorkManager несколько попыток.

Expected: после нескольких попыток работа завершается `failure`.
Actual: `Result.retry()` на любой `Throwable`, без ограничения.
Evidence: старый `DailyResetWorker.kt`.
Suspected cause: не было счётчика `runAttemptCount`.
Status: fixed in fd0887b

---

### BUG-012: «+» и CTA игнорируют выбранную вкладку

- Severity: S3
- Flow / case: F01 / F01-H2
- Screen: S01, S02
- Build: 9f6db71

Steps
1. Открыть вкладку «Ежедневные».
2. Нажать FAB или CTA пустого состояния.

Expected: форма открывается с типом «Ежедневная».
Actual: всегда открывалась привычка: маршрут `add` не нёс тип.
Evidence: старый `AppNavHost.kt` (`Routes.ADD = "add"`).
Suspected cause: тип не передавали аргументом навигации.
Status: fixed in fd0887b

---

### BUG-013: ошибка сохранения оставляет Save заблокированной

- Severity: S2
- Flow / case: F01 / F01-N1
- Screen: S02
- Build: 9f6db71

Steps
1. Смоделировать исключение при `createTask`.
2. Нажать Save.

Expected: snackbar, `saving = false`, можно повторить.
Actual: исключение уходило из `viewModelScope`, `saving` оставался `true`,
кнопка навсегда disabled; возможен краш.
Evidence: старый `AddHabitViewModel.save()` без `try/catch`.
Suspected cause: ошибки БД не обрабатывались на уровне ViewModel.
Status: fixed in fd0887b

---

### BUG-014: нет лимита длины названия

- Severity: S3
- Flow / case: F01 / F01-E3
- Screen: S02
- Build: 9f6db71

Steps
1. Вставить в название 100 000 символов.
2. Сохранить.

Expected: длина ограничена, UI не страдает.
Actual: строка целиком шла в БД и в строку списка.
Evidence: старый `setName()` без ограничения.
Suspected cause: лимит не задавали.
Status: fixed in fd0887b

---

### BUG-015: заметки без ограничения длины

- Severity: S4
- Flow / case: F01 / F01-E9
- Screen: S02
- Build: 9f6db71

Steps
1. Вставить в заметки 1 МБ текста.
2. Сохранить.

Expected: разумный лимит, Bundle не раздувается.
Actual: текст целиком шёл в БД и SavedStateHandle.
Evidence: старый `setNotes()` без ограничения.
Suspected cause: лимит не задавали.
Status: fixed in fd0887b

---

### BUG-016: убийство процесса теряет черновик формы

- Severity: S3
- Flow / case: F01 / F01-E8
- Screen: S02
- Build: 9f6db71

Steps
1. Ввести название и заметки.
2. Убить процесс, открыть приложение снова.

Expected: черновик восстановлен.
Actual: пустая форма.
Evidence: состояние жило только в `MutableStateFlow` ViewModel.
Suspected cause: не использовался `SavedStateHandle`.
Status: fixed in fd0887b

---

### BUG-017: подписи сегментов не помещаются на узком экране

- Severity: S4
- Flow / case: F01 / F01-E10
- Screen: S02
- Build: 9f6db71

Steps
1. Открыть S02 на ширине 320dp или с крупным шрифтом.

Expected: подпись обрезается с ellipsis.
Actual: длинный текст разъезжался и обрезался без многоточия.
Evidence: старый `Text(typeLabel(type))` без `maxLines`.
Suspected cause: не было ограничения строки.
Status: fixed in fd0887b

---

### BUG-018: REPLACE при вставке задачи каскадно удаляет историю

- Severity: S1
- Flow / case: F05 / F05-N2
- Screen: F05
- Build: 9f6db71

Steps
1. Вставить задачу `t1` и лог `l1`.
2. Повторно вставить `t1` (сценарий `@Insert(onConflict = REPLACE)`).

Expected: лог остаётся, задача обновляется.
Actual: REPLACE удаляет старую строку `t1`, `ON DELETE CASCADE` удаляет `l1`.
Evidence: SQLite-демонстрация: после REPLACE логов 0, после upsert — 1;
`TaskRepositoryTest.reinsertingSameTaskId_keepsTaskLogs`.
Suspected cause: `REPLACE` реализован как delete + insert.
Status: fixed in fd0887b

---

### BUG-019: минус обещает штраф, которого не было

- Severity: S3
- Flow / case: F03 / F03-N2
- Screen: S01
- Build: 9f6db71

Steps
1. Довести HP и XP до 0.
2. Нажать «−».

Expected: snackbar не обещает минус, если значения уже на нуле.
Actual: возвращались номинальные `-hpLoss` и `-xpLoss`, snackbar писал «−2 здоровья».
Evidence: `TaskRepositoryTest.tapMinus_reportsActualDelta_whenHpAndXpAreAlreadyZero`.
Suspected cause: отдавали номинал вместо фактической разницы.
Status: fixed in fd0887b

---

### BUG-020: открытое приложение живёт вчерашним днём после полуночи

- Severity: S2
- Flow / case: F06 / F06-E1
- Screen: S01
- Build: 9f6db71

Steps
1. Оставить приложение открытым через границу дня.
2. Посмотреть на чекбоксы и «сегодня».

Expected: ключ дня пересчитывается сам.
Actual: `observeToday` был `observeUser().map { dayKey(...) }` и менялся только
при записи пользователя; до перезапуска показывался вчерашний день.
Evidence: старый `TaskRepository.observeToday()`.
Suspected cause: не было таймера до границы дня.
Status: fixed in fd0887b

---

### BUG-021: cron обещает потерю HP, когда HP уже 0

- Severity: S3
- Flow / case: F06 / F06-N3
- Screen: S01
- Build: 9f6db71

Steps
1. HP = 0.
2. Пропустить ежедневную и запустить cron.

Expected: `hpLost = 0`, snackbar без минуса.
Actual: отчёт суммировал номинальные штрафы и писал «−N здоровья».
Evidence: `TaskRepositoryTest.cron_reportsActualHpLoss_whenHpIsAlreadyZero`.
Suspected cause: в отчёт шла сумма `difficulty.hpPenalty`, а не фактическая разница.
Status: fixed in fd0887b

---

### BUG-022: чекбоксы daily/todo без подписи для TalkBack

- Severity: S3
- Flow / case: F02 / X2
- Screen: S04
- Build: 9f6db71

Steps
1. Включить TalkBack.
2. Сфокусировать чекбокс ежедневной или разовой задачи.

Expected: «Отметить плюсом: <название>» или «Закрыть задачу: <название>».
Actual: «флажок, не отмечен» без названия задачи.
Evidence: старый `TaskRow.kt`; у кнопок «+»/«−» подпись была, у чекбоксов — нет.
Suspected cause: `contentDescription` задавали только CounterButton.
Status: fixed in fd0887b

---

### BUG-023: репозиторий не проверяет тип и направление действия

- Severity: S2
- Flow / case: F03 / F03-N3
- Screen: S01, S04
- Build: 9f6db71

Steps
1. Вызвать `tapMinus` для daily или todo.
2. Вызвать `completeTodo` для daily.
3. Вызвать `tapPlus` для привычки с `up = false`.

Expected: во всех случаях `null`, без награды и штрафа.
Actual: старый код проверял только `completed` у todo и день у daily;
минус и завершение могли сработать на чужом типе.
Evidence: `tapMinus_isRejected_whenMinusIsDisabled`,
`tapPlus_daily_isRejectedTwiceOnTheSameDay`; старый `TaskRepository.kt`.
Suspected cause: guards были только в UI.
Status: fixed in fd0887b

---

### BUG-024: при переключении вкладки на кадр видны чужие задачи

- Severity: S3
- Flow / case: F01 / F01-H2
- Screen: S01
- Build: 9f6db71

Steps
1. Открыть «Привычки» с несколькими задачами.
2. Переключиться на «Ежедневные».

Expected: новый таб и скелетон/пустое состояние, без строк Habits.
Actual: `combine` сразу отдавал новый `selectedTab` со старым списком задач,
поэтому под «Ежедневными» на кадр оставались привычки.
Evidence: старый `TasksViewModel.uiState` без фильтра по типу.
Suspected cause: `flatMapLatest` ещё не успевал отдать новый список.
Status: fixed in fd0887b

---

### BUG-025: dueToday не пересчитывается на границе дня

- Severity: S2
- Flow / case: F06 / F06-E1
- Screen: S01
- Build: 9f6db71

Steps
1. Оставить приложение открытым через полночь.
2. Посмотреть на weekly-ежедневную, которая нужна только по понедельникам.

Expected: чекбокс и «Не сегодня» обновляются по новому дню.
Actual: `observeTasks` считал `dueToday` только при изменении задачи или игрока;
до следующей записи флаг оставался вчерашним.
Evidence: старый `observeTasks` combine без `observeToday()`.
Suspected cause: день не входил в зависимости маппинга.
Status: fixed in fd0887b

---

### BUG-026: ошибка БД при отметке роняет приложение

- Severity: S2
- Flow / case: F02 / F02-N1
- Screen: S01
- Build: 9f6db71

Steps
1. Смоделировать исключение в `tapPlus`/`tapMinus`/`completeTodo`.
2. Нажать кнопку.

Expected: snackbar об ошибке, приложение живо.
Actual: исключение уходило из `viewModelScope` в необработанный обработчик.
Evidence: старый `TasksViewModel.runReward()` без `try/catch`.
Suspected cause: ошибки репозитория не доходили до UI.
Status: fixed in fd0887b

---

### BUG-027: ошибка cron при старте роняет экран

- Severity: S2
- Flow / case: F06 / F06-N1
- Screen: S01
- Build: 9f6db71

Steps
1. Смоделировать исключение в `ensureUser()` или `runDailyCron()`.
2. Открыть S01.

Expected: список всё равно загружается, сброс повторится позже.
Actual: исключение в `init` ViewModel уходило в необработанный обработчик.
Evidence: старый `TasksViewModel.init` без `try/catch`.
Suspected cause: стартовая догонка считалась «не может упасть».
Status: fixed in fd0887b

---

### BUG-028: воркер проглатывает отмену корутины

- Severity: S3
- Flow / case: F06 / F06-N2
- Screen: F06
- Build: 9f6db71

Steps
1. Отменить `DailyResetWorker` во время работы.

Expected: `CancellationException` пробрасывается, работа считается отменённой.
Actual: `catch (Throwable)` ловил отмену и возвращал retry/failure.
Evidence: старый `DailyResetWorker.kt`.
Suspected cause: отмена не отделялась от ошибки БД.
Status: fixed in fd0887b

---

### BUG-029: сохранение проглатывает отмену ViewModel

- Severity: S3
- Flow / case: F01 / F01-N1
- Screen: S02
- Build: 9f6db71

Steps
1. Начать сохранение и закрыть экран/убить ViewModel.

Expected: отмена пробрасывается.
Actual: `catch (Throwable)` превращал отмену в `saveError`, обновляя состояние
уже очищенной ViewModel.
Evidence: старый `AddHabitViewModel.save()`.
Suspected cause: отмена не отделялась от ошибки сохранения.
Status: fixed in fd0887b

---

### BUG-030: часовой пояс синхронизировался только при создании игрока

- Severity: S2
- Flow / case: F06 / F06-E1, X7
- Screen: S01
- Build: 9f6db71

Steps
1. Запустить приложение в Europe/Moscow.
2. Сменить зону устройства на America/New_York и открыть приложение.

Expected: `users.timezone` обновляется, граница дня пересчитывается.
Actual: зона записывалась только при первом `requireUser()`; граница дня
оставалась московской до переустановки/очистки данных.
Evidence: старый `ensureUser()` вызывал только `requireUser()`.
Suspected cause: не было сверки с `TimeZone.getDefault()`.
Status: fixed in fd0887b
