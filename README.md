# MicroSpringBootG

Prototipo de servidor web e IoC framework reflexivo en Java para el taller.

## Funcionalidades implementadas

- Servidor HTTP con soporte de multiples solicitudes no concurrentes.
- Entrega de recursos estaticos desde `src/main/resources/static`.
- Soporte de `text/html` y `image/png`.
- Carga de beans anotados con `@RestController` usando exploracion del classpath.
- Registro de endpoints con `@GetMapping`.
- Inyeccion de parametros de consulta con `@RequestParam(value, defaultValue)`.
- Modo legacy para invocar POJO por linea de comandos.

## Estructura principal

- `src/main/java/co/edu/escuelaing/microspringbootg/MicroSpringBootG.java`
- `src/main/java/co/edu/escuelaing/microspringbootg/RestController.java`
- `src/main/java/co/edu/escuelaing/microspringbootg/GetMapping.java`
- `src/main/java/co/edu/escuelaing/microspringbootg/RequestParam.java`
- `src/main/java/co/edu/escuelaing/microspringbootg/HelloController.java`
- `src/main/java/co/edu/escuelaing/microspringbootg/GreetingController.java`

## Compilar

```bash
mvn clean package -DskipTests
```

## Ejecutar servidor

```bash
java -cp target/classes co.edu.escuelaing.microspringbootg.MicroSpringBootG
```

Opciones:

- `--port=####` para cambiar el puerto (default `35000`).
- `--scan=paquete.base` para cambiar el paquete a explorar.

## Probar endpoints

- `http://localhost:35000/` (pagina HTML)
- `http://localhost:35000/logo.png` (imagen PNG)
- `http://localhost:35000/pi`
- `http://localhost:35000/greeting`
- `http://localhost:35000/greeting?name=Ana`

## Modo legacy (primera version sugerida)

```bash
java -cp target/classes co.edu.escuelaing.microspringbootg.MicroSpringBootG co.edu.escuelaing.microspringbootg.HelloController /pi
```
