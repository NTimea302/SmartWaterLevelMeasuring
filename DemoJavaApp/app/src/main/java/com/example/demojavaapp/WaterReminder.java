package com.example.demojavaapp;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.demojavaapp.databinding.FragmentFirstBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class WaterReminder extends Fragment {
    int totCups = 0;
    boolean empty = false;
    private FragmentFirstBinding binding;

    // Bluetooth variables
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    private InputStream inputStream = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID
    private static final String DEVICE_ADDRESS = "98:D3:31:F6:8C:F6";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1; // You can use any integer value
    private boolean isBluetoothConnected = false;

    // Handler to update UI with received Bluetooth data
    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // Handle the received data here
            String receivedData = (String) msg.obj;
            updateUI(receivedData);
            return true;
        }
    });

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle("Water Reminder");
        int totalCups = 0;
        binding.idTextTotalCups.setText(String.valueOf(totalCups));
        binding.idTextTotalCups.setInputType(InputType.TYPE_NULL);
        binding.idTextTotalCups.setFocusable(false);
        binding.idTextTotalCups.setFocusableInTouchMode(false);

        // Bluetooth setup
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if Bluetooth permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED) {
            // Request Bluetooth permission if not granted
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            return;
        }

        // Replace DEVICE_ADDRESS with your Arduino's Bluetooth address
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            isBluetoothConnected = true;
            readData();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Bluetooth connection failed", Toast.LENGTH_SHORT).show();
        }

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Perform Bluetooth-related actions here, if needed
                if (isBluetoothConnected) {
                    // For example, send data to Arduino when the button is clicked
                    // Note: Implement your own logic based on your requirements
                    // sendDataToArduino("YourDataToSend");
                }

                // Navigate to SecondFragment
                NavHostFragment.findNavController(WaterReminder.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
    }

    private void readData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;

                while (true) {
                    try {
                        bytes = inputStream.read(buffer);
                        String receivedData = new String(buffer, 0, bytes);
                        handler.obtainMessage(0, receivedData).sendToTarget();
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }).start();
    }
    private void updateUI(String receivedData) {
        // Update your TextView or other UI elements with the received data
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //max 25 - pahar gol
                //min 2 - pahar plin

                try {
                    // Trim leading and trailing spaces
                    String cleanedData = receivedData.trim();

                    // Convert the remaining string to an integer
                    int dataValue = Integer.parseInt(cleanedData);

                    // Map the range [2, 25] to [100%, 0%]

                    double percentage = 100 - (dataValue/20.0f) * 100;
                    // Set the percentage text to your TextView or handle it accordingly

                    if(percentage < 0)
                        percentage = 0;
                    if(percentage < 10 && !empty)
                    {
                        System.out.println("here");
                        empty = true;
                        totCups++;
                    }
                    if(percentage >= 85)
                    {
                        empty = false;
                    }

                    binding.currentlyInCup.setText((int)percentage + "% water");
                    System.out.println(dataValue + " " + percentage + "%");
                    binding.idTextTotalCups.setText(String.valueOf(totCups));
                } catch (NumberFormatException e) {
                    // Handle the case where cleanedData is not a valid integer
                    binding.currentlyInCup.setText("Invalid data");
                }

               // binding.currentlyInCup.setText(receivedData);

            }
        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
