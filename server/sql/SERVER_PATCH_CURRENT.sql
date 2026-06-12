-- MasyaLink v1.6 current server patch
-- Safe to run multiple times. Keeps only the current RPC definitions.

create extension if not exists pgcrypto;

insert into public.rooms (room_code)
values ('pink-masya-tema-mili')
on conflict (room_code) do nothing;

create table if not exists public.chat_accounts (
  id uuid primary key default gen_random_uuid(),
  room_id uuid not null references public.rooms(id) on delete cascade,
  username text not null,
  display_name text not null,
  pin_hash text not null,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  deleted_at timestamptz,
  avatar_path text
);

alter table public.chat_accounts add column if not exists avatar_path text;
alter table public.chat_accounts add column if not exists is_active boolean not null default true;
alter table public.chat_accounts add column if not exists deleted_at timestamptz;
alter table public.chat_accounts add column if not exists updated_at timestamptz not null default now();
alter table public.chat_accounts add column if not exists last_seen_at timestamptz not null default now();

create unique index if not exists chat_accounts_room_username_active_idx
on public.chat_accounts (room_id, lower(username))
where deleted_at is null;

alter table public.devices add column if not exists account_id uuid;
alter table public.devices add column if not exists is_active boolean not null default true;
alter table public.devices add column if not exists deleted_at timestamptz;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'devices_account_id_fkey') then
    alter table public.devices
      add constraint devices_account_id_fkey
      foreign key (account_id) references public.chat_accounts(id) on delete cascade;
  end if;
end $$;

alter table public.messages add column if not exists sender_account_id uuid;
alter table public.messages add column if not exists sender_username_snapshot text;
alter table public.messages add column if not exists reply_to_message_id uuid;
alter table public.messages add column if not exists reply_to_text_snapshot text;
alter table public.messages add column if not exists reply_to_username_snapshot text;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'messages_sender_account_id_fkey') then
    alter table public.messages
      add constraint messages_sender_account_id_fkey
      foreign key (sender_account_id) references public.chat_accounts(id) on delete cascade;
  end if;
end $$;

create table if not exists public.message_reads (
  message_id uuid not null references public.messages(id) on delete cascade,
  device_id uuid not null references public.devices(id) on delete cascade,
  account_id uuid,
  delivered_at timestamptz,
  read_at timestamptz,
  primary key (message_id, device_id)
);

alter table public.message_reads add column if not exists account_id uuid;
alter table public.message_reads add column if not exists delivered_at timestamptz;
alter table public.message_reads add column if not exists read_at timestamptz;

create table if not exists public.typing_events (
  room_id uuid not null references public.rooms(id) on delete cascade,
  device_id uuid not null references public.devices(id) on delete cascade,
  is_typing boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (room_id, device_id)
);

alter table public.chat_accounts enable row level security;
alter table public.devices enable row level security;
alter table public.messages enable row level security;
alter table public.message_reads enable row level security;
alter table public.typing_events enable row level security;

-- Storage policies for private media bucket.
drop policy if exists "chat_media_select_authenticated" on storage.objects;
drop policy if exists "chat_media_insert_authenticated" on storage.objects;
drop policy if exists "chat_media_update_authenticated" on storage.objects;
drop policy if exists "chat_media_delete_authenticated" on storage.objects;

create policy "chat_media_select_authenticated"
on storage.objects for select
to authenticated
using (bucket_id = 'media');

create policy "chat_media_insert_authenticated"
on storage.objects for insert
to authenticated
with check (bucket_id = 'media');

create policy "chat_media_update_authenticated"
on storage.objects for update
to authenticated
using (bucket_id = 'media')
with check (bucket_id = 'media');

create policy "chat_media_delete_authenticated"
on storage.objects for delete
to authenticated
using (bucket_id = 'media');

-- Drop RPCs with return types that may have changed.
drop function if exists public.get_fixed_profiles() cascade;
drop function if exists public.select_fixed_profile(text, text, text, text) cascade;
drop function if exists public.update_fixed_profile_avatar(text, text) cascade;

create or replace function public.get_fixed_profiles()
returns table (
  profile_key text,
  display_name text,
  avatar_path text
)
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_room_id uuid;
begin
  if auth.uid() is null then
    raise exception 'User is not authenticated';
  end if;

  insert into public.rooms (room_code)
  values ('pink-masya-tema-mili')
  on conflict (room_code) do nothing;

  select r.id into v_room_id
  from public.rooms as r
  where r.room_code = 'pink-masya-tema-mili';

  insert into public.chat_accounts (room_id, username, display_name, pin_hash, is_active, last_seen_at)
  values
    (v_room_id, 'A', 'Profile A', crypt(gen_random_uuid()::text, gen_salt('bf')), true, now()),
    (v_room_id, 'B', 'Profile B', crypt(gen_random_uuid()::text, gen_salt('bf')), true, now())
  on conflict do nothing;

  return query
  select ca.username::text, ca.display_name::text, ca.avatar_path::text
  from public.chat_accounts as ca
  where ca.room_id = v_room_id
    and ca.username in ('A', 'B')
    and ca.deleted_at is null
  order by ca.username;
