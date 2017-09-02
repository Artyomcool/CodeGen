package ru.mail.android.meetup.codegen;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("ru.mail.android.meetup.codegen.Statistic")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class StatProcessor extends AbstractProcessor {

    public static final String POSTFIX = "$$$StatGenerated";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement annotation : annotations) {
                Set<? extends Element> annotatedElements =
                        roundEnv.getElementsAnnotatedWith(annotation);
                for (Element element : annotatedElements) {
                    generate((TypeElement) element);
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "IOException: " + e);
        }
        return true;
    }

    private void generate(TypeElement element) throws IOException {
        String qualifiedName = element.getQualifiedName() + POSTFIX;
        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
        String nameWithoutPackage = packageElement == null
                ? qualifiedName
                : qualifiedName.substring(packageElement.getQualifiedName().toString().length() + 1);
        String name = nameWithoutPackage.replace('.', '$');
        String fileName = packageElement == null
                ? nameWithoutPackage
                : packageElement.getQualifiedName() + "." + name;
        JavaFileObject sourceFile =
                processingEnv.getFiler().createSourceFile(fileName, element);
        PrintWriter out = new PrintWriter(sourceFile.openWriter());
        try {

            if (packageElement != null) {
                out.print("package ");
                out.print(packageElement.getQualifiedName());
                out.println(";");
                out.println();
            }

            String codegenPackage = "ru.mail.android.meetup.codegen";
            if (packageElement == null || !codegenPackage.equals(packageElement.getSimpleName().toString())) {
                out.print("import ");
                out.print(codegenPackage);
                out.println(".*;");
            }

            out.println("import java.util.Arrays;");
            out.println("import java.util.Collections;");
            out.println("import java.util.List;");
            out.println();

            out.print("public class ");
            out.print(name);
            out.print(element.getKind() == ElementKind.INTERFACE ? " implements " : " extends ");
            out.print(element.getQualifiedName());
            out.println(" {");
            out.println();

            for (Element e : element.getEnclosedElements()) {
                if (e.getKind() == ElementKind.METHOD) {
                    if (e.getModifiers().contains(Modifier.ABSTRACT)) {
                        generateMethod((ExecutableElement) e, out);
                    }
                }
            }

            out.println("}");
        } finally {
            out.close();
        }
    }

    private void generateMethod(ExecutableElement element, PrintWriter out) {
        out.println("    @Override");
        out.print("    ");
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            out.print("public ");
        } else if (element.getModifiers().contains(Modifier.PROTECTED)) {
            out.print("protected ");
        }

        out.print(element.getReturnType());
        out.print(" ");
        out.print(element.getSimpleName());
        out.print("(");

        List<? extends VariableElement> parameters = element.getParameters();
        if (!parameters.isEmpty()) {
            for (VariableElement p : parameters.subList(0, parameters.size() - 1)) {
                generateParamDecl(p, out);
                out.print(", ");
            }
            generateParamDecl(parameters.get(parameters.size() - 1), out);
        }

        out.println(") {");

        out.print("        ");
        out.print("List<StatisticParams.Param> __params = ");
        if (!parameters.isEmpty()) {
            out.println("Arrays.asList(");
            for (VariableElement p : parameters.subList(0, parameters.size() - 1)) {
                generateParam(p, out);
                out.println(",");
            }
            generateParam(parameters.get(parameters.size() - 1), out);
            out.println();
            out.println("        );");
        } else {
            out.println("Collections.emptyList();");
        }

        out.print("        StatisticParams __statParams = new StatisticParams(\"");
        out.print(element.getEnclosingElement().getSimpleName());
        out.print("\", \"");
        out.print(element.getSimpleName());
        out.println("\", __params);");

        out.println("    }");
        out.println();

    }

    private void generateParamDecl(VariableElement p, PrintWriter out) {
        out.print(p.asType());
        out.print(" ");
        out.print(p);
    }

    private void generateParam(VariableElement p, PrintWriter out) {
        out.print("                new StatisticParams.Param(\"");
        out.print(p.getSimpleName());
        out.print("\", ");
        out.print(p.getSimpleName());

        for (AnnotationMirror mirror : p.getAnnotationMirrors()) {
            out.print(", ");
            generateAnnotation(mirror, out);
        }

        out.print(")");

    }

    private void generateAnnotation(AnnotationMirror annotationMirror, PrintWriter out) {
        out.print(annotationMirror.getAnnotationType());
        out.print(".class");
    }


}