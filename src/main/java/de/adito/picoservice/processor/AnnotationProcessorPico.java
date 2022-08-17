package de.adito.picoservice.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import de.adito.picoservice.PicoService;

/**
 * Generates instances of {@link de.adito.picoservice.IPicoRegistration} and the corresponding service entries in the
 * META-INF directory for classes annotated with annotations that are annotated with {@link de.adito.picoservice.PicoService}.
 *
 * @author j.boesl, 23.03.15
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public class AnnotationProcessorPico extends AbstractProcessor
{
  private static final String PICO_POSTFIX = "PicoService";
//@formatter:off
  private static final String REGISTRATION_TEMPLATE = ""
  		+ "package {0};\r\n"
  		+ "\r\n"
  		+ "import de.adito.picoservice.IPicoRegistration;\r\n"
  		+ "import java.util.Objects;\r\n"
  		+ "import {4};\r\n"
  		+ "\r\n"
  		+ "@Generated(value = \"de.adito.picoservice.processor.AnnotationProcessorPico\", date = \"{3}\")\r\n"
  		+ "public class {1} implements IPicoRegistration '{'\r\n"
  		+ "\r\n"
  		+ "	private static final Object INSTANCE_MUTEX = new Object();\r\n"
  		+ "	private static {1} _INSTANCE;\r\n"
  		+ "\r\n"
  		+ "	public static {1} provider() '{'\r\n"
  		+ "			if (_INSTANCE == null)\r\n"
  		+ "			synchronized (INSTANCE_MUTEX) '{'\r\n"
  		+ "				if (_INSTANCE == null)\r\n"
  		+ "					_INSTANCE = new {1}();\r\n"
  		+ "			'}'\r\n"
  		+ "		return _INSTANCE;\r\n"
  		+ "	'}'\r\n"
  		+ "\r\n"
  		+ "	@Override\r\n"
  		+ "	public Class<?> getAnnotatedClass() '{'\r\n"
  		+ "		return {2}.class;\r\n"
  		+ "	'}'\r\n"
  		+ "	\r\n"
  		+ "	@Override\r\n"
  		+ "	public int hashCode() '{'\r\n"
  		+ "		return Objects.hash({1}.class, getAnnotatedClass());\r\n"
  		+ "	'}'\r\n"
  		+ "\r\n"
  		+ "	@Override\r\n"
  		+ "	public boolean equals(Object obj) '{'\r\n"
  		+ "		if (this == obj)\r\n"
  		+ "			return true;\r\n"
  		+ "		if (obj == null)\r\n"
  		+ "			return false;\r\n"
  		+ "		if ({1}.class != obj.getClass())\r\n"
  		+ "			return false;\r\n"
  		+ "		{1} other = ({1}) obj;\r\n"
  		+ "		return Objects.equals(getAnnotatedClass(), other.getAnnotatedClass());\r\n"
  		+ "	'}'\r\n"
  		+ "	\r\n"
  		+ "'}'"
  		+ "";
  //@formatter:on
  private static final String SERVICE_REGISTRATION_PATH = "META-INF/services/de.adito.picoservice.IPicoRegistration";
  private static final List<ElementKind> ENCLOSING_TYPES =
      Arrays.asList(ElementKind.PACKAGE, ElementKind.CLASS, ElementKind.INTERFACE, ElementKind.ENUM);

  private final Set<TypeElement> annotatedElements = new LinkedHashSet<>();


  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
  {
    if (roundEnv.processingOver())
    {
      if (!annotatedElements.isEmpty())
        _generateRegistration(annotatedElements);
    }
    else
    {
      for (TypeElement annotation : annotations) {
        if (annotation.getAnnotation(PicoService.class) != null) {
          if (_isValidElement(annotation))
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(annotation))
              annotatedElements.add((TypeElement) annotatedElement);
        }
      }
    }
    return false;
  }

  private void _generateRegistration(Set<TypeElement> pAnnotatedElements)
  {
    Set<String> serviceSet = new LinkedHashSet<>();
    Filer filer = processingEnv.getFiler();
    for (TypeElement typeElement : pAnnotatedElements)
    {
      try
      {
        _ElementInfo eI = new _ElementInfo(typeElement);
        eI.write(filer);
        serviceSet.add(eI.fqn);
      }
      catch (IOException e)
      {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getLocalizedMessage(), typeElement);
      }
    }

    try
    {
      FileObject serviceFile = filer.getResource(StandardLocation.CLASS_OUTPUT, "", SERVICE_REGISTRATION_PATH);
      if (Files.isRegularFile(Paths.get(serviceFile.toUri())))
      {
        try (BufferedReader reader = new BufferedReader(serviceFile.openReader(true)))
        {
          String line;
          while ((line = reader.readLine()) != null)
            serviceSet.add(line);
        }
      }
    }
    catch (IOException e)
    {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Couldn't load existing serviceSet: " + e);
    }

    try
    {
      List<String> services = new ArrayList<>(serviceSet);
      Collections.sort(services);
      FileObject serviceFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "", SERVICE_REGISTRATION_PATH);
      try (Writer writer = serviceFile.openWriter())
      {
        for (String service : services)
          writer.append(service).append("\n");
      }
    }
    catch (IOException x)
    {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write service definition files: " + x);
    }
  }

  private boolean _isValidElement(Element pElement)
  {
    Retention retention = pElement.getAnnotation(Retention.class);
    if (retention == null || retention.value() != RetentionPolicy.RUNTIME)
    {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Retention should be RUNTIME", pElement);
      return false;
    }
    Target target = pElement.getAnnotation(Target.class);
    if (target == null || target.value().length == 0)
    {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Target has to be defined", pElement);
      return false;
    }
    else
    {
      for (ElementType elementType : target.value())
      {
        if (elementType != ElementType.TYPE)
        {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Unsupported type: " + elementType, pElement);
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Bag for element info.
   */
  private class _ElementInfo
  {
    private final TypeElement typeElement;
    private final String pckg;
    private final String annotatedClsName;
    private final String clsName;
    private final String fqn;

    _ElementInfo(TypeElement pTypeElement)
    {
      typeElement = pTypeElement;
      pckg = _getPackage(pTypeElement);
      annotatedClsName = _getAnnotatedClassName(pTypeElement);
      clsName = annotatedClsName.replaceAll("\\.", "\\$") + PICO_POSTFIX;
      fqn = pckg + "." + clsName;
    }

    void write(Filer pFiler) throws IOException
    {
      FileObject sourceFile = pFiler.getResource(StandardLocation.SOURCE_OUTPUT, pckg, clsName + ".java");
      Path path = Paths.get(sourceFile.toUri());
      try (Writer writer = Files.exists(path) ? Files.newBufferedWriter(path) : pFiler.createSourceFile(fqn, typeElement).openWriter())
      {
        String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(new Date());
        String importString = _getJavaVersion() >= 9 ?
            "javax.annotation.processing.Generated" :
            "javax.annotation.Generated";
        String content = MessageFormat.format(REGISTRATION_TEMPLATE, pckg, clsName, annotatedClsName, date, importString);
        writer.write(content);
      }
    }

    private int _getJavaVersion()
    {
      try {
        return Integer.parseInt(System.getProperty("java.specification.version"));
      }
      catch (NumberFormatException pE) {
        return 8;
      }
    }

    private String _getPackage(Element pElement)
    {
      Element element = pElement;
      while (element != null)
      {
        if (element.getKind() == ElementKind.PACKAGE)
			if (element instanceof QualifiedNameable)
				return ((QualifiedNameable) element).getQualifiedName().toString();
			else
				return element.toString();
        Element enclosingElement = element.getEnclosingElement();
        if (ENCLOSING_TYPES.contains(enclosingElement.getKind()))
          element = enclosingElement;
        else
        {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Element type not supported", pElement);
          break;
        }
      }
      return "";
    }

    private String _getAnnotatedClassName(Element pElement)
    {
      StringBuilder name = new StringBuilder();
      Element element = pElement;
      while (element != null && element.getKind() != ElementKind.PACKAGE)
      {
        if (name.length() > 0)
          name.insert(0, ".");
        name.insert(0, element.getSimpleName());
        Element enclosingElement = element.getEnclosingElement();
        if (ENCLOSING_TYPES.contains(enclosingElement.getKind()))
          element = enclosingElement;
        else
        {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Element type not supported", pElement);
          break;
        }
      }
      return name.toString();
    }
  }

}
