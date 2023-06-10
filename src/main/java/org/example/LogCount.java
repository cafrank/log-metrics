package org.example;

import java.io.*;
import java.text.ParseException;
public class LogCount {

    /**
     * log-count count-item [filter] ... log-file.csv
     *     filter:
     *         --gt size    (Not implemented: --eg size, --lt size)
     *         --op operation (upload/download)
     *         --user username
     *         --on date    (Not implemented: --before date, --after date)
     *
     * @param args
     * @throws ParseException
     * @throws IOException
     */
    public static void main(String[] args) throws ParseException, IOException {
        // Initialize the filtering class
        LogFilter filter = new LogFilter(args);
        // Log level DEBUG.  System.out.println(filter);

        // Open the log file
        BufferedReader reader = new BufferedReader(new FileReader(filter.getFileName()));
        String line = reader.readLine();    // Skip the column title line.
        int cnt = 0;
        while ((line = reader.readLine()) != null) {
            String[] token = line.split(",");       // timestamp,username,operation,size
            if (filter.visitFilters(token[0], token[1], token[2], token[3])) {
                cnt++;      // If the log line passes all filters, increment the counter.
            }
        }
        System.out.println(cnt);    // Output the result
    }
}