-- Encrypted message relay foundation. The database stores ciphertext only.
-- Plaintext message bodies are intentionally not represented in this schema.
create table if not exists conversations (
    id uuid primary key,
    kind varchar(8) not null check (kind in ('DIRECT', 'GROUP')),
    title varchar(80),
    created_by uuid not null references users(id) on delete restrict,
    created_at timestamptz not null default now(),
    constraint conversations_title_required_for_group
        check ((kind = 'GROUP' and length(trim(coalesce(title, ''))) between 1 and 80)
            or (kind = 'DIRECT' and title is null))
);

create table if not exists conversation_members (
    conversation_id uuid not null references conversations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    joined_at timestamptz not null default now(),
    left_at timestamptz,
    primary key (conversation_id, user_id)
);

create index if not exists conversation_members_user_idx
    on conversation_members (user_id, joined_at desc)
    where left_at is null;

create table if not exists encrypted_messages (
    id uuid primary key,
    conversation_id uuid not null references conversations(id) on delete cascade,
    sender_user_id uuid not null references users(id) on delete restrict,
    ciphertext text not null,
    ciphertext_version smallint not null check (ciphertext_version > 0),
    client_message_id uuid not null,
    created_at timestamptz not null default now(),
    edited_at timestamptz,
    deleted_at timestamptz,
    constraint encrypted_messages_ciphertext_size
        check (length(ciphertext) between 1 and 65536),
    constraint encrypted_messages_client_id_unique
        unique (sender_user_id, client_message_id)
);

create index if not exists encrypted_messages_timeline_idx
    on encrypted_messages (conversation_id, created_at desc, id desc);

-- Membership is required to read or send. Leaving a conversation is represented
-- by left_at; historical membership remains for audit and message attribution.
