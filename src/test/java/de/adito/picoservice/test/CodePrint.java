package de.adito.picoservice.test;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CodePrint {
	private static final Class<?> THIS_CLASS = new Object() {}.getClass().getEnclosingClass();

	public static void main(String[] args) {
		//@formatter:off
		String template = "package {0};\n"
				+ "\n"
				+ "import de.adito.picoservice.IPicoRegistration;\n"
				+ "\n"
				+ "import {4};\n"
				+ "\n"
				+ "@Generated(value = \"de.adito.picoservice.processor.AnnotationProcessorPico\", date = \"{3}\")\n"
				+ "public enum {1} implements IPicoRegistration\n"
				+ "'{'\n"
				+ "  INSTANCE;\n"
				+ "  \n"
				+ "  public static {1} provider()\n"
				+ "  '{'\n"
				+ "    return {1}.INSTANCE;  \n"
				+ "  '}'\n"
				+ "  \n"
				+ "  @Override\n"
				+ "  public Class<?> getAnnotatedClass()\n"
				+ "  '{'\n"
				+ "    return {2}.class;\n"
				+ "  '}'\n"
				+ "'}'\n"
				+ "";
		//@formatter:on
		int javaVersion = 11;
		String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(new Date());
		String importString = javaVersion >= 9 ? "javax.annotation.processing.Generated" : "javax.annotation.Generated";
		String content = MessageFormat.format(template, THIS_CLASS.getPackage().getName(), "ServiceDef",
				"com.xyz.ServiceImpl", date, importString);
		System.out.println(content);
	}
}
