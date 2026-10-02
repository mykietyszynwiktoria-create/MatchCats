package pl.viksi.catsmatch.backend.cats;

import jakarta.persistence.*;

@Entity @Table(name="mc_cat_photos")
public class CatPhoto {
    @Id @Column(name="cat_id") public Integer catId;
    @Column(nullable=false, columnDefinition="bytea") public byte[] content;
    protected CatPhoto() {}
    public CatPhoto(int catId, byte[] content) { this.catId=catId; this.content=content; }
}
