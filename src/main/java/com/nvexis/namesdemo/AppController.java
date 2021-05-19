package com.nvexis.namesdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

@Controller
public class AppController implements CommandLineRunner {

    @Autowired
    private ServiceOne serviceOne;

    @Override
    public void run(String... args) throws Exception {

        serviceOne.report();
    }
}
