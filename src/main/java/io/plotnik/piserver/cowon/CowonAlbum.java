package io.plotnik.piserver.cowon;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CowonAlbum {

    /**
     * Название папки с альбомом
     */
    String name;

    /**
     * Альбомы одного исполнителя могут быть объединены в одну папку (не обязательно)
     */
    String artist;

    /**
     * На плейере папки исполнителей могут быть разнесены в папки с алфавитом (не обязательно)
     */
    String az;

    /**
     * URL с адресом обложки альбома, которую мы хотим найти в нашей папке с музыкой
     */
    String cover;

    /**
     * Метка диска на плейере
     */
    String drive;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAz() {
        return az;
    }

    public void setAz(String az) {
        this.az = az;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getDrive() {
        return drive;
    }

    public void setDrive(String drive) {
        this.drive = drive;
    }

    @Override
    public String toString() {
        return ("x9".equals(drive) ? "1" : "2") + ":" + az + "/" + (artist != null ? artist + "/" : "") + name;
    }
}
