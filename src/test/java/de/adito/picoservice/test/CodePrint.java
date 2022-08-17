package de.adito.picoservice.test;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.adito.picoservice.processor.AnnotationProcessorPico;

public class CodePrint {
	private static final Class<?> THIS_CLASS = new Object() {}.getClass().getEnclosingClass();

	public static void main(String[] args)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field field = AnnotationProcessorPico.class.getDeclaredField("REGISTRATION_TEMPLATE");
		field.setAccessible(true);
		String template = (String) field.get(null);
		System.out.println(template);
		int javaVersion = 11;
		String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(new Date());
		String importString = javaVersion >= 9 ? "javax.annotation.processing.Generated" : "javax.annotation.Generated";
		String content = MessageFormat.format(template, THIS_CLASS.getPackage().getName(), "ServiceDef",
				"com.xyz.ServiceImpl", date, importString);
		System.out.println(content);
	}
}
