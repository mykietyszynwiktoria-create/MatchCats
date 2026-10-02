-- Existing saved pairs are not evidence of mutual consent.
ALTER TABLE mc_cat_pairs ADD COLUMN status varchar(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE mc_cat_pairs ADD COLUMN proposed_by integer REFERENCES mc_accounts(id) ON DELETE SET NULL;
ALTER TABLE mc_cat_pairs ADD COLUMN decided_at timestamptz;
ALTER TABLE mc_cat_pairs ADD CONSTRAINT mc_pair_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'WITHDRAWN'));
