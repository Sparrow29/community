package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant {

    public static final Logger logger = LoggerFactory.getLogger(ShareController.class);
    
    @Autowired
    private EventProducer eventProducer;
    
    @Value("${community.path.domain}")
    private String domain;
    
    @Value("${server.servlet.context-path}")
    private String contextPath;
    
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    
    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    @RequestMapping(path = "/share", method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl) {
        // 文件名
        String filename = CommunityUtils.generateUUID();
        
        // 异步生成长图---异步!
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("filename", filename)
                .setData("suffix", ".png");
        eventProducer.fireEvent(event);
        
        // 返回图片访问路径
        Map<String, Object> map = new HashMap<>();
        // map.put("shareUrl", domain + contextPath + "/share/image/" + filename);
        map.put("shareUrl", shareBucketUrl + "/" + filename);
        
        return CommunityUtils.getJSONString(0, null, map);
    }

    /**
     * 获取长图
     * Deprecated 废弃
     */
    @RequestMapping(path = "/share/image/{filename}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("filename") String filename, HttpServletResponse response) {
        if (StringUtils.isBlank(filename)) {
            throw new IllegalArgumentException("文件名不能为空!");
        }
        
        response.setContentType("image/png");
        File file = new File(wkImageStorage + "/" + filename + ".png");
        try {
            OutputStream os = response.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("获取长图失败: " + e.getMessage());
        }

    }
    
}
