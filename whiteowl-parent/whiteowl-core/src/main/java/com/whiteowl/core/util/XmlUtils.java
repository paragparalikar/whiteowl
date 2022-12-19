package com.whiteowl.core.util;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import lombok.NonNull;
import lombok.SneakyThrows;

public interface XmlUtils {

	@SneakyThrows
	public static String encode(@NonNull Object object) {
		try(final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final XMLEncoder xmlEncoder = new XMLEncoder(outputStream)){
			xmlEncoder.writeObject(object);
			xmlEncoder.flush();
			outputStream.flush();
			return outputStream.toString("utf-8");
		}
	}
	
	@SneakyThrows
	public static void write(@NonNull Path path, Object object) {
		if(null == object) {
			Files.deleteIfExists(path);
		} else {
			if(!Files.exists(path)) {
				Files.createDirectories(path.getParent());
				Files.createFile(path);
			}
			Files.writeString(path, encode(object), CREATE, TRUNCATE_EXISTING, WRITE);
		}
	}
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	public static <T> T decode(@NonNull String text) {
		try(final ByteArrayInputStream inputStream = new ByteArrayInputStream(text.getBytes("utf-8"));
		final XMLDecoder xmlDecoder = new XMLDecoder(inputStream)){
			return (T) xmlDecoder.readObject();
		}
	}
	
	@SneakyThrows
	public static <T> Optional<T> read(@NonNull Path path) {
		return Files.exists(path) ? Optional.of(decode(Files.readString(path))) : Optional.empty();
	}
	
}
