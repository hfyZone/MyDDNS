package hfy.zone;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeSubDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeSubDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;
import hfy.zone.entity.AliAccessKey;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DDNS {

    static String rec;

    /**
     * 获取主域名的所有解析记录列表
     */
    private DescribeSubDomainRecordsResponse describeSubDomainRecords(DescribeSubDomainRecordsRequest request, IAcsClient client) {
        try{
            // 调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
            // 发生调用错误，抛出运行时异常
            throw new RuntimeException();
        }
    }

    /**
     * 获取当前主机公网IP
     */
    private String getCurrenHostIP() {
        // 这里使用jsonip.com第三方接口获取本地IP
        String jsonip = "https://jsonip.com";
        // 接口返回结果
        String result = "";
        BufferedReader in = null;
        try {
            // 使用HttpURLConnection网络请求第三方接口
            URL url = new URL(jsonip);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        //  正则表达式，提取xxx.xxx.xxx.xxx，将IP地址从接口返回结果中提取出来
        String rexp = "(\\d{1,3}\\.){3}\\d{1,3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(result);
        String res = "";
        while (mat.find()) {
            res = mat.group();
            break;
        }
        return res;
    }

    /**
     * 修改解析记录
     */
    private UpdateDomainRecordResponse updateDomainRecord(UpdateDomainRecordRequest request,IAcsClient client) {
        try {
            //  调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
            //  发生调用错误，抛出运行时异常
            throw new RuntimeException();
        }
    }

    private static void log_print(String functionName, Object result) {
        Gson gson = new Gson();
        System.out.println("-------------------------------" + functionName + "-------------------------------");
        System.out.println(gson.toJson(result));
    }


    // MySQL 8.0 以下版本 - JDBC 驱动名及数据库 URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://db.youngcloud.top:3306/framework_net";

    // MySQL 8.0 以上版本 - JDBC 驱动名及数据库 URL
    //static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    //static final String DB_URL = "jdbc:mysql://localhost:3306/RUNOOB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";


    // 数据库的用户名与密码，需要根据自己的设置
    static final String USER = "root";
    static final String PASS = "123456";
    private static AliAccessKey getAccressKey() throws ClassNotFoundException, SQLException {
        Connection conn = null;
        Statement stmt = null;
        // 注册 JDBC 驱动
        Class.forName(JDBC_DRIVER);

        // 打开链接
        System.out.println("连接数据库...");
        conn = DriverManager.getConnection(DB_URL,USER,PASS);

        // 执行查询
        System.out.println(" 实例化Statement对象...");
        stmt = conn.createStatement();
        String sql;
        sql = "SELECT  param_key,param_value  FROM param WHERE TYPE = 'ALIYUN_ACCESS_KEY'";
        ResultSet rs = stmt.executeQuery(sql);
        AliAccessKey aliAccessKey = new AliAccessKey();
        // 展开结果集数据库
        while(rs.next()){
            // 通过字段检索
            String accessKeyID = rs.getString("param_key");
            String accessKeySecret = rs.getString("param_value");

            // 输出数据
            System.out.print("accessKeyID: " + accessKeyID);
            System.out.print(", accessKeySecret: " + accessKeySecret);
            System.out.print("\n");

            //装载数据
            aliAccessKey.setAccessKeyID(accessKeyID);
            aliAccessKey.setAccessKeySecret(accessKeySecret);
        }
        // 完成后关闭
        rs.close();
        stmt.close();
        conn.close();
        return aliAccessKey;
    }

    private static void doDDNS(String addr, String rec){
        try{
            AliAccessKey aliAccessKey = getAccressKey();
            //  设置鉴权参数，初始化客户端
            DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou",// 地域ID
                    aliAccessKey.getAccessKeyID(),// 您的AccessKey ID
                    aliAccessKey.getAccessKeySecret());// 您的AccessKey Secret
            IAcsClient client = new DefaultAcsClient(profile);

            DDNS ddns = new DDNS();
            //查询指定二级域名的最新解析记录
            DescribeSubDomainRecordsRequest describeSubDomainRecordsRequest = new DescribeSubDomainRecordsRequest();
            describeSubDomainRecordsRequest.setSubDomain(rec + "." + addr);
            DescribeSubDomainRecordsResponse describeSubDomainRecordsResponse = ddns.describeSubDomainRecords(describeSubDomainRecordsRequest, client);
            log_print("describeSubDomainRecordsResponse",describeSubDomainRecordsResponse);

            List<DescribeSubDomainRecordsResponse.Record> domainRecords = describeSubDomainRecordsResponse.getDomainRecords();
            domainRecords.forEach(p ->{
                System.out.println("记录：" + p);
            });
            System.out.println("展示完毕");
            //最新的一条解析记录
            if (domainRecords.size() != 0) {
                DescribeSubDomainRecordsResponse.Record record = domainRecords.get(0);
                //  记录ID
                String recordId = record.getRecordId();
                //  记录值
                String recordsValue = record.getValue();
                //  当前主机公网IP
                String currentHostIP = ddns.getCurrenHostIP();
                System.out.println("-------------------------------当前主机公网IP为："+currentHostIP+"-------------------------------");
                if (!currentHostIP.equals(recordsValue)) {
                    //  修改解析记录
                    UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                    //  主机记录
                    updateDomainRecordRequest.setRR(rec);
                    //  记录ID
                    updateDomainRecordRequest.setRecordId(recordId);
                    //  将主机记录值改为当前主机IP
                    updateDomainRecordRequest.setValue(currentHostIP);
                    //  解析记录类型
                    updateDomainRecordRequest.setType("A");
                    UpdateDomainRecordResponse updateDomainRecordResponse = ddns.updateDomainRecord(updateDomainRecordRequest, client);
                    log_print("updateDomainRecord",updateDomainRecordResponse);
                }
            }
        }catch (Exception e){
            System.out.println("获取AliAccessKey失败：" + e.getMessage());
        }

    }

    public static void main(String[] args) {
        /**
         * addr len * a
         * */
        System.out.println("-------------------------------" + "ARGS" + "-------------------------------");
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
        }
        String addr = args[0];
        int num = Integer.valueOf(args[1]);
        for (int i = 2; i < num; i++) {
            doDDNS(addr, args[i]);
        }




    }



}