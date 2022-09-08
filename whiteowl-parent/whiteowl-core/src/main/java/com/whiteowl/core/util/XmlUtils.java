package com.whiteowl.core.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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
	@SuppressWarnings("unchecked")
	public static <T> T decode(@NonNull String text) {
		try(final ByteArrayInputStream inputStream = new ByteArrayInputStream(text.getBytes("utf-8"));
		final XMLDecoder xmlDecoder = new XMLDecoder(inputStream)){
			return (T) xmlDecoder.readObject();
		}
	}
	
}
