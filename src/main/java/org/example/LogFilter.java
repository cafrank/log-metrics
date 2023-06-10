package org.example;

import org.apache.commons.cli.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LogFilter parses its parameters from the command line arguments, and
 * filters log lines based on the parameters specified.  It is designed
 * as an extensible list of sub-filters, one per parameter st the moment.
 * The main filter visits/executes each enabled sug-filter.
 */
public class LogFilter {

    /**
     * Create LogFilter from command line arguments
     * @param args
     * @throws java.text.ParseException
     */
    public LogFilter(String[] args) throws java.text.ParseException {
        Options options = new Options();

        Option input = new Option(null, "on", true, "Optional date filter");
        input.setRequired(false);
        options.addOption(input);
        input = new Option(null, "gt", true, "Optional file size filter");
        input.setRequired(false);
        options.addOption(input);
        input = new Option("u", "user", true, "User name");
        input.setRequired(false);
        options.addOption(input);
        input = new Option(null, "op", true, "Operation (upload/downlowd)");
        input.setRequired(false);
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        for (Option opt: cmd.getOptions()) {
            String longOpt = opt.getLongOpt();
            if ("gt".equals(longOpt)) {
                sizeFilter = SizeFilter.GT;
                sizeFilter.setSize(Long.parseLong(opt.getValue()));
            } else if ("on".equals(longOpt)) {
                dateFilter = DateFilter.EQ;
                dateFilter.setDate(opt.getValue());
            } else if ("user".equals(longOpt)) {
                userFilter = opt.getValue();
            } else if ("op".equals(longOpt)) {
                opFilter = opt.getValue();
                if ( ! ("download".equals(opFilter) || "upload".equals(opFilter)) ) {
                    System.out.println("Invalid operation name: "+ opFilter);
                    formatter.printHelp("utility-name", options);
                    System.exit(1);
                }
            }
        }

        List<String> argList = cmd.getArgList();
        if (argList.size() != 1) {
            System.out.println("Exactly one file name expected");
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
        fileName = argList.get(0);
    }

    /**
     * Debugging.  Prints parameters in a pseudo-jsom string
     * @return
     */
    @Override
    public String toString() {    // Debugging
        return "{Operation: "+ opFilter
                + ", user: "+ (userFilter!=null ? userFilter : "UNSET")
                + ", date: "+ (sizeFilter!=null ? sizeFilter : "UNSET")
                + ", date: "+ (dateFilter!=null ? dateFilter : "UNSET")
                + ", file: " +fileName + "}";
    }

    /**
     * DateFilter is a sub-filter that filters out log lines greater than a specified date.
     * This is written as an enum to encapsulate its associated commands and corresponding
     * command line arguments.  That makes it easier to add other command like before and after.
     */
    enum DateFilter {
        GT("a", "after"),     // Out of scope.  Added to simplify extensibility
        EQ("o", "on"),
        LT("b", "before");    // Out of scope.  Added to simplify extensibility

        public final String arg;
        public final String shortArg;
        public Date date = null;
        static String[] pat = {
                "EEE MMM dd HH:mm:ss zzz yyyy",     // Used in log file: "Mon Apr 13 13:07:48 UTC 2020",
                "yyyy-MM-dd"                        // Used on command line.
        };

        private DateFilter(String shortArg, String arg) {
            this.arg = arg;
            this.shortArg = shortArg;
        }

        static Map<String, DateFilter> ENUM_MAP;
        static List<SimpleDateFormat> sdfList;
        static {
            Map<String,DateFilter> map = new ConcurrentHashMap<String, DateFilter>();
            for (DateFilter instance : DateFilter.values()) {
                map.put(instance.arg,instance);
            }
            ENUM_MAP = Collections.unmodifiableMap(map);
            sdfList = Arrays.stream(pat).map(x -> new SimpleDateFormat(x)).toList();
        }

        public static DateFilter get (Option opt) {
            return ENUM_MAP.get(opt.getLongOpt());
        }

        public void setDate(String val) throws java.text.ParseException {
//            SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd");
//            this.date = sdformat.parse(val);
            this.date = parseDate(val); // Unified data parsing
        }

        public String toString() {
            return super.toString() + ": " + this.date;
        }

        /*
         * Parse a date string using an ordered list of expected string formats.
         */
        private Date parseDate(String strDate) throws java.text.ParseException {
            for (SimpleDateFormat sdf: sdfList) {
                try {
                    Date date = sdf.parse(strDate);       // Comparing days. Zero out hours and minutes since
                    return new Date(date.getYear(), date.getMonth(), date.getDate());
                } catch(java.text.ParseException e) { } // Skip parse exceptions.
            }
            throw new java.text.ParseException("Unable to parse date: "+ strDate, 0);
        }

        /**
         * Perform the DateFilter.  The main filter visits class to filter by date.
         * @param val
         * @return
         * @throws java.text.ParseException
         */
        public boolean visit(String val) throws java.text.ParseException {
            Date dVal = parseDate(val);
            switch(this) {                  // Apply appropriate comparison for the current filter
                case EQ:
                    return dVal.compareTo(this.date) == 0;
                case GT: // After
                    return dVal.compareTo(this.date) > 0;    // Out of scope.  Added to simplify extensibility
                case LT: // Before
                    return dVal.compareTo(this.date) < 0;    // Out of scope.  Added to simplify extensibility
                default:
                    throw new UnsupportedOperationException();      // Bug have been handled in arg parsing.
            }
        }
    }

    /**
     * SizeFilter is a sub-filter that filters out log lines greater than a specified file size.
     * This is written as an enum to encapsulate its associated commands and corresponding
     * command line arguments.  That makes it easier to add other command like less than and grater than.
    */
    enum SizeFilter {
        GT("g", "gt"),
        EQ("e", "eq"),    // Out of scope.  Added to simplify extensibility
        LT("l", "lt");    // Out of scope.  Added to simplify extensibility

        public final String arg;
        public final String shortArg;
        public Long size = 0L;
        private SizeFilter(String shortArg, String arg) {
            this.shortArg = shortArg;
            this.arg = arg;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String toString() {
            return super.toString() + ": " + this.size;
        }
        /**
         * Perform the SizeFilter.  The main filter visits class to filter by file size.
         * @param val
         * @return
         * @throws java.text.ParseException
         */
        public boolean visit(String val) throws java.text.ParseException {
            Long longVal = Long.parseLong(val);

            switch(this) {
                case EQ:
                    return longVal == this.size;    // Out of scope.  Added to simplify extensibility
                case GT: // Grater than
                    return longVal > this.size;
                case LT: // Less then
                    return longVal < this.size;    // Out of scope.  Added to simplify extensibility
                default:
                    throw new UnsupportedOperationException();      // Bug have been handled in arg parsing.
            }
        }
    }

    private DateFilter dateFilter = null;
    private SizeFilter sizeFilter = null;
    private String userFilter = null;       // Trivial string-compare filter
    private String opFilter = null;         // Trivial string-compare filter
    private String fileName = null;

    public String getFileName() {
        return fileName;
    }

    /**
     * Filters out log lines using the parameters specified on the command line.
     * If a parameter like user name is not specified it does not affect filtering.
     * @param date
     * @param user
     * @param operation
     * @param fileSize
     * @return
     * @throws java.text.ParseException
     */
    public boolean visitFilters(String date, String user, String operation, String fileSize) throws java.text.ParseException {
        boolean rc = true;
        return  (opFilter   == null || opFilter.equals(operation)) &&
                (sizeFilter == null || sizeFilter.visit(fileSize)) &&
                (userFilter == null || userFilter.equals(user)) &&
                (dateFilter == null || dateFilter.visit(date));
    }
}
