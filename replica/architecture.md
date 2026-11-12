# Architecture: Honey Mart marketplace (User / Owner / Admin)

Источник: `TheChance101/Honey-Mart-Android-Client` + `Honey-Mart-Server`, README проекта.
Тип приложения: **клиент-серверный маркетплейс локальных рынков** (покупатель / владелец магазина / админ).
Бэкенд: **Supabase** (PostgreSQL + Auth + Realtime + Storage) + Edge Functions.
Date: 2026-11-12

## Stack

| layer | choice | why |
| --- | --- | --- |
| mobile | Kotlin + Jetpack Compose | MVVM + UDF; как в оригинале |
| модулизация | App Modularization: `:user`, `:owner`, `:admin`, `:core`, `:design_system`, `:buildSrc` | каждая роль — своя feature-line внутри Android-проекта |
| UI | Compose + Material 3; design system из Figma (`design_system`, токены в `replica/design/`) | |
| архитектура | MVVM + UDF (ViewModel → StateFlow → UI) | |
| DI | Hilt | |
| локальное хранилище | DataStore (JWT/сессия, настройки) | |
| БД | PostgreSQL на Supabase | единственная БД, RLS включён |
| сеть | Ktor Client → Supabase (PostgREST + Auth/GoTrue + Realtime) | Ktor Client из оригинала |
| маппинг | DTO + kotlinx.serialization + ручной mapping | PostgREST отдаёт JSON; ORM не нужен |
| auth | Supabase Auth (GoTrue) | JWT в SecureStorage |
| realtime | Supabase Realtime | уведомления о статусе заказа |
| jobs | Supabase Edge Functions + pg_cron | оформление заказа, одобрение магазина, списание стока |
| money | integer cents + currency column | ... |

Отличия от набора Habitica (предыдущий прогон в этом файле): теперь **клиент-сервер, Supabase вместо Room**, RLS вместо скоупа по `user_id`.

## Schema

Таблицы: **5** — `profiles`, `stores`, `products`, `orders`, `order_items`.
SQL: `replica/schema.sql` (PostgreSQL/Supabase + RLS).

- `profiles`: id (→ `auth.users.id`), email, full_name, phone, **role** (`customer` / `owner` / `admin`), timestamps.
- `stores`: id, owner_id (→ `profiles`), name, description, category, address…, status (`pending` / `approved` / `rejected` / `suspended`).
- `products`: id, store_id (→ `stores`), owner_id (→ `profiles`), name, description, price (integer cents), currency, stock, category, image_url, status (`draft` / `active` / `archived`).
- `orders`: id, user_id (→ `profiles`, покупатель), store_id (→ `stores`), status (`pending` / `confirmed` / `preparing` / `ready` / `completed` / `cancelled`), total (integer cents), currency, customer_* snapshot, note, placed_at, timestamps.
- `order_items`: id, order_id (→ `orders`), product_id (→ `products`, set null), store_id (→ `stores`), product_name (snapshot), price (integer cents, snapshot), currency, quantity.

Ключевые ограничения:

- Денежные суммы — **integer cents + currency** (не float/decimal).
- Время — `timestamptz` (UTC).
- Заказ = покупка из одного магазина (`store_id`); состав — `order_items` с снапшотом цены/имени.
- Статусные поля — `check` constraints.
- Идемпотентность оформления заказа + атомарное списание стока (`stock >= quantity`).

## Access / RLS

RLS включён на всех таблицах; вспомогательные `security definer` функции:
`public.current_role()`, `public.is_admin()`, `public.is_store_owner(store_id)`, `public.is_approved_store(store_id)`, `public.can_read_order(order_id, user_id)`.

- **Customer**: свои `orders` (select/insert/update pending), `products`: только `active` у одобренных магазинов, `stores`: только `approved`.
- **Owner**: свои `stores` (insert/select/update), свои `products` (insert/select/update через `owner_id` + `is_store_owner`), `orders` своих магазинов (update статуса).
- **Admin**: все `stores` / `products` / `orders` (select/update), в т.ч. смена статуса магазина (`pending → approved / rejected / suspended`).

## API

PostgREST-эндпоинты Supabase + Edge Functions:

| method path | what it does | who can call | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| POST /auth/v1/token?grant_type=password | вход (GoTrue) → JWT | user | credentials | tokens | auth |
| GET /rest/v1/stores?status=eq.approved | каталог одобренных магазинов | anon/customer | — | stores | browse |
| GET /rest/v1/products?store_id=eq.&status=eq.active | товары магазина | customer | store_id | products | catalog |
| POST /rest/v1/orders | оформление заказа | customer (user_id = auth.uid()) | order + items | order | order |
| GET /rest/v1/orders?user_id=eq. | свои заказы | customer | user_id | orders | orders |
| PATCH /rest/v1/orders?id=eq.&status=eq.pending | отмена заказа | customer (свой, pending) | status | order | orders |
| PATCH /rest/v1/stores?id=eq.&status=eq.approved | одобрение магазина | admin | status | store | admin |
| POST /functions/v1/stores | создать магазин | owner | store data | store (pending) | owner |
| POST /functions/v1/orders | атомарное оформление заказа | customer | cart | order+items | order |

## The parts that bite

- **Idempotency**: оформление заказа (двойной тап) — idempotency key / уникальность.
- **Race conditions**: списание стока — атомарный `UPDATE ... WHERE stock >= qty`.
- **Money**: integer cents + currency; `total = sum(price*quantity)`.
- **Realtime**: уведомления о смене статуса заказа (Supabase Realtime).
- **RLS**: security definer функции; `auth.users.id = profiles.id`.

## Build order

1. **Вертикальный срез**: вход → каталог одобренных магазинов → товар → корзина → заказ; owner: магазин → pending; admin: approve; owner: product draft→active.
2. Must-haves: owner CRUD товаров, статусы заказов, wishlist, поиск.
3. Should/could: coupons, realtime-уведомления, аналитика.

## Next

`replica/schema.sql` — DDL с RLS — готова. Следующий шаг: `@replica-design` — дизайн-система и токены.
