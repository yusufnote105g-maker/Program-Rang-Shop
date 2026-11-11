# Компоненты: трекер привычек (Compose / Material 3)

Спецификации для всех компонентов из `replica/recon.md`. Токены — из
`replica/design/tokens.json`, тема — `replica/design/theme/` (`HabitTrackerTheme`).
Иконки — открытый набор (Lucide/Tabler, MIT). Текст — свой, не из оригинала.

Базовые правила:

- **Касание:** любой интерактив ≥ 48dp (даже если визуально меньше — padding внутри).
- **Фокус:** видимое кольцо 2dp цвета `accent`; на Android — через `Modifier.focusRing`/`indication`.
- **Состояния:** у каждого компонента default / pressed / focused / disabled (+ loading, error, empty, где применимо).
- **Тёмная тема:** все цвета только из `MaterialTheme.colorScheme` и `MaterialTheme.extra`.

---

## TaskRow — строка задачи

```
variants  habit (кнопки «+»/«−»), daily (чекбокс + streak), todo (чекбокс)
sizes     min высота 64dp (Dimens.taskRowMin), padding 16dp/12dp
states    default, pressed, focused, done (today), streak-broken, disabled, archived
tokens    bg surface, text text, notes text-muted, разделитель border,
          radius md(12dp), отступы screenPadding=16dp
compose   Row + Modifier.clickable; habit -> CounterButton(plus/minus) + CounterBadge;
          daily/todo -> Checkbox; подпись streak -> Text style labelMedium
a11y      весь ряд — одна кликабельная цель (semantics(mergeDescendants=true));
          contentDescription для «+/−»: «Отметить плюсом/минусом: <название>»
used on   S01, S07
```

## CounterBadge — счётчик отметок за день

```
variants  plus («+N», accent), minus («−M», danger)
sizes     высота 20dp (labelMedium + 2dp×2), padding 8dp по горизонтали, radius xs(4dp)
states    hidden (count = 0 — не рисуется), plus, minus
tokens    plus: bg accent-container, текст on-accent-container;
          minus: bg danger-container, текст on-danger-container
compose   Surface(shape = extraSmall) + Text(labelMedium); в строке привычки
          стоит рядом с соответствующей кнопкой; при count = 0 не занимает места
a11y      contentDescription «Отмечено плюсом: N» / «Отмечено минусом: M»;
          не только цвет — есть знак «+»/«−» и цифра
used on   S04, S01
```

## CounterButton — кнопка «+» / «−»

```
variants  plus (accent), minus (danger, только если down=true)
sizes     44dp (Dimens.counterButton), иконка 24dp; hit target 48dp
states    default, pressed, disabled, loading (короткая задержка записи)
tokens    plus: container accent-container, icon on-accent-container;
          minus: container danger-container, icon on-danger-container; radius pill
compose   FilledIconButton / IconButton с фоном; ripple по умолчанию
a11y      contentDescription «Отметить плюсом: <название>» / «Отметить минусом: <название>»;
          не только цвет — есть иконка (+/−)
used on   S04, S01
```

## Tabs — переключатель типов задач

```
variants  PrimaryTabRow (Habits / Dailies / To-Dos)
sizes     высота 48dp, отступы 16dp по горизонтали
states    selected, unselected, pressed, disabled
tokens    selected: text text, indicator accent; unselected: text-muted;
          фон transparent, снизу divider border
compose   PrimaryTabRow + Tab; состояние в ViewModel (Flow<List<Task>>)
a11y      role=Tab, выбранная вкладка озвучивается; переключение свайпом — опционально
used on   S01
```

## FAB / AddButton — добавить задачу

```
variants  FloatingActionButton (иконка), ExtendedFAB (иконка+текст)
sizes     56dp (Dimens.fab); ExtendedFAB высота 56dp
states    default, pressed, focused, disabled
tokens    container accent, icon on-accent, radius lg(16dp), elevation level3
compose   FloatingActionButton / ExtendedFloatingActionButton
a11y      contentDescription «Добавить задачу»
used on   S01
```

## AvatarHeader — шапка игрока (HP/XP/уровень/золото)

```
variants  compact (только полосы), full (аватар + полосы + валюта)
sizes     высота 120dp (Dimens.avatarHeader)
states    normal, low-hp (HP < 25% — полоса danger), level-up (вспышка)
tokens    bg surface, XP-полоса accent, HP-полоса success (danger при low-hp),
          золото gold, текст text/text-muted
compose   Column + LinearProgressIndicator (progress = xp/maxXp)
a11y      значения озвучиваются: «Уровень 5, опыт 120 из 200, здоровье 40 из 50»;
          прогресс не только цветом — есть числовая подпись
used on   S01
```

