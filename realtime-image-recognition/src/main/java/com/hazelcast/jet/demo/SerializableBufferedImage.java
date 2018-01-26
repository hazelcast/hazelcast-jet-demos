package com.hazelcast.jet.demo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.imageio.ImageIO;

/**
 * date: 1/26/18
 * author: emindemirci
 */
public class SerializableBufferedImage implements Serializable {

    private transient BufferedImage image;

    public SerializableBufferedImage(BufferedImage image) {
        this.image = image;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        ImageIO.write(image, "jpg", out); // png is lossless
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        image = ImageIO.read(in);
    }

    public BufferedImage getImage() {
        return image;
    }
}
