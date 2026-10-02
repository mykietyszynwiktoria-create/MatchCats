CREATE TABLE mc_cat_photos (
    cat_id INTEGER PRIMARY KEY REFERENCES mc_cats(id) ON DELETE CASCADE,
    content BYTEA NOT NULL
);
