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

import com.hazelcast.core.IMap;

import java.util.List;

import static java.util.Arrays.asList;

public class SampleReviews {
    public static void populateReviewsMap(IMap<Long, String> reviewsMap) {
        List<String> reviews = asList(
                "the movie was good",
                "the movie was bad",
                "excellent movie best piece ever",
                "absolute disaster, worst movie ever",
                "had both good and bad parts, excellent casting, but camera was poor");

        long key = 0;
        for (String review : reviews) {
            reviewsMap.put(key++, review);
        }
    }
}
