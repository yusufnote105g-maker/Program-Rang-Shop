# Recon map: Habitica (Android)

Scope: создание привычки (экран создания), список привычек и логика ежедневных отметок (check-in)
For: личное обучение и практика
Date: 2026-11-11

> Учебный прогон (practice run). Живой аккаунт не использовался, скриншоты не снимались.
> Уверенность помечена честно: high — видно в публичном листинге/общеизвестно,
> medium — общепринятый паттерн жанра, guess — догадка.

## Sources

| # | source | URL | notes |
| --- | --- | --- | --- |
| 1 | Google Play listing | https://play.google.com/store/apps/details?id=com.habitrpg.android.habitica | достижим. Даёт оффер, список фич, отзывы, превью-скриншоты (URL картинок). Основной источник. |
| 2 | Habitica features page | https://habitica.com/static/features | запрошен, контент не вернулся (пусто) |
| 3 | Habitica FAQ | https://habitica.com/static/faq | запрошен, контент не вернулся (пусто) |
| 4 | Habitica wiki (Tasks) | https://habitica.fandom.com/wiki/Tasks | 403 Forbidden |
| 5 | Публичные API-документы Habitica | https://habitica.com/apidoc/ | не запрашивался в этом прогоне; полезен для модели данных на след. шаге |
| 6 | Общеизвестные паттерны habit-tracker'ов | — | для логики сброса дня, streak, счётчиков |

Примечание по условиям использования: перед реальным клонированием нужно прочитать ToS Habitica
(не использовать аккаунт для сборки конкурента — работать только по публичным источникам).
В этом учебном прогоне мы и так опираемся только на публичный листинг.

## Core loop

Пользователь заводит привычку/задачу и каждый день «отмечает» её в списке,
получая мгновенную награду (XP/золото), которая подкрепляет регулярность.

## Screens

| ID | screen | route / how to reach | purpose | key components | states seen |
| --- | --- | --- | --- | --- | --- |
| S01 | Главный список задач | запуск приложения / нижняя вкладка Tasks | список привычек с отметками | табы типов (Habits/Dailies/To-Dos), строка задачи, кнопка «+»/FAB, шапка аватара | empty, filled, loading, offline, error |
| S02 | Форма создания привычки | S01 → «+» | задать новую привычку | поле названия, заметки, выбор сложности, теги, счётчик/направление, кнопка Save | empty, filled, validation-error, saving |
| S03 | Выбор типа задачи | S01 → «+» → переключатель типа | Habit / Daily / To-Do | сегмент/табы, иконки типов | default |
| S04 | Строка привычки (компонент) | внутри S01 | быстрая отметка | «+», «−», название, streak, цвет/иконка, меню «⋮» | default, pressed, completed-today, streak-broken, disabled |
| S05 | Всплывающее вознаграждение | после отметки в S01 | фидбек за действие | тост/попап «+XP / +золото», анимация, модал level-up | reward, level-up, drop, HP-loss |
| S06 | Детали/редактирование привычки | S01 → тап по строке | посмотреть и изменить | те же поля, что в S02 + Delete | view, edit, delete-confirm |
| S07 | Пустое состояние списка | S01 без задач | подсказать первый шаг | иллюстрация, текст, CTA «Добавить привычку» | empty |
| S08 | Контекстное меню задачи | S01 → «⋮» | действия над задачей | список: Edit, Delete, Duplicate, Move | default |

## Flows

```
F01 Создать привычку
    S01 (+) -> S03 (выбрать Habit) -> S02 (заполнить) -> Save -> S01
    happy path clicks: 5
    edge: пустое имя (блок Save), лимит задач, оффлайн (сохранить локально), отмена на полпути

F02 Отметить привычку (положительно)
    S01 (тап «+» на строке) -> S05 (награда) -> S01
    happy path clicks: 1
    edge: несколько «+» в день (счётчик), достижение уровня, оффлайн-очередь синка

F03 Отметить привычку (отрицательно)
    S01 (тап «−») -> S05 (штраф/потеря HP) -> S01
    happy path clicks: 1
    edge: HP = 0, нет права на минус, случайный «drops»

F04 Изменить привычку
    S01 (тап по строке) -> S06 (правка) -> Save -> S01
    happy path clicks: 3
    edge: конфликт синка, смена сложности меняет будущие награды

F05 Удалить привычку
    S01 (⋮) -> S08 -> Delete -> подтверждение -> S01
    happy path clicks: 4
    edge: удаление необратимо (или корзина), удаление с историей

F06 Ежедневный сброс и streak
    автоматически на границе дня -> пересчёт Dailies -> S01 (обновлённые streak)
    happy path clicks: 0 (система)
    edge: часовой пояс, пропущенный день -> streak сбрасывается, незавершённые Dailies бьют по HP
```