end;
$$;

create or replace function public.select_fixed_profile(
  p_profile_key text,
  p_display_name text default null,
  p_device_name text default null,
  p_fcm_token text default null
)
returns table (
  account_id uuid,
  room_id uuid,
  device_id uuid,
  profile_key text,
  display_name text,
  avatar_path text
)
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
  v_room_id uuid;
  v_account_id uuid;
  v_device_id uuid;
  v_profile_key text;
  v_default_display_name text;
  v_effective_display_name text;
  v_avatar_path text;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;

  v_profile_key := upper(trim(p_profile_key));
  if v_profile_key not in ('A', 'B') then raise exception 'Invalid profile key'; end if;

  v_default_display_name := case when v_profile_key = 'A' then 'Profile A' else 'Profile B' end;

  insert into public.rooms (room_code)
  values ('pink-masya-tema-mili')
  on conflict (room_code) do nothing;

  select r.id into v_room_id
  from public.rooms as r
  where r.room_code = 'pink-masya-tema-mili';

  select ca.id, ca.display_name, ca.avatar_path
  into v_account_id, v_effective_display_name, v_avatar_path
  from public.chat_accounts as ca
  where ca.room_id = v_room_id
    and ca.username = v_profile_key
    and ca.deleted_at is null;

  if v_account_id is null then
    insert into public.chat_accounts as ca (room_id, username, display_name, pin_hash, is_active, last_seen_at)
    values (v_room_id, v_profile_key, v_default_display_name, crypt(gen_random_uuid()::text, gen_salt('bf')), true, now())
    returning ca.id, ca.display_name, ca.avatar_path
    into v_account_id, v_effective_display_name, v_avatar_path;
  else
    update public.chat_accounts as ca
    set is_active = true, last_seen_at = now(), updated_at = now()
    where ca.id = v_account_id
    returning ca.display_name, ca.avatar_path
    into v_effective_display_name, v_avatar_path;
  end if;

  insert into public.devices as d (room_id, user_id, account_id, display_name, device_name, fcm_token, last_seen_at, is_active, deleted_at)
  values (v_room_id, v_user_id, v_account_id, v_effective_display_name, p_device_name, p_fcm_token, now(), true, null)
  on conflict on constraint devices_room_id_user_id_key
  do update set
    account_id = excluded.account_id,
    display_name = excluded.display_name,
    device_name = excluded.device_name,
    fcm_token = excluded.fcm_token,
    last_seen_at = now(),
    is_active = true,
    deleted_at = null
  returning d.id into v_device_id;

  return query select v_account_id, v_room_id, v_device_id, v_profile_key, v_effective_display_name, v_avatar_path;
end;
$$;

create or replace function public.update_fixed_profile_name(
  p_profile_key text,
  p_display_name text
)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
  v_profile_key text;
  v_room_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;

  v_profile_key := upper(trim(p_profile_key));
  if v_profile_key not in ('A', 'B') then raise exception 'Invalid profile key'; end if;
  if p_display_name is null or length(trim(p_display_name)) < 1 then raise exception 'Display name is empty'; end if;

  select r.id into v_room_id from public.rooms as r where r.room_code = 'pink-masya-tema-mili';
  if v_room_id is null then raise exception 'Default room not found'; end if;

  if not exists (
    select 1
    from public.devices as d
    join public.chat_accounts as ca on ca.id = d.account_id
    where d.user_id = v_user_id and d.room_id = v_room_id and ca.username = v_profile_key
  ) then
    raise exception 'Current device is not linked to this profile';
  end if;

  update public.chat_accounts as ca
  set display_name = trim(p_display_name), updated_at = now()
  where ca.room_id = v_room_id and ca.username = v_profile_key and ca.deleted_at is null;

  update public.devices as d
  set display_name = trim(p_display_name)
  where d.account_id in (
    select ca.id from public.chat_accounts as ca where ca.room_id = v_room_id and ca.username = v_profile_key and ca.deleted_at is null
  );
end;
$$;

