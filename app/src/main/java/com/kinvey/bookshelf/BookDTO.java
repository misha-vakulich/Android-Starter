package com.kinvey.bookshelf;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

/**
 * Created by Prots on 3/15/16.
 */
public class BookDTO extends GenericJson {
    public static final String COLLECTION = "Book";

    @Key("name")
    private String name;

    @Key("image_id")
    private String imageId;

    public BookDTO(){};

    public BookDTO(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}
