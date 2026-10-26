-- Run once in Supabase SQL Editor. Preserve existing users and orders.
ALTER TABLE public.users ALTER COLUMN password_hash DROP NOT NULL;
CREATE TABLE IF NOT EXISTS public.social_accounts (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
 provider varchar(20) NOT NULL CHECK(provider IN ('google','kakao')),
 provider_user_id varchar(255) NOT NULL,
 provider_email varchar(255),
 created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(provider,provider_user_id)
);
CREATE INDEX IF NOT EXISTS idx_social_accounts_user ON public.social_accounts(user_id);
-- Custom Tomcat session auth is NOT Supabase Auth. Avoid direct Data API access.
ALTER TABLE public.social_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
