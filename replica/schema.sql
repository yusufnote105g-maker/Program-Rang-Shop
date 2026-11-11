-- replica/schema.sql
-- Локальная БД Android-трекера привычек. Диалект: SQLite (Room).
-- Отличия от Postgres-дефолта набора:
--   * id  -> text (UUID генерируется в приложении, gen_random_uuid() в SQLite нет)
--   * время -> integer (epoch millis, UTC), "timestamptz" недоступен
--   * "день" -> text 'YYYY-MM-DD' в локальной зоне пользователя (для группировки/streak)
--   * access rules -> RLS нет; скоуп по user_id в data-слое

PRAGMA foreign_keys = ON;

-- 1. Пользователь (в локальном приложении одна строка, но таблица нужна для модели и будущих профилей)
create table users (
  id                text primary key,
  username          text not null,
  level             integer not null default 1,
  xp                integer not null default 0,
  hp                integer not null default 50,
  max_hp            integer not null default 50,
  gold              integer not null default 0,
  gems              integer not null default 0,
  class             text not null default 'none'
                      check (class in ('none','warrior','rogue','healer','mage')),
  timezone          text not null default 'UTC',       -- IANA, напр. Europe/Moscow
  day_start_minutes integer not null default 0,        -- минут после местной полуночи, когда флипается день
  last_cron_day     text,                              -- 'YYYY-MM-DD' локально; идемпотентность сброса
  created_at        integer not null default (strftime('%s','now') * 1000),
  updated_at        integer not null default (strftime('%s','now') * 1000)
);

-- 2. Задачи: привычка (habit), ежедневная (daily), разовая (todo)
create table tasks (
  id                 text primary key,
  user_id            text not null references users(id) on delete cascade,
  type               text not null check (type in ('HABIT','DAILY','TODO')),
  text               text not null,
  notes              text not null default '',
  difficulty         text not null default 'EASY'
                       check (difficulty in ('TRIVIAL','EASY','MEDIUM','HARD')),
  -- habit
  up                 integer not null default 1,       -- включена кнопка «+»
  down               integer not null default 0,       -- включена кнопка «−»
  counter_up         integer not null default 0,
  counter_down       integer not null default 0,
  -- daily
  streak             integer not null default 0,
  best_streak        integer not null default 0,
  frequency          text not null default 'DAILY'
                       check (frequency in ('DAILY','WEEKLY','MONTHLY')),
  every_x            integer not null default 1,
  repeat_days        text not null default '',         -- CSV дней недели, пн=0 … вс=6, напр. '0,2,4'
  start_date         text not null,                    -- 'YYYY-MM-DD' локально
  last_completed_day text,                             -- для расчёта streak
  -- todo
  due_date           text,                             -- 'YYYY-MM-DD' локально
  completed          integer not null default 0,
  completed_at       integer,
  -- общее
  reminder_at        integer,                          -- epoch millis UTC, null = нет
  reminder_enabled   integer not null default 0,
  position           integer not null default 0,       -- ручной порядок
  archived           integer not null default 0,
  created_at         integer not null default (strftime('%s','now') * 1000),
  updated_at         integer not null default (strftime('%s','now') * 1000),
  constraint todo_has_no_streak check (type = 'DAILY' or streak = 0)
);
create index idx_tasks_user_type on tasks (user_id, type, archived, position);
create index idx_tasks_reminder on tasks (reminder_enabled, reminder_at);

-- 3. Теги
create table tags (
  id         text primary key,
  user_id    text not null references users(id) on delete cascade,
  name       text not null,
  color      text not null default '#888888',
  created_at integer not null default (strftime('%s','now') * 1000),
  updated_at integer not null default (strftime('%s','now') * 1000),
  unique (user_id, name)
);

-- 4. Связь задач и тегов (n-n)
create table task_tags (
  task_id text not null references tasks(id) on delete cascade,
  tag_id  text not null references tags(id) on delete cascade,
  primary key (task_id, tag_id)
);
create index idx_task_tags_tag on task_tags (tag_id);

-- 5. Журнал отметок (история наград, нужна для аналитики и отката)
create table task_logs (
  id         text primary key,
  task_id    text not null references tasks(id) on delete cascade,
  user_id    text not null references users(id) on delete cascade,
  logged_at  integer not null,                         -- epoch millis UTC
  logged_day text not null,                            -- 'YYYY-MM-DD' локально
  direction  text not null check (direction in ('up','down','complete','miss')),
  delta_xp   integer not null,
  delta_gold integer not null,
  value      real not null                             -- value задачи в момент действия
);
create index idx_task_logs_task on task_logs (task_id, logged_at);
create index idx_task_logs_user_day on task_logs (user_id, logged_day);
