ALTER TABLE mc_documents ADD COLUMN verification_status VARCHAR(24) NOT NULL DEFAULT 'OWNER_UPLOADED';
ALTER TABLE mc_documents ADD COLUMN verification_requested_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE mc_documents ADD COLUMN verification_reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE mc_documents ADD COLUMN verification_note VARCHAR(1000);
ALTER TABLE mc_documents ADD CONSTRAINT mc_documents_verification_status_check
    CHECK (verification_status IN ('OWNER_UPLOADED','REVIEW_REQUESTED','VERIFIED','REJECTED'));
