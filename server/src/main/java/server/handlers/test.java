package server.handlers;

import com.google.gson.Gson;
import server.FileInfo;

import java.nio.file.Path;
import java.util.Arrays;

public class test {
    public static void main(String[] args) {
        Gson g = new Gson();
//        String n = "[{filename='1.txt', type=FILE, size=8, lastModified=2021-05-25T10:37:12.072412} /, {filename='2.txt', type=FILE, size=6, lastModified=2021-05-25T10:37:12.077413} $$]";
//        n.substring(1);
        String n = String.format("filename:'1.txt',type:FILE,size:8,lastModified:2021-05-25T10:37:12.072412");
        String[] s = n.split("/");
        System.out.println(s[0].substring(1));
        System.out.println(Arrays.toString(s));
//        FileInfo fileInfo = g.fromJson(s[0].substring(1), FileInfo.class);
//        System.out.println(fileInfo.getFilename());
        String s1 = g.toJson(new FileInfo(Path.of("cloud", "framzik", "1.txt")));
        System.out.println(s1);
        FileInfo fileInfo = g.fromJson(s1, FileInfo.class);
        System.out.println(fileInfo);
    }
}
