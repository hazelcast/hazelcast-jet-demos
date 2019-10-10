/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package support;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

/**
 * Utility to work with the word index for the IMDB review database.
 */
public class WordIndex implements Serializable {
    private static final int PAD = 0;
    private static final int START = 1;
    private static final int UNKNOWN = 2;
    private final Map<String, Integer> wordIndex;

    public WordIndex(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            System.err.println("The directory " + dir.getAbsolutePath() + " doesn't exist.\n"
                    + "To create the models, run the `bin/imdb_review_train.py` script.");
            System.exit(1);
        }
        try {
            wordIndex = loadWordIndex(new FileReader(new File(dir, "imdb_word_index.json")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a tensor input from an input text.
     */
    public float[][] createTensorInput(String text) {
        float[] indexedPadded = new float[256];
        Arrays.fill(indexedPadded, PAD);
        indexedPadded[0] = START;
        int i = 1;
        for (String word : text.split("\\W+")) {
            indexedPadded[i++] = wordIndex.getOrDefault(word, UNKNOWN);
            if (i == indexedPadded.length) {
                break;
            }
        }
        return new float[][]{indexedPadded};
    }

    private static Map<String, Integer> loadWordIndex(Reader in) {
        System.out.println("loading word index...");
        Type type = new MapTypeToken().getType();
        Map<String, Integer> wordIndex = new Gson().fromJson(in, type);
        // First 3 indices are reserved
        wordIndex.entrySet().forEach(entry -> entry.setValue(entry.getValue() + 3));
        return wordIndex;
    }

    private static class MapTypeToken extends TypeToken<Map<String, Integer>> {
    }
}
