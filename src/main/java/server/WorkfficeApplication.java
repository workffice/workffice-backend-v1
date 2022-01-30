package server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScans(
        value = {
                @ComponentScan("controller"),
                @ComponentScan("authentication"),
                @ComponentScan("backoffice"),
                @ComponentScan("booking"),
                @ComponentScan("news"),
                @ComponentScan("shared"),
                @ComponentScan("search"),
                @ComponentScan("report"),
                @ComponentScan("review"),
        }
)
@EnableAsync
public class WorkfficeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkfficeApplication.class, args);
    }
}
