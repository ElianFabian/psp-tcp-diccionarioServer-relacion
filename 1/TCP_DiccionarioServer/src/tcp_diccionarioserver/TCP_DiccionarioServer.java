package tcp_diccionarioserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author elian
 */
public class TCP_DiccionarioServer
{

    //<editor-fold desc="Atributos" defaultstate="collapsed">
    static final String STR_PAT_LETRAS = "[a-zA-Z]+";

    static final Pattern patConsulta = Pattern.compile("\\?(" + STR_PAT_LETRAS + ")");
    static final Pattern patAsignacion = Pattern.compile("!(" + STR_PAT_LETRAS + ")=(" + STR_PAT_LETRAS + ")");

    static final String STR_PROMPT = "dic> ";
    static final String STR_EXIT = "exit";
    static final String STR_QUIT = "quit";
    static final String STR_BYE = "bye";
    static final String STR_ERROR = "ERR: comando incorrecto";

    static final Hashtable<String, String> diccionario = new Hashtable<>();

    private final static String COD_TEXTO = "UTF-8";
    static final int NUM_PUERTO = 7890;
    //</editor-fold>

    public static void main(String[] args)
    {
        try ( var socketServidor = new ServerSocket(NUM_PUERTO))
        {
            System.out.printf("--- Creado socket de servidor en puerto %d. Esperando conexiones de clientes. ---\n", NUM_PUERTO);

            while (true)
            {   // Acepta una conexión de cliente tras otra
                try ( var socketComunicacion = socketServidor.accept())
                {
                    System.out.printf(
                            "Cliente conectado desde %s:%d.\n",
                            socketComunicacion.getInetAddress().getHostAddress(),
                            socketComunicacion.getPort()
                    );

                    try ( var isDeCliente = socketComunicacion.getInputStream();
                          var osACliente = socketComunicacion.getOutputStream();
                          var isrDeCliente = new InputStreamReader(isDeCliente, COD_TEXTO);
                          var brDeCliente = new BufferedReader(isrDeCliente);
                          var oswACliente = new OutputStreamWriter(osACliente, COD_TEXTO);
                          var bwACliente = new BufferedWriter(oswACliente))
                    {
                        String lineaRecibida;

                        while ((lineaRecibida = brDeCliente.readLine()) != null && lineaRecibida.length() > 0)
                        {
                            if (analizarLineaRecibida(lineaRecibida, bwACliente)) break;

                            bwACliente.newLine();
                            bwACliente.flush();
                        }
                    }
                }
                System.out.println("Cliente desconectado.");
            }
        }
        catch (IOException ex)
        {
            System.out.println("Excepción de E/S");
            System.exit(1);
        }
    }

    private static boolean analizarLineaRecibida(final String lineaRecibida, final BufferedWriter bwACliente)
    {
        Matcher m;

        try
        {
            // <editor-fold desc="Bye" defaultstate="collapsed">
            if (lineaRecibida.equals(STR_EXIT) || lineaRecibida.equals(STR_QUIT))
            {
                bwACliente.write(STR_BYE);
                return true;
            }
            // </editor-fold>

            // <editor-fold desc="Consultar palabra" defaultstate="collapsed">
            if ((m = patConsulta.matcher(lineaRecibida)).matches())
            {
                var palabra = m.group(1);
                if (diccionario.containsKey(palabra))
                {
                    //               dic> palabra:significado
                    bwACliente.write(STR_PROMPT + palabra + ":" + diccionario.get(palabra));
                }
                else
                {
                    diccionario.put(palabra, "");
                    bwACliente.write(STR_PROMPT + "La palabra ha sido añadida");
                }
            }
            // </editor-fold>

            // <editor-fold desc="Asignar significado" defaultstate="collapsed">
            else if ((m = patAsignacion.matcher(lineaRecibida)).matches())
            {
                var palabra = m.group(1);
                var significado = m.group(2);

                if (diccionario.containsKey(palabra))
                {
                    diccionario.put(palabra, significado);
                    bwACliente.write(STR_PROMPT + "OK, " + lineaRecibida);
                }
                else bwACliente.write("La palabra a la que pretende asignar un significado no exisite");
            }
            // </editor-fold>

            // <editor-fold desc="Comando incorrecto" defaultstate="collapsed">
            else bwACliente.write(STR_ERROR);
            // </editor-fold>
        }
        catch (IOException ex)
        {
            Logger.getLogger(TCP_DiccionarioServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
}
