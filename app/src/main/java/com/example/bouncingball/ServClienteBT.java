package com.example.bouncingball;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ServClienteBT {
    public static final String NOMBRE_SEGURO = "ServClienteBTSecure";
    public static final int ESTADO_NINGUNO = 0;
    public static final int ESTADO_CONECTADO = 1;
    public static final int ESTADO_REALIZANDO_CONEXION = 2;
    public static final int ESTADO_ATENDIENDO_PETICIONES = 3;
    public static final int MSG_CAMBIO_ESTADO = 10;
    public static final int MSG_LEER = 11;
    public static final int MSG_ESCRIBIR = 12;
    public static final int MSG_ATENDER_PETICIONES = 13;
    public static final int MSG_ALERTA = 14;
    private static final String TAG = "ServClienteBT";
    private static final boolean DEBUG_MODE = true;
    public static UUID UUID_SEGURO = UUID.fromString("12345678-4321-4111-ADDA-345127542950");
    private final Handler handler;
    private final Context context;
    private final BluetoothAdapter bAdapter;
    private int estado;
    private HiloCliente hiloCliente = null;
    private HiloConexion hiloConexion = null;

    public ServClienteBT(Context context, Handler handler, BluetoothAdapter adapter) {
        debug("BluetoothService()", "Iniciando metodo");
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

    // Hilo encargado de mantener la conexion y realizar las lecturas y
// escrituras de los mensajes intercambiados entre dispositivos.
    private class HiloConexion extends Thread {
        private final BluetoothSocket socket; // Socket
        private final InputStream inputStream; // Flujo de entrada (lecturas)
        private final OutputStream outputStream; // Flujo de salida (escrituras)

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
                    // Enviamos la informacion a la actividad con el handler.
                    // El metodo handleMessage sera el encargado de recibir el
                    // mensaje y mostrar los datos recibidos en el TextView
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
                // Enviamos la informacion a la actividad con el handler.
                // El metodo handleMessage sera el encargado de recibir el
                // mensaje y mostrar los datos enviados en el Toast
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

    private class HiloCliente extends Thread {
        private final BluetoothDevice dispositivo;
        private final BluetoothSocket socket;

        public HiloCliente(BluetoothDevice dispositivo) {
            BluetoothSocket tmpSocket = null;
            this.dispositivo = dispositivo;
            // Obtenemos un socket para el dispositivo con el que se quiere
            // conectar
            try {
                tmpSocket = dispositivo.createRfcommSocketToServiceRecord(UUID_SEGURO);
            } catch (IOException e) {
                Log.e(TAG, "HiloCliente.HiloCliente(): Error al abrir el socket", e);
            }
            socket = tmpSocket;
        }

        public void run() {
            setName("HiloCliente");
            if (bAdapter.isDiscovering())
                bAdapter.cancelDiscovery();

            try {
                socket.connect();
                setEstado(ESTADO_REALIZANDO_CONEXION);
            } catch (IOException e) {
                Log.e(TAG, "HiloCliente.run(): socket.connect(): Error realizando la conexion", e);
                try {
                    socket.close();
                } catch (IOException inner) {
                    Log.e(TAG, "HiloCliente.run(): Error cerrando el socket", inner);
                }
                setEstado(ESTADO_NINGUNO);
            }

            // Reiniciamos el hilo cliente, ya que no lo necesitaremos mas
            synchronized (ServClienteBT.this) {
                hiloCliente = null;
            }

            // Realizamos la conexion
            hiloConexion = new HiloConexion(socket);
            hiloConexion.start();
        }

        public void cancelarConexion() {
            debug("cancelarConexion()", "Iniciando metodo");
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "HiloCliente.cancelarConexion(): Error al cerrar el socket",
                        e);
            }
            setEstado(ESTADO_NINGUNO);
        }
    } // Fin HiloCliente

    // Instancia un hilo conector
    public synchronized void solicitarConexion(BluetoothDevice dispositivo) {
        debug("solicitarConexion()", "Iniciando metodo");
        // Comprobamos si existia un intento de conexion en curso.
        // Si es el caso, se cancela y se vuelve a iniciar el proceso
        if(estado == ESTADO_REALIZANDO_CONEXION) {
            if(hiloCliente != null) {
                hiloCliente.cancelarConexion();
                hiloCliente = null;
            }
        }
        // Si existia una conexion abierta, se cierra y se inicia una nueva
        if(hiloConexion != null) {
            hiloConexion.cancelarConexion();
            hiloConexion = null;
        }
        // Se instancia un nuevo hilo conector, encargado de solicitar una
        // conexion al servidor, que sera la otra parte.
        hiloCliente = new HiloCliente(dispositivo);
        hiloCliente.start();
        setEstado(ESTADO_REALIZANDO_CONEXION);
    }
    public synchronized void realizarConexion(BluetoothSocket socket, BluetoothDevice
            dispositivo) {
        debug("realizarConexion()", "Iniciando metodo");
        hiloConexion = new HiloConexion(socket);
        hiloConexion.start();
    }

    public int enviar(byte[] buffer) {
        debug("enviar()", "Iniciando metodo");
        HiloConexion tmpConexion;
        synchronized(this) {
            if(estado != ESTADO_CONECTADO)
                return -1;
            tmpConexion = hiloConexion;
        }
        tmpConexion.escribir(buffer);
        return buffer.length;
    }

    public void finalizarServicio() {
        debug("finalizarServicio()", "Iniciando metodo");
        if(hiloCliente != null)
            hiloCliente.cancelarConexion();
        if(hiloConexion != null)
            hiloConexion.cancelarConexion();
        hiloCliente = null;
        hiloConexion = null;
        setEstado(ESTADO_NINGUNO);
    }
    public void debug(String metodo, String msg) {
        if(DEBUG_MODE)
            Log.d(TAG, metodo + ": " + msg);
    }
}

