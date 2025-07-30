package org.lim.aiautocode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("org.lim.aiautocode.mapper")
public class AiAutoCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAutoCodeApplication.class, args);
    }

}
