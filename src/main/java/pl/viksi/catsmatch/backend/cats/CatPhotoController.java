package pl.viksi.catsmatch.backend.cats;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.viksi.catsmatch.backend.common.ApiException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

@RestController
@RequestMapping("/cats/{id}/photo")
public class CatPhotoController {
    private final CatService cats;
    private final CatRepository catRepository;
    private final CatPhotoRepository photos;
    public CatPhotoController(CatService cats, CatRepository catRepository, CatPhotoRepository photos) {
        this.cats=cats; this.catRepository=catRepository; this.photos=photos;
    }
    private void lockOwned(int id, Authentication auth) {
        Cat cat=catRepository.lockCats(List.of(id)).stream().findFirst().orElseThrow(()->ApiException.missing("Cat"));
        if(!cat.ownerId.equals(cats.userId(auth)))throw ApiException.forbidden();
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void upload(@PathVariable int id, Authentication auth, @RequestPart MultipartFile file) throws IOException {
        lockOwned(id,auth);
        if(file.isEmpty() || file.getSize()>5*1024*1024)throw ApiException.invalid("Select a JPEG or PNG up to 5 MiB");
        byte[] output;
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext())throw ApiException.invalid("Invalid photo");
            var reader=readers.next();
            try {
                reader.setInput(input);
                String format=reader.getFormatName();
                if(!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))throw ApiException.invalid("Only JPEG and PNG photos are accepted");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1 || height<1 || (long)width*height>24_000_000L)throw ApiException.invalid("Photo exceeds 24 megapixels");
                BufferedImage source=reader.read(0);
                double scale=Math.min(1.0,1600.0/Math.max(width,height));
                BufferedImage target=new BufferedImage(Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)),BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics=target.createGraphics();
                try {graphics.setColor(Color.WHITE);graphics.fillRect(0,0,target.getWidth(),target.getHeight());graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(source,0,0,target.getWidth(),target.getHeight(),null);}finally{graphics.dispose();}
                var encoded=new ByteArrayOutputStream();ImageIO.write(target,"JPEG",encoded);output=encoded.toByteArray();
            }finally{reader.dispose();}
        }catch(javax.imageio.IIOException ex){throw ApiException.invalid("Invalid or damaged photo");}
        // Decode and re-encode pixels instead of serving untrusted original bytes or metadata.
        photos.saveAndFlush(new CatPhoto(id,output));
    }
    @GetMapping @Transactional(readOnly=true)
    public ResponseEntity<byte[]> get(@PathVariable int id) {
        cats.cat(id);
        CatPhoto photo=photos.findById(id).orElseThrow(()->ApiException.missing("Photo"));
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options","nosniff").body(photo.content);
    }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void delete(@PathVariable int id,Authentication auth) {lockOwned(id,auth);photos.deleteById(id);photos.flush();}
}
