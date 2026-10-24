-- Run in a NEW Supabase project SQL Editor. Do not put service_role keys in Android.
create extension if not exists pgcrypto;
create table if not exists public.club_roles (
 user_id uuid primary key references auth.users(id) on delete cascade,
 role text not null check (role in ('super_admin','assistant_admin','member')),
 created_at timestamptz not null default now()
);
create table if not exists public.club_content (
 id uuid primary key default gen_random_uuid(),
 section text not null check (section in ('members','advisors','founders','committee','donations','notices','events','gallery','settings')),
 payload jsonb not null default '{}'::jsonb,
 updated_at timestamptz not null default now()
);
create table if not exists public.club_finances (
 id uuid primary key default gen_random_uuid(),
 entry_date date not null,
 kind text not null check(kind in ('income','expense')),
 category text not null,
 title text not null,
 amount numeric(14,2) not null check(amount > 0),
 counterparty text,
 method text,
 receipt_reference text,
 notes text,
 created_by uuid references auth.users(id),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
create index if not exists idx_club_finances_date on public.club_finances(entry_date desc);
create index if not exists idx_club_content_section on public.club_content(section);
create or replace function public.club_has_role(allowed text[])
returns boolean language sql stable security definer set search_path = '' as $$
 select exists(select 1 from public.club_roles r where r.user_id = (select auth.uid()) and r.role = any(allowed));
$$;
revoke all on function public.club_has_role(text[]) from public;
grant execute on function public.club_has_role(text[]) to authenticated;
alter table public.club_roles enable row level security;
alter table public.club_content enable row level security;
alter table public.club_finances enable row level security;
drop policy if exists "read own role" on public.club_roles;
create policy "read own role" on public.club_roles for select to authenticated using(user_id = (select auth.uid()));
drop policy if exists "super admin manages roles" on public.club_roles;
create policy "super admin manages roles" on public.club_roles for all to authenticated
 using(public.club_has_role(array['super_admin'])) with check(public.club_has_role(array['super_admin']));
drop policy if exists "public content read" on public.club_content;
create policy "public content read" on public.club_content for select to anon,authenticated using(true);
drop policy if exists "staff content insert" on public.club_content;
create policy "staff content insert" on public.club_content for insert to authenticated
 with check(public.club_has_role(array['super_admin','assistant_admin']));
drop policy if exists "staff content update" on public.club_content;
create policy "staff content update" on public.club_content for update to authenticated
 using(public.club_has_role(array['super_admin','assistant_admin'])) with check(public.club_has_role(array['super_admin','assistant_admin']));
drop policy if exists "staff content delete" on public.club_content;
create policy "staff content delete" on public.club_content for delete to authenticated
 using(public.club_has_role(array['super_admin','assistant_admin']));
drop policy if exists "finance staff select" on public.club_finances;
create policy "finance staff select" on public.club_finances for select to authenticated
 using(public.club_has_role(array['super_admin','assistant_admin']));
drop policy if exists "finance staff insert" on public.club_finances;
create policy "finance staff insert" on public.club_finances for insert to authenticated
 with check(public.club_has_role(array['super_admin','assistant_admin']) and created_by = (select auth.uid()));
drop policy if exists "finance staff update" on public.club_finances;
create policy "finance staff update" on public.club_finances for update to authenticated
 using(public.club_has_role(array['super_admin','assistant_admin'])) with check(public.club_has_role(array['super_admin','assistant_admin']));
drop policy if exists "finance super admin delete" on public.club_finances;
create policy "finance super admin delete" on public.club_finances for delete to authenticated
 using(public.club_has_role(array['super_admin']));
-- FIRST ADMIN BOOTSTRAP: create a user in Supabase Authentication, then
-- run once replacing UUID with that user's Auth user ID:
-- insert into public.club_roles(user_id,role) values ('REPLACE_WITH_AUTH_USER_UUID','super_admin');
-- Never publish personal phone/email in public club_content without consent.
