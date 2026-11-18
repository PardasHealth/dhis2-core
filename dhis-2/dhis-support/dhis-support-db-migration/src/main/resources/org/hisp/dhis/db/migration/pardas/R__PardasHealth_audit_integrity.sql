-- Nullable for legacy compatibility. Enforcement rejects unsigned rows when explicitly enabled.
-- Never sign existing history automatically: its provenance must be established separately.
alter table audit add column if not exists integritytag varchar(67);
