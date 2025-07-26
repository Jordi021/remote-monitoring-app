package ec.edu.utn.com.example.remotemonitoringapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ApiServerBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_SERVER_STATUS = "server_status";
    private static final String ARG_IS_RUNNING = "is_running";

    private ApiServerListener listener;

    public interface ApiServerListener {
        void onStartServer();
        void onStopServer();
    }

    public void setApiServerListener(ApiServerListener listener) {
        this.listener = listener;
    }

    public static ApiServerBottomSheetDialog newInstance(String serverStatus, boolean isRunning) {
        ApiServerBottomSheetDialog fragment = new ApiServerBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_STATUS, serverStatus);
        args.putBoolean(ARG_IS_RUNNING, isRunning);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_api_server, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvServerStatus = view.findViewById(R.id.tv_server_status_modal);
        Button btnStart = view.findViewById(R.id.btn_start_server_modal);
        Button btnStop = view.findViewById(R.id.btn_stop_server_modal);

        if (getArguments() != null) {
            tvServerStatus.setText(getArguments().getString(ARG_SERVER_STATUS));
            boolean isRunning = getArguments().getBoolean(ARG_IS_RUNNING);
            btnStart.setEnabled(!isRunning);
            btnStop.setEnabled(isRunning);
        }

        btnStart.setOnClickListener(v -> {
            if (listener != null) listener.onStartServer();
            dismiss();
        });

        btnStop.setOnClickListener(v -> {
            if (listener != null) listener.onStopServer();
            dismiss();
        });
    }
}