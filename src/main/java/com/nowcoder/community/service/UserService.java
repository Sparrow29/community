package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtils;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    //@Autowired
    //private LoginTicketMapper loginTicketMapper;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    //查询用户By Id
    public User findUserById(int id) {
//        return userMapper.selectById(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    /**
     * 用户注册
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        //验证账号 邮箱
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册");
            return map;
        }
        //注册用户
        user.setSalt(CommunityUtils.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtils.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtils.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        //激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    /**
     * 验证激活码 激活用户
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getActivationCode().equals(code)) {
            if (user.getStatus() == 0) {
                userMapper.updateStatus(userId, 1);
                clearCache(userId);
                return ACTIVATION_SUCCESS;
            } else {
                return ACTIVATION_REPEATE;
            }
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 登录业务逻辑 (第一次登录)
     */
    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
        }

        //验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }
        //验证激活状态 0-未激活 1-已激活
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }
        //验证密码
        password = CommunityUtils.md5(password + user.getSalt());
        if (!password.equals(user.getPassword())) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        //生成登录凭证
        String ticket = CommunityUtils.generateUUID();
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(ticket);
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);
        String ticketKey = RedisKeyUtils.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey, loginTicket);

        map.put("ticket", ticket);
        return map;
    }


    //用户退出业务逻辑
    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket, 1);
        String ticketKey = RedisKeyUtils.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
    }


    /**
     * 忘记密码 获取邮箱验证码
     */
    public Map<String, Object> getVerifyCode(String email) {
        Map<String, Object> map = new HashMap<>();
        //空值处理 前端处理了
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        //验证邮箱
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "该邮箱尚未注册!");
            return map;
        }

        //生成验证码
        String verifyCode = CommunityUtils.generateUUID().substring(0, 6).toUpperCase();
        map.put("verifyCode", verifyCode);
        //发送邮件
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("verifyCode", verifyCode);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email, "忘记密码", content);

        return map;
    }

    /**
     * 忘记密码->修改密码
     */
    public Map<String, Object> resetPassword(String email, String password) {
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        //验证邮箱
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "该邮箱尚未注册!");
            return map;
        }
        //重置密码
        password = CommunityUtils.md5(password + user.getSalt());
        userMapper.updatePassword(user.getId(), password);
        clearCache(user.getId());

        return map;
    }

    /**
     * 获取LoginTicket
     */
    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        String ticketKey = RedisKeyUtils.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }

    /**
     * 更新用户头像
     */
    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    /**
     * 修改密码
     */
    public Map<String, Object> changePassword(String oldPwd, String newPwd, User user) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(oldPwd)) {
            map.put("oldPwdMsg", "原密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(newPwd)) {
            map.put("newPwdMsg", "新密码不能为空!");
            return map;
        }
        oldPwd = CommunityUtils.md5(oldPwd + user.getSalt());
        if (!oldPwd.equals(user.getPassword())) {
            map.put("oldPwdMsg", "您输入的原密码不正确!");
            return map;
        }
        newPwd = CommunityUtils.md5(newPwd + user.getSalt());
        userMapper.updatePassword(user.getId(), newPwd);
        clearCache(user.getId());
        return map;
    }


    // 根据用户名查询用户
    public User findUserByName(String name) {
        return userMapper.selectByName(name);
    }

    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String userKey = RedisKeyUtils.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        String userKey = RedisKeyUtils.getUserKey(userId);
        User user = userMapper.selectById(userId);
        redisTemplate.opsForValue().set(userKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String userKey = RedisKeyUtils.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

    // 获取用户权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

}
