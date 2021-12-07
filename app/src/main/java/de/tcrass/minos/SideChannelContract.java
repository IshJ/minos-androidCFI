package de.tcrass.minos;


import android.provider.BaseColumns;
import android.util.Log;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.sort;

public class SideChannelContract {
    public static final String TABLE_NAME = "Side_Channel_Info";
    public static final String GROUND_TRUTH = "Ground_Truth";
    public static final String GROUND_TRUTH_AOP = "Ground_Truth_AOP";

    /**
     * Static class to return column names for the database
     */
    public static class Columns {
        //public static final String _ID = BaseColumns._ID;
        public static final String TIMESTAMP = "Timestamp";
        public static final String SYSTEM_TIME = "System_Time";
        public static final String SCAN_TIME = "Scan_Time";
        public static final String ADDRESS = "Address";
        public static final String COUNT = "Count";
        public static final String LABEL = "Label";


        public static final String METHOD_ID = "Method_Id";
        public static final String START_COUNT = "Start_Count";
        public static final String END_COUNT = "End_Count";


         private Columns() {}
        // private constructor to prevent instantiation
    }

    public static String[] CLASSES = new String[]{
            "QueryInformation",
            "Camera",
            //"Calendar",
            //"ReadSMS",
            "RequestLocation",
            "AudioRecording",
            //"ReadContacts"
    };
}

