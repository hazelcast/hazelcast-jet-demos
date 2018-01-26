package com.hazelcast.jet.demo;

import boofcv.abst.scene.ImageClassifier;
import boofcv.abst.scene.ImageClassifier.Score;
import boofcv.deepboof.ImageClassifierVggCifar10;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.hazelcast.jet.Util.entry;

/**
 * date: 1/23/18
 * author: emindemirci
 */
public class ClassifierProcessor extends AbstractProcessor {

    private ImageClassifier<Planar<GrayF32>> classifier;
    private List<String> categories;

    @Override
    protected void init(Context context) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = classLoader.getResource("likevgg_cifar10.zip").getPath();
        unzip(path, "unzipped");
        classifier = new ImageClassifierVggCifar10();
        classifier.loadModel(new File("unzipped/likevgg_cifar10"));
        categories = classifier.getCategories();
    }

    @Override
    protected boolean tryProcess(int ordinal, Object item) throws Exception {
        TimestampedEntry entry = (TimestampedEntry) item;
        SerializableBufferedImage serializableBufferedImage = (SerializableBufferedImage) entry.getKey();
        BufferedImage image = serializableBufferedImage.getImage();

        Planar<GrayF32> planar = new Planar<>(GrayF32.class, image.getWidth(), image.getHeight(), 3);
        ConvertBufferedImage.convertFromPlanar(image, planar, true, GrayF32.class);

        classifier.classify(planar);
        List<Score> results = classifier.getAllResults();
        List<Entry<String, Double>> categoryWithScores = results.stream().map(score -> entry(categories.get(score.category), score.score)).collect(Collectors.toList());
        Entry<String, Double> maxScoredCategory = categoryWithScores
                .stream()
                .max(Comparator.comparing(Entry::getValue))
                .get();
        return emitFromTraverser(Traverser.over(new TimestampedEntry<>(entry.getTimestamp(), serializableBufferedImage, maxScoredCategory)));
    }

    private void unzip(String zipFilePath, String destDir) {
        try (ZipFile file = new ZipFile(zipFilePath)) {
            FileSystem fileSystem = FileSystems.getDefault();
            //Get file entries
            Enumeration<? extends ZipEntry> entries = file.entries();

            //We will unzip files in this folder
            String uncompressedDirectory = destDir + "/";
            Path path = fileSystem.getPath(uncompressedDirectory);
            if (path.toFile().exists()) {
                return;
            }
            Files.createDirectory(path);

            //Iterate over entries
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                //If directory then create a new directory in uncompressed folder
                if (entry.isDirectory()) {
                    System.out.println("Creating Directory:" + uncompressedDirectory + entry.getName());
                    Files.createDirectories(fileSystem.getPath(uncompressedDirectory + entry.getName()));
                }
                //Else create the file
                else {
                    InputStream is = file.getInputStream(entry);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    String uncompressedFileName = uncompressedDirectory + entry.getName();
                    Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
                    Files.createFile(uncompressedFilePath);
                    FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
                    while (bis.available() > 0) {
                        fileOutput.write(bis.read());
                    }
                    fileOutput.close();
                    System.out.println("Written :" + entry.getName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
