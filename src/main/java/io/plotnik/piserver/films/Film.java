package io.plotnik.piserver.films;

public class Film {

    String name;
    String image;
    String director;
    String usb;
    String torrent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getUsb() {
        return usb;
    }

    public void setUsb(String usb) {
        this.usb = usb;
    }

    public String getTorrent() {
        return torrent;
    }

    public void setTorrent(String torrent) {
        this.torrent = torrent;
    }
    
}