create or replace function public.update_fixed_profile_avatar(
  p_profile_key text,
  p_avatar_path text
)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
  v_profile_key text;
  v_room_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;

  v_profile_key := upper(trim(p_profile_key));
  if v_profile_key not in ('A', 'B') then raise exception 'Invalid profile key'; end if;

  select r.id into v_room_id from public.rooms as r where r.room_code = 'pink-masya-tema-mili';
  if v_room_id is null then raise exception 'Default room not found'; end if;

  if not exists (
    select 1
    from public.devices as d
    join public.chat_accounts as ca on ca.id = d.account_id
    where d.user_id = v_user_id and d.room_id = v_room_id and ca.username = v_profile_key
  ) then
    raise exception 'Current device is not linked to this profile';
  end if;

  update public.chat_accounts as ca
  set avatar_path = p_avatar_path, updated_at = now()
  where ca.room_id = v_room_id and ca.username = v_profile_key and ca.deleted_at is null;
end;
$$;

create or replace function public.send_message_v2(
  p_room_id uuid,
  p_sender_device_id uuid,
  p_type text,
  p_text text default null,
  p_media_path text default null,
  p_media_mime text default null,
  p_media_size bigint default null,
  p_media_duration_ms integer default null,
  p_media_original_name text default null,
  p_reply_to_message_id uuid default null
)
returns public.messages
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_message public.messages;
  v_sender_user_id uuid;
  v_sender_account_id uuid;
  v_sender_name text;
  v_reply_text text;
  v_reply_username text;
begin
  if p_type not in ('text', 'voice', 'video', 'video_note', 'image', 'file') then raise exception 'Invalid message type'; end if;

  select d.user_id, d.account_id, coalesce(a.display_name, d.display_name, a.username)
  into v_sender_user_id, v_sender_account_id, v_sender_name
  from public.devices as d
  left join public.chat_accounts as a on a.id = d.account_id
  where d.id = p_sender_device_id and d.room_id = p_room_id;

  if v_sender_user_id is null then raise exception 'Sender device is not registered in this room'; end if;
  if p_type = 'text' and (p_text is null or length(trim(p_text)) = 0) then raise exception 'Text message cannot be empty'; end if;
  if p_type <> 'text' and p_media_path is null then raise exception 'Media message requires media_path'; end if;

  if p_reply_to_message_id is not null then
    select coalesce(nullif(m.text, ''), m.media_original_name,
      case when m.type = 'image' then 'Image' when m.type = 'video' then 'Video' when m.type = 'video_note' then 'Circle video' when m.type = 'voice' then 'Voice message' else 'Attachment' end),
      coalesce(m.sender_username_snapshot, a.display_name, a.username, d.display_name, 'message')
    into v_reply_text, v_reply_username
    from public.messages as m
    left join public.chat_accounts as a on a.id = m.sender_account_id
    left join public.devices as d on d.id = m.sender_device_id
    where m.id = p_reply_to_message_id and m.room_id = p_room_id;
    if v_reply_text is null then raise exception 'Reply target not found'; end if;
  end if;

  insert into public.messages (
    room_id, sender_device_id, sender_user_id, sender_account_id, sender_username_snapshot,
    type, text, media_path, media_mime, media_size, media_duration_ms, media_original_name,
    expires_at, reply_to_message_id, reply_to_text_snapshot, reply_to_username_snapshot
  ) values (
    p_room_id, p_sender_device_id, v_sender_user_id, v_sender_account_id, v_sender_name,
    p_type, p_text, p_media_path, p_media_mime, p_media_size, p_media_duration_ms, p_media_original_name,
    case when p_type = 'text' then null else now() + interval '30 days' end,
    p_reply_to_message_id, v_reply_text, v_reply_username
  ) returning * into v_message;

  return v_message;
end;
$$;

create or replace function public.logout_device(p_device_id uuid)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;
  delete from public.devices as d where d.id = p_device_id and d.user_id = v_user_id;
end;
$$;

create or replace function public.touch_device_presence(p_device_id uuid)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
  v_account_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;

  update public.devices as d
  set last_seen_at = now(), is_active = true, deleted_at = null
  where d.id = p_device_id and d.user_id = v_user_id
  returning d.account_id into v_account_id;

  if not found then raise exception 'Device does not belong to current user'; end if;

  if v_account_id is not null then
    update public.chat_accounts as ca set last_seen_at = now(), updated_at = now() where ca.id = v_account_id;
  end if;
end;
$$;

create or replace function public.set_typing_state(p_room_id uuid, p_device_id uuid, p_is_typing boolean)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;
  if not exists (select 1 from public.devices as d where d.id = p_device_id and d.room_id = p_room_id and d.user_id = v_user_id) then
    raise exception 'Device is not registered in this room';
  end if;
  insert into public.typing_events as t (room_id, device_id, is_typing, updated_at)
  values (p_room_id, p_device_id, p_is_typing, now())
  on conflict (room_id, device_id) do update set is_typing = excluded.is_typing, updated_at = now();
end;
$$;

