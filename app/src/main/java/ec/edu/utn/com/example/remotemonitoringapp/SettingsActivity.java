package ec.edu.utn.com.example.remotemonitoringapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MonitoringPrefs";
    private static final String KEY_DAYS = "collection_days";
    private static final String KEY_START_HOUR = "start_hour";
    private static final String KEY_END_HOUR = "end_hour";

    private List<Chip> dayChips;
    private NumberPicker npStartHour, npEndHour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Habilitar Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_settings);

        // Aplicar insets para las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_container), (v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return WindowInsetsCompat.toWindowInsetsCompat(WindowInsetsCompat.CONSUMED.toWindowInsets());
        });

        initializeComponents();
        loadPreferences();
        setupButtons();
    }

    private void initializeComponents() {
        dayChips = new ArrayList<>();
        dayChips.add(findViewById(R.id.chip_sunday));
        dayChips.add(findViewById(R.id.chip_monday));
        dayChips.add(findViewById(R.id.chip_tuesday));
        dayChips.add(findViewById(R.id.chip_wednesday));
        dayChips.add(findViewById(R.id.chip_thursday));
        dayChips.add(findViewById(R.id.chip_friday));
        dayChips.add(findViewById(R.id.chip_saturday));

        npStartHour = findViewById(R.id.np_start_hour);
        npEndHour = findViewById(R.id.np_end_hour);

        npStartHour.setMinValue(0);
        npStartHour.setMaxValue(23);
        npEndHour.setMinValue(0);
        npEndHour.setMaxValue(23);
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String daysString = prefs.getString(KEY_DAYS, "1111111"); // Default to all days active

        for (int i = 0; i < dayChips.size() && i < daysString.length(); i++) {
            dayChips.get(i).setChecked(daysString.charAt(i) == '1');
        }

        int startHour = prefs.getInt(KEY_START_HOUR, 0);
        int endHour = prefs.getInt(KEY_END_HOUR, 23);
        npStartHour.setValue(startHour);
        npEndHour.setValue(endHour);
    }

    private void setupButtons() {
        Button btnSave = findViewById(R.id.btn_save);
        Button btnCancel = findViewById(R.id.btn_cancel);

        btnSave.setOnClickListener(v -> savePreferences());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StringBuilder daysString = new StringBuilder();
        boolean atLeastOneDaySelected = false;
        for (Chip chip : dayChips) {
            if (chip.isChecked()) {
                daysString.append("1");
                atLeastOneDaySelected = true;
            } else {
                daysString.append("0");
            }
        }

        int startHour = npStartHour.getValue();
        int endHour = npEndHour.getValue();

        if (startHour > endHour) {
            Toast.makeText(this, "La hora de inicio debe ser menor o igual a la hora de fin", Toast.LENGTH_LONG).show();
            return;
        }

        if (!atLeastOneDaySelected) {
            Toast.makeText(this, "Debe seleccionar al menos un día", Toast.LENGTH_LONG).show();
            return;
        }

        editor.putString(KEY_DAYS, daysString.toString());
        editor.putInt(KEY_START_HOUR, startHour);
        editor.putInt(KEY_END_HOUR, endHour);
        editor.apply();

        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
