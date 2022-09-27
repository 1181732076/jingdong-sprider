package com.jinshida.jingdongsprider;

import com.jinshida.jingdongsprider.common.Data;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class SpirderThread extends Thread{


    private int threadnum;//第几个线程

    private int threadcount;//总共多少线程

    private JdbcTemplate jdbcTemplate;

    OkHttpClient okHttpClient = new OkHttpClient()
            .newBuilder()
            .connectTimeout(2000, TimeUnit.MILLISECONDS)
            .build();

    public SpirderThread(int i,int threadpool,JdbcTemplate jdbc) {
        threadnum=i;
        threadcount=threadpool;
        jdbcTemplate=jdbc;
    }

    @Override
    public void run() {
        List<String> keywords=subDict(threadnum,threadcount);
        for (String keyword:keywords){
            try {
                request(keyword);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void request(String keyword) throws IOException {
        Data.Counter.getAndIncrement();
        System.out.println("爬取第"+Data.Counter.get()+"个关键词"+keyword);
        Response response = okHttpClient.newCall(
                new Request.Builder()
                        .url("https://search.jd.com/Search?enc=utf-8&keyword="+keyword)
                        .header("user-agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3314.0 Safari/537.36 SE 2.X MetaSr 1.0")
                        .build()
        ).execute();
        if(response.isSuccessful()){
            String html=response.body().string();
            Document document= Jsoup.parse(html);
            Element element = document.select("span.fp-text").first();
            if(element!=null){
                Integer page = Integer.parseInt(element.selectFirst("i").text());
                requestAll(page,keyword);
            }else {
                System.out.println("没有其他页信息");
                System.out.println(html);
            }
        }
    }

    private void requestAll(Integer page, String keyword) throws IOException {
        for(int i=3;i<=page*2-1;i=i+2){
            Response response = okHttpClient.newCall(
                    new Request.Builder()
                            .url("https://search.jd.com/s_new.php?enc=utf-8&keyword="+keyword+"&page="+i)
                            .header("user-agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3314.0 Safari/537.36 SE 2.X MetaSr 1.0")
                            .build()
            ).execute();
            if(response.isSuccessful()){
                String html=response.body().string();
                Document document=Jsoup.parse(html);
                Element element = document.select("div.J-goods-list").first();
                if(element!=null){
                    Elements elements = element.select("div.gl-i-wrap");
                    for (Element good : elements){
                        Element nameElement=good.selectFirst("div.p-name");
                        String product_name = nameElement.selectFirst("em").text();
                        String product_url=good.select("a").attr("href");
                        Element priceElement=good.selectFirst("div.p-price");
                        Double product_price=0.0;
                        if(priceElement.selectFirst("i")!=null){
                           product_price= NumberUtils.toDouble(priceElement.selectFirst("i").text());
                        }
                        Element shopElement=good.selectFirst("div.p-shop");
                        String product_shop="无厂商";
                        if(shopElement!=null){
                            product_shop=shopElement.select("span").text();
                        }
                        String sql="insert into product(name,url,price,shop) values(?,?,?,?)";
                        int update = jdbcTemplate.update(sql, product_name, product_url, product_price, product_shop);
                        if(update>0){
                            System.out.println("插入成功！");
                        }else {
                            System.out.println("插入失败！");
                        }
                }
                }else {
                    System.out.println("未获取商品信息！");
                    System.out.println(html);
                }
            }
        }
    }

    /**
     * 根据当前任务序号，线程池总数，字典大小，获取当前分区的关键词列表片段
     *
     * @param thread       第几个线程
     * @param threadpool   总共多少线程
     * @return
     */
    private List<String> subDict(Integer thread,Integer threadpool) {
        Integer pools = threadpool;
        int dictTotal = Data.dict.size();
        int region = dictTotal / pools;
        int start = thread * region;
        int end = (thread + 1) * region;
        if ((thread + 1) == pools && end < dictTotal) {
            end = dictTotal;
        }
        List<String> keywords = Data.dict.subList(start, end);

        return keywords;
    }
}
