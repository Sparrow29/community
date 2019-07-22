package com.nowcoder.community.controller;

import com.nowcoder.community.service.AlphaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/alpha")
public class AlphaController {

    @Autowired
    private AlphaService alphaService;

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello(){
        return "Hello spring boot!";
    }

    @RequestMapping("/data")
    @ResponseBody
    public String getData(){
        return alphaService.select();
    }


    //get请求
    @RequestMapping(path = "/students", method = RequestMethod.GET)
    @ResponseBody
    public String getStudents(
            @RequestParam(name = "current", required = false, defaultValue = "1") int current,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit){
        System.out.println(current);
        System.out.println(limit);
        return "some student";
    }

    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudentById(@PathVariable int id){
        System.out.println(id);
        return "a student";
    }

    //post请求
    @RequestMapping(path = "/save", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(@RequestParam String name, @RequestParam("age") int age){
        System.out.println(name);
        System.out.println(age);
        return "save success";
    }

    //响应html数据
    //方式一
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public String getTeacher(Model model){
        model.addAttribute("name", "sparrow");
        model.addAttribute("age", "20");
        return "/demo/data";
    }

    //方式二
    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public ModelAndView getSchool(){
        ModelAndView mav = new ModelAndView();
        mav.addObject("name", "北邮");
        mav.addObject("age", 100);
        mav.setViewName("/demo/data");
        return mav;
    }

    //响应json数据
    @RequestMapping(path = "/json", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getEmp(){
        List<Map<String, Object>> emps = new ArrayList<>();
        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "张三");
        emp.put("age", 18);
        emps.add(emp);

        emp = new HashMap<>();
        emp.put("name", "李四");
        emp.put("age", 19);
        emps.add(emp);

        return emps;
    }

    //创建cookie
    @RequestMapping(path = "/cookie/set", method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response){
        Cookie cookie = new Cookie("code", "123456");
        cookie.setPath("/community/alpha");
        cookie.setMaxAge(60 * 10);
        response.addCookie(cookie);
        return "set cookie";
    }

    //获取cookie
    @RequestMapping(path = "/cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code){
        System.out.println(code);
        return "get cookie";
    }

    //创建sesiion
    @RequestMapping(path = "/session/set", method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session){
        session.setAttribute("username","sparrow");
        session.setAttribute("password", "123");
        return "set session";
    }
    //获取session
    @RequestMapping(path = "/session/get", method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session){
        System.out.println(session.getAttribute("username"));
        System.out.println(session.getAttribute("password"));
        return "get session";
    }
}
