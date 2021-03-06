package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtils;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;
    
    @Autowired
    private LikeService likeService;
    
    @Autowired
    private FollowService followService;
    
    @Autowired
    private DiscussPostService discussPostService;
    
    @Autowired
    private CommentService commentService;
    
    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;
    

    /**
     *账户设置页面
     */
    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // 生成上传文件名称
        String filename = CommunityUtils.generateUUID();
        
        // 指定七牛云的响应信息
        StringMap policy = new StringMap();
        // 指定上传成功时希望返回的是一个code=0的json字符串
        policy.put("returnBody", CommunityUtils.getJSONString(0));
        
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, filename, 3600, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("filename", filename);
        
        return "/site/setting";
    }

    /**
     * 更新用户的头像路径 headerUrl
     */
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String filename) {
        if (StringUtils.isBlank(filename)) {
            return CommunityUtils.getJSONString(1, "文件名不能为空!");
        }

        String url = headerBucketUrl + "/" + filename;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtils.getJSONString(0);
    }

    /**
     * 退出登录
     */
    @LoginRequired
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    /**
     * 上传头像
     * Deprecated 废弃 
     */
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        //空值处理
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }

        //获取文件名后缀
        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        //生成随机文件名
        filename = CommunityUtils.generateUUID() + suffix;

        //存储文件到本地
        File dest = new File(uploadPath + "/" + filename);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        //更新当前用户头像访问路径(web路径)
        //http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";

    }

    /**
     * 获取头像
     * Deprecated 废弃 
     */
    @RequestMapping(path = "/header/{filename}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response) {
        //获取本地存放路径
        filename = uploadPath + "/" + filename;
        //获取后缀
        String suffix = filename.substring(filename.lastIndexOf(".") + 1);
        //响应图片
        response.setContentType("image/" + suffix);
        try (
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(filename);
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("获取头像失败: " + e.getMessage());
        }

    }

    /**
     * 修改密码
     */
    @LoginRequired
    @RequestMapping(path = "/change", method = RequestMethod.POST)
    public String changePassword(String oldPwd, String newPwd, Model model){
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.changePassword(oldPwd, newPwd, user);
        if(map == null || map.isEmpty()){
            return "redirect:/user/logout";
        }
        else{
            model.addAttribute("oldPwdMsg", map.get("oldPwdMsg"));
            model.addAttribute("newPwdMsg", map.get("newPwdMsg"));
            return "/site/setting";
        }
    }

    /**
     * 获取个人主页
     */
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在!");
        }
        // 用户
        model.addAttribute("user", user);
        // 收到的赞的数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        
        // 关注数
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);
        
        return "/site/profile";
    }

    /**
     * 我的帖子
     */
    @RequestMapping(path = "/mypost/{userId}", method = RequestMethod.GET)
    public String getMyPost(@PathVariable("userId") int userId, Page page, Model model){
        User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);
        
        // 分页
        page.setLimit(10);
        page.setPath("/user/mypost/" + userId);
        page.setRows(discussPostService.findDiscussPostRows(userId));
        
        // 帖子列表
        List<DiscussPost> postList = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(postList != null) {
            for (DiscussPost post : postList) {
                Map<String, Object> map = new HashMap<>();
                map.put("discussPost", post);
                // 帖子点赞数量
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        
        return "/site/my-post";
    }

    /**
     * 我的回复
     */
    @RequestMapping(path = "/myreply/{userId}", method = RequestMethod.GET)
    public String getMyReply(@PathVariable("userId") int userId, Page page, Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);
        
        // 分页
        page.setLimit(10);
        page.setPath("/user/myreply/" + userId);
        page.setRows(commentService.findUserCommentCount(userId));
        
        // 回复列表
        List<Comment> commentList = commentService.findUserComments(userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> comments = new ArrayList<>();
        if(commentList != null){
            for(Comment comment : commentList){
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                map.put("discussPost", discussPostService.findDiscussPostById(comment.getEntityId()));
                comments.add(map);
            }
        }
        model.addAttribute("comments", comments);
        
        return "/site/my-reply";
    }


}
