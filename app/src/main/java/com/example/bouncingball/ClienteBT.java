package com.example.bouncingball;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class ClienteBT extends Activity implements View.OnClickListener {
    private static final String TAG = "ClienteBT";
    // Declaramos una constante para lanzar los Intent de activacion
// de Bluetooth
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String ALERTA = "alerta";
    // Declaramos una variable privada para cada control de la actividad
    private Button btnEnviar;
    private Button btnBluetooth;
    private Button btnBuscarDispositivo;
    private Button btnConectarDispositivo;
    private Button btnSalir;
    private EditText txtMensaje;
    private TextView tvMensaje;
    private TextView tvConexion;
    private ListView lvDispositivos;
    private BluetoothAdapter adaptadorBT;
    private ArrayList<BluetoothDevice> arrayDisp;
    private ArrayAdapter arrayAdaptador;
    private ServClienteBT servicio;
    private BluetoothDevice ultimoDispositivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente_bt);
        // Invocamos el metodo de configuracion de nuestros controles
        configurarControles();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bouncing_ball, menu);
        return true;
    }

    private void configurarControles() {
        // Instanciamos el array de dispositivos
        arrayDisp = new ArrayList<BluetoothDevice>();
        // Referenciamos los controles y registramos los listeners
        referenciarControles();
        registrarEventosControles();
        // Por defecto, desactivamos los botones que no puedan utilizarse
        btnEnviar.setEnabled(false);
        btnBuscarDispositivo.setEnabled(false);
        btnConectarDispositivo.setEnabled(false);
        // Configuramos el adaptador bluetooth y nos suscribimos a sus eventos
        configurarAdaptadorBluetooth();
        registrarEventosBluetooth();
    }

    private void referenciarControles() {
        // Referenciamos los elementos de interfaz
        btnEnviar = (Button) findViewById(R.id.btnEnviar);
        btnBluetooth = (Button) findViewById(R.id.btnBluetooth);
        btnBuscarDispositivo = (Button) findViewById(R.id.btnBuscarDispositivo);
        btnConectarDispositivo = (Button) findViewById(R.id.btnConectarDispositivo);
        btnSalir = (Button) findViewById(R.id.btnSalir);
        txtMensaje = (EditText) findViewById(R.id.txtMensaje);
        tvMensaje = (TextView) findViewById(R.id.tvMensaje);
        tvConexion = (TextView) findViewById(R.id.tvConexion);
        lvDispositivos = (ListView) findViewById(R.id.lvDispositivos);
    }

    private void configurarAdaptadorBluetooth() {
        // Obtenemos el adaptador Bluetooth. Si es NULL, significara que el
        // dispositivo no posee Bluetooth, por lo que deshabilitamos el boton
        // encargado de activar/desactivar esta caracteristica.
        adaptadorBT = BluetoothAdapter.getDefaultAdapter();
        if (adaptadorBT == null) {
            btnBluetooth.setEnabled(false);
            return;
        }
        // Comprobamos si el Bluetooth esta activo y cambiamos el texto de
        // los botones dependiendo del estado. Tambien activamos o
        // desactivamos los botones asociados a la conexion
        if (adaptadorBT.isEnabled()) {
            btnBluetooth.setText(R.string.DesactivarBluetooth);
            btnBuscarDispositivo.setEnabled(true);
            btnConectarDispositivo.setEnabled(true);
        } else {
            btnBluetooth.setText(R.string.ActivarBluetooth);
        }
    }

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // BluetoothAdapter.ACTION_STATE_CHANGED
            // Codigo que se ejecutara cuando el Bluetooth cambie su estado.
            // Manejaremos los siguientes estados:
            //    - STATE_OFF: El Bluetooth se desactiva
            //    - STATE ON: El Bluetooth se activa
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (estado) {
                    // Apagado
                    case BluetoothAdapter.STATE_OFF: {
                        Log.v(TAG, "onReceive: Apagando");
                        ((Button)
                                findViewById(R.id.btnBluetooth)).setText(R.string.ActivarBluetooth);
                        ((Button)
                                findViewById(R.id.btnBuscarDispositivo)).setEnabled(false);
                        ((Button)
                                findViewById(R.id.btnConectarDispositivo)).setEnabled(false);
                        break;
                    }

                    // Encendido
                    case BluetoothAdapter.STATE_ON: {
                        Log.v(TAG, "onReceive: Encendiendo");
                        ((Button)
                                findViewById(R.id.btnBluetooth)).setText(R.string.DesactivarBluetooth);
                        ((Button)
                                findViewById(R.id.btnBuscarDispositivo)).setEnabled(true);
                        ((Button)
                                findViewById(R.id.btnConectarDispositivo)).setEnabled(true);
                        break;
                    }
                    default:
                        break;
                } // Fin switch
            } // Fin if
            // BluetoothDevice.ACTION_FOUND
            // Cada vez que se descubra un nuevo dispositivo por Bluetooth,
            // se ejecutara este fragmento de codigo
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (arrayDisp == null)
                    arrayDisp = new ArrayList<BluetoothDevice>();
                BluetoothDevice dispositivo =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                arrayDisp.add(dispositivo);
                String descripcionDispositivo = dispositivo.getName() + " [" +
                        dispositivo.getAddress() + "]";
                Toast.makeText(getBaseContext(), getString(R.string.DetectadoDispositivo)
                        + ": " + descripcionDispositivo, Toast.LENGTH_SHORT).show();
                Log.v(TAG, "ACTION_FOUND: Dispositivo encontrado: " +
                        descripcionDispositivo);
            }
            // BluetoothAdapter.ACTION_DISCOVERY_FINISHED
            // Codigo que se ejecutara cuando el Bluetooth finalice la
            // busqueda de dispositivos.
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Instanciamos un nuevo adapter para el ListView
                arrayAdaptador = new ArrayAdapterBT(getBaseContext(),
                        android.R.layout.simple_list_item_2, arrayDisp);
                lvDispositivos.setAdapter(arrayAdaptador);
                Toast.makeText(getBaseContext(), R.string.FinBusqueda,
                        Toast.LENGTH_SHORT).show();
            }
        } // Fin onReceive
    }; // Fin BroadcastReceiver

    private void registrarEventosBluetooth() {
        // Registramos el BroadcastReceiver que instanciamos previamente para
        // detectar los distintos eventos que queremos recibir
        IntentFilter filtro = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filtro.addAction(BluetoothDevice.ACTION_FOUND);
        filtro.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(bReceiver, filtro);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // Codigo ejecutado al pulsar el Button que se va a encargar de
            // enviar los datos al otro dispositivo.
            case R.id.btnEnviar: {
                if (servicio != null) {
                    servicio.enviar(txtMensaje.getText().toString().getBytes());
                    txtMensaje.setText("");
                }
                break;
            }
            // Codigo ejecutado al pulsar el Button que se va a encargar de
            // activar y desactivar el Bluetooth.
            case R.id.btnBluetooth: {
                if (adaptadorBT.isEnabled()) {
                    if (servicio != null)
                        servicio.finalizarServicio();
                    adaptadorBT.disable();
                } else {
                    // Lanzamos el Intent que mostrara la interfaz de
                    // activacion del Bluetooth. La respuesta de este Intent
                    // se manejara en el metodo onActivityResult
                    Intent enableBtIntent = new
                            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                break;
            }
            // Codigo ejecutado al pulsar el Button que se va a encargar de
            // descubrir nuevos dispositivos
            case R.id.btnBuscarDispositivo: {
                arrayDisp.clear();
                // Comprobamos si existe un descubrimiento en curso. En caso
                // afirmativo, se cancela.
                if (adaptadorBT.isDiscovering())
                    adaptadorBT.cancelDiscovery();
                // Iniciamos la busqueda de dispositivos
                if (adaptadorBT.startDiscovery())
                    // Mostramos el mensaje de que el proceso ha comenzado
                    Toast.makeText(this, R.string.IniciandoDescubrimiento,
                            Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, R.string.ErrorIniciandoDescubrimiento,
                            Toast.LENGTH_SHORT).show();
                break;
            }
            // Codigo ejecutado al pulsar el Button que se encarga de mostrar
            // todos los dispositivos previamente enlazados al dispositivo
            // actual.
            case R.id.btnConectarDispositivo: {
                Set<BluetoothDevice> dispositivosEnlazados =
                        adaptadorBT.getBondedDevices();
                // Instanciamos un nuevo adapter para el ListView
                arrayDisp = new ArrayList<BluetoothDevice>(dispositivosEnlazados);
                arrayAdaptador = new ArrayAdapterBT(getBaseContext(),
                        android.R.layout.simple_list_item_1, arrayDisp);
                lvDispositivos.setAdapter(arrayAdaptador);
                Toast.makeText(getBaseContext(), R.string.FinBusqueda,
                        Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.btnSalir: {
                if (servicio != null)
                    servicio.finalizarServicio();
                finish();
                System.exit(0);
                break;
            }
            default:
                break;
        }
    }

    private void registrarEventosControles() {
        // Asignamos los handlers de los botones
        btnEnviar.setOnClickListener(this);
        btnBluetooth.setOnClickListener(this);
        btnBuscarDispositivo.setOnClickListener(this);
        btnConectarDispositivo.setOnClickListener(this);
        btnSalir.setOnClickListener(this);
        // Configuramos la lista de dispositivos para que cuando seleccionemos
        // uno de sus elementos realice la conexion al dispositivo
        configurarListaDispositivos();
    }

    private void configurarListaDispositivos() {
        lvDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapter, View view, int position, long
                    arg) {
                // El ListView tiene un adaptador de tipo
                // BluetoothDeviceArrayAdapter. Invocamos el metodo getItem()
                // del adaptador para recibir el dispositivo bluetooth y
                // realizar la conexion.
                BluetoothDevice dispositivo = (BluetoothDevice)
                        lvDispositivos.getAdapter().getItem(position);
                AlertDialog dialog = crearDialogoConexion(getString(R.string.Conectar),
                        getString(R.string.MsgConfirmarConexion) + " " +
                                dispositivo.getName() + "?",
                        dispositivo.getAddress());
                dialog.show();
            }
        });
    }

    private AlertDialog crearDialogoConexion(String titulo, String mensaje, final String
            direccion) {
        // Instanciamos un nuevo AlertDialog Builder y le asociamos titulo y
        // mensaje
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(titulo);
        alertDialogBuilder.setMessage(mensaje);
        // Creamos un nuevo OnClickListener para que el boton OK realice la
        // conexion
        DialogInterface.OnClickListener listenerOk = new
                DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        conectarDispositivo(direccion);
                    }
                };
        // Creamos un nuevo OnClickListener para el boton Cancelar
        DialogInterface.OnClickListener listenerCancelar = new
                DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                };
        // Asignamos los botones positivo y negativo a sus respectivos
        // listeners
        alertDialogBuilder.setPositiveButton(R.string.Conectar, listenerOk);
        alertDialogBuilder.setNegativeButton(R.string.Cancelar, listenerCancelar);
        return alertDialogBuilder.create();
    }

    public void conectarDispositivo(String direccion) {
        Toast.makeText(this, "Conectando a " + direccion, Toast.LENGTH_LONG).show();
        if (servicio != null) {
            BluetoothDevice dispositivoRemoto = adaptadorBT.getRemoteDevice(direccion);
            servicio.solicitarConexion(dispositivoRemoto);
            this.ultimoDispositivo = dispositivoRemoto;
        }
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] buffer = null;
            String mensaje = null;
            // Atendemos al tipo de mensaje
            switch (msg.what) {
                // Mensaje de lectura: se mostrara en el TextView
                case ServClienteBT.MSG_LEER: {
                    buffer = (byte[]) msg.obj;
                    mensaje = new String(buffer, 0, msg.arg1);
                    tvMensaje.setText(mensaje);
                    break;
                }
                // Mensaje de escritura: se mostrara en el Toast
                case ServClienteBT.MSG_ESCRIBIR: {
                    buffer = (byte[]) msg.obj;
                    mensaje = new String(buffer);
                    mensaje = getString(R.string.EnviandoMensaje) + ": " + mensaje;
                    Toast.makeText(getApplicationContext(), mensaje,
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                // Mensaje de cambio de estado
                case ServClienteBT.MSG_CAMBIO_ESTADO: {
                    switch (msg.arg1) {
                        case ServClienteBT.ESTADO_ATENDIENDO_PETICIONES:
                            break;
                        // CONECTADO: Se muestra el dispositivo al que se ha conectado y
                        // se activa el boton de enviar
                        case ServClienteBT.ESTADO_CONECTADO: {
                            mensaje = getString(R.string.ConexionActual) + " " +
                                    servicio.getNombreDispositivo();
                            Toast.makeText(getApplicationContext(), mensaje,
                                    Toast.LENGTH_SHORT).show();
                            tvConexion.setText(mensaje);
                            btnEnviar.setEnabled(true);
                            break;
                        }
                        // REALIZANDO CONEXION: Se muestra el dispositivo al
                        // que se esta conectando
                        case ServClienteBT.ESTADO_REALIZANDO_CONEXION: {
                            mensaje = getString(R.string.ConectandoA) + " " +
                                    ultimoDispositivo.getName() + " [" + ultimoDispositivo.getAddress() + "]";
                            Toast.makeText(getApplicationContext(), mensaje,
                                    Toast.LENGTH_SHORT).show();
                            btnEnviar.setEnabled(false);
                            break;
                        }
                        // NINGUNO: Mensaje por defecto. Desactivacion del
                        // boton de enviar
                        case ServClienteBT.ESTADO_NINGUNO: {
                            mensaje = getString(R.string.SinConexion);
                            Toast.makeText(getApplicationContext(), mensaje,
                                    Toast.LENGTH_SHORT).show();
                            tvConexion.setText(mensaje);
                            btnEnviar.setEnabled(false);
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }
                // Mensaje de alerta: se mostrara en el Toast
                case ServClienteBT.MSG_ALERTA: {
                    mensaje = msg.getData().getString(ALERTA);
                    Toast.makeText(getApplicationContext(), mensaje,
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                default:
                    break;
            }
        }
    }; // Fin Handler

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: {
                Log.v(TAG, "onActivityResult: REQUEST_ENABLE_BT");
                if (resultCode == RESULT_OK) {
                    btnBluetooth.setText(R.string.DesactivarBluetooth);
                    if (servicio != null) {
                        servicio.finalizarServicio();
                    } else
                        servicio = new ServClienteBT(this, handler, adaptadorBT);
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(bReceiver);
        if (servicio != null)
            servicio.finalizarServicio();
    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if (servicio != null) {
            if (servicio.getEstado() == ServClienteBT.ESTADO_NINGUNO) {
                super.onResume();
            }
        }
    }
    @Override
    public synchronized void onPause() {
        super.onPause();
    }
}

