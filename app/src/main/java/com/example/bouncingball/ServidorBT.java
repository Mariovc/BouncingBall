package com.example.bouncingball;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class ServidorBT extends Activity implements View.OnClickListener {

    private static final String TAG = "ServidorBT";
    // Declaramos una constante para lanzar los Intent de activacion de
    // Bluetooth
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String ALERTA = "alerta";
    // Declaramos una variable privada para cada control de la actividad
    private Button btnEnviar;
    private Button btnBluetooth;
    private Button btnSalir;
    private EditText txtMensaje;
    private TextView tvMensaje;
    private TextView tvConexion;
    private BouncingBallView bouncingBallView;
    private BluetoothAdapter bAdapter;   //Adapter para uso del Bluetooth
    private ServServidorBT servicio;  //Servicio de mensajes de Bluetooth
    private BluetoothDevice ultimoDispositivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servidor_bt);
        // Invocamos el metodo de configuracion de nuestros controles
        configurarControles();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bouncing_ball, menu);
        return true;
    }

    private void configurarControles() {
        // Referenciamos los controles y registramos los listeners
        referenciarControles();
        registrarEventosControles();
        // Por defecto, desactivamos los botones que no puedan utilizarse
        btnEnviar.setEnabled(false);
        // Configuramos el adaptador bluetooth y nos suscribimos a sus eventos
        configurarAdaptadorBluetooth();
        registrarEventosBluetooth();
    }

    private void referenciarControles() {
        // Referenciamos los elementos de interfaz
        btnEnviar = (Button) findViewById(R.id.btnEnviar);
        btnBluetooth = (Button) findViewById(R.id.btnBluetooth);
        btnSalir = (Button) findViewById(R.id.btnSalir);
        txtMensaje = (EditText) findViewById(R.id.txtMensaje);
        tvMensaje = (TextView) findViewById(R.id.tvMensaje);
        tvConexion = (TextView) findViewById(R.id.tvConexion);
        bouncingBallView = (BouncingBallView) findViewById(R.id.bouncing_ball_view);
    }

    private void configurarAdaptadorBluetooth() {
        // Obtenemos el adaptador Bluetooth. Si es NULL, significara que el
        // dispositivo no posee Bluetooth, por lo que deshabilitamos el boton
        // encargado de activar/desactivar esta caracteristica.
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bAdapter == null) {
            btnBluetooth.setEnabled(false);
            return;
        }

        // Comprobamos si el Bluetooth esta activo y cambiamos el texto de
        // los botones dependiendo del estado. Tambien activamos o
        // desactivamos los botones asociados a la conexion
        if (bAdapter.isEnabled()) {
            btnBluetooth.setText(R.string.DesactivarBluetooth);
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

                        ((Button) findViewById(R.id.btnBluetooth)).setText(R.string.ActivarBluetooth);
                        break;
                    }
                    // Encendido
                    case BluetoothAdapter.STATE_ON: {
                        Log.v(TAG, "onReceive: Encendiendo");

                        ((Button) findViewById(R.id.btnBluetooth)).setText(R.string.DesactivarBluetooth);
                        Intent discoverableIntent = new
                                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                        startActivity(discoverableIntent);
                        break;
                    }
                    default:
                        break;
                } // Fin switch
            } // Fin if
        } // Fin onReceive
    };

    private void registrarEventosBluetooth() {
        // Registramos el BroadcastReceiver que instanciamos previamente para
        // detectar los distintos eventos que queremos recibir
        IntentFilter filtro = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
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
                if (bAdapter.isEnabled()) {
                    if (servicio != null)
                        servicio.finalizarServicio();
                    bAdapter.disable();
                } else {
                    // Lanzamos el Intent que mostrara la interfaz de
                    // activacion del Bluetooth. La respuesta de este
                    // Intent se manejara en el metodo onActivityResult
                    Intent enableBtIntent = new
                            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
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
        btnSalir.setOnClickListener(this);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: {
                Log.v(TAG, "onActivityResult: REQUEST_ENABLE_BT");
                if (resultCode == RESULT_OK) {
                    btnBluetooth.setText(R.string.DesactivarBluetooth);
                    if (servicio != null) {
                        servicio.finalizarServicio();
                        servicio.iniciarServicio();
                    } else
                        servicio = new ServServidorBT(this, handler, bAdapter);
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
            if (servicio.getEstado() == ServServidorBT.ESTADO_NINGUNO) {
                servicio.iniciarServicio();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @SuppressWarnings("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] buffer = null;
            String mensaje = null;
            // Atendemos al tipo de mensaje
            switch (msg.what) {
                // Mensaje de lectura: se mostrara en el TextView
                case ServServidorBT.MSG_LEER: {
                    buffer = (byte[]) msg.obj;
                    mensaje = new String(buffer, 0, msg.arg1);
                    tvMensaje.setText(mensaje);
                    int number = Integer.parseInt(mensaje);
                    float velocity = getVelocity(number);
                    bouncingBallView.setxDirection((int) (bouncingBallView.getxDirection() * velocity));
                    bouncingBallView.setyDirection((int) (bouncingBallView.getyDirection() * velocity));
                    BouncingBallView.setBallColor(getBallColor(number));
                    break;
                }
                // Mensaje de escritura: se mostrara en el Toast
                case ServServidorBT.MSG_ESCRIBIR: {
                    buffer = (byte[]) msg.obj;
                    mensaje = new String(buffer);
                    mensaje = getString(R.string.EnviandoMensaje) + ": " + mensaje;
                    Toast.makeText(getApplicationContext(), mensaje,
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                // Mensaje de cambio de estado
                case ServServidorBT.MSG_CAMBIO_ESTADO: {
                    switch (msg.arg1) {
                        case ServServidorBT.ESTADO_ATENDIENDO_PETICIONES:
                            break;
                        // CONECTADO: Se muestra el dispositivo al que se ha
                        // conectado y se activa el boton de enviar
                        case ServServidorBT.ESTADO_CONECTADO: {
                            mensaje = getString(R.string.ConexionActual) + " " +
                                    servicio.getNombreDispositivo();
                            Toast.makeText(getApplicationContext(), mensaje,
                                    Toast.LENGTH_SHORT).show();
                            tvConexion.setText(mensaje);
                            btnEnviar.setEnabled(true);
                            break;
                        }
                        // NINGUNO: Mensaje por defecto. Desactivacion del
                        // boton de enviar
                        case ServServidorBT.ESTADO_NINGUNO: {
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
                case ServServidorBT.MSG_ALERTA: {
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

    private float getVelocity(int number) {
        float newVelocity = (float) number / 4.5f;
        return newVelocity;
    }

    private int getBallColor(int number) {
        Random rnd = new Random();
        int red = 256 / number;
        int green = rnd.nextInt(256);
        int blue = rnd.nextInt(256);
        return Color.rgb(red, green, blue);
    }
}

