package ec.edu.utn.com.example.remotemonitoringapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String ETIQUETA = "ActividadPrincipal";
    private static final int CODIGO_SOLICITUD_PERMISO_UBICACION = 1001;
    private static final long INTERVALO_ACTUALIZACION_UBICACION = 30000;
    private static final String PREFS_NAME = "MonitoringPrefs";
    private static final String KEY_DAYS = "collection_days";
    private static final String KEY_START_HOUR = "start_hour";
    private static final String KEY_END_HOUR = "end_hour";
    private static final String KEY_IS_COLLECTING = "is_collecting";
    private static final String KEY_IS_SERVER_RUNNING = "is_server_running";

    private LocationManager gestorUbicacion;
    private DatabaseHelper ayudanteBaseDatos;
    private HttpServer servidorHttp;
    private Handler manejadorUbicacion;
    private Runnable tareaUbicacion;
    private SharedPreferences prefs;

    private CardView cardGpsData, cardApiServer;
    private TextView tvAuthor;

    private boolean estaRecolectando = false;
    private boolean servidorEnEjecucion = false;
    private boolean[] diasProgramacion = new boolean[7];
    private int horaInicio = 0, horaFin = 23;

    private ActivityResultLauncher<Intent> settingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Habilitar Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // Aplicar insets para las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content), (v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            // Consumimos solo el top y bottom, dejamos que los hijos manejen left/right si es necesario
            return insets;
        });

        inicializarComponentes();
        configurarInterfazUsuario();
        solicitarPermisoUbicacion();
        cargarPreferencias();
        actualizarEstadoTarjetas();

        settingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        cargarPreferencias();
                        if (estaRecolectando) {
                            detenerRecoleccionDatos();
                            iniciarRecoleccionDatos();
                        }
                    }
                });
    }

    private void inicializarComponentes() {
        gestorUbicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        ayudanteBaseDatos = new DatabaseHelper(this);
        servidorHttp = new HttpServer(this, ayudanteBaseDatos);
        manejadorUbicacion = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        cardGpsData = findViewById(R.id.card_gps_data);
        cardApiServer = findViewById(R.id.card_api_server);
        tvAuthor = findViewById(R.id.tv_author);
    }

    private void configurarInterfazUsuario() {
        tvAuthor.setText("Autor: Jordan Puruncajas");

        findViewById(R.id.card_device_status).setOnClickListener(v -> mostrarDialogoEstadoDispositivo());
        cardGpsData.setOnClickListener(v -> mostrarDialogoGps());
        cardApiServer.setOnClickListener(v -> mostrarDialogoServidorApi());
        findViewById(R.id.card_settings).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });
        findViewById(R.id.card_instructions).setOnClickListener(v -> mostrarDialogoInstrucciones());
        findViewById(R.id.card_authentication).setOnClickListener(v -> mostrarDialogoAutenticacion());
        findViewById(R.id.card_collection_data).setOnClickListener(v -> mostrarPantallaDatosRecoleccion());
    }

    private void actualizarEstadoTarjetas() {
        int activeColor = Color.parseColor("#C8E6C9"); // Verde claro
        int defaultColor = Color.WHITE;

        cardGpsData.setCardBackgroundColor(estaRecolectando ? activeColor : defaultColor);
        cardApiServer.setCardBackgroundColor(servidorEnEjecucion ? activeColor : defaultColor);
    }

    private void mostrarDialogoGps() {
        String lastLocation = prefs.getString("last_location", "No hay datos de ubicación.");
        GpsDataBottomSheetDialog dialog = GpsDataBottomSheetDialog.newInstance(lastLocation, estaRecolectando);
        dialog.setGpsDataListener(isChecked -> {
            if (isChecked) {
                iniciarRecoleccionDatos();
            } else {
                detenerRecoleccionDatos();
            }
        });
        dialog.show(getSupportFragmentManager(), "gps_data_dialog");
    }

    private void mostrarDialogoServidorApi() {
        String serverStatus = "Servidor: No está en ejecución";
        if (servidorEnEjecucion) {
            String ipAddress = obtenerDireccionIpLocal();
            serverStatus = String.format("Servidor: http://%s:8080\nEndpoints: /api/sensor_data, /api/device_status", ipAddress);
        }
        ApiServerBottomSheetDialog dialog = ApiServerBottomSheetDialog.newInstance(serverStatus, servidorEnEjecucion);
        dialog.setApiServerListener(new ApiServerBottomSheetDialog.ApiServerListener() {
            @Override
            public void onStartServer() {
                iniciarServidorHttp();
            }
            @Override
            public void onStopServer() {
                detenerServidorHttp();
            }
        });
        dialog.show(getSupportFragmentManager(), "api_server_dialog");
    }

    // Otros métodos de diálogo y lógicos (sin cambios)
    private void mostrarDialogoEstadoDispositivo() {
        DeviceInfoHelper deviceInfoHelper = new DeviceInfoHelper(this);
        String info = String.format(
                "Dispositivo: %s\nSO: Android %s\nBatería: %d%%\nRed: %s",
                deviceInfoHelper.obtenerModeloDispositivo(),
                deviceInfoHelper.obtenerVersionSO(),
                deviceInfoHelper.obtenerNivelBateria(),
                deviceInfoHelper.estaConectadoAInternet() ? "Conectado" : "Desconectado"
        );
        MessageBottomSheetDialog.newInstance("Estado del Dispositivo", info).show(getSupportFragmentManager(), "device_status_dialog");
    }

    private void mostrarDialogoInstrucciones() {
        String instructions = "1. Habilita la recolección de datos GPS.\n" +
                "2. Inicia el servidor HTTP para acceso remoto.\n" +
                "3. Usa una herramienta como Postman o cURL para consultar los endpoints.\n" +
                "4. Asegúrate de que el dispositivo esté conectado a una red.";
        MessageBottomSheetDialog.newInstance("Instrucciones", instructions).show(getSupportFragmentManager(), "instructions_dialog");
    }

    private void mostrarDialogoAutenticacion() {
        String authInfo = "Token Bearer: token_super_secret\n\n" +
                "Autenticación Básica:\n" +
                "Usuario: admin\n" +
                "Contraseña: adminXD";
        MessageBottomSheetDialog.newInstance("Autenticación API", authInfo).show(getSupportFragmentManager(), "auth_dialog");
    }

    private void mostrarPantallaDatosRecoleccion() {
        Intent intent = new Intent(this, DataListActivity.class);
        startActivity(intent);
    }

    private void cargarPreferencias() {
        String daysString = prefs.getString(KEY_DAYS, "1111111");
        for (int i = 0; i < diasProgramacion.length && i < daysString.length(); i++) {
            diasProgramacion[i] = daysString.charAt(i) == '1';
        }
        horaInicio = prefs.getInt(KEY_START_HOUR, 0);
        horaFin = prefs.getInt(KEY_END_HOUR, 23);
        estaRecolectando = prefs.getBoolean(KEY_IS_COLLECTING, false);
        servidorEnEjecucion = prefs.getBoolean(KEY_IS_SERVER_RUNNING, false);
    }

    private void solicitarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, CODIGO_SOLICITUD_PERMISO_UBICACION);
        }
    }

    private void iniciarRecoleccionDatos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            solicitarPermisoUbicacion();
            return;
        }
        estaRecolectando = true;
        prefs.edit().putBoolean(KEY_IS_COLLECTING, true).apply();
        actualizarEstadoTarjetas();
        Toast.makeText(this, "Recolección de datos iniciada.", Toast.LENGTH_SHORT).show();

        tareaUbicacion = new Runnable() {
            @Override
            public void run() {
                if (estaRecolectando) {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Guayaquil"));
                    int dia = cal.get(Calendar.DAY_OF_WEEK) - 1;
                    int hora = cal.get(Calendar.HOUR_OF_DAY);
                    if (diasProgramacion[dia] && hora >= horaInicio && hora <= horaFin) {
                        solicitarActualizacionUbicacion();
                    }
                    manejadorUbicacion.postDelayed(this, INTERVALO_ACTUALIZACION_UBICACION);
                }
            }
        };
        manejadorUbicacion.post(tareaUbicacion);
    }

    private void detenerRecoleccionDatos() {
        estaRecolectando = false;
        prefs.edit().putBoolean(KEY_IS_COLLECTING, false).apply();
        if (manejadorUbicacion != null && tareaUbicacion != null) {
            manejadorUbicacion.removeCallbacks(tareaUbicacion);
        }
        actualizarEstadoTarjetas();
        Toast.makeText(this, "Recolección de datos detenida.", Toast.LENGTH_SHORT).show();
    }

    private void solicitarActualizacionUbicacion() {
        try {
            if (gestorUbicacion.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gestorUbicacion.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, Looper.getMainLooper());
            } else if (gestorUbicacion.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                gestorUbicacion.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, Looper.getMainLooper());
            }
        } catch (SecurityException e) {
            Log.e(ETIQUETA, "Excepción de seguridad al solicitar ubicación", e);
        }
    }

    @Override
    public void onLocationChanged(Location ubicacion) {
        if (ubicacion != null) {
            String idDispositivo = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            ayudanteBaseDatos.insertarDatosSensorConTiempoEcuador(ubicacion.getLatitude(), ubicacion.getLongitude(), idDispositivo);
            String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String lastLocation = String.format(Locale.US, "Lat: %.6f\nLng: %.6f\nHora: %s",
                    ubicacion.getLatitude(), ubicacion.getLongitude(), formattedTime);
            prefs.edit().putString("last_location", lastLocation).apply();
        }
    }

    private void iniciarServidorHttp() {
        if (servidorEnEjecucion) return;
        try {
            servidorHttp.iniciar();
            servidorEnEjecucion = true;
            prefs.edit().putBoolean(KEY_IS_SERVER_RUNNING, true).apply();
            actualizarEstadoTarjetas();
            Toast.makeText(this, "Servidor HTTP iniciado.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(ETIQUETA, "Error al iniciar el servidor", e);
        }
    }

    private void detenerServidorHttp() {
        if (!servidorEnEjecucion) return;
        servidorHttp.detener();
        servidorEnEjecucion = false;
        prefs.edit().putBoolean(KEY_IS_SERVER_RUNNING, false).apply();
        actualizarEstadoTarjetas();
        Toast.makeText(this, "Servidor HTTP detenido.", Toast.LENGTH_SHORT).show();
    }

    private String obtenerDireccionIpLocal() {
        try {
            WifiManager gestorWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int direccionIp = gestorWifi.getConnectionInfo().getIpAddress();
            return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                    (direccionIp & 0xff), (direccionIp >> 8 & 0xff), (direccionIp >> 16 & 0xff), (direccionIp >> 24 & 0xff));
        } catch (Exception e) {
            return "No disponible";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerRecoleccionDatos();
        if (servidorHttp != null && servidorHttp.estaEjecutando()) {
            detenerServidorHttp();
        }
        if (ayudanteBaseDatos != null) ayudanteBaseDatos.close();
    }
}
