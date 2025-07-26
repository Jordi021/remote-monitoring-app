package ec.edu.utn.com.example.remotemonitoringapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ec.edu.utn.com.example.remotemonitoringapp.R;
import java.util.TimeZone;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.DataViewHolder> {

    private List<JSONObject> dataList;

    public DataAdapter(List<JSONObject> dataList) {
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public DataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_data, parent, false);
        return new DataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataViewHolder holder, int position) {
        JSONObject data = dataList.get(position);
        try {
            double lat = data.getDouble("latitud");
            double lon = data.getDouble("longitud");
            long timestamp = data.getLong("marca_tiempo");
            String deviceId = data.getString("id_dispositivo");

            holder.tvCoordinates.setText(String.format(Locale.US, "Lat: %.6f, Lng: %.6f", lat, lon));
            holder.tvDeviceId.setText("Device ID: " + deviceId);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("America/Guayaquil"));
            holder.tvTimestamp.setText("Timestamp: " + sdf.format(new Date(timestamp)));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class DataViewHolder extends RecyclerView.ViewHolder {
        TextView tvCoordinates, tvTimestamp, tvDeviceId;

        public DataViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCoordinates = itemView.findViewById(R.id.tv_coordinates);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvDeviceId = itemView.findViewById(R.id.tv_device_id);
        }
    }
}