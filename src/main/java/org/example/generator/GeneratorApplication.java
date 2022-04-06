package org.example.generator;

import org.example.generator.config.GeneratorConfig;
import org.example.generator.service.CustomGeneratorService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class GeneratorApplication {

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication(GeneratorApplication.class);
		springApplication.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = springApplication.run(args);

		CustomGeneratorService generatorService = context.getBean("customGeneratorService", CustomGeneratorService.class);
		GeneratorConfig generatorConfig = context.getBean("generatorConfig", GeneratorConfig.class);

		// 执行生成代码
		try {
			generatorService.generatorCode(generatorConfig);
			System.out.println("代码生成成功！");
		} catch (Exception ex) {
			ex.printStackTrace();
//			System.err.println(e.getMessage());
		}

	}
}
