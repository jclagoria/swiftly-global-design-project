# 04_Schema_Entities

This file defines the normalized PostgreSQL schema for the User Service and its related entities.

---

## 1. users

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## enable the pgcrypto extension once per database:
```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

### set the default value for the id column:
```sql
ALTER TABLE users
    ALTER COLUMN id
        SET DEFAULT gen_random_uuid();
```

### Add soft-delete columns to users:
```sql
ALTER TABLE users
  ADD COLUMN deleted        BOOLEAN        NOT NULL DEFAULT FALSE,
  ADD COLUMN deleted_at     TIMESTAMPTZ    NULL;
```

### (Optional) add an index on deleted for faster lookups:
```sql
CREATE INDEX idx_users_deleted ON users(deleted);
```

## 2. user_profiles

``` sql
CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY
        REFERENCES users(id)
        ON DELETE CASCADE,
    phone VARCHAR(20),
    address TEXT,
    locale VARCHAR(10) NOT NULL DEFAULT 'en-US',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## 3. transactions

```sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY gen_random_uuid(),
    user_id UUID NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,
    amount NUMERIC(19,4) NOT NULL,
    currency_from CHAR(3) NOT NULL,
    currency_to CHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## 4. payment_providers

```sql
CREATE TABLE payment_providers (
    id SERIAL PRIMARY KEY gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    config JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## 5.transaction_providers

```sql
CREATE TABLE transaction_providers (
    transaction_id UUID PRIMARY KEY
        REFERENCES transactions(id)
        ON DELETE CASCADE,
    provider_id INT NOT NULL
        REFERENCES payment_providers(id),
    status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMPTZ
);
```

## 6.refresh_tokens

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,
    token UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(token)
);
```

### Add index for token lookup
```sql
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

## 6. exchange_rates

```sql
CREATE TABLE exchange_rates (
    currency_pair CHAR(7) PRIMARY KEY,  -- e.g. 'USD:EUR'
    rate NUMERIC(18,8) NOT NULL,create a
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## 7. Create a table to hold revoked JWTs
```sql
CREATE TABLE revoked_tokens (
    token TEXT PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);
```

## 8. (Optional) Index on expires_at so you can purge old entries via a background job
```sql
CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens(expires_at);
```
## 9. Create table to hold reset tokens:
```sql
CREATE TABLE password_reset_tokens (
  token      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ  NOT NULL
);
```

## 10. Index for cleanup:
```sql  
CREATE INDEX idx_prt_expires_at ON password_reset_tokens(expires_at);
``` 

## 9.1. Create a table for refresh tokens:
```sql
CREATE TABLE refresh_tokens (
  token      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ  NOT NULL,
  revoked    BOOLEAN      NOT NULL DEFAULT FALSE
);
```

## 9.2 Index for cleanup:
```sql
CREATE INDEX idx_rt_expires_at ON refresh_tokens(expires_at);
```

## Indexes & Constraints

- users: unique index on email
- user_profiles: PK on user_id enforces one profile per user
- transactions: index on user_id for fast lookups
- exchange_rates: primary key on currency_pair
- Foreign-key ON DELETE CASCADE ensures cleanup of dependent records.