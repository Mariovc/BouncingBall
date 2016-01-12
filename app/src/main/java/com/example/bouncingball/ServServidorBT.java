package com.example.bouncingball;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ServServidorBT {
    private static final String TAG = "ServServidorBT";
    private static final boolean DEBUG_MODE = true;
    private final Handler handler;
    private final Context context;
    private final BluetoothAdapter bAdapter;
    public static final String NOMBRE_SEGURO = "ServServidorBTSecure";
    public static UUID UUID_SEGURO = UUID.fromString("12345678-4321-4111-ADDA-345127542950");
    public static final int ESTADO_NINGUNO = 0;
    public static final int ESTADO_CONECTADO = 1;
    public static final int ESTADO_REALIZANDO_CONEXION = 2;
    public static final int ESTADO_ATENDIENDO_PETICIONES = 3;
    public static final int MSG_CAMBIO_ESTADO = 10;
    public static final int MSG_LEER = 11;
    public static final int MSG_ESCRIBIR = 12;
    public static final int MSG_ATENDER_PETICIONES = 13;
    public static final int MSG_ALERTA = 14;
    private int estado;
    private HiloServidor hiloServidor = null;
    private HiloConexion hiloConexion = null;

    public ServServidorBT(Context context, Handler handler, BluetoothAdapter adapter) {
        debug("ServServidorBT()", "Iniciando metodo");
        this.context = context;
        this.handler = handler;
        this.bAdapter = adapter;
        this.estado = ESTADO_NINGUNO;
    }

    private synchronized void setEstado(int estado) {
        this.estado = estado;
        handler.obtainMessage(MSG_CAMBIO_ESTADO, estado, -1).sendToTarget();
    }

    public synchronized int getEstado() {
        return estado;
    }

    public String getNombreDispositivo() {
        String nombre = "";
        if (estado == ESTADO_CONECTADO) {
            if (hiloConexion != null)
                nombre = hiloConexion.getName();
        }

        return nombre;
    }

    private class HiloConexion extends Thread {
        private final BluetoothSocket socket; // Socket
        private final InputStream inputStream; // Flujo de entrada
        private final OutputStream outputStream; // Flujo de salida

        public HiloConexion(BluetoothSocket socket) {
            this.socket = socket;
            setName(socket.getRemoteDevice().getName() + " [" +
                    socket.getRemoteDevice().getAddress() + "]");
            // Se usan variables temporales debido a que los atributos se
            // declaran como final no seria posible asignarles valor
            // posteriormente si fallara esta llamada
            InputStream tmpInputStream = null;
            OutputStream tmpOutputStream = null;
            // Obtenemos los flujos de entrada y salida del socket.
            try {
                tmpInputStream = socket.getInputStream();
                tmpOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "HiloConexion(): Error al obtener flujos de E/S", e);
            }
            inputStream = tmpInputStream;
            outputStream = tmpOutputStream;
        }

        // Metodo principal del hilo, encargado de realizar las lecturas
        public void run() {
            debug("HiloConexion.run()", "Iniciando metodo");
            byte[] buffer = new byte[1024];
            int bytes;
            setEstado(ESTADO_CONECTADO);
            // Mientras se mantenga la conexion el hilo se mantiene en espera
            // ocupada leyendo del flujo de entrada
            while (true) {
                try {
                    // Leemos del flujo de entrada del socket
                    bytes = inputStream.read(buffer);
                    // Enviamos la inf a la actividad a traves del handler.
                    // El metodo handleMessage se encarga de recibir el mensaje
                    // y mostrar los datos recibidos en el TextView
                    handler.obtainMessage(MSG_LEER, bytes, -1, buffer).sendToTarget();
                    sleep(500);
                } catch (IOException e) {
                    Log.e(TAG, "HiloConexion.run(): Error al realizar la lectura", e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void escribir(byte[] buffer) {
            try {
                // Escribimos en el flujo de salida del socket
                outputStream.write(buffer);
                // Enviamos la informacion a la actividad a traves del handler.
                // El metodo handleMessage sera el encargado de recibir el mensaje
                // y mostrar los datos enviados en el Toast
                handler.obtainMessage(MSG_ESCRIBIR, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "HiloConexion.escribir(): Error al realizar la escritura", e);
            }
        }

        public void cancelarConexion() {
            debug("HiloConexion.cancelarConexion()", "Iniciando metodo");
            try {
                // Forzamos el cierre del socket
                socket.close();
                // Cambiamos el estado del servicio
                setEstado(ESTADO_NINGUNO);
            } catch (IOException e) {
                Log.e(TAG, "HiloConexion.cerrarConexion(): Error al cerrar la conexion",
                        e);
            }
        }
    } // Fin HiloConexion


    private class HiloServidor extends Thread {
        private final BluetoothServerSocket serverSocket;

        public HiloServidor() {
            BluetoothServerSocket tmpServerSocket = null;
            // Creamos un socket para escuchar las peticiones de conexion
            try {
                tmpServerSocket =
                        bAdapter.listenUsingRfcommWithServiceRecord(NOMBRE_SEGURO, UUID_SEGURO);
            } catch (IOException e) {
                Log.e(TAG, "HiloServidor(): Error al abrir el socket servidor", e);
            }
            serverSocket = tmpServerSocket;
        }

        public void run() {
            debug("HiloServidor.run()", "Iniciando metodo");
            BluetoothSocket socket = null;
            setName("HiloServidor");
            setEstado(ESTADO_ATENDIENDO_PETICIONES);
            // El hilo se mantendra en estado de espera ocupada aceptando
            // conexiones entrantes siempre y cuando no exista una conexion
            // activa. En el momento en el que entre una nueva conexion,
            while (estado != ESTADO_CONECTADO) {
                try {
                    // Cuando un cliente solicite la conexion se abrira el socket.
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "HiloServidor.run(): Error al aceptar conexiones entrantes",
                            e);
                    break;
                }
                // Si el socket tiene valor sera porque un cliente ha solicitado
                // la conexion
                if (socket != null) {
                    // Realizamos un lock del objeto
                    synchronized (ServServidorBT.this) {
                        switch (estado) {
                            case ESTADO_ATENDIENDO_PETICIONES:
                            case ESTADO_REALIZANDO_CONEXION: {
                                // Estado esperado, se crea el hilo de conexión
                                // que recibira y enviara los mensajes
                                hiloConexion = new HiloConexion(socket);
                                hiloConexion.start();
                                break;
                            }
                            case ESTADO_NINGUNO:
                            case ESTADO_CONECTADO: {
                                // No preparado o conexion ya realizada. Se
                                // cierra el nuevo socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "HiloServidor.run(): socket.close(). Error al cerrar el socket.", e);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                    }
                }
            } // Fin while
        }

        public void cancelarConexion() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "HiloServidor.cancelarConexion(): Error al cerrar el socket",
                        e);
            }
        }
    }// Fin HiloServidor


    // Inicia el servicio, creando un HiloServidor que se dedicara a atender
    // las peticiones de conexion.
    public synchronized void iniciarServicio() {
        debug("iniciarServicio()", "Iniciando metodo");
        // Si existe una conexion previa, se cancela
        if (hiloConexion != null) {
            hiloConexion.cancelarConexion();
            hiloConexion = null;
        }
        // Arrancamos el hilo servidor para que empiece a recibir peticiones
        // de conexion
        if (hiloServidor == null) {
            hiloServidor = new HiloServidor();
            hiloServidor.start();
        }
        debug("iniciarServicio()", "Finalizando metodo");
    }

    public void finalizarServicio() {
        debug("finalizarServicio()", "Iniciando metodo");
        if (hiloConexion != null)
            hiloConexion.cancelarConexion();
        if (hiloServidor != null)
            hiloServidor.cancelarConexion();
        hiloConexion = null;
        hiloServidor = null;
        setEstado(ESTADO_NINGUNO);
    }

    public synchronized void realizarConexion(BluetoothSocket socket, BluetoothDevice
            dispositivo) {
        debug("realizarConexion()", "Iniciando metodo");
        hiloConexion = new HiloConexion(socket);
        hiloConexion.start();
    }

    // Sincroniza el objeto con el hilo HiloConexion e invoca a su metodo
// escribir() para enviar el mensaje como flujo de salida del socket.
    public int enviar(byte[] buffer) {
        debug("enviar()", "Iniciando metodo");
        HiloConexion tmpConexion;
        synchronized (this) {
            if (estado != ESTADO_CONECTADO)
                return -1;
            tmpConexion = hiloConexion;
        }
        tmpConexion.escribir(buffer);
        return buffer.length;
    }

    public void debug(String metodo, String msg) {
        if (DEBUG_MODE)
            Log.d(TAG, metodo + ": " + msg);
    }
}

