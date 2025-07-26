package ec.edu.utn.com.example.remotemonitoringapp;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.util.List;

public class DataListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DataAdapter dataAdapter;
    private DatabaseHelper dbHelper;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Habilitar Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_data_list);

        // Aplicar insets para las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.data_list_container), (v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return WindowInsetsCompat.toWindowInsetsCompat(WindowInsetsCompat.CONSUMED.toWindowInsets());
        });

        recyclerView = findViewById(R.id.recyclerView);
        btnBack = findViewById(R.id.btn_back);
        dbHelper = new DatabaseHelper(this);

        btnBack.setOnClickListener(v -> finish());

        List<JSONObject> dataList = dbHelper.obtenerTodosDatosSensorComoLista();
        dataAdapter = new DataAdapter(dataList);
        recyclerView.setAdapter(dataAdapter);
    }
}
