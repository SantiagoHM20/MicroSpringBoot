package co.edu.escuelaing.microspringbootg;

@RestController
public class HelloController {
    @GetMapping("/")
    public static String index(){
        return "Greetings from SpringBoot";
    }
    @GetMapping("/pi")
    public static String getPi(){
        return "PI= " + Math.PI;
    }
    @GetMapping("/helli")
    public static String hello(){
        return "Hello World";
    }
}
