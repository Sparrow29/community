package com.nowcoder.community.controller;

import com.nowcoder.community.service.AlphaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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
}
