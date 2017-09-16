package ru.mail.android.meetup.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("ru.mail.android.meetup.codegen.Statistic")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class StatProcessor extends AbstractProcessor {

    private static final String POSTFIX = "$$$StatGenerated";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            Map<TypeElement, List<TypeElement>> rootToNested = new LinkedHashMap<>();

            for (TypeElement annotation : annotations) {
                Set<? extends Element> annotatedElements =
                        roundEnv.getElementsAnnotatedWith(annotation);
                for (Element element : annotatedElements) {
                    TypeElement typeElement = (TypeElement) element;
                    TypeElement root = findRoot(typeElement);
                    if (typeElement == root) {
                        processingEnv.getMessager()
                                .printMessage(Diagnostic.Kind.ERROR, "Can't generate", root);
                        continue;
                    }
                    List<TypeElement> nested = rootToNested.get(root);
                    if (nested == null) {
                        rootToNested.put(root, nested = new ArrayList<>());
                    }
                    nested.add(typeElement);
                }
            }

            for (Map.Entry<TypeElement, List<TypeElement>> entry : rootToNested.entrySet()) {
                TypeElement root = entry.getKey();
                List<TypeElement> nested = entry.getValue();
                generate(root, nested);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "IOException: " + e);
        }
        return true;
    }

    private TypeElement findRoot(TypeElement element) {
        while (true) {
            Element enclosingElement = element.getEnclosingElement();
            if (enclosingElement == null || enclosingElement.getKind() == ElementKind.PACKAGE) {
                return element;
            }
            element = (TypeElement) enclosingElement;
        }
    }

    private void generate(TypeElement root, List<TypeElement> elements) throws IOException {
        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(root);

        String packageName = packageElement == null
                ? ""
                : packageElement.getQualifiedName().toString();

        ClassName generatedClassName = ClassName.get(packageName, root.getSimpleName() + POSTFIX);
        TypeSpec.Builder rootBuilder = TypeSpec.classBuilder(generatedClassName)
                .addModifiers(Modifier.PUBLIC);
        addSuperType(rootBuilder, root);

        rootBuilder.addField(getClassMapField());

        TypeVariableName t = TypeVariableName.get("T");
        ParameterizedTypeName classOfT = ParameterizedTypeName.get(ClassName.get(Class.class), t);

        rootBuilder.addMethod(MethodSpec.methodBuilder("getInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(t)
                .returns(t)
                .addParameter(classOfT, "clazz")
                .addStatement("return clazz.cast(__impl.get(clazz))")
                .build()
        );

        CodeBlock.Builder staticBlock = CodeBlock.builder();
        for (TypeElement element : elements) {
            TypeSpec.Builder builder = TypeSpec.classBuilder(element.getSimpleName().toString())
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC);
            addSuperType(builder, element);

            builder.addField(
                    FieldSpec.builder(Sender.class, "__sender", Modifier.PRIVATE, Modifier.FINAL)
                            .initializer("new $T()", getSender(element))
                            .build()
            );

            for (Element e : element.getEnclosedElements()) {
                if (e.getKind() == ElementKind.METHOD) {
                    if (e.getModifiers().contains(Modifier.ABSTRACT)) {
                        generateMethod(builder, (ExecutableElement) e);
                    }
                }
            }

            TypeSpec typeSpec = builder.build();
            rootBuilder.addType(typeSpec);
            staticBlock.addStatement("__impl.put($T.class, new $L())", element, typeSpec.name);
        }

        rootBuilder.addStaticBlock(staticBlock.build());

        TypeSpec typeSpec = rootBuilder.build();

        JavaFile.builder(packageName, typeSpec)
                .indent("    ")
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private void addSuperType(TypeSpec.Builder builder, TypeElement superype) {
        ClassName rootClassName = ClassName.get(superype);
        if (superype.getKind() == ElementKind.INTERFACE) {
            builder.addSuperinterface(rootClassName);
        } else {
            builder.superclass(rootClassName);
        }
    }

    private FieldSpec getClassMapField() {
        ParameterizedTypeName classWildcard = ParameterizedTypeName.get(
                ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)
        );
        ParameterizedTypeName mapClassToObject = ParameterizedTypeName.get(
                ClassName.get(Map.class), classWildcard, ClassName.OBJECT
        );

        FieldSpec.Builder builder = FieldSpec.builder(
                mapClassToObject,
                "__impl",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL
        );

        return builder
                .initializer("new $T<>()", HashMap.class)
                .build();
    }

    private TypeMirror getSender(TypeElement element) {
        TypeMirror sender;
        try {
            element.getAnnotation(Statistic.class).value();
            throw new IllegalStateException("MirroredTypeException should be thrown");
        } catch (MirroredTypeException e) {
            sender = e.getTypeMirror();
        }
        return sender;
    }

    private Set<Modifier> notAbstract(Set<Modifier> modifiers) {
        modifiers = new HashSet<>(modifiers);
        modifiers.remove(Modifier.ABSTRACT);
        return modifiers;
    }

    private Iterable<ParameterSpec> toParamSpec(List<? extends VariableElement> params) {
        return params.stream().map(ParameterSpec::get).collect(Collectors.toList());
    }

    private static <E> boolean join(Iterable<? extends E> iterable, Consumer<E> action, Consumer<E> between) {
        Spliterator<? extends E> spliterator = iterable.spliterator();

        if (spliterator.tryAdvance(action)) {
            spliterator.forEachRemaining(between.andThen(action));
            return true;
        }
        return false;
    }

    private void generateMethod(TypeSpec.Builder builder, ExecutableElement element) {
        List<? extends VariableElement> parameters = element.getParameters();

        CodeBlock.Builder code = CodeBlock.builder();
        CodeBlock.Builder params = CodeBlock.builder();

        if (parameters.isEmpty()) {
            params.add("$T.emptyList()", Collections.class);
        } else {
            CodeBlock.Builder pList = CodeBlock.builder();
            join(parameters,
                    p -> {
                        String pName = p.getSimpleName().toString();
                        pList.add("new $T($S, $L", StatisticParams.Param.class, pName, pName);
                        for (AnnotationMirror t : p.getAnnotationMirrors()) {
                            pList.add(",$W");
                            pList.add("$T.class", t.getAnnotationType());
                        }
                        pList.add(")");
                    },
                    p -> pList.add(",\n")
            );
            params.add("$T.asList($>\n$L$<\n)", Arrays.class, pList.build());
        }
        code.addStatement("$T<$T> __params = $L", List.class, StatisticParams.Param.class, params.build());

        Name className = element.getEnclosingElement().getSimpleName();
        Name methodName = element.getSimpleName();

        code.addStatement("$T __statParams = new $T($S, $S, __params)",
                StatisticParams.class, StatisticParams.class, className, methodName);

        code.addStatement("__sender.send(__statParams)");

        builder.addMethod(MethodSpec.methodBuilder(methodName.toString())
                .returns(ClassName.get(element.getReturnType()))
                .addParameters(toParamSpec(parameters))
                .addModifiers(notAbstract(element.getModifiers()))
                .addAnnotation(Override.class)
                .addCode(code.build())
                .build());
    }

}