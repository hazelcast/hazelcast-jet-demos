/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.jet.datamodel.Tuple2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.hazelcast.jet.datamodel.Tuple2.tuple2;
import static com.hazelcast.jet.impl.util.Util.uncheckRun;
import static java.util.stream.Collectors.toMap;

/**
 * Pre-process the CSV file from
 * https://catalog.data.gov/dataset/nys-thruway-origin-and-destination-points-for-all-vehicles-15-minute-intervals-2016-q1
 *
 * The output file needs to be additionally sorted.
 */
public class Preprocess {
    public static void main(String[] args) throws IOException {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MM/dd/yyyy HHmm");

        Map<Tuple2, Integer> map = Files.lines(
                Paths.get("c:\\work\\hazelcast\\hazelcast-jet\\project-x-ml\\NYS_Thruway_Origin_and_Destination_Points_for_All_Vehicles_-_15_Minute_Intervals__2016_Q1.csv"))
                                        .skip(1)
                                        .map(line -> line.split(","))
                                        .collect(toMap(line -> tuple2(LocalDateTime.parse(line[0] + " " + line[3], f), line[1]),
                                                      line -> Integer.parseInt(line[5]),
                                                      Integer::sum));

        try (BufferedWriter wr = Files.newBufferedWriter(Paths.get("c:\\work\\hazelcast\\hazelcast-jet\\project-x-ml\\15-minute-counts.csv"))) {
            map.forEach((k, v) -> uncheckRun(() -> wr.write(k.f0() + "," + k.f1() + "," + v + "\n")));
        }
    }
}
