package rcg;

/**
 * Generates a minimal HTML page with JavaScript client.
 * Provides a static method to emit the UI as a string.
 */
public class ClientPage {
    /**
     * Builds the client page that allows entering commands and seeing results.
     * Uses XMLHttpRequest to perform asynchronous GET requests.
     *
     * @return HTML content as a string
     */
    public static String html() {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset=\"UTF-8\"><title>Reflective ChatGPT</title>" +
                "<style>body{font-family:Arial,Helvetica,sans-serif;margin:20px}#out{white-space:pre-wrap;border:1px solid #ccc;padding:10px;border-radius:6px}</style>"
                +
                "</head><body>" +
                "<h1>Reflective ChatGPT</h1>" +
                "<p>Comandos: Class(...), invoke(...), unaryInvoke(...), binaryInvoke(...)</p>" +
                "<input id=\"cmd\" style=\"width:80%\" placeholder=\"Class(java.lang.Math)\">" +
                "<button onclick=\"send()\">Enviar</button>" +
                "<pre id=\"out\"></pre>" +
                "<script>function send(){var c=document.getElementById('cmd').value;var x=new XMLHttpRequest();x.onload=function(){document.getElementById('out').textContent=this.responseText;};x.open('GET','/consulta?comando='+encodeURIComponent(c));x.send();}</script>"
                +
                "</body></html>";
    }
}
