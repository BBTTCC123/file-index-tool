package chen;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class JDBCM {
    // 连接信息从 classpath 上的 db.properties 读，不再写死在代码里
    private static final String DB_URL;
    private static final String USER;
    private static final String PASS;

    static {
        Properties props = new Properties();
        // 静态块里没有 this，用不了 getClass()，写成 JDBCM.class 是同一个加载器
        try (InputStream in = JDBCM.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("classpath 里找不到 db.properties，照着 db.properties.example 建一份");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DB_URL = props.getProperty("db.url");
        USER = props.getProperty("db.user");
        PASS = props.getProperty("db.password");
    }

    private Connection connection;
    private Connection conn() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
        }
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public  void insert(String addr,String file_name,String ext_name,double size,String time) {
        String sql = " insert into file_schema (addr, file_name, ext_name, size, change_time) VALUES (?,?,?,?,?)";
        try(PreparedStatement ps = conn().prepareStatement(sql)) {
            //打开链接
            ps.setString(1, addr);
            ps.setString(2, file_name);
            ps.setString(3, ext_name);
            ps.setDouble(4, size);
            ps.setString(5, time);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    public void query(String addre) {
        String sql = " select * from file_schema where addr = ?";
//        Statement stmt = null;
        try(PreparedStatement ps = conn().prepareStatement(sql)) {
//            stmt = conn.createStatement();
            ps.setString(1, addre);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String addr = rs.getString("addr");
                String name = rs.getString("file_name");
                String ext = rs.getString("ext_name");
                double size = rs.getDouble("size");
                String time = rs.getString("change_time");
                System.out.println("filepath:"+addr + "\t" +"filename"+ name + "\t" +"filetype:"+ ext + "\t" +"filesize(byte):"+ size +  "\t" +"setting_time:"+ time);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void eachQuery() {
        String sql = " select * from file_schema ";
        try(PreparedStatement ps = conn().prepareStatement(sql)){
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String addr = rs.getString("addr");
                String name = rs.getString("file_name");
                String ext = rs.getString("ext_name");
                double size = rs.getDouble("size");
                String time = rs.getString("change_time");
                System.out.println("filepath:"+addr + "\t" +"filename"+ name + "\t" +"filetype:"+ ext + "\t" +"filesize(byte):"+ size +  "\t" +"setting_time:"+ time);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void type(){
        String sql = " select ext_name, count(*) as n from file_schema group by ext_name; ";
        int total=0;
        try(PreparedStatement ps = conn().prepareStatement(sql)){
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String ext = rs.getString("ext_name");
                int count = rs.getInt("n");
                total+=count;
                System.out.println("filetype: "+ext+"\t"+"amount: "+count);
            }
            System.out.println("total(except directory): "+total);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String addre) {
        String sql = " delete from file_schema where addr = ?";
        try(PreparedStatement ps = conn().prepareStatement(sql)) {

//          stmt = conn.createStatement();
            ps.setString(1, addre);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
