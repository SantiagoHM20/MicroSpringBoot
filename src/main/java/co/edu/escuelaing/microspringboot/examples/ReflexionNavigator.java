package co.edu.escuelaing.microspringboot.examples;

import static java.lang.System.out;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class ReflexionNavigator {

    public static void main(String[] args) {
        Class<?> c = "hello".getClass();

        printMembers(c.getDeclaredMethods(), "Method");

    }

    private static void printMembers(Member[] mbrs, String s) {
        out.format("%s:%n", s);
        for (Member mbr : mbrs) {
            if (mbr instanceof Field field)
                out.format("  %s%n", field.toGenericString());
            else if (mbr instanceof Constructor<?> constructor)
                out.format("  %s%n", constructor.toGenericString());
            else if (mbr instanceof Method method)
                out.format("  %s%n", method.toGenericString());
        }
        if (mbrs.length == 0)
            out.format("  -- No %s --%n", s);
        out.format("%n");

    }
}
