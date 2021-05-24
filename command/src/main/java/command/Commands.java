package command;

public class Commands {
    public static final Integer PORT = 5678;
    public static final String HOST = "localhost";
    public static final String END = "/exit ";
    public static final String AUTH = "/auth ";



    public static final String LS_COMMAND = "/ls";
    public static final String TOUCH_COMMAND = "/touch [filename]";
    public static final String CD_COMMAND = "/cd [path]";
    public static final String RM_COMMAND = "/rm [filename | dirname]";
    public static final String COPY_COMMAND = "/copy [src] [target]";
    public static final String CAT_COMMAND = "/cat [filename]";
    public static final String MKDIR_COMMAND = "/mkdir";
    public static final String CHANGE_NICKNAME = "/nick";
}