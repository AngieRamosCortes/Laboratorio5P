# Reflective ChatGPT - Minimal Implementation

## Qué es

Tres componentes simples basados en sockets/HTTP:

- Backend (`BackendServer`): expone `/compreflex?comando=...` y ejecuta reflexión.
- Fachada (`FacadeServer`): expone `/cliente` (HTML+JS) y `/consulta?comando=...` que reenvía al backend.
- Cliente: HTML entregado por la fachada, hace llamadas asíncronas con `XMLHttpRequest`.

Sin frameworks. Una clase por responsabilidad y respuestas JSON/TEXTO válidas.

## Requisitos

- JDK 11+ en PATH (probado con 11–21)

## Compilar (Windows cmd)

```cmd
javac -d out src\rcg\*.java
```

## Ejecutar

1) Backend (puerto 45000):

```cmd
java -cp out rcg.BackendServer
```

2) Fachada (puerto 35000):

```cmd
java -cp out rcg.FacadeServer
```

3) Cliente: http://localhost:35000/cliente

## Comandos de ejemplo

- `Class(java.lang.Math)`
- `invoke(java.lang.System, getenv)`
- `unaryInvoke(java.lang.Math, abs, int, -3)`
- `unaryInvoke(java.lang.Integer, valueOf, String, "3")`
- `binaryInvoke(java.lang.Math, max, double, 4.5, double, -3.7)`

Solo invoca métodos estáticos con parámetros `int`, `double` o `String`.

## Despliegue en 2 máquinas

- Ejecute el Backend en la VM A: `http://<IP-A>:45000`.
- Inicie la Fachada en la VM B cambiando el `backendBaseUrl` en `FacadeServer.main` a `http://<IP-A>:45000`.

## Notas

- Respuestas con `Content-Type`, `Content-Length`, `Connection: close` y CORS `*`.
