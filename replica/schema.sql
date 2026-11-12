-- replica/schema.sql
-- Honey Mart — маркетплейс локальных рынков (Supabase / PostgreSQL).
-- Проект: Honey-Mart-Android-Client (роли: User / Owner / Admin).
-- Диалект: PostgreSQL (Supabase). Отличается от SQLite-версии Habitica-трекера:
--   * id  -> uuid, default gen_random_uuid()
--   * время -> timestamptz (UTC)
--   * money -> integer cents + currency
--   * access rules -> Row Level Security (Supabase)

-- ============ Таблицы ============

-- 1. Профили пользователей со ролями
create table public.profiles (
  id         uuid primary key references auth.users (id) on delete cascade,
  email      text not null,
  full_name  text,
  phone      text,
  role       text not null default 'customer'
             check (role in ('customer', 'owner', 'admin')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_profiles_role on public.profiles (role);

-- 2. Магазины
create table public.stores (
  id        uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles (id) on delete cascade,
  name     text not null,
  description text,
  category text,
  address  text,
  city     text,
  region   text,
  country  text,
  latitude double precision,
  longitude double precision,
  status   text not null default 'pending'
            check (status in ('pending', 'approved', 'rejected', 'suspended')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_stores_owner on public.stores (owner_id);
create index idx_stores_status on public.stores (status);

-- 3. Товары
create table public.products (
  id        uuid primary key default gen_random_uuid(),
  store_id uuid not null references public.stores (id) on delete cascade,
  owner_id uuid not null references public.profiles (id) on delete cascade,
  name     text not null,
  description text,
  price    integer not null, -- cents
  currency text not null default 'USD',
  stock    integer not null default 0,
  category text,
  image_url text,
  status   text not null default 'draft'
            check (status in ('draft', 'active', 'archived')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_products_store_status on public.products (store_id, status);
create index idx_products_owner on public.products (owner_id);

-- 4. Заказы
create table public.orders (
  id         uuid primary key default gen_random_uuid(),
  user_id   uuid not null references public.profiles (id) on delete cascade,
  store_id  uuid not null references public.stores (id) on delete cascade,
  status    text not null default 'pending'
            check (status in ('pending', 'confirmed', 'preparing', 'ready', 'completed', 'cancelled')),
  total     integer not null, -- cents
  currency  text not null default 'USD',
  customer_name text,
  customer_phone text,
  customer_address text,
  note text,
  placed_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_orders_user on public.orders (user_id, status, placed_at);
create index idx_orders_store on public.orders (store_id, status, placed_at);

-- 5. Состав заказа (снапшот товара на момент покупки)
create table public.order_items (
  id          uuid primary key default gen_random_uuid(),
  order_id   uuid not null references public.orders (id) on delete cascade,
  product_id uuid references public.products (id) on delete set null,
  store_id   uuid not null references public.stores (id) on delete cascade,
  product_name text not null, -- snapshot
  price      integer not null, -- cents, snapshot
  currency   text not null default 'USD',
  quantity   integer not null,
  created_at timestamptz not null default now()
);

create index idx_order_items_order on public.order_items (order_id);
create index idx_order_items_store on public.order_items (store_id);

-- ============ Вспомогательные функции доступа (security definer) ============

-- текущая роль пользователя (через profiles)
create or replace function public.current_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select p.role from public.profiles p where p.id = auth.uid();
$$;

-- админ ли пользователь
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.role = 'admin'
  );
$$;

-- владелец ли магазина
create or replace function public.is_store_owner(store_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.stores s
    where s.id = store_id and s.owner_id = auth.uid()
  );
$$;

-- магазин ли одобрен
create or replace function public.is_approved_store(store_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.stores s
    where s.id = store_id and s.status = 'approved'
  );
$$;

-- чтение заказа: покупатель (user_id), владелец магазина или админ
create or replace function public.can_read_order(order_id uuid, user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.orders o
    where o.id = order_id
      and (o.user_id = user_id or public.is_store_owner(o.store_id) or public.is_admin())
  );
$$;

-- ============ updated_at ============

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger trg_profiles_updated
  before update on public.profiles
  for each row execute function public.set_updated_at();

create trigger trg_stores_updated
  before update on public.stores
  for each row execute function public.set_updated_at();

create trigger trg_products_updated
  before update on public.products
  for each row execute function public.set_updated_at();

create trigger trg_orders_updated
  before update on public.orders
  for each row execute function public.set_updated_at();

-- ============ Row Level Security ============

alter table public.profiles enable row level security;
alter table public.stores enable row level security;
alter table public.products enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

-- profiles: читают/правят только себя; админ — все
create policy profiles_select on public.profiles
  for select using (auth.uid() = id or public.is_admin());

create policy profiles_update on public.profiles
  for update using (auth.uid() = id or public.is_admin())
  with check (auth.uid() = id or public.is_admin());

create policy profiles_insert on public.profiles
  for insert with check (auth.uid() = id);

-- stores: клиент — только одобренные; владелец — свои; админ — все
create policy stores_select_customer on public.stores
  for select using (status = 'approved');

create policy stores_select_owner on public.stores
  for select using (owner_id = auth.uid());

create policy stores_select_admin on public.stores
  for select using (public.is_admin());

create policy stores_owner_insert on public.stores
  for insert with check (public.current_role() = 'owner' and new.owner_id = auth.uid());

create policy stores_owner_update on public.stores
  for update using (owner_id = auth.uid or public.is_admin());

-- products: клиент — только active товары одобренных магазинов; владелец — свои; админ — все
create policy products_select_active on public.products
  for select using (status = 'active' and public.is_approved_store(store_id));

create policy products_select_owner on public.products
  for select using (owner_id = auth.uid());

create policy products_select_admin on public.products
  for select using (public.is_admin());

create policy products_owner_insert on public.products
  for insert with check (
    public.current_role() = 'owner'
    and new.owner_id = auth.uid()
    and public.is_store_owner(new.store_id)
  );

create policy products_owner_update on public.products
  for update using (owner_id = auth.uid or public.is_admin());

-- orders: покупатель — свои; владелец магазина — заказы своего магазина; админ — все
create policy orders_select_customer on public.orders
  for select using (user_id = auth.uid());

create policy orders_select_owner on public.orders
  for select using (public.is_store_owner(orders.store_id));

create policy orders_select_admin on public.orders
  for select using (public.is_admin());

create policy orders_customer_insert on public.orders
  for insert with check (user_id = auth.uid());

create policy orders_customer_update on public.orders
  for update using (user_id = auth.uid() and status = 'pending');

create policy orders_owner_update on public.orders
  for update using (public.is_store_owner(orders.store_id));

create policy orders_admin_update on public.orders
  for update using (public.is_admin());

-- order_items: чтение — через доступ к заказу; запись — только service role / Edge Functions
create policy order_items_select on public.order_items
  for select using (public.can_read_order(order_items.order_id, auth.uid()));

-- ============ Grants (Supabase) ============

grant usage on schema public to authenticated;
grant select, insert, update on all tables in schema public to authenticated;
grant usage, select on all sequences in schema public to authenticated;
