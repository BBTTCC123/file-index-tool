package chen;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Dirscanner {
    JDBCM jdbcm =  new JDBCM();
    public int reScan(String path){
        List<File> list = new ArrayList<>();
        int count=0;
        File file = new File(path);
        if(file.exists()){
            if(file.isDirectory()){
                File[] files = file.listFiles();
                if (files == null) {
                    files = new File[0];   // ← 用空数组代替 null
                }
                for(File f : files){
                    list.add(f);
                }
            }else{
                list.add(file);
            }
            for(int i = 0; i < list.size(); i++){
                File f = list.get(i);
                if(f.isDirectory()){
                    count+= reScan(f.getAbsolutePath());
                }else{
                    String[] arr =  extract(f.getName());

                    LocalDateTime dateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(f.lastModified()),
                            ZoneId.systemDefault()
                    );
                    String mysqlDateTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    jdbcm.delete(f.getAbsolutePath());
                    jdbcm.insert(f.getAbsolutePath(), arr[0], arr[1], f.length(), mysqlDateTime);
                    count++;
                }
            }
            System.out.println("文件(夹)"+path+"录入成功！！");
            System.out.println("本次成功录入"+count+"条信息");
            return count;
        }else{
            System.out.println("文件或文件夹不存在");
            return 0;
        }
    }
    public void typeQuery(){
        jdbcm.type();
    }
    public void traverseQuery(){
        jdbcm.eachQuery();
    }
    public void scan(String path){
        List<File> list = new ArrayList<>();
        int count=0;
        File file = new File(path);
        if(file.exists()){
            if(file.isDirectory()){
                File[] files = file.listFiles();
                if (files == null) {
                    System.out.println("无权限访问,跳过: " + file.getAbsolutePath());
                    return;
                }
                for(File f : files){
                    list.add(f);
                }
            }else{
                list.add(file);
            }
            for(int i = 0; i < list.size(); i++){
                File f = list.get(i);
                if(f.isDirectory()){
                    continue;
                }
                String[] arr =  extract(f.getName());

                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(f.lastModified()),
                       ZoneId.systemDefault()
                );
                String mysqlDateTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                jdbcm.delete(f.getAbsolutePath());
                jdbcm.insert(f.getAbsolutePath(), arr[0], arr[1], f.length(), mysqlDateTime);
                count++;
            }
            System.out.println("共录入"+count+"条信息");
        }else {
            System.out.println("文件或文件夹不存在");
        }
    }
    static String[] extract(String wholeName){
        String fileName;
        String extName;
        if(!(wholeName.contains("."))||wholeName.indexOf(".")==0){
            fileName = wholeName;
            extName = "file";
        }else{
            fileName = wholeName.substring(0, wholeName.lastIndexOf("."));
            extName = wholeName.substring(wholeName.lastIndexOf(".")+1);
        }
        String[] arr = new String[2];
        arr[0]= fileName;
        arr[1]= extName;
        return arr;
    }
    public void delete(String addre) throws  Exception{
        jdbcm.delete(addre);
        System.out.println("删除操作已执行");
    }
}
