package com.spring4all.service.impl;

import com.spring4all.enums.LanguageEnum;
import com.spring4all.enums.TranslatorNameEnum;
import com.spring4all.factory.AbstractTranslatorFactory;
import com.spring4all.factory.TranslatorFactory;
import com.spring4all.service.WeeklyPostService;
import com.spring4all.util.MyArrayList;
import com.spring4all.util.QrCodeUtil;
import com.spring4all.util.UploadImageUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WeeklyPostServiceImpl implements WeeklyPostService {

    private static final Logger logger = LoggerFactory.getLogger(WeeklyPostServiceImpl.class);

    @Value("${springio.website.index}")
    private String SPRING_URL;

    @Value("${springio.website.blog}")
    private String SPRING_WEEK_BLOG_URL;

    @Value("${springio.weekly}")
    private String MATCH_RULE;

    @Value("${springio.titlematch}")
    private String MATCH_PATH;

    @Override
    public String crawlWeeklyPost() {
        MyArrayList hrefList = new MyArrayList();
        String linkHref;
        try {

            AbstractTranslatorFactory factory = new TranslatorFactory();//完成翻译工厂类

            Document doc = Jsoup.connect(SPRING_WEEK_BLOG_URL).get();

            if (doc == null) {
                logger.info("打开Spring周报博客失败");
                return "打开Spring周报博客失败";
            }

            String targetLink = null;

            Elements hrefs = doc.select("h2.blog--title").select("a");

            for (Element href : hrefs) {
                if (href.text().startsWith(MATCH_RULE)) {
                    targetLink = href.attr("href");
                    break;
                }
            }

            if (!StringUtils.hasText(targetLink)) {
                return "解析最新的文章标题链接地址为空，页面结构可能已经变化！";
            }

            linkHref = SPRING_URL + targetLink;

            Document latestBlog = Jsoup.connect(linkHref).get();
            Elements allPHref = latestBlog.select("div.blog--post").select("p");
            Elements allLiHref = latestBlog.select("div.blog--post").select("ul").select("li");
            //处理p标签
            for (Element e : allPHref) {
                String traslateedSentence = factory.getTranslator(TranslatorNameEnum.GOOGLE).trans(LanguageEnum.EN, LanguageEnum.ZH, e.text());
                String traslateedContent = new StringBuilder()
                        .append(traslateedSentence)
                        .append("</br></br>")
                        .toString();

                hrefList.add(traslateedContent);
                for (Element el : e.select("a")) {
                    hrefList.add(
                            new StringBuilder()
                                    .append(el.attr("href"))
                                    .append("</br></br>")
                                    .toString()
                    );
                    String url = el.attr("href");
                    addImage(hrefList, url);
                }
                hrefList.add("</br>");
            }
            //处理Li标签
            for (int i = 0; i < allLiHref.size(); i++) {
                hrefList.add(i + 1 + ":&nbsp&nbsp");
                //把翻译结果添加List中去
                hrefList.add(factory.getTranslator(TranslatorNameEnum.GOOGLE).trans(LanguageEnum.EN, LanguageEnum.ZH, allLiHref.get(i).text()) + "</br></br>");
                for (Element el : allLiHref.get(i).select("a")) {
                    hrefList.add(el.attr("href") + "</br></br>");
                    String url = el.attr("href");
                    addImage(hrefList, url);

                }
            }
            saveContent(hrefList, linkHref);
        } catch (Exception e) {
            logger.error("errorMessage:{}", e);
        } finally {
            return hrefList.toString();
        }
    }

    private void saveContent(MyArrayList hrefList, String linkHref) throws IOException {
        Pattern p = Pattern.compile(MATCH_PATH);
        Matcher m = p.matcher(linkHref);
        boolean matched = m.matches();
        if (matched) {
            String fileName = m.group(1).replaceAll("\\/","") + ".md";
            String targetPath = new File(System.getProperty("user.dir")) + "/translated/" + fileName;
            // 对当前翻译内容进行归档保存
            Files.write(Paths.get(targetPath), hrefList.toString().getBytes());
        }
    }

    //添加图片
    public void addImage(MyArrayList hrefList, String url) {
        String returnUrl = UploadImageUtil.upLoad(QrCodeUtil.createCode(url), url.substring(0, 13));
        hrefList.add("<img src='" + returnUrl + "' style='margin-left:500px;'><br/><br/>");
    }

}

















