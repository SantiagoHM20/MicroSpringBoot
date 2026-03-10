package co.edu.escuelaing.microspringbootg;

@RestController
public class HelloController {
    @GetMapping("/")
    public String index(){
        return "Greetings from SpringBoot";
    }
    @GetMapping("/pi")
    public String getPi(){
        return "PI= " + Math.PI;
    }
    @GetMapping("/helli")
    public String hello(){
        return "Hello World";
    }
}