## RewardToast / Popup — фидбек за отметку

```
variants  reward (+XP/+золото), penalty (−HP), drop (случайная награда)
sizes     Snackbar; авто-скрытие 2с (medium2=200ms на появление)
states    show, dismissed, queued (несколько подряд)
tokens    surface (inverse), text onSurface, иконка accent/danger
compose   SnackbarHost + Snackbar; не блокирует ввод, живёт поверх списка
a11y      live region (assertive), объявляется через screen reader
used on   S05
```

## LevelUpDialog — модал повышения уровня

```
variants  level-up (только кнопка «Ок»)
sizes     AlertDialog, radius xl(28dp), ширина до 320dp
states    open, dismissed
tokens    container surface, title text (titleLarge), accent для цифры уровня
compose   AlertDialog / Dialog
a11y      focus trap внутри диалога, кнопка «Ок» в фокусе; закрывается по «Назад»
used on   S05
```

## FormField — поле формы

```
variants  text (название), multiline (заметки), number (счётчик), select
sizes     OutlinedTextField, высота ≥ 56dp, padding 16dp
states    empty, focused, filled, error (с текстом ошибки), disabled
tokens    border-input outline, фокус accent, error danger, placeholder text-muted
compose   OutlinedTextField + supportingText; ошибка через isError
a11y      label всегда видим; ошибка озвучивается, не только красным;
          ввод названия обязателен — кнопка Save disabled при пустом
used on   S02, S06
```

## DifficultySelector — выбор сложности

```
variants  SegmentedButton: trivial / easy / medium / hard
sizes     высота 40dp, 4 сегмента, скругление pill
states    selected, unselected, pressed, disabled
tokens    selected: container accent-container, label on-accent-container;
          unselected: container transparent, label text-muted
compose   SingleChoiceSegmentedButtonRow + SegmentedButton
a11y      role=RadioButton; выбор озвучивается; сложность влияет на награду — есть подпись
used on   S02, S06
```

## TagChip — чип тега

```
variants  AssistChip (выбор тега), FilterChip (фильтр списка)
sizes     высота 32dp, padding 12dp, иконка 18dp
states    selected, unselected, pressed, disabled
tokens    цвет тега из tags.color (точка слева), фон surface-variant,
          выбранный — accent-container
compose   FilterChip / AssistChip
a11y      озвучивается имя тега и статус выбора
used on   S02, S06, S01
```

## OverflowMenu — меню «⋮»

```
variants  IconButton + DropdownMenu (Edit / Delete / Duplicate / Move)
sizes     IconButton 48dp, пункты меню 48dp
states    closed, open, item-pressed, item-disabled
tokens    container surface, elevation level2, divider border,
          Delete — danger
compose   IconButton + DropdownMenu + DropdownMenuItem
a11y      contentDescription «Действия с задачей»; меню закрывается по «Назад»
used on   S04, S08
```

## ConfirmDialog — подтверждение удаления

```
variants  destructive (удаление)
sizes     AlertDialog, радиус xl(28dp)
states    open, confirm, cancel
tokens    confirm — danger (текстовая кнопка), cancel — text-muted
compose   AlertDialog
a11y      фокус на «Отмена» по умолчанию (безопасный выбор), Esc/«Назад» отменяет
used on   S06, S08
```

## EmptyState — пустой список

```
variants  habits / dailies / todos / search-empty
sizes     центр экрана, иллюстрация ≤ 160dp, отступы xxl(32dp)
states    empty (единственное)
tokens    иллюстрация text-muted, заголовок text (titleMedium),
          подпись text-muted, CTA — Button primary
compose   Column (по центру) + Image/Icon + Text + Button
a11y      заголовок и подпись — обычный текст; CTA с понятным label «Добавить привычку»
used on   S07
```

---

## Чек-лист примитивов (построить до экранов)

1. `TaskRow` (+ `CounterButton`, `CounterBadge`, `Checkbox`)
2. `PrimaryTabRow` типов
3. `FloatingActionButton`
4. `AvatarHeader`
5. `Snackbar`-хост наград
6. `AlertDialog` (level-up и подтверждение)
7. `OutlinedTextField`, `SegmentedButton`, `FilterChip`
8. `DropdownMenu`

Собрать их изолированно (экран `/design` в debug-сборке), снять скриншоты светлой и
тёмной темы — это и есть проверка дизайн-системы перед `replica-build`.

## Контраст

`python3 <skills>/replica-design/contrast.py replica/design/tokens.json` → **0 провалов AA**
(светлая и тёмная темы, 15 пар каждая). Порог: тело 4.5:1, крупный текст и границы/фокус 3:1.
