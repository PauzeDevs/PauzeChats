create extension if not exists citext;

create table if not exists users (
    id uuid primary key,
    email citext not null unique,
    password_hash text not null,
    created_at timestamptz not null default now(),
    disabled_at timestamptz
);

create table if not exists profiles (
    user_id uuid primary key references users(id) on delete cascade,
    username citext not null unique,
    display_name varchar(40) not null,
    bio varchar(200) not null default '',
    avatar_mime_type varchar(64),
    avatar_asset_key text,
    updated_at timestamptz not null default now(),
    constraint profiles_username_format check (username::text ~ '^[a-z0-9_]{3,24}$'),
    constraint profiles_display_name_not_blank check (length(trim(display_name)) between 1 and 40)
);

create table if not exists invites (
    id uuid primary key,
    code_hash bytea not null unique,
    max_uses integer not null default 1 check (max_uses > 0),
    use_count integer not null default 0 check (use_count >= 0),
    expires_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);

create table if not exists sessions (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    refresh_token_hash bytea not null unique,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    revoked_at timestamptz
);

create table if not exists friend_requests (
    id uuid primary key,
    from_user_id uuid not null references users(id) on delete cascade,
    to_user_id uuid not null references users(id) on delete cascade,
    status varchar(16) not null default 'PENDING',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint friend_requests_not_self check (from_user_id <> to_user_id),
    constraint friend_requests_status check (
        status in ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'BLOCKED')
    )
);

create unique index if not exists friend_requests_pending_unique
    on friend_requests (from_user_id, to_user_id)
    where status = 'PENDING';

create index if not exists friend_requests_to_user_idx
    on friend_requests (to_user_id, created_at desc);

create index if not exists sessions_user_idx
    on sessions (user_id, expires_at desc);
