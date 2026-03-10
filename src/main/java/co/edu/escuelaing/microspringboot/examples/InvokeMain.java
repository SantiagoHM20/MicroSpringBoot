package co.edu.escuelaing.microspringboot.examples;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class InvokeMain {
    public static void main(String... args) throws IllegalAccessException, InvocationTargetException {
        try {
            Class<?> c = Class.forName(args[0]);
            Class<?>[] argTypes = new Class<?>[]{String[].class};
            Method main = c.getDeclaredMethod("main", argTypes);
            String[] mainArgs = Arrays.copyOfRange(args, 1, args.length);
            System.out.format("invoking %s.main()%n", c.getName());
            main.invoke(null, (Object) mainArgs);
        } catch (ClassNotFoundException | NoSuchMethodException x) {
            throw new IllegalArgumentException("Invalid class or missing main method", x);
        }
    }
}