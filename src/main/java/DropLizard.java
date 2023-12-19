import annotations.ControlMe;
import annotations.GetMe;
import annotations.GiveItToMe;
import com.google.gson.Gson;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class DropLizard {
    private static String DROPLIZARD = """
                 ______   _______      ___   _______  _____     _____  ________       _       _______     ______    \s
                |_   _ `.|_   __ \\   .'   `.|_   __ \\|_   _|   |_   _||  __   _|     / \\     |_   __ \\   |_   _ `.  \s
                  | | `. \\ | |__) | /  .-.  \\ | |__) | | |       | |  |_/  / /      / _ \\      | |__) |    | | `. \\ \s
                  | |  | | |  __ /  | |   | | |  ___/  | |   _   | |     .'.' _    / ___ \\     |  __ /     | |  | | \s
                 _| |_.' /_| |  \\ \\_\\  `-'  /_| |_    _| |__/ | _| |_  _/ /__/ | _/ /   \\ \\_  _| |  \\ \\_  _| |_.' / \s
                |______.'|____| |___|`.___.'|_____|  |________||_____||________||____| |____||____| |___||______.'  \s
                                                                                                                    \s
                                
                """;
    public static void writeDropLizardAndStart() throws LifecycleException {
        for(int i = 0; i<DROPLIZARD.length();i++){
            System.out.print(DROPLIZARD.charAt(i));
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        startServer();
    }
    public static void startServer() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(""))
                .setScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner()));

        Set<Class<?>> clazs = reflections.getTypesAnnotatedWith(ControlMe.class);
        clazs.getClass().getMethods()[0].isAnnotationPresent(GetMe.class);
        int i = 0;
        for (Class<?> clas : clazs) {
            ControlMe controlMe = clas.getAnnotation(ControlMe.class);
            for (Method method : clas.getMethods()) {
                if (method.isAnnotationPresent(GetMe.class)) {
                    GetMe getMe = method.getAnnotation(GetMe.class);
                    List<Parameter> parameters = Arrays.asList(method.getParameters()).stream().filter(parameter-> parameter.isAnnotationPresent(GiveItToMe.class)).collect(Collectors.toList());
                    Context ctx = tomcat.addContext(controlMe.basePath(), new File(".").getAbsolutePath());
                    List<Object> objects = new LinkedList<>();
                    tomcat.addServlet(ctx, method.getName() + i, new HttpServlet() {
                        @Override
                        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                            if (parameters.size()>0) {
                                for (Parameter parameter : parameters) {
                                    if(parameter.getType().equals(int.class) || parameter.getType().equals(Integer.class)){
                                        Object parameterValue = Integer.parseInt(req.getParameter(parameter.getAnnotation(GiveItToMe.class).name()));
                                        Object parameterValueObject = parameterValue;;
                                        System.out.println(parameterValueObject);
                                        objects.add(parameterValue);
                                        continue;
                                    }
                                    if (parameter.getType().equals(String.class)) {
                                        Object parameterValue = req.getParameter(parameter.getAnnotation(GiveItToMe.class).name());
                                        Object parameterValueObject = parameterValue;;
                                        System.out.println(parameterValueObject);
                                        objects.add(parameterValue);
                                        continue;
                                    }
                                    if (parameter.getType().equals(List.class)) {
                                        Object parameterValue = Arrays.asList(req.getParameter(parameter.getAnnotation(GiveItToMe.class).name()).split(","));
                                        Object parameterValueObject = parameterValue;;
                                        //System.out.println(parameterValueObject);
                                        objects.add(parameterValue);
                                        continue;
                                    }
                                    /*Object parameterValue = req.getParameter(parameter.getAnnotation(GiveItToMe.class).name());
                                    Object parameterValueObject = parameterValue;;
                                    System.out.println(parameterValueObject);
                                    objects.add(parameterValue);*/
                                }
                            }
                            Writer writer = resp.getWriter();
                            resp.setContentType("application/json");
                            resp.setCharacterEncoding("UTF-8");
                            try {
                                writer.write(new Gson().toJson(method.invoke(clas.newInstance(), objects.toArray())));
                                objects.removeAll(objects);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            } catch (InvocationTargetException e) {
                                throw new RuntimeException(e);
                            } catch (InstantiationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                    ctx.addServletMappingDecoded(getMe.url() + "/*", method.getName() + i);
                }
            }
        }
        tomcat.getConnector();
        tomcat.start();
        tomcat.getServer().await();
    }
}
