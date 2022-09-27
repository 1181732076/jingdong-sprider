package com.jinshida.jingdongsprider;

import com.jinshida.jingdongsprider.common.Data;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;



@Component
public class Bootstrap implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    LinkedHashSet<String> linkedHashSet=new LinkedHashSet<>();

    private final Integer threadpool=100;

    private String user="ceshi";

//    @Autowired
//    private StringRedisTemplate redis;

    @Override
    public void run(String... args) throws Exception {
        getKeyword();
        initialize();
        startSprider();

    }


    private void startSprider() throws IOException {
//        RedisTemplateBitArray redisBitArray = new RedisTemplateBitArray(redis, "JingDong");

        for(int i=0;i<threadpool;i++){
            SpirderThread thread=new SpirderThread(i,threadpool,jdbcTemplate);
            thread.start();
        }
    }

//    private void getData(){
//        for(int i=0;i<22;i++){
//            DataThread thread=new DataThread(i,22,jdbcTemplate);
//            thread.start();
//        }
//    }

    private void initialize() throws IOException {
        InputStream dictStream = new ClassPathResource("keyword.txt").getInputStream();
        // SpringBoot 只能用 InputStream 读取 Jar 中的文件
        if (dictStream != null) {
            Data.dict = IOUtils.readLines(dictStream);
            System.out.println("read all dict");
        } else {
            System.out.println("dict read error");
            System.exit(0);
        }
    }

    private void getKeyword() throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder()
                .connectTimeout(2000, TimeUnit.MILLISECONDS)
                .build();
        Response response = okHttpClient.newCall(
                new Request.Builder()
                        .url("https://www.jd.com/allSort.aspx")
                        .build()
        ).execute();
        if(response.isSuccessful()){
            String html=response.body().string();
            Document document= Jsoup.parse(html);
            Element element=document.select("div.category-items").first();
            Elements elements=element.select("dl.clearfix > dd > a");
            for (Element alink:elements){
                String keyword=alink.text();
                System.out.println(keyword);
                linkedHashSet.add(keyword);
            }
            int count=linkedHashSet.size();
            if(count>0){
                for (String set:linkedHashSet){
                    Writer(set);;
                }
            }
        }
    }

    private void Writer(String keyword) throws IOException {
        File f = new File("user.txt");
        FileOutputStream fop = new FileOutputStream(f, true);
        // 构建FileOutputStream对象,文件不存在会自动新建
        OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");
        // 构建OutputStreamWriter对象,参数可以指定编码,默认为操作系统默认编码,windows上是gbk
        writer.append(keyword);
        // 写入到缓冲区
        writer.append("\r\n");
        // 换行
        writer.close();
        // 关闭写入流,同时会把缓冲区内容写入文件,所以上面的注释掉
        fop.close();
        // 关闭输出流,释放系统资源
    }
}
