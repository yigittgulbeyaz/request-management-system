package com.yigit.requestms;

import org.springframework.boot.SpringApplication;

public class TestRequestManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(RequestManagementSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
