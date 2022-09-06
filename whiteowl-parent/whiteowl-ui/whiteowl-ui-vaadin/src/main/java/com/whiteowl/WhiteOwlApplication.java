package com.whiteowl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Push
@EnableRetry
@EnableCaching
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties
@EnableAsync(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Theme(themeClass = Lumo.class, variant = Lumo.DARK)
@NpmPackage(value = "line-awesome", version = "1.3.0")
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@PWA(name = "Mongoose", shortName = "M", offlineResources = {})
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class WhiteOwlApplication implements AppShellConfigurator {
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(WhiteOwlApplication.class, args);
	}
}
