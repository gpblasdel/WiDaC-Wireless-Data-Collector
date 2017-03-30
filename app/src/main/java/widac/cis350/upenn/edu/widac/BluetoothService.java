package widac.cis350.upenn.edu.widac;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by Matt on 3/29/2017.
 */

public class BluetoothService {
    private static final String TAG = "WiDaC DEBUG";
    private Handler mHandler; // handler that gets info from Bluetooth service
    Context context;

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    public BluetoothService(Context context) {
        this.context = context;
    }

    public void runService(BluetoothDevice device) {
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.run();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

            try {
                // Use the UUID of the device that discovered // TODO Maybe need extra device object
                if (mmDevice != null)
                {
                    Log.i(TAG, "Device Name: " + mmDevice.getName());
                    Log.i(TAG, "Device UUID: " + mmDevice.getUuids()[0].getUuid());
                    //tmp = device.createInsecureRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());
                    try {
                        Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                        tmp = (BluetoothSocket) m.invoke(device, 1);
                        Toast.makeText(context, "ayy", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Sucks " + e.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
                else Log.d(TAG, "Device is null.");
            } catch (NullPointerException e)
            {
                Log.d(TAG, " UUID from device is null, Using Default UUID, Device name: " + device.getName());
                try {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(DEFAULT_UUID);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            mmSocket = tmp;
        }

        public void run() {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                Toast.makeText(context, "TRYING TO CONNECT", Toast.LENGTH_SHORT).show();
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Toast.makeText(context, "UNABLE TO CONNECT", Toast.LENGTH_SHORT).show();
                Toast.makeText(context, connectException.toString(), Toast.LENGTH_LONG).show();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Toast.makeText(context, "COULD NOT CLOSE SOCKET", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Toast.makeText(context, "worked!!!", Toast.LENGTH_SHORT).show();
            ConnectedThread connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
            //this.cancel();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Toast.makeText(context, "Error occurred when creating input stream", Toast.LENGTH_SHORT).show();
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(context, "Error occurred when creating output stream", Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            Toast.makeText(context, "here 4", Toast.LENGTH_SHORT).show();
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            Toast.makeText(context, "here 5", Toast.LENGTH_SHORT).show();

            // Keep listening to the InputStream until an exception occurs.
            //while (true) {
            try {
                // Read from the InputStream.
                numBytes = mmInStream.read(mmBuffer);
                for (int i = 0; i < numBytes; i++) {
                    Toast.makeText(context, Byte.toString(mmBuffer[i]), Toast.LENGTH_SHORT).show();
                }
                //Log.d(TAG, "BYTES: " + Integer.toString(numBytes));
                // Send the obtained bytes to the UI activity.
                    //Message readMsg = mHandler.obtainMessage(
                            //MessageConstants.MESSAGE_READ, numBytes, -1,
                            //mmBuffer);
                //readMsg.sendToTarget();
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                Toast.makeText(context, "disconnected", Toast.LENGTH_SHORT).show();
                //break;
            //}
            } catch (NullPointerException e) {
                Toast.makeText(context, "input stream null " + e.toString(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "jesus " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                /*Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);*/
                //writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                /*Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);*/
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                //writeErrorMsg.setData(bundle);
                //mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}