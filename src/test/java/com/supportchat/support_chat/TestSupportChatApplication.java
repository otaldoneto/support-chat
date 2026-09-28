package com.supportchat.support_chat;

import org.springframework.boot.SpringApplication;

public class TestSupportChatApplication {

	public static void main(String[] args) {
		SpringApplication.from(SupportChatApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