create or replace function public.get_typing_states(p_room_id uuid)
returns table (room_id uuid, device_id uuid, account_id uuid, display_name text, is_typing boolean, updated_at timestamptz)
language plpgsql
security definer
set search_path = public, extensions
as $$
begin
  if auth.uid() is null then raise exception 'User is not authenticated'; end if;
  return query
  select t.room_id, t.device_id, d.account_id, coalesce(ca.display_name, d.display_name)::text, t.is_typing, t.updated_at
  from public.typing_events as t
  join public.devices as d on d.id = t.device_id
  left join public.chat_accounts as ca on ca.id = d.account_id
  where t.room_id = p_room_id and t.updated_at > now() - interval '8 seconds';
end;
$$;

create or replace function public.mark_messages_delivered(p_room_id uuid, p_device_id uuid)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
  v_account_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;
  select d.account_id into v_account_id from public.devices as d where d.id = p_device_id and d.user_id = v_user_id and d.room_id = p_room_id;
  if v_account_id is null then raise exception 'Device is not registered in this room'; end if;
  insert into public.message_reads as r (message_id, device_id, account_id, delivered_at)
  select m.id, p_device_id, v_account_id, now()
  from public.messages as m
  where m.room_id = p_room_id and coalesce(m.sender_account_id, '00000000-0000-0000-0000-000000000000'::uuid) <> v_account_id
  on conflict (message_id, device_id) do update set delivered_at = coalesce(r.delivered_at, excluded.delivered_at), account_id = excluded.account_id;
end;
$$;

create or replace function public.mark_messages_read(p_room_id uuid, p_device_id uuid)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
  v_account_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;
  select d.account_id into v_account_id from public.devices as d where d.id = p_device_id and d.user_id = v_user_id and d.room_id = p_room_id;
  if v_account_id is null then raise exception 'Device is not registered in this room'; end if;
  insert into public.message_reads as r (message_id, device_id, account_id, delivered_at, read_at)
  select m.id, p_device_id, v_account_id, now(), now()
  from public.messages as m
  where m.room_id = p_room_id and coalesce(m.sender_account_id, '00000000-0000-0000-0000-000000000000'::uuid) <> v_account_id
  on conflict (message_id, device_id) do update set read_at = now(), delivered_at = coalesce(r.delivered_at, now()), account_id = excluded.account_id;
end;
$$;

create or replace function public.get_message_receipts(p_room_id uuid)
returns table (message_id uuid, account_id uuid, delivered_at timestamptz, read_at timestamptz)
language plpgsql
security definer
set search_path = public, extensions
as $$
begin
  if auth.uid() is null then raise exception 'User is not authenticated'; end if;
  return query
  select r.message_id, r.account_id, r.delivered_at, r.read_at
  from public.message_reads as r
  join public.messages as m on m.id = r.message_id
  where m.room_id = p_room_id;
end;
$$;

create or replace function public.delete_message_for_everyone(p_room_id uuid, p_message_id uuid, p_device_id uuid)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;
  if not exists (select 1 from public.devices as d where d.id = p_device_id and d.room_id = p_room_id and d.user_id = v_user_id) then
    raise exception 'Device is not registered in this room';
  end if;
  delete from public.messages as m where m.id = p_message_id and m.room_id = p_room_id;
end;
$$;

create or replace function public.clear_room_messages(p_room_id uuid, p_device_id uuid)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_user_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then raise exception 'User is not authenticated'; end if;
  if not exists (select 1 from public.devices as d where d.id = p_device_id and d.room_id = p_room_id and d.user_id = v_user_id) then
    raise exception 'Device is not registered in this room';
  end if;
  delete from public.typing_events as t where t.room_id = p_room_id;
  delete from public.messages as m where m.room_id = p_room_id;
end;
$$;

grant execute on function public.get_fixed_profiles() to authenticated;
grant execute on function public.select_fixed_profile(text, text, text, text) to authenticated;
grant execute on function public.update_fixed_profile_name(text, text) to authenticated;
grant execute on function public.update_fixed_profile_avatar(text, text) to authenticated;
grant execute on function public.send_message_v2(uuid, uuid, text, text, text, text, bigint, integer, text, uuid) to authenticated;
grant execute on function public.logout_device(uuid) to authenticated;
grant execute on function public.touch_device_presence(uuid) to authenticated;
grant execute on function public.set_typing_state(uuid, uuid, boolean) to authenticated;
grant execute on function public.get_typing_states(uuid) to authenticated;
grant execute on function public.mark_messages_delivered(uuid, uuid) to authenticated;
grant execute on function public.mark_messages_read(uuid, uuid) to authenticated;
grant execute on function public.get_message_receipts(uuid) to authenticated;
grant execute on function public.delete_message_for_everyone(uuid, uuid, uuid) to authenticated;
grant execute on function public.clear_room_messages(uuid, uuid) to authenticated;

notify pgrst, 'reload schema';