## Components

| component | variants | states | used on |
| --- | --- | --- | --- |
| TaskRow | habit (с «+»/«−»), daily (чекбокс), to-do (чекбокс) | default, pressed, done, streak-broken | S01, S07 |
| CounterButton | plus, minus | default, pressed, disabled | S04, S01 |
| Tabs | типы задач | active, inactive | S01, S03 |
| FAB / AddButton | floating, inline | default, pressed | S01 |
| AvatarHeader | HP bar, XP bar, уровень, золото | normal, low-HP | S01 |
| RewardToast / Popup | reward, penalty | show, level-up | S05 |
| Modal (level-up) | level-up | open, dismiss | S05 |
| FormField | text, textarea (notes), number | empty, filled, error, disabled | S02, S06 |
| DifficultySelector | trivial/easy/medium/hard | selected | S02, S06 |
| TagChip | с цветом | selected, unselected | S02, S06 |
| OverflowMenu | «⋮» | closed, open | S04, S08 |
| ConfirmDialog | destructive | open, confirm, cancel | S06, S08 |
| EmptyState | иллюстрация + CTA | — | S07 |

## Inferred data model

```
User    id, username, level, xp, hp, gold, gems, class,
        timezone, day_start (граница дня), last_cron
        evidence: S01 шапка аватара (уровень/XP/HP/золото), листинг (leveling, gear)
        confidence: high

Task    id, user_id, type (habit | daily | todo),
        text (название), notes,
        difficulty (trivial | easy | medium | hard),
        up (bool, есть «+»), down (bool, есть «−»),
        counter_up, counter_down,            # для привычек: сколько раз отмечено
        streak,                              # для daily: серия
        frequency (daily | weekly | monthly), every_x, repeat_days[], start_date,
        due_date, completed,                 # для todo
        reminder, tags[], created_at, updated_at
        evidence: S02 поля формы, S04 строка с «+»/«−» и streak, листинг (daily/weekly/monthly, negative points)
        confidence: high (поля) / medium (точные имена)

TaskLog id, task_id, date, direction (up | down), delta_xp, delta_gold
        evidence: логика наград и история отметок (нужна для аналитики/отката)
        confidence: medium

Tag     id, user_id, name, color
        evidence: чипы тегов в S02, группировка в S01
        confidence: medium

Relationships: User 1-n Task, User 1-n Tag, Task n-n Tag, Task 1-n TaskLog.
```

## Feature matrix

See `features.csv`. Must: 12, should: 6, could: 3, skip: 5 (всего 26).

## Out of scope (cannot or should not be cloned)

- **RPG-контент Habitica** (арт аватаров, питомцы, снаряжение, классы, сезонные события) — это их
  собственные ассеты и контент. Механику наград повторить можно, арт — нет.
- **Соцслой**: parties, guilds, challenges — это сеть и чужие пользователи, не часть продукта,
  который можно пересобрать.
- **Монетизация**: гемы, подписка, магазин — бизнес-модель, вне выбранного среза.
- **Wear OS-приложение** — вне среза.
- **Каталог готовых задач/шаблонов** (если есть у Habitica) — их контент.

## Size

Screens 8, flows 6, entities 4. Hard parts: ежедневный сброс с учётом часового пояса и
границы дня, логика streak и штрафов, оффлайн-first хранение + синхронизация, математика
наград/уровней. Size: **M** (несколько недель) для полного среза; **S** (выходные), если
выкинуть синк и оставить локальное хранилище.
