-- BookMarket v2 - Supabase PostgreSQL schema
-- Run this file in Supabase SQL Editor.

create extension if not exists pgcrypto;

create table if not exists public.users (
    id uuid primary key default gen_random_uuid(),
    email varchar(255) not null unique,
    password_hash varchar(100) not null,
    name varchar(100) not null,
    phone varchar(30),
    address varchar(255),
    detail_address varchar(255),
    role varchar(20) not null default 'USER'
        check (role in ('USER', 'ADMIN')),
    created_at timestamptz not null default now()
);

create table if not exists public.books (
    id uuid primary key default gen_random_uuid(),
    isbn varchar(30) not null unique,
    title varchar(300) not null,
    author varchar(200),
    publisher varchar(200),
    description text,
    category varchar(100),
    price integer not null default 0 check (price >= 0),
    stock integer not null default 0 check (stock >= 0),
    published_date date,
    thumbnail_url text,
    created_at timestamptz not null default now()
);

create table if not exists public.orders (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.users(id),
    total_price integer not null check (total_price >= 0),
    shipping_address text not null,
    status varchar(30) not null default 'PAID'
        check (status in ('PENDING', 'PAID', 'CANCELLED', 'SHIPPED', 'COMPLETED')),
    created_at timestamptz not null default now()
);

create table if not exists public.order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references public.orders(id) on delete cascade,
    book_id uuid not null references public.books(id),
    quantity integer not null check (quantity > 0),
    price integer not null check (price >= 0)
);

create table if not exists public.email_verifications (
    id uuid primary key default gen_random_uuid(),
    email varchar(255) not null,
    verification_code varchar(20) not null,
    expires_at timestamptz not null,
    verified boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists idx_books_title on public.books(title);
create index if not exists idx_books_category on public.books(category);
create index if not exists idx_orders_user_created on public.orders(user_id, created_at desc);
create index if not exists idx_order_items_order on public.order_items(order_id);
create index if not exists idx_email_verifications_email on public.email_verifications(email);
