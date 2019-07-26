package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if(map == null || map.isEmpty()){
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }
        else{
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    // http://localhost:8080/community/activation/101/code
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable int userId, @PathVariable("code") String code){
        int result = userService.activation(userId, code);
        if(result == 0){
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        }
        else if(result == 1){
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        }
        else{
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        //把验证码存入session
        session.setAttribute("kaptcha", text);

        //把图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败" + e.getMessage());
        }
    }

    /**
     *
     * @param username 接受客户端输入的用户名
     * @param password 接受客户端输入的密码
     * @param code  接受客户端输入的验证码
     * @param rememberMe 接受客户端勾选的记住我
     * @param model 用于封装向前端控制器返回的数据 Message
     * @param session 用于获取服务器内存的验证码
     * @param response 用于向客户端发送cookie 存放登录凭证
     * @return
     */
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberMe,
                        Model model, HttpSession session, HttpServletResponse response){
        //检查验证码
        String kaptcha = (String)session.getAttribute("kaptcha");
        if(StringUtils.isBlank(code)){
            model.addAttribute("codeMsg", "验证码不能为空!");
            return "/site/login";
        }
        if(!code.equalsIgnoreCase(kaptcha)){
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }
        //是否勾选记住我

        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        //检查账号、密码、rememberMe
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        //若登录成功, 将ticket响应给客户端
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }
        else{
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    //处理用户登出请求
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }

    //返回忘记密码页面
    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage(){
        return "/site/forget";
    }

    //忘记密码->获取邮箱验证码
    @RequestMapping(path = "/forget/code", method = RequestMethod.GET)
    @ResponseBody
    public String getVerifyCode(String email, HttpSession session){
        Map<String, Object> map = userService.getVerifyCode(email);
        if(map.containsKey("verifyCode")){
            session.setAttribute(email, map.get("verifyCode"));
            session.setMaxInactiveInterval(60 * 5);
            return CommunityUtils.getJSONString(0);
        }
        else{
           return CommunityUtils.getJSONString(1, (String)map.get("emailMsg"));
        }

    }

    //忘记密码 修改密码 post请求
    @RequestMapping(path = "/forget", method = RequestMethod.POST)
    public String resetPassword(String email, String verifyCode, String password, HttpSession session, Model model){
        String code = (String) session.getAttribute(email);
        if(StringUtils.isBlank(code) || StringUtils.isBlank(verifyCode) || !verifyCode.equalsIgnoreCase(code)){
            model.addAttribute("verifyCodeMsg", "验证码错误");
            return "/site/forget";
        }
        Map<String, Object> map = userService.resetPassword(email, password);
        if(map == null || map.isEmpty()){
            return "redirect:/login";
        }
        else{
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }

}
