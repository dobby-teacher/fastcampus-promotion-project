package com.fastcampus.pointservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui")
public class PageController {

    @GetMapping("/point")
    public String pointPage() {
        return "point";
    }
}
