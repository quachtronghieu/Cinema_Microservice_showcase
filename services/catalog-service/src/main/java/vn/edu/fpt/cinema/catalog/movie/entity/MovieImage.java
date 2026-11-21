package vn.edu.fpt.cinema.catalog.movie.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Movie_Image", schema = "dbo")
public class MovieImage {
    @Id @Column(name = "image_id", length = 36, nullable = false)
    private String imageId;
    @Column(name = "content_type", nullable = false, length = 20) private String contentType;
    @Column(nullable = false, columnDefinition = "varbinary(max)") private byte[] data;

    protected MovieImage() {}
    public MovieImage(String imageId, String contentType, byte[] data) {
        this.imageId = imageId; this.contentType = contentType; this.data = data;
    }
    public String getImageId() { return imageId; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}
