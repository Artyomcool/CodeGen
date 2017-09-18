package ru.mail.android.meetup.codegen

import com.github.javaparser.JavaParser
import com.google.testing.compile.JavaFileObjects

import javax.tools.*
import java.nio.charset.Charset

class StatProcessorTest extends GroovyTestCase {

    void testCompile() {
        ClassLoader loader = generate("""
            package ru.mail.android.meetup.codegen;

            public class Stat {

                @Statistic(AllSender.class)
                public interface Main {

                    void fabClicked(@Value int length);

                    void settingsClicked();

                    void someMoreComplexEvent(String type,
                                              @Value long duration,
                                              boolean important);

                }

            }
            """,
            """
            package ru.mail.android.meetup.codegen;

            public class AllSender implements Sender {

                public static StatisticParams lastParams;

                @Override
                public void send(StatisticParams params) {
                    lastParams = params;
                }
            }

            """,
            """
            package ru.mail.android.meetup.codegen;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.PARAMETER)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Value {
            }
            """
        )

        def Stat$$$StatGenerated = loader.loadClass('ru.mail.android.meetup.codegen.Stat$$$StatGenerated')
        def Stat$Main = loader.loadClass('ru.mail.android.meetup.codegen.Stat$Main')
        def AllSender = loader.loadClass('ru.mail.android.meetup.codegen.AllSender')

        assert AllSender.lastParams == null

        def main = Stat$$$StatGenerated.getInstance(Stat$Main)
        main.fabClicked(7)

        StatisticParams params = AllSender.lastParams
        assert params.className == 'Main'
        assert params.methodName == 'fabClicked'
        assert params.params[0].name == 'length'
        assert params.params[0].value == 7
        assert params.params[0].stringValue == '7'
        assert params.params[0].hasAnnotation(loader.loadClass('ru.mail.android.meetup.codegen.Value'))
    }

    def generate(String... sources) {
        def objects = sources.collect { String code ->
            def unit = JavaParser.parse(code)
            def packageName = unit.packageDeclaration.get()?.name?.toString()
            def className = unit.getType(0).name.toString()
            def fullName = packageName == null ? className : "$packageName.$className"

            JavaFileObjects.forSourceLines(fullName, code)
        }
        def compiler = ToolProvider.getSystemJavaCompiler();

        def generatedSouces = []
        def generatedClasses = []

        def stdFileManager = compiler.getStandardFileManager({}, Locale.ENGLISH, Charset.forName("UTF-8"))
        JavaFileManager inMemoryFileManager = Class.forName("com.google.testing.compile.InMemoryJavaFileManager").newInstance(stdFileManager) as JavaFileManager

        JavaFileManager fileManager = new ForwardingJavaFileManager(inMemoryFileManager) {
            JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
                switch (location.name) {
                    case StandardLocation.SOURCE_OUTPUT.name:
                        generatedSouces << className
                        break
                    case StandardLocation.CLASS_OUTPUT.name:
                        generatedClasses << className
                        break
                }
                return super.getJavaFileForOutput(location, className, kind, sibling)
            }
        }

        def diagnostics = []

        def task = compiler.getTask(null, fileManager, { diagnostics << it }, [], [], objects);

        task.setProcessors([new StatProcessor()])

        try {
            if (!task.call()) {
                throw new IllegalStateException("Compilation error");
            }

            ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
                @Override
                Class<?> findClass(String name) throws ClassNotFoundException {
                    if (name in generatedClasses) {
                        def bytes = fileManager.getJavaFileForOutput(
                                [getName: { 'CLASS_OUTPUT' }] as JavaFileManager.Location,
                                name,
                                JavaFileObject.Kind.CLASS,
                                null
                        ).openInputStream().bytes
                        return defineClass(name, bytes, 0, bytes.length)
                    }
                    throw new ClassNotFoundException("Class not found: " + name)
                }
            }

            return loader
        } catch (Exception e) {
            def exception = "Compilation error.\n====== Sources =======\n${getSources(inMemoryFileManager, generatedSouces)}\n\n======= Diagnostic logs ======\n${diagnostics.join('\n')}\n\n"
            throw new IllegalStateException(exception, e);
        }
    }

    static getSources(JavaFileManager fileManager, List<String> generatedSources) {
        generatedSources.collect {
            "====== File $it ======\n" + fileManager.getJavaFileForOutput(
                    [getName: { 'SOURCE_OUTPUT' }] as JavaFileManager.Location,
                    it,
                    JavaFileObject.Kind.SOURCE,
                    null
            ).getCharContent(true)
        }.join('\n\n')
    }

}
